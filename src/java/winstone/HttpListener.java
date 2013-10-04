/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import winstone.cmdline.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc. Note that this
 * class is also used as the base class for the HTTPS listener.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpListener.java,v 1.15 2007/05/01 04:39:49 rickknowles Exp $
 */
public class HttpListener implements Listener {
    protected static boolean DEFAULT_HNL = false;
    protected int keepAliveTimeout;
    protected HostGroup hostGroup;
    protected ObjectPool objectPool;
    protected boolean doHostnameLookups;
    protected int listenPort;
    protected String listenAddress;

    protected HttpListener() {
    }

    /**
     * Constructor
     */
    public HttpListener(Map args, ObjectPool objectPool, HostGroup hostGroup) {
        // Load resources
        this.hostGroup = hostGroup;
        this.objectPool = objectPool;
        this.listenPort = Integer.parseInt(WebAppConfiguration.stringArg(args,
                getConnectorName() + Option._PORT, "" + getDefaultPort()));
        this.listenAddress = WebAppConfiguration.stringArg(args,
                getConnectorName() + Option._LISTEN_ADDRESS, null);
        this.doHostnameLookups = WebAppConfiguration.booleanArg(args,
                getConnectorName() + Option._DO_HOSTNAME_LOOKUPS, DEFAULT_HNL);
        this.keepAliveTimeout = WebAppConfiguration.intArg(args,
                        getConnectorName() + Option._KEEP_ALIVE_TIMEOUT, Option._KEEP_ALIVE_TIMEOUT.defaultValue);
    }

    public boolean start(Server server) throws IOException {
        if (this.listenPort < 0) {
            return false;
        } else {
            SelectChannelConnector connector = createConnector(server);
            connector.setPort(listenPort);
            connector.setHost(this.listenAddress);

            server.addConnector(connector);
            return true;
        }
    }

    /**
     * The default port to use - this is just so that we can override for the
     * SSL connector.
     */
    protected int getDefaultPort() {
        return 8080;
    }

    /**
     * The name to use when getting properties - this is just so that we can
     * override for the SSL connector.
     */
    protected String getConnectorName() {
        return getConnectorScheme();
    }

    protected String getConnectorScheme() {
        return "http";
    }
    
    /**
     * Gets a server socket - this is mostly for the purpose of allowing an
     * override in the SSL connector.
     */
    protected SelectChannelConnector createConnector(Server server) {
        return new SelectChannelConnector();
    }

    protected void parseSocketInfo(Socket socket, WinstoneRequest req)
            throws IOException {
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "HttpListener.ParsingSocketInfo");
        req.setScheme(getConnectorScheme());
        req.setServerPort(socket.getLocalPort());
        req.setLocalPort(socket.getLocalPort());
        req.setLocalAddr(socket.getLocalAddress().getHostAddress());
        req.setRemoteIP(socket.getInetAddress().getHostAddress());
        req.setRemotePort(socket.getPort());
        if (this.doHostnameLookups) {
            req.setServerName(getHostName(socket.getLocalAddress()));
            req.setRemoteName(socket.getInetAddress().getHostName());
            req.setLocalName(getHostName(socket.getLocalAddress()));
        } else {
            req.setServerName(getHostAddress(socket.getLocalAddress()));
            req.setRemoteName(socket.getInetAddress().getHostAddress());
            req.setLocalName(getHostAddress(socket.getLocalAddress()));
        }
        // setRemoteName pairs with getRemoteHost(), and in Jetty
        // this returns ::1 not [::1].
        // but getServerName() and getLocalName() returns [::1] and not ::1.
        // that's why setRemoteName() is left without the IPv6-aware wrapper method
    }

    private String getHostAddress(InetAddress adrs) {
        if (adrs instanceof Inet6Address)
            return '['+adrs.getHostAddress()+']';
        else
            return adrs.getHostAddress();
    }

    private String getHostName(InetAddress adrs) {
        if (adrs instanceof Inet6Address) {
            String n = adrs.getHostName();
            if (n.indexOf(':')>=0)  return '['+n+']';
            return n;
        } else
            return adrs.getHostName();
    }
}
