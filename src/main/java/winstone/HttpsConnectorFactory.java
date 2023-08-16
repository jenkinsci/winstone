/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;

import winstone.cmdline.Option;

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsConnectorFactory.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
public class HttpsConnectorFactory extends AbstractSecuredConnectorFactory implements ConnectorFactory {

    @Override
    public Connector start( Map<String, String> args, Server server) throws IOException {

        int listenPort = Option.HTTPS_PORT.get(args);

        if (listenPort<0) {
            // not running HTTPS listener
            return null;
        }

        if(Option.HTTPS_REDIRECT_HTTP.get(args)) {
            // setup a redirect from http to https
            Handler currentHandler = server.getHandler();
            if(currentHandler == null) {
                server.setHandler(new SecuredRedirectHandler());
            } else {
                SecuredRedirectHandler securedRedirectHandler = new SecuredRedirectHandler();
                securedRedirectHandler.setHandler(currentHandler);
                server.setHandler(securedRedirectHandler);
            }
        }
        configureSsl(args, server);

        ServerConnectorBuilder scb = new ServerConnectorBuilder()
            .withServer(server)
            .withAcceptors(Option.JETTY_ACCEPTORS.get(args))
            .withSelectors(Option.JETTY_SELECTORS.get(args))
            .withListenerPort(listenPort)
            .withListenerAddress(Option.HTTPS_LISTEN_ADDRESS.get(args))
            .withRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args))
            .withResponseHeaderSize(Option.RESPONSE_HEADER_SIZE.get(args))
            .withKeepAliveTimeout(Option.HTTPS_KEEP_ALIVE_TIMEOUT.get(args))
            .withSniHostCheck(Option.HTTPS_SNI_HOST_CHECK.get(args))
            .withSniRequired(Option.HTTPS_SNI_REQUIRED.get(args))
            .withSslContext(getSSLContext(args));
        Connector c = scb.build();
        server.addConnector(c);
        return c;
    }
}
