package winstone.accesslog;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import winstone.AbstractWinstoneTest;
import winstone.Launcher;

/**
 * @author Kohsuke Kawaguchi
 */
class SimpleAccessLoggerTest extends AbstractWinstoneTest {
    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    @Test
    void testSimpleConnection() throws Exception {
        Path logFile = Paths.get("target/test.log");
        Files.deleteIfExists(logFile);

        // Initialise container
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/examples");
        args.put("httpPort", "0");
        args.put("accessLoggerClassName", SimpleAccessLogger.class.getName());
        args.put("simpleAccessLogger.file", logFile.toAbsolutePath().toString());
        args.put("simpleAccessLogger.format", "###ip### - ###user### ###uriLine### ###status###");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        // make a request
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                makeRequest("http://localhost:" + port + "/examples/CountRequestsServlet", Protocol.HTTP_1));
        // check the log file with awaitility
        String expected = String.format("127.0.0.1 - - GET /examples/CountRequestsServlet HTTP/1.1 200%n");
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertEquals(expected, Files.readString(logFile, StandardCharsets.UTF_8)));
    }
}
