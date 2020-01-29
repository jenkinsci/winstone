package winstone;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import java.util.Map;

public class ServerConnectorBuilder {

    private Server server;
    private Map args;
    private SslContextFactory scf;
    private int listenerPort;
    private String listenerAddress;
    private int keepAliveTimeout;

    public ServerConnectorBuilder withServer(Server server){
        this.server = server;
        return this;
    }

    public ServerConnectorBuilder withArgs(Map args){
        this.args = args;
        return this;
    }

    public ServerConnectorBuilder withSslConfig(SslContextFactory scf){
        this.scf = scf;
        return this;
    }

    public ServerConnectorBuilder withListenerPort(int listenerPort){
        this.listenerPort = listenerPort;
        return this;
    }

    public ServerConnectorBuilder withListenerAddress(String listenerAddress){
        this.listenerAddress = listenerAddress;
        return this;
    }

    public ServerConnectorBuilder withKeepAliveTimeout(int keepAliveTimeout){
        this.keepAliveTimeout = keepAliveTimeout;
        return this;
    }

    public ServerConnector build() {
        ServerConnector sc = new ServerConnector(server, Option.JETTY_ACCEPTORS.get(args), Option.JETTY_SELECTORS.get(args));
        sc.addConnectionFactory(new SslConnectionFactory(scf, HttpVersion.HTTP_1_1.asString()));

        sc.setPort(listenerPort);
        sc.setHost(listenerAddress);
        sc.setIdleTimeout(keepAliveTimeout);

        HttpConfiguration config = sc.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));
        return sc;
    }

}