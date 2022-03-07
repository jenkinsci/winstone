/*
 * Copyright Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package winstone;

import org.eclipse.jetty.alpn.ALPN;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import java.io.IOException;
import java.util.Map;

/**
 * This class add the HTTP/2 Listener This is the class that gets launched
 * by the command line, and owns the server socket, etc.
 *  @since 4.1
 */
public class Http2ConnectorFactory extends AbstractSecuredConnectorFactory implements ConnectorFactory {
    @Override
    public Connector start( Map<String, String> args, Server server ) throws IOException
    {
        int listenPort = Option.HTTP2_PORT.get( args );
        String listenAddress = Option.HTTP2_LISTEN_ADDRESS.get( args );

        if ( listenPort < 0 ) {
            // not running HTTP2 listener
            return null;
        }


        try {
            configureSsl( args, server );
            SslContextFactory sslContextFactory = getSSLContext( args );
            sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);

            // HTTPS Configuration
            HttpConfiguration https_config = new HttpConfiguration();
            https_config.setSecureScheme("https");
            https_config.setSecurePort(listenPort);
            https_config.addCustomizer(new SecureRequestCustomizer());

            // HTTP/2 Connection Factory
            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(https_config);
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol("h2");

            // SSL Connection Factory
            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory,alpn.getProtocol());

            // HTTP/2 Connector
            ServerConnector http2Connector =
                new ServerConnector(server,Option.JETTY_ACCEPTORS.get( args ), Option.JETTY_SELECTORS.get( args )
                    ,ssl,alpn,h2,new HttpConnectionFactory(https_config));
            http2Connector.setPort(listenPort);
            http2Connector.setHost( listenAddress );
            server.addConnector(http2Connector);
            server.setDumpAfterStart( Boolean.getBoolean( "dumpAfterStart" ) );

            ALPN.debug = Boolean.getBoolean( "alpnDebug" );

            return http2Connector;
        } catch (IllegalStateException e) {
            Logger.log( Logger.WARNING, Launcher.RESOURCES, "Http2ConnectorFactory.FailedStart.ALPN", e );
        }
        return null;
    }
}
