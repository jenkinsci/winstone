/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import winstone.cmdline.CmdLineParser;
import winstone.cmdline.Option;

import javax.servlet.http.HttpUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Implements the main launcher daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Launcher.java,v 1.29 2007/04/23 02:55:35 rickknowles Exp $
 */
public class Launcher implements Runnable {
    
    static final String HTTP_LISTENER_CLASS = "winstone.HttpListener";
    static final String HTTPS_LISTENER_CLASS = "winstone.ssl.HttpsListener";
    static final String AJP_LISTENER_CLASS = "winstone.ajp13.Ajp13Listener";

    public static final byte SHUTDOWN_TYPE = (byte) '0';
    public static final byte RELOAD_TYPE = (byte) '4';
    
    private int CONTROL_TIMEOUT = 2000; // wait 2s for control connection

    private Thread controlThread;
    public final static WinstoneResourceBundle RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    private int controlPort;
    private HostGroup hostGroup;
    private ObjectPool objectPool;
    private Map args;

    public final Server server = new Server();
    
    /**
     * Constructor - initialises the web app, object pools, control port and the
     * available protocol listeners.
     */
    public Launcher(Map args) throws IOException {
        boolean success=false;
        try {
            Logger.log(Logger.MAX, RESOURCES, "Launcher.StartupArgs", args + "");

            this.args = args;
            this.controlPort =Option.CONTROL_PORT.get(args);

            // Check for java home
            List<URL> jars = new ArrayList<URL>();
            List<File> commonLibCLPaths = new ArrayList<File>();
            String defaultJavaHome = System.getProperty("java.home");
            File javaHome = Option.JAVA_HOME.get(args,new File(defaultJavaHome));
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.UsingJavaHome", javaHome.getPath());
            File toolsJar = Option.TOOLS_JAR.get(args);
            if (toolsJar == null) {
                toolsJar = new File(javaHome, "lib/tools.jar");

                // first try - if it doesn't exist, try up one dir since we might have
                // the JRE home by mistake
                if (!toolsJar.exists()) {
                    File javaHome2 = javaHome.getParentFile();
                    File toolsJar2 = new File(javaHome2, "lib/tools.jar");
                    if (toolsJar2.exists()) {
                        javaHome = javaHome2;
                        toolsJar = toolsJar2;
                    }
                }
            }

            // Add tools jar to classloader path
            if (toolsJar.exists()) {
                jars.add(toolsJar.toURL());
                commonLibCLPaths.add(toolsJar);
                Logger.log(Logger.DEBUG, RESOURCES, "Launcher.AddedCommonLibJar",
                        toolsJar.getName());
            } else if (Option.USE_JASPER.get(args))
                Logger.log(Logger.WARNING, RESOURCES, "Launcher.ToolsJarNotFound");

            // Set up common lib class loader
            File libFolder = Option.COMMON_LIB_FOLDER.get(args,new File("lib"));
            if (libFolder.exists() && libFolder.isDirectory()) {
                Logger.log(Logger.DEBUG, RESOURCES, "Launcher.UsingCommonLib",
                        libFolder.getCanonicalPath());
                File children[] = libFolder.listFiles();
                for (File aChildren : children)
                    if (aChildren.getName().endsWith(".jar")
                            || aChildren.getName().endsWith(".zip")) {
                        jars.add(aChildren.toURL());
                        commonLibCLPaths.add(aChildren);
                        Logger.log(Logger.DEBUG, RESOURCES, "Launcher.AddedCommonLibJar",
                                aChildren.getName());
                    }
            } else {
                Logger.log(Logger.DEBUG, RESOURCES, "Launcher.NoCommonLib");
            }
            ClassLoader commonLibCL = new URLClassLoader(jars.toArray(new URL[jars.size()]),
                    getClass().getClassLoader());

            Logger.log(Logger.MAX, RESOURCES, "Launcher.CLClassLoader",
                    commonLibCL.toString());
            Logger.log(Logger.MAX, RESOURCES, "Launcher.CLClassLoader",
                    commonLibCLPaths.toString());

            this.objectPool = new ObjectPool(args);

            // Open the web apps
            this.hostGroup = new HostGroup(server, this.objectPool, commonLibCL,
                    commonLibCLPaths.toArray(new File[0]), args);

            // Create connectors (http, https and ajp)
            spawnListener(HTTP_LISTENER_CLASS);
            spawnListener(AJP_LISTENER_CLASS);
            spawnListener(HTTPS_LISTENER_CLASS);

            server.setThreadPool(new ExecutorThreadPool(objectPool.getRequestHandler()));

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

    /**
     * Instantiates listeners. Note that an exception thrown in the 
     * constructor is interpreted as the listener being disabled, so 
     * don't do anything too adventurous in the constructor, or if you do, 
     * catch and log any errors locally before rethrowing.
     */
    protected void spawnListener(String listenerClassName) throws IOException {
        try {
            Class listenerClass = Class.forName(listenerClassName);
            Constructor listenerConstructor = listenerClass.getConstructor(Map.class);
            Listener listener = (Listener) listenerConstructor.newInstance(args);
            listener. start(server);
        } catch (Throwable err) {
            throw (IOException)new IOException("Failed to start a listener: "+listenerClassName).initCause(err);
        }
    }

    /**
     * The main run method. This handles the normal thread processing.
     */
    public void run() {
        boolean interrupted = false;
        try {
            ServerSocket controlSocket = null;

            if (this.controlPort > 0) {
                controlSocket = new ServerSocket(this.controlPort);
                controlSocket.setSoTimeout(CONTROL_TIMEOUT);
            }

            Logger.log(Logger.INFO, RESOURCES, "Launcher.StartupOK",
                    RESOURCES.getString("ServerVersion"),
                    (this.controlPort > 0 ? "" + this.controlPort
                            : RESOURCES.getString("Launcher.ControlDisabled")));

            // Enter the main loop
            while (!interrupted) {
//                this.objectPool.removeUnusedRequestHandlers();
//                this.hostGroup.invalidateExpiredSessions();

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
                    Logger.log(Logger.ERROR, RESOURCES,
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
            Logger.log(Logger.ERROR, RESOURCES, "Launcher.ShutdownError", err);
        }
        Logger.log(Logger.INFO, RESOURCES, "Launcher.ControlThreadShutdownOK");
    }

    protected void handleControlRequest(Socket csAccepted) throws IOException {
        InputStream inSocket = null;
        OutputStream outSocket = null;
        ObjectInputStream inControl = null;
        try {
            inSocket = csAccepted.getInputStream();
            int reqType = inSocket.read();
            if ((byte) reqType == SHUTDOWN_TYPE) {
                Logger.log(Logger.INFO, RESOURCES,
                        "Launcher.ShutdownRequestReceived");
                shutdown();
            } else if ((byte) reqType == RELOAD_TYPE) {
                inControl = new ObjectInputStream(inSocket);
                String host = inControl.readUTF();
                String prefix = inControl.readUTF();
                Logger.log(Logger.INFO, RESOURCES, "Launcher.ReloadRequestReceived", host + prefix);
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
            if (outSocket != null) {
                try {outSocket.close();} catch (IOException err) {}
            }
        }
    }
    
    public void shutdown() {
        // Release all listeners/pools/webapps
        this.objectPool.destroy();

        if (this.controlThread != null) {
            this.controlThread.interrupt();
        }
        Thread.yield();

        Logger.log(Logger.INFO, RESOURCES, "Launcher.ShutdownOK");
    }

    public boolean isRunning() {
        return (this.controlThread != null) && this.controlThread.isAlive();
    }
    
    /**
     * Main method. This basically just accepts a few args, then initialises the
     * listener thread. For now, just shut it down with a control-C.
     */
    public static void main(String argv[]) throws IOException {
        Map args = getArgsFromCommandLine(argv);
        
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


        int maxParameterCount = Option.MAX_PARAM_COUNT.get(args);
        if (maxParameterCount>0) {
            HttpUtils.MAX_PARAMETER_COUNT = maxParameterCount;
        }
        
        // Launch
        try {
            new Launcher(args);
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, RESOURCES, "Launcher.ContainerStartupError", err);
        }
    }
    
    public static Map getArgsFromCommandLine(String argv[]) throws IOException {
        Map args = new CmdLineParser(Option.all(Option.class)).parse(argv,"nonSwitch");
        
        // Small hack to allow re-use of the command line parsing inside the control tool
        String firstNonSwitchArgument = (String) args.get("nonSwitch");
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

    protected static void deployEmbeddedWarfile(Map args) throws IOException {
        String embeddedWarfileName = RESOURCES.getString("Launcher.EmbeddedWarFile");
        InputStream embeddedWarfile = Launcher.class.getResourceAsStream(
                embeddedWarfileName);
        if (embeddedWarfile != null) {
            File tempWarfile = File.createTempFile("embedded", ".war").getAbsoluteFile();
            tempWarfile.getParentFile().mkdirs();
            tempWarfile.deleteOnExit();

            String embeddedWebroot = RESOURCES.getString("Launcher.EmbeddedWebroot");
            File tempWebroot = new File(tempWarfile.getParentFile(), embeddedWebroot);
            tempWebroot.mkdirs();
            
            Logger.log(Logger.DEBUG, RESOURCES, "Launcher.CopyingEmbeddedWarfile",
                    tempWarfile.getAbsolutePath());
            OutputStream out = new FileOutputStream(tempWarfile, true);
            int read;
            byte buffer[] = new byte[2048];
            while ((read = embeddedWarfile.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.close();
            embeddedWarfile.close();
            
            Option.WARFILE.put(args, tempWarfile.getAbsolutePath());
            Option.WARFILE.put(args, tempWebroot.getAbsolutePath());
            Option.WEBAPPS_DIR.remove(args);
        }
    }
    
    public static void initLogger(Map args) throws IOException {
        // Reset the log level
        int logLevel = WebAppConfiguration.intArg(args, Option.DEBUG.name, Logger.INFO.intValue());
        boolean showThrowingLineNo = Option.LOG_THROWING_LINE_NO.get(args);
        boolean showThrowingThread = Option.LOG_THROWING_THREAD.get(args);
        OutputStream logStream;
        if (args.get("logfile") != null) {
            logStream = new FileOutputStream((String) args.get("logfile"));
        } else if (WebAppConfiguration.booleanArg(args, "logToStdErr", false)) {
            logStream = System.err;
        } else {
            logStream = System.out;
        }
//        Logger.init(logLevel, logStream, showThrowingLineNo, showThrowingThread);
        Logger.init(Level.parse(String.valueOf(logLevel)), logStream, showThrowingThread);
    }

    protected static void printUsage() {
        // if the caller overrides the usage, use that instead.
        String usage = USAGE;
        if(usage==null)
            usage = RESOURCES.getString("Launcher.UsageInstructions",
                RESOURCES.getString("ServerVersion"));
        System.out.println(usage);
    }

    /**
     * Overridable usage screen
     */
    public static String USAGE;
}
