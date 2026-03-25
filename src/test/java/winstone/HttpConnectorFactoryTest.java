package winstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static winstone.Launcher.WINSTONE_PORT_FILE_NAME_PROPERTY;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.unixdomain.server.UnixDomainServerConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Kohsuke Kawaguchi
 */
class HttpConnectorFactoryTest extends AbstractWinstoneTest {

    @Test
    void testListenUnixDomainPath() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpUnixDomainPath", "target/jetty.socket");

        try {
            winstone = new Launcher(args);
        } catch (IOException ioe) {
            if (ioe.getCause() instanceof UnsupportedOperationException) {
                /* skip JDKs less than 16 */
                return;
            }
            throw ioe;
        }

        String path = ((UnixDomainServerConnector) winstone.server.getConnectors()[0])
                .getUnixDomainPath()
                .toString();

        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                makeRequest(path, "http://127.0.0.1:80/CountRequestsServlet", Protocol.HTTP_1));

        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    void testListenAddress() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        // see README development section for getting this to work on macOS
        args.put("httpListenAddress", "127.0.0.2");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        assertConnectionRefused("127.0.0.1", port);

        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                makeRequest("http://127.0.0.2:" + port + "/CountRequestsServlet", Protocol.HTTP_1));

        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    void writePortInFile(@TempDir Path tmp) throws Exception {
        Path portFile = tmp.resolve("subdir/jenkins.port");
        Future<Integer> futurePort = Executors.newSingleThreadExecutor().submit(() -> {
            Map<String, String> args = new HashMap<>();
            args.put("warfile", "target/test-classes/test.war");
            args.put("prefix", "/");
            args.put("httpPort", "0");
            System.setProperty(WINSTONE_PORT_FILE_NAME_PROPERTY, portFile.toString());
            try {
                winstone = new Launcher(args);
                return ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
            } finally {
                System.clearProperty(WINSTONE_PORT_FILE_NAME_PROPERTY);
            }
        });

        Awaitility.await()
                .pollInterval(1, TimeUnit.MICROSECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> Files.exists(portFile));
        String portFileString = Files.readString(portFile, StandardCharsets.UTF_8);
        assertFalse(portFileString.isEmpty(), "Port value should not be empty at any time");
        assertEquals(Integer.toString(futurePort.get()), portFileString);
        assertNotEquals(8080, futurePort.get().longValue());
    }

    @Test
    void helloSuspiciousPathCharacters() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();

        assertEquals(
                "<html><body>Hello winstone </body></html>\r\n",
                makeRequest("http://127.0.0.1:" + port + "/hello/winstone", Protocol.HTTP_1));

        // %5C == \
        assertEquals(
                "<html><body>Hello win\\stone </body></html>\r\n",
                makeRequest(
                        "http://127.0.0.1:" + port + "/hello/"
                                + URLEncoder.encode("win\\stone", StandardCharsets.UTF_8),
                        Protocol.HTTP_1));
    }

    @Test
    void testH2cPriorKnowledge() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();

        try (HttpClient httpClient = getHttpClient()) {
            ContentResponse response = httpClient
                    .newRequest("http://127.0.0.1:" + port + "/CountRequestsServlet")
                    .version(HttpVersion.HTTP_2)
                    .send();
            assertEquals(HttpVersion.HTTP_2, response.getVersion(), "Response should use HTTP/2");
            assertEquals(HttpStatus.OK_200, response.getStatus());
            assertEquals(
                    "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                    response.getContentAsString());
        }
    }

    @Test
    void testH2cUpgradeWithHeaders() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();

        try (Socket client = new Socket("127.0.0.1", port)) {
            client.setSoTimeout(5000);
            OutputStream output = client.getOutputStream();
            output.write(("GET /CountRequestsServlet HTTP/1.1\r\n" + "Host: 127.0.0.1\r\n"
                            + "Connection: Upgrade, HTTP2-Settings\r\n"
                            + "Upgrade: h2c\r\n"
                            + "HTTP2-Settings: AAEAAEAAAAIAAAABAAMAAABkAAQBAAAAAAUAAEAA\r\n"
                            + "\r\n")
                    .getBytes(StandardCharsets.ISO_8859_1));
            output.flush();

            InputStream input = client.getInputStream();
            StringBuilder response = new StringBuilder();
            int crlfs = 0;
            while (true) {
                int read = input.read();
                if (read == '\r' || read == '\n') {
                    ++crlfs;
                } else {
                    crlfs = 0;
                }
                response.append((char) read);
                if (crlfs == 4) {
                    break;
                }
            }
            assertTrue(
                    response.toString().startsWith("HTTP/1.1 101 "),
                    "Expected 101 Switching Protocols but got: " + response);

            assertTrue(
                    response.toString().contains("Upgrade: h2c"),
                    "Expected Upgrade: h2c header in response but got: " + response);

            assertTrue(
                    response.toString().contains("Connection: Upgrade"),
                    "Expected Connection: Upgrade header in response but got: " + response);
        }
    }
}
