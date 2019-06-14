/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import java.util.Map;

public class ServerConnectorFactory {

    private ServerConnector sc;
    private Server server;
    private Map args;

    public ServerConnectorFactory(Server server, Map args, SslContextFactory scf) {
        this.server = server;
        this.args = args;

        if(scf != null) {
            sc = new ServerConnector(server, Option.JETTY_ACCEPTORS.get(args), Option.JETTY_SELECTORS.get(args), scf);
        }
        else {
            sc = new ServerConnector(server, Option.JETTY_ACCEPTORS.get(args), Option.JETTY_SELECTORS.get(args));
        }

    }

    public ServerConnector getConnector(int listenPort, String listenAddress, int keepAliveTimeout) {


        sc.setPort(listenPort);
        sc.setHost(listenAddress);
        sc.setIdleTimeout(keepAliveTimeout);

        HttpConfiguration config = sc.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));

        return sc;

    }
}