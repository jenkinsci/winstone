package winstone.accesslog;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import winstone.AbstractWinstoneTest;
import winstone.Launcher;

/**
 * @author Kohsuke Kawaguchi
 */
public class SimpleAccessLoggerTest extends AbstractWinstoneTest {
    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    @Test
    public void testSimpleConnection() throws Exception {
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
        // check the log file
        // check the log file every 100ms for 5s
        String text = "";
        for (int i = 0; i < 50; ++i) {
            Thread.sleep(100);
            text = Files.readString(logFile, StandardCharsets.UTF_8);
            if (!"".equals(text)) {
                break;
            }
        }
        assertEquals(String.format("127.0.0.1 - - GET /examples/CountRequestsServlet HTTP/1.1 200%n"), text);
    }
}
