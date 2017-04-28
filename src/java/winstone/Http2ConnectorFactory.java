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

import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class Http2ConnectorFactory implements ConnectorFactory
{
    @Override
    public boolean start( Map args, Server server )
        throws IOException
    {
        int listenPort = Option.HTTP2_PORT.get( args);
        String listenAddress = Option.HTTP2_LISTEN_ADDRESS.get(args);

        if (listenPort<0) {
            // not running HTTP2 listener
            return false;
        }

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        http_config.setSecurePort(8443);
        http_config.setSendXPoweredBy(true);
        http_config.setSendServerVersion(true);

        /*
        ServerConnector connector =
            new ServerConnector( server, new HTTP2CServerConnectionFactory(http_config));
        connector.setPort(listenPort);
        connector.setHost(listenAddress);
         */

        HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory( http_config);

        SslContextFactory sslContextFactory = new SslContextFactory(true);
        ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
        alpn.setDefaultProtocol(http.getDefaultProtocol());

        // SSL Connection Factory
        SslConnectionFactory ssl = new SslConnectionFactory( sslContextFactory, alpn.getProtocol());
        ServerConnector http2Connector =
            new ServerConnector(server,ssl,alpn,h2);
        http2Connector.setPort(listenPort);
        http2Connector.setHost( listenAddress );

        server.addConnector(http2Connector);

        return false;
    }
}
