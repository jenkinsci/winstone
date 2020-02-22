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
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsConnectorFactory.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
public class HttpsConnectorFactory extends AbstractSecuredConnectorFactory implements ConnectorFactory {

    public boolean start(Map args, Server server) throws IOException {

        int listenPort = Option.HTTPS_PORT.get(args);

        if (listenPort<0) {
            // not running HTTPS listener
            return false;
        }

        configureSsl(args, server);

        ServerConnectorBuilder scb = new ServerConnectorBuilder();
        scb.withAcceptors(Option.JETTY_ACCEPTORS.get(args));
        scb.withSelectors(Option.JETTY_SELECTORS.get(args));
        scb.withListenerPort(listenPort);
        scb.withListenerAddress(Option.HTTPS_LISTEN_ADDRESS.get(args));
        scb.withRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));
        scb.withSslContext(getSSLContext(args));

        server.addConnector(scb.build());

        return true;
    }

}