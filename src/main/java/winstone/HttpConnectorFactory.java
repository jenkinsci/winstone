/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;
import winstone.cmdline.Option;

import java.io.IOException;
import java.util.Map;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc. Note that this
 * class is also used as the base class for the HTTPS listener.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpConnectorFactory.java,v 1.15 2007/05/01 04:39:49 rickknowles Exp $
 */
public class HttpConnectorFactory implements ConnectorFactory {
    public boolean start(Map args, Server server) throws IOException {
        // Load resources
        int listenPort = Option.HTTP_PORT.get(args);

        if (listenPort < 0) {
            return false;
        }
        else {
            ServerConnectorBuilder scb = new ServerConnectorBuilder()
                .withServer(server)
                .withAcceptors(Option.JETTY_ACCEPTORS.get(args))
                .withSelectors(Option.JETTY_SELECTORS.get(args))
                .withListenerPort(listenPort)
                .withSecureListenerPort(Option.HTTPS_PORT.get(args, -1))
                .withListenerAddress(Option.HTTP_LISTEN_ADDRESS.get(args))
                .withRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args))
                .withKeepAliveTimeout(Option.HTTP_KEEP_ALIVE_TIMEOUT.get(args));
            server.addConnector(scb.build());
            return true;

        }
    }
}
