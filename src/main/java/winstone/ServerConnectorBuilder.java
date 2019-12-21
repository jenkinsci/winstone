package winstone;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
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
        ServerConnector sc;
        if(scf == null){
            sc = new ServerConnector(server, Option.JETTY_ACCEPTORS.get(args), Option.JETTY_SELECTORS.get(args));
        }
        else {
            sc = new ServerConnector(server, Option.JETTY_ACCEPTORS.get(args), Option.JETTY_SELECTORS.get(args), scf);
        }
        sc.setPort(listenerPort);
        sc.setHost(listenerAddress);
        sc.setIdleTimeout(keepAliveTimeout);

        HttpConfiguration config = sc.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));
        return sc;
    }

}