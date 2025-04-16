package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

public class Http2ConnectorFactoryTest extends AbstractWinstoneTest {

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
                makeRequest("https://localhost:" + port + "/CountRequestsServlet", Protocol.HTTP_2));
        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    public void helloSuspiciousPathCharacters() throws Exception {
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

        assertEquals(
                "<html><body>Hello winstone </body></html>\r\n",
                makeRequest("https://127.0.0.1:" + port + "/hello/winstone", Protocol.HTTP_2));

        assertEquals(
                "<html><body>Hello win\\stone </body></html>\r\n",
                makeRequest(
                        "https://127.0.0.1:" + port + "/hello/"
                                + URLEncoder.encode("win\\stone", StandardCharsets.UTF_8),
                        Protocol.HTTP_2)); // %5C == \
    }
}
