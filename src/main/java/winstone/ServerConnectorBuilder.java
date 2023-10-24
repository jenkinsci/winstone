package winstone;

import java.nio.file.Path;

import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.unixdomain.server.UnixDomainServerConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

class ServerConnectorBuilder {

    private int listenerPort;
    private int secureListenerPort;
    private int keepAliveTimeout;
    private int acceptors;
    private int selectors;
    private int requestHeaderSize;
    private int responseHeaderSize;
    private String listenerAddress;
    private String listenerUnixDomainPath;
    private Server server;
    private SslContextFactory.Server sslContextFactory;
    private boolean sniHostCheck = true;
    private boolean sniRequired = false;

    public ServerConnectorBuilder withListenerPort(int listenerPort) {
        this.listenerPort = listenerPort;
        return this;
    }

    public ServerConnectorBuilder withSecureListenerPort(int secureListenerPort) {
        this.secureListenerPort = secureListenerPort;
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

    public ServerConnectorBuilder withListenerUnixDomainPath(String listenerUnixDomainPath) {
        this.listenerUnixDomainPath = listenerUnixDomainPath;
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

    public ServerConnectorBuilder withSslContext(SslContextFactory.Server sslContextFactory) {
        this.sslContextFactory = sslContextFactory;
        return this;
    }

    public ServerConnectorBuilder withRequestHeaderSize(int requestHeaderSize) {
        this.requestHeaderSize = requestHeaderSize;
        return this;
    }

    public ServerConnectorBuilder withResponseHeaderSize(int responseHeaderSize) {
      this.responseHeaderSize = responseHeaderSize;
      return this;
  }

    public ServerConnectorBuilder withSniHostCheck(boolean sniHostCheck) {
        this.sniHostCheck = sniHostCheck;
        return this;
    }

    public ServerConnectorBuilder withSniRequired(boolean sniRequired) {
        this.sniRequired = sniRequired;
        return this;
    }

    public Connector build() {

        Connector c;

        if (listenerUnixDomainPath != null) {

            UnixDomainServerConnector usc;
            
            usc = new UnixDomainServerConnector(server, acceptors, selectors,
                    new HttpConnectionFactory());

            usc.setUnixDomainPath(Path.of(listenerUnixDomainPath));
            usc.setIdleTimeout(keepAliveTimeout);

            c = usc;

        } else {

            ServerConnector sc;

            if (sslContextFactory != null) {
                sc = new ServerConnector(server, acceptors, selectors, sslContextFactory);
            } else {
                sc = new ServerConnector(server, acceptors, selectors);
            }

            sc.setPort(listenerPort);
            sc.setHost(listenerAddress);
            sc.setIdleTimeout(keepAliveTimeout);

            c = sc;

        }

        HttpConfiguration hc = c.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        if(secureListenerPort > 0) {
            hc.setSecurePort(secureListenerPort);
        }
        hc.setHttpCompliance(HttpCompliance.RFC7230);
        hc.setUriCompliance(UriCompliance.LEGACY);
        hc.addCustomizer(new ForwardedRequestCustomizer());
        hc.setRequestHeaderSize(requestHeaderSize);
        hc.setResponseHeaderSize(responseHeaderSize);

        SecureRequestCustomizer src = hc.getCustomizer(SecureRequestCustomizer.class);
        if(src!=null&&!sniHostCheck){
            src.setSniHostCheck(false);
        }
        if(src!=null&&sniRequired){
            src.setSniRequired(true);
        }

        return c;

    }

}
