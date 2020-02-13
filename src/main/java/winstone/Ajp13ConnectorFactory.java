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
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13ConnectorFactory.java,v 1.12 2006/03/24 17:24:22 rickknowles Exp $
 */
public class Ajp13ConnectorFactory implements ConnectorFactory {
    public boolean start(Map args, Server server) throws IOException {
        int listenPort = Option.AJP13_PORT.get(args);

        if (listenPort < 0) {
            return false;
        }

        throw new UnsupportedOperationException(
                RESOURCES.getString("Ajp13ConnectorFactory.NotSupported"));

        // if we are going to resurrect AJP support, look for Ajp13ConnectorFactoryTest in the history
        // that you can resurrect as a test.

        /* Jetty9 has no AJP support
        Ajp13SocketConnector connector = new Ajp13SocketConnector();
        connector.setPort(listenPort);
        connector.setHost(listenAddress);
        connector.setRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));
        connector.setRequestBufferSize(Option.REQUEST_BUFFER_SIZE.get(args));

        server.addConnector(connector);
        return true;
        */
    }

    public final static WinstoneResourceBundle RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
}
