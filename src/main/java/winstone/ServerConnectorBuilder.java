package winstone;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class ServerConnectorBuilder {

    private int listenerPort;
    private int keepAliveTimeout;
    private int acceptors;
    private int selectors;
    private int requestHeaderSize;
    private String listenerAddress;
    private Server server;
    private SslContextFactory sslContextFactory;

    public ServerConnectorBuilder withListenerPort(int listenerPort) {
        this.listenerPort = listenerPort;
        return this;
    }

    public ServerConnectorBuilder withKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    public ServerConnectorBuilder withListenerAddress(String listenerAddress) {
        this.listenerAddress = listenerAddress;
        return this;
    }

    public ServerConnectorBuilder withServer(Server server) {
        this.server = server;
        return this;
    }

    public ServerConnectorBuilder withAcceptors(int acceptors) {
        this.acceptors = acceptors;
        return this;
    }

    public ServerConnectorBuilder withSelectors(int selectors) {
        this.selectors = selectors;
        return this;
    }

    public ServerConnectorBuilder withSslContext(SslContextFactory sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    public ServerConnectorBuilder withRequestHeaderSize(int requestHeaderSize) {
        this.requestHeaderSize = requestHeaderSize;
        return this;
    }

    public ServerConnector build() {

        ServerConnector sc;

        if(sslContextFactory != null) {
            sc = new ServerConnector(server, acceptors, selectors, sslContextFactory);
        }
        else {
            sc = new ServerConnector(server, acceptors, selectors);
        }

        sc.setPort(listenerPort);
        sc.setHost(listenerAddress);
        sc.setIdleTimeout(keepAliveTimeout);

        HttpConfiguration hc = sc.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        hc.addCustomizer(new ForwardedRequestCustomizer());
        hc.setRequestHeaderSize(requestHeaderSize);

        return sc;

    }

}