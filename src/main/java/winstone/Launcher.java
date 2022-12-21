/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.lib.support_log_formatter.SupportLogFormatter;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import winstone.cmdline.CmdLineParser;
import winstone.cmdline.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * Implements the main launcher daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Launcher.java,v 1.29 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Launcher implements Runnable {

    static final String HTTP_LISTENER_CLASS = "winstone.HttpConnectorFactory";
    static final String HTTPS_LISTENER_CLASS = "winstone.HttpsConnectorFactory";
    static final String HTTP2_LISTENER_CLASS = "winstone.Http2ConnectorFactory";

    public static final byte SHUTDOWN_TYPE = (byte) '0';
    public static final byte RELOAD_TYPE = (byte) '4';
    public static final String WINSTONE_PORT_FILE_NAME_PROPERTY = "winstone.portFileName";

    private int CONTROL_TIMEOUT = 2000; // wait 2s for control connection

    private Thread controlThread;
    public final static WinstoneResourceBundle RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    private int controlPort;
    private HostGroup hostGroup;
    private Map<String, String> args;

    public final Server server;

    /**
     * Constructor - initialises the web app, object pools, control port and the
     * available protocol listeners.
     */
    @SuppressFBWarnings(value ="DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED", justification = "cf. https://github.com/spotbugs/spotbugs/issues/1515")
    public Launcher(Map<String, String> args) throws IOException {
        boolean success=false;
        try {
            Logger.log(Level.ALL, RESOURCES, "Launcher.StartupArgs", args + "");

            this.args = args;
            this.controlPort =Option.CONTROL_PORT.get(args);

            // Check for java home
            List<URL> jars = new ArrayList<>();
            String defaultJavaHome = System.getProperty("java.home");
            File javaHome = Option.JAVA_HOME.get(args,new File(defaultJavaHome));
            Logger.log(Level.FINER, RESOURCES, "Launcher.UsingJavaHome", javaHome.getPath());

            // Set up common lib class loader
            File libFolder = Option.COMMON_LIB_FOLDER.get(args,new File("lib"));
            if (libFolder.exists() && libFolder.isDirectory()) {
                Logger.log(Level.FINER, RESOURCES, "Launcher.UsingCommonLib",
                        libFolder.getCanonicalPath());
                File[] children = libFolder.listFiles();
                if (children != null)
                    for (File aChildren : children)
                        if (aChildren.getName().endsWith(".jar")
                                || aChildren.getName().endsWith(".zip")) {
                            jars.add(aChildren.toURI().toURL());
                            Logger.log(Level.FINER, RESOURCES, "Launcher.AddedCommonLibJar",
                                    aChildren.getName());
                        }
            } else {
                Logger.log(Level.FINER, RESOURCES, "Launcher.NoCommonLib");
            }

            File extraLibFolder = Option.EXTRA_LIB_FOLDER.get(args);
            List<URL> extraJars = new ArrayList<>();
            if(extraLibFolder != null && extraLibFolder.exists()){
                Logger.log(Level.WARNING, RESOURCES, "Launcher.ExtraLibFolder");
                File[] children = extraLibFolder.listFiles();
                if (children != null)
                    for (File aChildren : children)
                        if (aChildren.getName().endsWith(".jar")
                            || aChildren.getName().endsWith(".zip")) {
                            extraJars.add(aChildren.toURI().toURL());
                        }
            }

            ClassLoader commonLibCL = new URLClassLoader(jars.toArray(new URL[0]),
                    getClass().getClassLoader());

            Logger.log(Level.ALL, RESOURCES, "Launcher.CLClassLoader",
                    commonLibCL.toString());


            if(!extraJars.isEmpty()){
                ClassLoader extraClassLoader = new URLClassLoader(extraJars.toArray(new URL[0]),
                                                             getClass().getClassLoader());
                Thread.currentThread().setContextClassLoader( extraClassLoader );
            }

            int qtpMaxThread = Option.QTP_MAXTHREADS.get(args);
            QueuedThreadPool queuedThreadPool = qtpMaxThread>0?new QueuedThreadPool(qtpMaxThread):new QueuedThreadPool();
            queuedThreadPool.setName("Jetty (winstone)");
            this.server = new Server(queuedThreadPool);

            // add LowResourceMonitor
            LowResourceMonitor lowResourceMonitor = new LowResourceMonitor(this.server);
            lowResourceMonitor.setMonitorThreads(true);
            this.server.addBean(lowResourceMonitor);


            // Open the web apps
            this.hostGroup = new HostGroup(server, commonLibCL, args);

            List<Connector> connectors = new ArrayList<>();
            // Create connectors (http & https)
            spawnListener(HTTP_LISTENER_CLASS, connectors);
            spawnListener(HTTPS_LISTENER_CLASS, connectors);
            spawnListener(HTTP2_LISTENER_CLASS, connectors);

            lowResourceMonitor.setMonitoredConnectors(connectors);

            if(Option.USE_JMX.get(args)) {
                // Setup JMX if needed
                MBeanContainer mbeanContainer = new MBeanContainer( ManagementFactory.getPlatformMBeanServer());
                server.addBean(mbeanContainer);
            }

            try {
                server.start();
                writePortToFileIfNeeded();

            } catch (Exception e) {
                throw new IOException("Failed to start Jetty",e);
            }

            this.controlThread = new Thread(this, RESOURCES.getString(
                    "Launcher.ThreadName", "" + this.controlPort));
            this.controlThread.setDaemon(false);
            this.controlThread.start();

            success = true;
        } finally {
            if (!success)
                shutdown();
        }

        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "Not applicable, this is called from command line")
    private void writePortToFileIfNeeded() throws IOException {
        String portFileName = System.getProperty(WINSTONE_PORT_FILE_NAME_PROPERTY);
        if (portFileName != null) {
            Connector[] connectors = server.getConnectors();
            if (connectors.length > 0) {
                Connector connector = connectors[0];
                if (connector instanceof ServerConnector) {
                    int port = ((ServerConnector) connector).getLocalPort();
                    Path portFile = Paths.get(portFileName);
                    Path portDir = portFile.getParent();
                    if (portDir == null) {
                        throw new IllegalArgumentException("Given port file name doesn't have a parent: " + portFileName);
                    }
                    Files.createDirectories(portDir);
                    Path fileName = portFile.getFileName();
                    if (fileName == null) {
                        // Should never happen, but spotbugs
                        throw new IllegalArgumentException("Given port file name doesn't have a name: " + portFileName);
                    }
                    Path tmpPath = Files.createTempFile(portDir, fileName.toString(), null);
                    Files.writeString(tmpPath, Integer.toString(port), StandardCharsets.UTF_8);
                    try {
                        Files.move(tmpPath, portFile, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Logger.logDirectMessage(Level.WARNING, null, "Atomic move not supported. Falling back to non-atomic move.", e);
                        try {
                            Files.move(tmpPath, portFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e2) {
                            e2.addSuppressed(e);
                            throw e2;
                        }
                    }
                } else {
                    throw new IllegalStateException("Only ServerConnector is supported");
                }
            } else {
                throw new IllegalStateException("No connectors found");
            }
        }
    }

    /**
     * Instantiates listeners. Note that an exception thrown in the
     * constructor is interpreted as the listener being disabled, so
     * don't do anything too adventurous in the constructor, or if you do,
     * catch and log any errors locally before rethrowing.
     */
    protected Connector spawnListener(String listenerClassName, List<Connector> connectors) throws IOException {
        try {
            ConnectorFactory connectorFactory = (ConnectorFactory) Class.forName(listenerClassName)
                    .getDeclaredConstructor().newInstance();
            Connector connector = connectorFactory.start(args, server);
            if(connector!=null){
                connectors.add(connector);
            }
            return connector;
        } catch (Throwable err) {
            throw new IOException("Failed to start a listener: "+listenerClassName, err);
        }
    }

    /**
     * The main run method. This handles the normal thread processing.
     */
    @Override
    public void run() {
        boolean interrupted = false;
        try {
            ServerSocket controlSocket = null;

            if (this.controlPort > 0) {
                controlSocket = new ServerSocket(this.controlPort);
                controlSocket.setSoTimeout(CONTROL_TIMEOUT);
            }

            Logger.log(Level.INFO, RESOURCES, "Launcher.StartupOK",
                    RESOURCES.getString("ServerVersion"),
                    (this.controlPort > 0 ? "" + this.controlPort
                            : RESOURCES.getString("Launcher.ControlDisabled")));

            // Enter the main loop
            while (!interrupted) {

                // Check for control request
                Socket accepted = null;
                try {
                    if (controlSocket != null) {
                        accepted = controlSocket.accept();
                        if (accepted != null) {
                            handleControlRequest(accepted);
                        }
                    } else {
                        Thread.sleep(CONTROL_TIMEOUT);
                    }
                } catch (InterruptedIOException err) {
                } catch (InterruptedException err) {
                    interrupted = true;
                } catch (Throwable err) {
                    Logger.log(Level.SEVERE, RESOURCES,
                            "Launcher.ShutdownError", err);
                } finally {
                    if (accepted != null) {
                        try {accepted.close();} catch (IOException err) {}
                    }
                    if (Thread.interrupted()) {
                        interrupted = true;
                    }
                }
            }

            // Close server socket
            if (controlSocket != null) {
                controlSocket.close();
            }
        } catch (Throwable err) {
            Logger.log(Level.SEVERE, RESOURCES, "Launcher.ShutdownError", err);
        }
        Logger.log(Level.INFO, RESOURCES, "Launcher.ControlThreadShutdownOK");
    }

    protected void handleControlRequest(Socket csAccepted) throws IOException {
        InputStream inSocket = null;
        ObjectInputStream inControl = null;
        try {
            inSocket = csAccepted.getInputStream();
            int reqType = inSocket.read();
            if ((byte) reqType == SHUTDOWN_TYPE) {
                Logger.log(Level.INFO, RESOURCES,
                        "Launcher.ShutdownRequestReceived");
                shutdown();
            } else if ((byte) reqType == RELOAD_TYPE) {
                inControl = new ObjectInputStream(inSocket);
                String host = inControl.readUTF();
                String prefix = inControl.readUTF();
                Logger.log(Level.INFO, RESOURCES, "Launcher.ReloadRequestReceived", host + prefix);
                HostConfiguration hostConfig = this.hostGroup.getHostByName(host);
                hostConfig.reloadWebApp(prefix);
            }
        } finally {
            if (inControl != null) {
                try {inControl.close();} catch (IOException err) {}
            }
            if (inSocket != null) {
                try {inSocket.close();} catch (IOException err) {}
            }
        }
    }

    public void shutdown() {
        try {
            server.stop();
        } catch (Exception e) {
            Logger.log(Level.INFO, RESOURCES, "Launcher.FailedShutdown", e);
        }

        if (this.controlThread != null) {
            this.controlThread.interrupt();
        }
        Thread.yield();

        Logger.log(Level.INFO, RESOURCES, "Launcher.ShutdownOK");
    }

    public boolean isRunning() {
        return (this.controlThread != null) && this.controlThread.isAlive();
    }

    /**
     * Main method. This basically just accepts a few args, then initialises the
     * listener thread. For now, just shut it down with a control-C.
     */
    public static void main(String[] argv) throws IOException {
        Map<String, String> args = getArgsFromCommandLine(argv);

        if (System.getProperty("java.util.logging.config.file") == null) {
          for (Handler h : java.util.logging.Logger.getLogger("").getHandlers()) {
              if (h instanceof ConsoleHandler) {
                  ((ConsoleHandler) h).setFormatter(new SupportLogFormatter());
              }
          }
        }

        if (Option.USAGE.isIn(args) || Option.HELP.isIn(args)) {
            printUsage();
            return;
        }

        // Check for embedded war
        deployEmbeddedWarfile(args);

        // Check for embedded warfile
        if (!Option.WEBROOT.isIn(args) && !Option.WARFILE.isIn(args)
         && !Option.WEBAPPS_DIR.isIn(args)) {
            printUsage();
            return;
        }


        // Launch
        try {
            new Launcher(args);
        } catch (Throwable err) {
            err.printStackTrace();
            Logger.log(Level.SEVERE, RESOURCES, "Launcher.ContainerStartupError", err);
            System.exit(1);
        }
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "TODO needs triage")
    public static Map<String, String> getArgsFromCommandLine(String[] argv) throws IOException {
        Map<String, String> args = new CmdLineParser(Option.all(Option.class)).parse(argv,"nonSwitch");

        // Small hack to allow re-use of the command line parsing inside the control tool
        String firstNonSwitchArgument = args.get("nonSwitch");
        args.remove("nonSwitch");

        // Check if the non-switch arg is a file or folder, and overwrite the config
        if (firstNonSwitchArgument != null) {
            File webapp = new File(firstNonSwitchArgument);
            if (webapp.exists()) {
                if (webapp.isDirectory()) {
                    args.put(Option.WEBROOT.name, firstNonSwitchArgument);
                } else if (webapp.isFile()) {
                    args.put(Option.WARFILE.name, firstNonSwitchArgument);
                }
            }
        }
        return args;
    }

    @SuppressFBWarnings(value = { "NP_LOAD_OF_KNOWN_NULL_VALUE", "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE" }, justification = "false positive https://github.com/spotbugs/spotbugs/issues/1338")
    protected static void deployEmbeddedWarfile(Map<String, String> args) throws IOException {
        String embeddedWarfileName = RESOURCES.getString("Launcher.EmbeddedWarFile");
        try (InputStream embeddedWarfile = Launcher.class.getResourceAsStream(
                embeddedWarfileName)) {
            if (embeddedWarfile == null) {
                return;
            }
            File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
            File parentTempWarFile = tempWarfile.getParentFile();
            try {
                Files.createDirectories(parentTempWarFile.toPath());
            } catch (Exception ex) {
                Logger.logDirectMessage(Level.WARNING, null, "Failed to mkdirs " + parentTempWarFile.getAbsolutePath(), ex);
            }
            tempWarfile.deleteOnExit();

            String embeddedWebroot = RESOURCES.getString("Launcher.EmbeddedWebroot");
            File tempWebroot = new File(tempWarfile.getParentFile(), embeddedWebroot);
            try {
                Files.createDirectories(tempWebroot.toPath());
            } catch (Exception ex) {
                Logger.logDirectMessage(Level.WARNING, null, "Failed to mkdirs " + tempWebroot.getAbsolutePath(), ex);
            }

            Logger.log(Level.FINER, RESOURCES, "Launcher.CopyingEmbeddedWarfile",
                    tempWarfile.getAbsolutePath());
            try (OutputStream out = new FileOutputStream(tempWarfile, true)) {
                // TODO use Files#copy(InputStream,Path,CopyOption...)
                int read;
                byte[] buffer = new byte[2048];
                while ((read = embeddedWarfile.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            Option.WARFILE.put(args, tempWarfile.getAbsolutePath());
            Option.WARFILE.put(args, tempWebroot.getAbsolutePath());
            Option.WEBAPPS_DIR.remove(args);
        }
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive, args come from command line")
    public static void initLogger(Map<String, String> args) throws IOException {
        // Reset the log level
        int logLevel = Option.DEBUG.get(args);
        // boolean showThrowingLineNo = Option.LOG_THROWING_LINE_NO.get(args);
        boolean showThrowingThread = Option.LOG_THROWING_THREAD.get(args);
        if (args.get("logfile") != null) {
            Path logPath;
            try {
                logPath = Paths.get(args.get("logfile"));
            } catch (InvalidPathException e) {
                throw new IOException(e);
            }
            OutputStream outputStream = Files.newOutputStream(logPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            // TODO: Ideally, should change this to UTF-8, but this could cause problems for Windows users when appending to existing logs.
            PrintStream printStream = new PrintStream(outputStream, false, Charset.defaultCharset());
            System.setOut(printStream);
            System.setErr(printStream);
        }
        Logger.init(Level.parse(String.valueOf(logLevel)), showThrowingThread);
    }

    protected static void printUsage() {
        // if the caller overrides the usage, use that instead.
        String usage = USAGE;

        String header = RESOURCES.getString("Launcher.UsageInstructions.Header",
                RESOURCES.getString("ServerVersion"));
        String options = RESOURCES.getString("Launcher.UsageInstructions.Options");
        String footer = RESOURCES.getString("Launcher.UsageInstructions.Options");

        if(usage==null) {
            usage = header+options+footer;
        } else {
            usage = usage.replace("{HEADER}",header).replace("{OPTIONS}",options).replace("{FOOTER}",footer);
        }
        System.out.println(usage);
    }

    /**
     * Overridable usage screen
     */
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "Intentionally overridable")
    public static String USAGE;
}
