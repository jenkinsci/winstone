/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.ajp13;

import org.eclipse.jetty.ajp.Ajp13SocketConnector;
import org.eclipse.jetty.server.Server;
import winstone.Listener;
import winstone.cmdline.Option;

import java.io.IOException;
import java.util.Map;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13Listener.java,v 1.12 2006/03/24 17:24:22 rickknowles Exp $
 */
public class Ajp13Listener implements Listener {
    private int listenPort;
    private String listenAddress;

    /**
     * Constructor
     */
    public Ajp13Listener(Map args) {
        this.listenPort = Option.AJP13_PORT.get(args);
        this.listenAddress = Option.AJP13_LISTEN_ADDRESS.get(args);
    }

    public boolean start(Server server) throws IOException {
        if (this.listenPort < 0) {
            return false;
        } else {
            Ajp13SocketConnector connector = new Ajp13SocketConnector();
            connector.setPort(listenPort);
            connector.setHost(this.listenAddress);

            server.addConnector(connector);
            return true;
        }
    }
}
