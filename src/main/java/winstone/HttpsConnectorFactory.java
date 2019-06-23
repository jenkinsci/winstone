/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
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
        String listenAddress = Option.HTTPS_LISTEN_ADDRESS.get(args);
        int keepAliveTimeout = Option.HTTPS_KEEP_ALIVE_TIMEOUT.get(args);

        if (listenPort < 0) {
            // not running HTTPS listener
            return false;
        }

        configureSsl(args, server);
        SslContextFactory sslConfig = getSSLContext(args);

        ServerConnectorFactory scf = new ServerConnectorFactory(server, args, sslConfig);
        server.addConnector(scf.getConnector(listenPort, listenAddress, keepAliveTimeout));

        return true;

    }

}