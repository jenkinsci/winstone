/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import org.eclipse.jetty.server.Server;

/**
 * Manages the references to individual hosts within the container. This object handles
 * the mapping of ip addresses and hostnames to groups of webapps, and init and
 * shutdown of any hosts it manages.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HostGroup.java,v 1.4 2006/03/24 17:24:21 rickknowles Exp $
 */
public class HostGroup {

    private static final String DEFAULT_HOSTNAME = "default";
    private final Server server;

    //    private Map args;
    private Map<String, HostConfiguration> hostConfigs;
    private String defaultHostName;

    public HostGroup(Server server, ClassLoader commonLibCL, Map<String, String> args) throws IOException {
        this.server = server;
        this.hostConfigs = new Hashtable<>();

        // If host mode
        initHost(DEFAULT_HOSTNAME, commonLibCL, args);
        this.defaultHostName = DEFAULT_HOSTNAME;
        Logger.log(
                Level.FINER,
                Launcher.RESOURCES,
                "HostGroup.InitSingleComplete",
                this.hostConfigs.size() + "",
                this.hostConfigs.keySet() + "");
    }

    public HostConfiguration getHostByName(String hostname) {
        if ((hostname != null) && (this.hostConfigs.size() > 1)) {
            HostConfiguration host = this.hostConfigs.get(hostname);
            if (host != null) {
                return host;
            }
        }
        return this.hostConfigs.get(this.defaultHostName);
    }

    protected void initHost(String hostname, ClassLoader commonLibCL, Map<String, String> args) throws IOException {
        Logger.log(Level.FINER, Launcher.RESOURCES, "HostGroup.DeployingHost", hostname);
        HostConfiguration config = new HostConfiguration(server, hostname, commonLibCL, args);
        this.hostConfigs.put(hostname, config);
    }
}
