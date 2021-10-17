package winstone;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import winstone.Launcher;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jvnet.hudson.test.Issue;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpsConnectorFactoryTest extends AbstractWinstoneTest {
    @Test
    public void testHttps() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "0");
        args.put("httpsListenAddress", "localhost");
        args.put("httpsPrivateKey", "src/ssl/server.key");
        args.put("httpsCertificate", "src/ssl/server.crt");
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();

        assertConnectionRefused("127.0.0.2", port);

        request(new TrustManagerImpl(), port);

        LowResourceMonitor lowResourceMonitor = winstone.server.getBean( LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    private void request(X509TrustManager tm, int port) throws Exception {
        HttpsURLConnection con = (HttpsURLConnection)new URL("https://localhost:"+port+"/CountRequestsServlet").openConnection();
        con.setHostnameVerifier( ( s, sslSession ) -> true );
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new X509TrustManager[] {tm}, null);
        con.setSSLSocketFactory(ssl.getSocketFactory());
        IOUtils.toString(con.getInputStream());
    }

    /**
     * Without specifying the certificate and key, it uses the random key
     */
    @Test
    public void testHttpsRandomCert() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "0");
        winstone = new Launcher(args);
        int port = (( ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();


        try {
            request(new TrustManagerImpl(), port);
            fail("we should have generated a unique key");
        } catch (SSLHandshakeException e) {
            // expected
        }

        request(new TrustEveryoneManager(), port);
    }

    @Issue("JENKINS-60857")
    @Test
    public void wildcard() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "0");
        args.put("httpsListenAddress", "localhost");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        winstone = new Launcher(args);
        int port = (( ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        request(new TrustEveryoneManager(), port);
    }

    @Test
    public void httpRedirect() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpsPort", "0");
        args.put("httpsRedirectHttp", "true");
        winstone = new Launcher(args);
        List<ServerConnector> serverConnectors =
            Arrays.asList( winstone.server.getConnectors() )
                .stream().map(connector -> (ServerConnector)connector ).collect(Collectors.toList());
        
        int httpsPort = serverConnectors.stream()
                            .filter(serverConnector -> serverConnector.getDefaultProtocol().startsWith("SSL"))
                            .findFirst().get().getLocalPort();
        ServerConnector scNonSsl = serverConnectors.stream()
                .filter(serverConnector -> !serverConnector.getDefaultProtocol().startsWith("SSL"))
                .findFirst().get();
        int httpPort = scNonSsl.getLocalPort();

        scNonSsl.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setSecurePort(httpsPort);

        requestRedirect(new TrustEveryoneManager(), httpPort, httpsPort);

        // also verify that directly accessing the resource works.
        request(new TrustEveryoneManager(), httpsPort);
    }

    private void requestRedirect(X509TrustManager tm, int httpPort, int httpsPort) throws Exception {
        HttpURLConnection con = (HttpURLConnection)new URL("http://localhost:"+httpPort+"/CountRequestsServlet").openConnection();
        assertEquals(302, con.getResponseCode());
        assertTrue("Should have a Location header of the resource", con.getHeaderFields().containsKey("Location"));
        String newUrl = con.getHeaderField("Location");
        assertNotNull(newUrl);
        assertTrue(newUrl.contains("https"));
        assertTrue(newUrl.contains(Integer.toString(httpsPort)));
        HttpsURLConnection secureCon = (HttpsURLConnection)new URL(newUrl).openConnection();
        secureCon.setHostnameVerifier( ( s, sslSession ) -> true );
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new X509TrustManager[] {tm}, null);
        secureCon.setSSLSocketFactory(ssl.getSocketFactory());
        IOUtils.toString(secureCon.getInputStream());
    }

}
