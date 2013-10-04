/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import winstone.cmdline.Option;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    private Map args;
    private Map<String,WebAppContext> webapps;
    private ClassLoader commonLibCL;
    private File commonLibCLPaths[];
    private MimeTypes mimeTypes = new MimeTypes();

    public HostConfiguration(Server server, String hostname, ClassLoader commonLibCL,
                             File commonLibCLPaths[], Map args, File webappsDir) throws IOException {
        this.server = server;
        this.hostname = hostname;
        this.args = args;
        this.webapps = new Hashtable<String,WebAppContext>();
        this.commonLibCL = commonLibCL;
        this.commonLibCLPaths = commonLibCLPaths;
        
        // Is this the single or multiple configuration ? Check args
        File appFile = Option.WARFILE.get(args);
        if (appFile==null)
            appFile = Option.WEBROOT.get(args);

        Handler handler;
        // If single-webapp mode
        if (webappsDir == null && appFile != null) {
            String prefix = Option.PREFIX.get(args);
            if (prefix.endsWith("/"))   // trim off the trailing '/' that Jetty doesn't like
                prefix = prefix.substring(0,prefix.length()-1);
            handler = configureAccessLog(create(appFile, prefix),"webapp");
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
                for (; mappingST.hasMoreTokens(); ) {
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
        InputStream in = getClass().getResourceAsStream("mime.properties");
        try {
            Properties props = new Properties();
            props.load(in);
            for (Entry<Object, Object> e : props.entrySet()) {
                mimeTypes.addMimeMapping(e.getKey().toString(),e.getValue().toString());
            }
        } catch (IOException e) {
            throw new Error("Failed to load the built-in MIME types",e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }


    /**
     * @param webAppName
     *      Unique name given to the access logger.
     */
    private Handler configureAccessLog(Handler handler, String webAppName) {
        try {
            Class loggerClass = Option.ACCESS_LOGGER_CLASSNAME.get(args, RequestLog.class, commonLibCL);
            if (loggerClass!=null) {
                // Build the realm
                Constructor loggerConstr = loggerClass.getConstructor(new Class[] {
                        String.class, Map.class });
                RequestLogHandler rlh = new RequestLogHandler();
                rlh.setHandler(handler);
                rlh.setRequestLog((RequestLog) loggerConstr.newInstance(this, webAppName, args));
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
            }

            @Override
            public void postConfigure() throws Exception {
                super.postConfigure();

                // if specified, override the value in web.xml
                int sessionTimeout = Option.SESSION_TIMEOUT.get(args);
                if (sessionTimeout>0)
                    getSessionHandler().getSessionManager().setMaxInactiveInterval(sessionTimeout * 60);
            }
        };
        wac.setMimeTypes(mimeTypes);
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
                tempFile.delete();
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
                unzippedDir.mkdirs();
            } else {
                // files are up to date
                return unzippedDir;
            }
            
            // Iterate through the files
            byte buffer[] = new byte[8192];
            JarFile warArchive = new JarFile(warfile);
            for (Enumeration e = warArchive.entries(); e.hasMoreElements();) {
                JarEntry element = (JarEntry) e.nextElement();
                if (element.isDirectory()) {
                    continue;
                }
                String elemName = element.getName();

                // If archive date is newer than unzipped file, overwrite
                File outFile = new File(unzippedDir, elemName);
                if (outFile.exists() && (outFile.lastModified() > warfile.lastModified())) {
                    continue;
                }
                outFile.getParentFile().mkdirs();

                // Copy out the extracted file
                InputStream inContent = warArchive.getInputStream(element);
                OutputStream outStream = new FileOutputStream(outFile);
                int readBytes = inContent.read(buffer);
                while (readBytes != -1) {
                    outStream.write(buffer, 0, readBytes);
                    readBytes = inContent.read(buffer);
                }
                inContent.close();
                outStream.close();
            }

            // extraction completed
            new FileOutputStream(timestampFile).close();
            timestampFile.setLastModified(warfile.lastModified());

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
        dir.delete();
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
            File children[] = webappsDir.listFiles();
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
                                context.start();
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
                        outputDir.mkdirs();
                        try {
                            WebAppContext context = create(
                                    getWebRoot(new File(webappsDir, outputName), aChildren), prefix);
                            context.start();
                            webApps.addHandler(configureAccessLog(context,outputName));
                            Logger.log(Logger.INFO, Launcher.RESOURCES, "HostConfig.DeployingWebapp", childName);
                        } catch (Throwable err) {
                            Logger.log(Logger.ERROR, Launcher.RESOURCES, "HostConfig.WebappInitError", prefix, err);
                        }
                    }
                }
            }
        }

        return webApps;
    }
}
