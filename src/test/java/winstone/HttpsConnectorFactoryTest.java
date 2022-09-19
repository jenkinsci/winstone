package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    private static final String DISABLE_HOSTNAME_VERIFICATION =
            "jdk.internal.httpclient.disableHostnameVerification";

    private String request(X509TrustManager tm, int port) throws Exception {
        String disableHostnameVerification = System.getProperty(DISABLE_HOSTNAME_VERIFICATION);
        try {
            System.setProperty(DISABLE_HOSTNAME_VERIFICATION, Boolean.TRUE.toString());
            HttpRequest request =
                    HttpRequest.newBuilder(new URI("https://localhost:" + port + "/CountRequestsServlet")).GET().build();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[] {tm}, null);
            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
            return response.body();
        } finally {
            if (disableHostnameVerification != null) {
                System.setProperty(DISABLE_HOSTNAME_VERIFICATION, disableHostnameVerification);
            } else {
                System.clearProperty(DISABLE_HOSTNAME_VERIFICATION);
            }
        }
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
        assertConnectionRefused("127.0.0.2", port);
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                request(new TrustEveryoneManager(), port));
        LowResourceMonitor lowResourceMonitor = winstone.server.getBean( LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    public void httpRedirect() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpsPort", "0");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        args.put("httpsRedirectHttp", "true");
        winstone = new Launcher(args);
        List<ServerConnector> serverConnectors =
            Arrays.stream( winstone.server.getConnectors() )
                .map(connector -> (ServerConnector)connector ).collect(Collectors.toList());

        int httpsPort = serverConnectors.stream()
                            .filter(serverConnector -> serverConnector.getDefaultProtocol().startsWith("SSL"))
                            .findFirst().get().getLocalPort();
        ServerConnector scNonSsl = serverConnectors.stream()
                .filter(serverConnector -> !serverConnector.getDefaultProtocol().startsWith("SSL"))
                .findFirst().get();
        int httpPort = scNonSsl.getLocalPort();

        scNonSsl.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration().setSecurePort(httpsPort);

        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                requestRedirect(new TrustEveryoneManager(), httpPort, httpsPort));

        // also verify that directly accessing the resource works.
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1002 times</body></html>\r\n",
                request(new TrustEveryoneManager(), httpsPort));
    }

    private String requestRedirect(X509TrustManager tm, int httpPort, int httpsPort) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder(new URI("http://localhost:" + httpPort + "/CountRequestsServlet")).GET().build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, response.statusCode());
        assertTrue(response.body().isEmpty());
        String newUrl = response.headers().firstValue("Location").orElse(null);
        assertNotNull(newUrl);
        assertEquals("https://localhost:" + httpsPort + "/CountRequestsServlet", newUrl);

        String disableHostnameVerification = System.getProperty(DISABLE_HOSTNAME_VERIFICATION);
        try {
            System.setProperty(DISABLE_HOSTNAME_VERIFICATION, Boolean.TRUE.toString());
            request = HttpRequest.newBuilder(new URI(newUrl)).GET().build();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[] {tm}, null);
            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
            return response.body();
        } finally {
            if (disableHostnameVerification != null) {
                System.setProperty(DISABLE_HOSTNAME_VERIFICATION, disableHostnameVerification);
            } else {
                System.clearProperty(DISABLE_HOSTNAME_VERIFICATION);
            }
        }
    }

}
