/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.ajp13;

import org.eclipse.jetty.ajp.Ajp13SocketConnector;
import org.eclipse.jetty.server.Server;
import winstone.HostGroup;
import winstone.Listener;
import winstone.ObjectPool;
import winstone.WinstoneResourceBundle;
import winstone.cmdline.Option;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Ajp13Listener.java,v 1.12 2006/03/24 17:24:22 rickknowles Exp $
 */
public class Ajp13Listener implements Listener {
    public final static WinstoneResourceBundle AJP_RESOURCES = new WinstoneResourceBundle("winstone.ajp13.LocalStrings");
    
    private final static int LISTENER_TIMEOUT = 5000; // every 5s reset the listener socket
    private final static int CONNECTION_TIMEOUT = 60000;
    private final static int BACKLOG_COUNT = 1000;
    private final static int KEEP_ALIVE_TIMEOUT = -1;
//    private final static int KEEP_ALIVE_SLEEP = 50;
//    private final static int KEEP_ALIVE_SLEEP_MAX = 500;
    private final static String TEMPORARY_URL_STASH = "winstone.ajp13.TemporaryURLAttribute";
    
    private HostGroup hostGroup;
    private ObjectPool objectPool;
    private int listenPort;
    private boolean interrupted;
    private String listenAddress;
    private ServerSocket serverSocket;

    /**
     * Constructor
     */
    public Ajp13Listener(Map args, ObjectPool objectPool, HostGroup hostGroup) {
        // Load resources
        this.hostGroup = hostGroup;
        this.objectPool = objectPool;

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
