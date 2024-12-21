/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.SessionTrackingMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import winstone.cmdline.CompressionScheme;
import winstone.cmdline.Option;

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
    private Map<String, WebAppContext> webapps;
    private ClassLoader commonLibCL;
    private final LoginService loginService;

    public HostConfiguration(Server server, String hostname, ClassLoader commonLibCL, @NonNull Map<String, String> args)
            throws IOException {
        this.server = server;
        this.hostname = hostname;
        this.args = new HashMap<>(args);
        this.webapps = new Hashtable<>();
        this.commonLibCL = commonLibCL;

        try {
            // Build the realm
            Class<? extends LoginService> realmClass =
                    Option.REALM_CLASS_NAME.get(this.args, LoginService.class, commonLibCL);
            Constructor<? extends LoginService> realmConstr = realmClass.getConstructor(Map.class);
            loginService = realmConstr.newInstance(this.args);
        } catch (Throwable err) {
            throw new IOException("Failed to setup authentication realm", err);
        }

        // Is this the single or multiple configuration ? Check args
        File warfile = Option.WARFILE.get(this.args);
        File webroot = Option.WEBROOT.get(this.args);

        String prefix = Option.PREFIX.get(this.args);
        if (prefix.endsWith("/")) {
            // trim off the trailing '/' that Jetty doesn't like
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        WebAppContext webAppContext = create(getWebRoot(webroot, warfile), prefix);
        RequestLog requestLog = configureAccessLog("webapp");
        if (requestLog != null) {
            server.setRequestLog(requestLog);
        }

        { // load additional mime types
            loadBuiltinMimeTypes();
            String types = Option.MIME_TYPES.get(this.args);
            if (types != null) {
                StringTokenizer mappingST = new StringTokenizer(types, ":", false);
                while (mappingST.hasMoreTokens()) {
                    String mapping = mappingST.nextToken();
                    int delimPos = mapping.indexOf('=');
                    if (delimPos == -1) {
                        continue;
                    }
                    String extension = mapping.substring(0, delimPos);
                    String mimeType = mapping.substring(delimPos + 1);
                    server.getMimeTypes().addMimeMapping(extension.toLowerCase(), mimeType);
                }
            }
        }

        CompressionScheme compressionScheme = Option.COMPRESSION.get(this.args);
        switch (compressionScheme) {
            case GZIP:
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setHandler(webAppContext);
                server.setHandler(gzipHandler);
                break;
            case NONE:
                server.setHandler(webAppContext);
                break;
            default:
                throw new IllegalArgumentException("Unexpected compression scheme: " + compressionScheme);
        }

        Logger.log(
                Level.FINER,
                Launcher.RESOURCES,
                "HostConfig.InitComplete",
                this.webapps.size() + "",
                this.webapps.keySet() + "");
    }

    private void loadBuiltinMimeTypes() {
        try (InputStream in = getClass().getResourceAsStream("mime.properties")) {
            Properties props = new Properties();
            props.load(in);
            for (Entry<Object, Object> e : props.entrySet()) {
                server.getMimeTypes()
                        .addMimeMapping(e.getKey().toString(), e.getValue().toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load the built-in MIME types", e);
        }
    }

    /**
     * @param webAppName
     *      Unique name given to the access logger.
     */
    private RequestLog configureAccessLog(String webAppName) {
        try {
            Class<? extends RequestLog> loggerClass =
                    Option.ACCESS_LOGGER_CLASSNAME.get(args, RequestLog.class, commonLibCL);
            if (loggerClass != null) {
                // Build the realm
                Constructor<? extends RequestLog> loggerConstr = loggerClass.getConstructor(String.class, Map.class);
                return loggerConstr.newInstance(webAppName, args);
            } else {
                Logger.log(Level.FINER, Launcher.RESOURCES, "WebAppConfig.LoggerDisabled");
            }
        } catch (Throwable err) {
            Logger.log(Level.SEVERE, Launcher.RESOURCES, "WebAppConfig.LoggerError", "", err);
        }
        return null;
    }

    private WebAppContext create(File app, String prefix) {
        WebAppContext wac = new WebAppContext(app.getAbsolutePath(), prefix) {
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
                setMaxFormKeys(Option.MAX_PARAM_COUNT.get(args));
                setMaxFormContentSize(Option.REQUEST_FORM_CONTENT_SIZE.get(args));
            }

            @Override
            public void postConfigure() throws Exception {
                super.postConfigure();

                // if specified, override the value in web.xml
                int sessionTimeout = Option.SESSION_TIMEOUT.get(args);
                if (sessionTimeout > 0) {
                    getSessionHandler().setMaxInactiveInterval(sessionTimeout * 60);
                }
                int sessionEviction = Option.SESSION_EVICTION.get(args);
                getSessionHandler().getSessionCache().setEvictionPolicy(sessionEviction);
            }
        };
        wac.setServer(server);
        JettyWebSocketServletContainerInitializer.configure(wac, null);
        wac.getSecurityHandler().setLoginService(loginService);
        wac.setThrowUnavailableOnStartupException(
                true); // if boot fails, abort the process instead of letting empty Jetty run
        wac.getSessionHandler().setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
        wac.getSessionHandler().setSessionCookie(WinstoneSession.SESSION_COOKIE_NAME);
        this.webapps.put(wac.getContextPath(), wac);
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
                throw new WinstoneException("Failed to redeploy " + prefix, e);
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
    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "false positive, we're not being called from a webapp")
    protected File getWebRoot(File requestedWebroot, File warfile) throws IOException {
        if (warfile != null) {
            Logger.log(Level.INFO, Launcher.RESOURCES, "HostConfig.BeginningWarExtraction");

            // open the war file
            if (!warfile.exists() || !warfile.isFile()) {
                throw new WinstoneException(Launcher.RESOURCES.getString("HostConfig.WarFileInvalid", warfile));
            }

            // Get the webroot folder (or a temp dir if none supplied)
            File unzippedDir;
            if (requestedWebroot != null) {
                unzippedDir = requestedWebroot;
            } else {
                File tempFile = File.createTempFile("dummy", "dummy");
                String userName = System.getProperty("user.name");
                unzippedDir = new File(
                        tempFile.getParent(),
                        (userName != null
                                        ? WinstoneResourceBundle.globalReplace(
                                                        userName, new String[][] {{"/", ""}, {"\\", ""}, {",", ""}})
                                                + "/"
                                        : "")
                                + "winstone/"
                                + warfile.getName());

                try {
                    Files.delete(tempFile.toPath());
                } catch (Exception ex) {
                    Logger.logDirectMessage(Level.WARNING, null, "Failed To delete dummy file", ex);
                }
            }
            if (unzippedDir.exists()) {
                if (!unzippedDir.isDirectory()) {
                    throw new WinstoneException(
                            Launcher.RESOURCES.getString("HostConfig.WebRootNotDirectory", unzippedDir.getPath()));
                } else {
                    Logger.log(
                            Level.FINER,
                            Launcher.RESOURCES,
                            "HostConfig.WebRootExists",
                            unzippedDir.getCanonicalPath());
                }
            }

            // check consistency and if out-of-sync, recreate
            File timestampFile = new File(unzippedDir, ".timestamp");
            if (!timestampFile.exists() || Math.abs(timestampFile.lastModified() - warfile.lastModified()) > 1000) {
                // contents of the target directory is inconsistent from the war.
                deleteRecursive(unzippedDir);
                try {
                    Files.createDirectories(unzippedDir.toPath());
                } catch (Exception ex) {
                    Logger.logDirectMessage(
                            Level.WARNING, null, "Failed to recreate dirs " + unzippedDir.getAbsolutePath(), ex);
                }
            } else {
                // files are up to date
                return unzippedDir;
            }

            // Iterate through the files
            byte[] buffer = new byte[8192];
            try (JarFile warArchive = new JarFile(warfile)) {
                for (Enumeration<JarEntry> e = warArchive.entries(); e.hasMoreElements(); ) {
                    JarEntry element = e.nextElement();
                    if (element.isDirectory()) {
                        continue;
                    }
                    String elemName = element.getName();

                    // If archive date is newer than unzipped file, overwrite
                    File outFile = new File(unzippedDir, elemName);
                    Path outPath = outFile.toPath();

                    // Disallow unzipping files outside the target dir
                    if (!outPath.normalize().startsWith(unzippedDir.toPath().normalize())) {
                        throw new IOException("Bad zip entry: " + elemName);
                    }

                    if (outFile.exists() && (outFile.lastModified() > warfile.lastModified())) {
                        continue;
                    }
                    try {
                        Path parent = outPath.getParent();
                        if (parent == null) {
                            Logger.logDirectMessage(Level.WARNING, null, outPath + "has no parent dir ", null);
                        } else {
                            Files.createDirectories(parent);
                        }
                    } catch (IOException | InvalidPathException | SecurityException ex) {
                        Logger.logDirectMessage(
                                Level.WARNING,
                                null,
                                "Failed to create dirs "
                                        + outFile.getParentFile().getAbsolutePath(),
                                null);
                    }

                    // Copy out the extracted file
                    try (InputStream inContent = warArchive.getInputStream(element);
                            OutputStream outStream = new FileOutputStream(outFile)) {
                        int readBytes = inContent.read(buffer);
                        while (readBytes != -1) {
                            outStream.write(buffer, 0, readBytes);
                            readBytes = inContent.read(buffer);
                        }
                    }
                }
            }

            // extraction completed
            new FileOutputStream(timestampFile).close();
            if (!timestampFile.setLastModified(warfile.lastModified())) {
                Logger.logDirectMessage(
                        Level.WARNING, null, "Failed to set timestamp " + timestampFile.getAbsolutePath(), null);
            }

            // Return webroot
            return unzippedDir;
        } else {
            return requestedWebroot;
        }
    }

    private void deleteRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        try {
            Files.deleteIfExists(dir.toPath());
        } catch (Exception ex) {
            Logger.logDirectMessage(Level.WARNING, null, "Failed to delete dirs " + dir.getAbsolutePath(), ex);
        }
    }
}
