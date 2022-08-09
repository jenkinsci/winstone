/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import winstone.cmdline.Option;

import javax.servlet.SessionTrackingMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

/**
 * Manages the references to individual webapps within the container. This object handles
 * the mapping of url-prefixes to webapps, and init and shutdown of any webapps it manages.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostConfiguration.java,v 1.8 2007/08/02 06:16:00 rickknowles Exp $
 */
public class HostConfiguration {
    private final Server server;
    private String hostname;
    private Map<String, String> args;
    private Map<String,WebAppContext> webapps;
    private ClassLoader commonLibCL;
    private File[] commonLibCLPaths;
    private MimeTypes mimeTypes = new MimeTypes();
    private final LoginService loginService;

    public HostConfiguration(Server server, String hostname, ClassLoader commonLibCL,
                             File[] commonLibCLPaths, Map<String, String> args, File webappsDir) throws IOException {
        this.server = server;
        this.hostname = hostname;
        this.args = args;
        this.webapps = new Hashtable<>();
        this.commonLibCL = commonLibCL;
        this.commonLibCLPaths = commonLibCLPaths;

        try {
            // Build the realm
            Class<? extends LoginService> realmClass = Option.REALM_CLASS_NAME.get(args, LoginService.class, commonLibCL);
            Constructor<? extends LoginService> realmConstr = realmClass.getConstructor(Map.class);
            loginService = (LoginService) realmConstr.newInstance(args);
        } catch (Throwable err) {
            throw new IOException("Failed to setup authentication realm", err);
        }

        // Is this the single or multiple configuration ? Check args
        File warfile = Option.WARFILE.get(args);
        File webroot = Option.WEBROOT.get(args);

        Handler handler;
        // If single-webapp mode
        if (webappsDir == null && ((warfile != null) || (webroot != null))) {
            String prefix = Option.PREFIX.get(args);
            if (prefix.endsWith("/"))   // trim off the trailing '/' that Jetty doesn't like
                prefix = prefix.substring(0,prefix.length()-1);
            handler = configureAccessLog(create(getWebRoot(webroot,warfile), prefix),"webapp");
        }
        // Otherwise multi-webapp mode
        else {
            handler = initMultiWebappDir(webappsDir);
        }

        {// load additional mime types
            loadBuiltinMimeTypes();
            String types = Option.MIME_TYPES.get(args);
            if (types!=null) {
                StringTokenizer mappingST = new StringTokenizer(types, ":", false);
                while (mappingST.hasMoreTokens()) {
                    String mapping = mappingST.nextToken();
                    int delimPos = mapping.indexOf('=');
                    if (delimPos == -1)
                        continue;
                    String extension = mapping.substring(0, delimPos);
                    String mimeType = mapping.substring(delimPos + 1);
                    this.mimeTypes.addMimeMapping(extension.toLowerCase(), mimeType);
                }
            }
        }

        server.setHandler(handler);
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.InitComplete",
                this.webapps.size() + "", this.webapps.keySet() + "");
    }

    private void loadBuiltinMimeTypes() {
        try (InputStream in = getClass().getResourceAsStream("mime.properties")) {
            Properties props = new Properties();
            props.load(in);
            for (Entry<Object, Object> e : props.entrySet()) {
                mimeTypes.addMimeMapping(e.getKey().toString(),e.getValue().toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load the built-in MIME types", e);
        }
    }


    /**
     * @param webAppName
     *      Unique name given to the access logger.
     */
    private Handler configureAccessLog(Handler handler, String webAppName) {
        try {
            Class<? extends RequestLog> loggerClass = Option.ACCESS_LOGGER_CLASSNAME.get(args, RequestLog.class, commonLibCL);
            if (loggerClass!=null) {
                // Build the realm
                Constructor<? extends RequestLog> loggerConstr = loggerClass.getConstructor(String.class, Map.class);
                RequestLogHandler rlh = new RequestLogHandler();
                rlh.setHandler(handler);
                rlh.setRequestLog((RequestLog) loggerConstr.newInstance(webAppName, args));
                return rlh;
            } else {
                Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WebAppConfig.LoggerDisabled");
            }
        } catch (Throwable err) {
            Logger.log(Logger.ERROR, Launcher.RESOURCES,
                    "WebAppConfig.LoggerError", "", err);
        }
        return handler;
    }

    private WebAppContext create(File app, String prefix) {
        WebAppContext wac = new WebAppContext(app.getAbsolutePath(),prefix) {
            @Override
            public void preConfigure() throws Exception {
                // to have WebAppClassLoader inherit from commonLibCL,
                // we need to set it as the context classloader.
                Thread t = Thread.currentThread();
                ClassLoader ccl = t.getContextClassLoader();
                t.setContextClassLoader(commonLibCL);
                try {
                    super.preConfigure();
                } finally {
                    t.setContextClassLoader(ccl);
                }
                int maxParameterCount = Option.MAX_PARAM_COUNT.get(args);
                if (maxParameterCount > 0) {
                    setMaxFormKeys(maxParameterCount);
                }
                setMaxFormContentSize(Option.REQUEST_FORM_CONTENT_SIZE.get(args));
            }

            @Override
            public void postConfigure() throws Exception {
                super.postConfigure();

                // if specified, override the value in web.xml
                int sessionTimeout = Option.SESSION_TIMEOUT.get(args);
                if (sessionTimeout>0) {
                    getSessionHandler().setMaxInactiveInterval(sessionTimeout * 60);
                }
                int sessionEviction = Option.SESSION_EVICTION.get(args);
                getSessionHandler().getSessionCache().setEvictionPolicy( sessionEviction );
            }
        };
        JettyWebSocketServletContainerInitializer.configure(wac, null);
        wac.getSecurityHandler().setLoginService(loginService);
        wac.setThrowUnavailableOnStartupException(true);    // if boot fails, abort the process instead of letting empty Jetty run
        wac.setMimeTypes(mimeTypes);
        wac.getSessionHandler().setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
        wac.getSessionHandler().setSessionCookie(WinstoneSession.SESSION_COOKIE_NAME);
        this.webapps.put(wac.getContextPath(),wac);
        return wac;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void reloadWebApp(String prefix) {
        WebAppContext webApp = this.webapps.get(prefix);
        if (webApp != null) {
            try {
                webApp.stop();
                webApp.start();
            } catch (Exception e) {
                throw new WinstoneException("Failed to redeploy "+prefix,e);
            }
        } else {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.PrefixUnknown", prefix));
        }
    }

    /**
     * Setup the webroot. If a warfile is supplied, extract any files that the
     * war file is newer than. If none is supplied, use the default temp
     * directory.
     */
    protected File getWebRoot(File requestedWebroot, File warfile) throws IOException {
        if (warfile != null) {
            Logger.log(Logger.INFO, Launcher.RESOURCES,
                    "HostConfig.BeginningWarExtraction");

            // open the war file
            if (!warfile.exists() || !warfile.isFile())
                throw new WinstoneException(Launcher.RESOURCES.getString(
                        "HostConfig.WarFileInvalid", warfile));

            // Get the webroot folder (or a temp dir if none supplied)
            File unzippedDir;
            if (requestedWebroot != null) {
                unzippedDir = requestedWebroot;
            } else {
                File tempFile = File.createTempFile("dummy", "dummy");
                String userName = System.getProperty("user.name");
                unzippedDir = new File(tempFile.getParent(),
                        (userName != null ? WinstoneResourceBundle.globalReplace(userName,
                                new String[][] {{"/", ""}, {"\\", ""}, {",", ""}}) + "/" : "") +
                        "winstone/" + warfile.getName());

                try {
                    Files.delete(tempFile.toPath());
                } catch (Exception ex) {
                    Logger.logDirectMessage(Logger.WARNING, null, "Failed To delete dummy file", ex);
                }
            }
            if (unzippedDir.exists()) {
                if (!unzippedDir.isDirectory()) {
                    throw new WinstoneException(Launcher.RESOURCES.getString(
                            "HostConfig.WebRootNotDirectory", unzippedDir.getPath()));
                } else {
                    Logger.log(Logger.DEBUG, Launcher.RESOURCES,
                            "HostConfig.WebRootExists", unzippedDir.getCanonicalPath());
                }
            }

            // check consistency and if out-of-sync, recreate
            File timestampFile = new File(unzippedDir,".timestamp");
            if(!timestampFile.exists() || Math.abs(timestampFile.lastModified()- warfile.lastModified())>1000) {
                // contents of the target directory is inconsistent from the war.
                deleteRecursive(unzippedDir);
                try {
                    Files.createDirectories(unzippedDir.toPath());
                } catch (Exception ex) {
                    Logger.logDirectMessage(Logger.WARNING, null, "Failed to recreate dirs " + unzippedDir.getAbsolutePath(), ex);
                }
            } else {
                // files are up to date
                return unzippedDir;
            }

            // Iterate through the files
            byte[] buffer = new byte[8192];
            try (JarFile warArchive = new JarFile(warfile)) {
                for (Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements();) {
                    JarEntry element = e.nextElement();
                    if (element.isDirectory()) {
                        continue;
                    }
                    String elemName = element.getName();

                    // If archive date is newer than unzipped file, overwrite
                    File outFile = new File(unzippedDir, elemName);

                    // Fix Zip Slip vulnerability
                    if (!outFile.toPath().normalize().startsWith(unzippedDir.toPath().normalize())) {
                        throw new IOException("Bad zip entry");
                    }

                    if (outFile.exists() && (outFile.lastModified() > warfile.lastModified())) {
                        continue;
                    }
                    try {
                        Files.createDirectories(outFile.toPath().getParent());
                    } catch (IOException | InvalidPathException | SecurityException ex) {
                        Logger.logDirectMessage(Logger.WARNING, null, "Failed to create dirs " + outFile.getParentFile().getAbsolutePath(), null);
                    }

                    // Copy out the extracted file
                    try (InputStream inContent = warArchive.getInputStream(element);
                            OutputStream outStream = new FileOutputStream(outFile)) {
                        int readBytes = inContent.read( buffer );
                        while ( readBytes != -1 ) {
                            outStream.write( buffer, 0, readBytes );
                            readBytes = inContent.read( buffer );
                        }
                    }
                }
            }

            // extraction completed
            new FileOutputStream(timestampFile).close();
            if(!timestampFile.setLastModified(warfile.lastModified())) {
                Logger.logDirectMessage(Logger.WARNING, null, "Failed to set timestamp " + timestampFile.getAbsolutePath(), null);
            }

            // Return webroot
            return unzippedDir;
        } else {
            return requestedWebroot;
        }
    }

    private void deleteRecursive(File dir) {
        File[] children = dir.listFiles();
        if(children!=null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        try {
            Files.deleteIfExists(dir.toPath());
        } catch (Exception ex) {
            Logger.logDirectMessage(Logger.WARNING, null, "Failed to delete dirs " + dir.getAbsolutePath(), ex);
        }
    }

    protected ContextHandlerCollection initMultiWebappDir(File webappsDir) {
        ContextHandlerCollection webApps = new ContextHandlerCollection();

        if (webappsDir == null) {
            webappsDir = new File("webapps");
        }
        if (!webappsDir.exists()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WebAppDirNotFound", webappsDir.getPath()));
        } else if (!webappsDir.isDirectory()) {
            throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WebAppDirIsNotDirectory", webappsDir.getPath()));
        } else {
            File[] children = webappsDir.listFiles();
            if (children != null) {
                for (File aChildren : children) {
                    String childName = aChildren.getName();

                    // Check any directories for warfiles that match, and skip: only deploy the war file
                    if (aChildren.isDirectory()) {
                        File matchingWarFile = new File(webappsDir, aChildren.getName() + ".war");
                        if (matchingWarFile.exists() && matchingWarFile.isFile()) {
                            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostConfig.SkippingWarfileDir", childName);
                        } else {
                            String prefix = childName.equalsIgnoreCase("ROOT") ? "" : "/" + childName;
                            if (!this.webapps.containsKey(prefix)) {
                                try {
                                    WebAppContext context = create(aChildren, prefix);
                                    webApps.addHandler(configureAccessLog(context,childName));
                                    Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.DeployingWebapp", childName);
                                } catch (Throwable err) {
                                    Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
                                }
                            }
                        }
                    } else if (childName.endsWith(".war")) {
                        String outputName = childName.substring(0, childName.lastIndexOf(".war"));
                        String prefix = outputName.equalsIgnoreCase("ROOT") ? "" : "/" + outputName;

                        if (!this.webapps.containsKey(prefix)) {
                            File outputDir = new File(webappsDir, outputName);
                            try {
                                Files.createDirectories(outputDir.toPath());
                            } catch (Exception ex) {
                                Logger.logDirectMessage(Logger.WARNING, null, "Failed to mkdirs " + outputDir.getAbsolutePath(), ex);
                            }
                            try {
                                WebAppContext context = create(
                                        getWebRoot(new File(webappsDir, outputName), aChildren), prefix);
                                webApps.addHandler(configureAccessLog(context,outputName));
                                Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.DeployingWebapp", childName);
                            } catch (Throwable err) {
                                Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
                            }
                        }
                    }
                }
            }
        }

        return webApps;
    }
}
