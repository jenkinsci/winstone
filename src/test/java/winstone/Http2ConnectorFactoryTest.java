package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

public class Http2ConnectorFactoryTest extends AbstractWinstoneTest {

    private static final String DISABLE_HOSTNAME_VERIFICATION = "jdk.internal.httpclient.disableHostnameVerification";

    private String request(X509TrustManager tm, int port) throws Exception {
        String disableHostnameVerification = System.getProperty(DISABLE_HOSTNAME_VERIFICATION);
        try {
            System.setProperty(DISABLE_HOSTNAME_VERIFICATION, Boolean.TRUE.toString());
            HttpRequest request = HttpRequest.newBuilder(new URI("https://localhost:" + port + "/CountRequestsServlet"))
                    .version(HttpClient.Version.HTTP_2)
                    .GET()
                    .build();
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new X509TrustManager[] {tm}, null);
            HttpClient client = HttpClient.newBuilder().sslContext(sslContext).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

    @Test
    public void wildcard() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("http2Port", "0");
        args.put("http2ListenAddress", "localhost");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        assertConnectionRefused("127.0.0.2", port);
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                request(new TrustEveryoneManager(), port));
        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }
}
