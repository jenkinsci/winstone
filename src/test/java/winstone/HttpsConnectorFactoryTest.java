package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpsConnectorFactoryTest extends AbstractWinstoneTest {

    @Issue("JENKINS-60857")
    @Test
    public void wildcard() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "0");
        args.put("httpsListenAddress", "localhost");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        assertConnectionRefused("127.0.0.2", port);
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                makeRequest(null, "https://localhost:" + port + "/CountRequestsServlet", Protocol.HTTP_1));
        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    public void httpRedirect() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpsPort", "0");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        args.put("httpsRedirectHttp", "true");
        winstone = new Launcher(args);
        List<ServerConnector> serverConnectors = Arrays.stream(winstone.server.getConnectors())
                .map(connector -> (ServerConnector) connector)
                .collect(Collectors.toList());

        int httpsPort = serverConnectors.stream()
                .filter(serverConnector -> serverConnector.getDefaultProtocol().startsWith("SSL"))
                .findFirst()
                .get()
                .getLocalPort();
        ServerConnector scNonSsl = serverConnectors.stream()
                .filter(serverConnector -> !serverConnector.getDefaultProtocol().startsWith("SSL"))
                .findFirst()
                .get();

        scNonSsl.getConnectionFactory(HttpConnectionFactory.class)
                .getHttpConfiguration()
                .setSecurePort(httpsPort);

        int httpPort = scNonSsl.getLocalPort();

        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                requestRedirect(httpPort));

        // also verify that directly accessing the resource works.
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1002 times</body></html>\r\n",
                makeRequest(null, "https://localhost:" + httpsPort + "/CountRequestsServlet", Protocol.HTTP_1));
    }

    private String requestRedirect(int httpPort) throws Exception {
        HttpClient httpClient = getHttpClient(null);
        try {
            ContentResponse contentResponse = httpClient.GET("http://localhost:" + httpPort + "/CountRequestsServlet");
            assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, contentResponse.getStatus());
            assertTrue(contentResponse.getContentAsString().isEmpty());

            String newUrl = contentResponse.getHeaders().get("Location");
            assertNotNull(newUrl);
            contentResponse = httpClient.GET(newUrl);
            assertEquals(HttpURLConnection.HTTP_OK, contentResponse.getStatus());
            return contentResponse.getContentAsString();
        } finally {
            httpClient.close();
        }
    }
}
