/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;
import winstone.cmdline.Option;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

/**
 * Manages the references to individual hosts within the container. This object handles
 * the mapping of ip addresses and hostnames to groups of webapps, and init and 
 * shutdown of any hosts it manages.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {
    
    private final static String DEFAULT_HOSTNAME = "default";
    private final Server server;

    //    private Map args;
    private Map hostConfigs;
    private String defaultHostName;

    public HostGroup(
            Server server, ClassLoader commonLibCL,
            File commonLibCLPaths[], Map args) throws IOException {
        this.server = server;
//        this.args = args;
        this.hostConfigs = new Hashtable();
        
        // Is this the single or multiple configuration ? Check args
        File webappsDir = Option.WEBAPPS_DIR.get(args);

        // If host mode
        initHost(webappsDir, DEFAULT_HOSTNAME, commonLibCL,
                commonLibCLPaths, args);
        this.defaultHostName = DEFAULT_HOSTNAME;
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.InitSingleComplete",
                this.hostConfigs.size() + "", this.hostConfigs.keySet() + "");
    }

    public HostConfiguration getHostByName(String hostname) {
        if ((hostname != null) && (this.hostConfigs.size() > 1)) {
            HostConfiguration host = (HostConfiguration) this.hostConfigs.get(hostname);
            if (host != null) {
                return host;
            }
        }
        return (HostConfiguration) this.hostConfigs.get(this.defaultHostName);
    }
    
    protected void initHost(File webappsDir, String hostname,
                            ClassLoader commonLibCL,
                            File commonLibCLPaths[], Map args) throws IOException {
        Logger.log(Logger.DEBUG, Launcher.RESOURCES, "HostGroup.DeployingHost", hostname);
        HostConfiguration config = new HostConfiguration(server, hostname, commonLibCL,
                commonLibCLPaths, args, webappsDir);
        this.hostConfigs.put(hostname, config);
    }
}
