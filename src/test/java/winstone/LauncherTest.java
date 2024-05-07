package winstone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Filter;
import java.util.logging.LogRecord;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

/**
 * @author Kohsuke Kawaguchi
 */
public class LauncherTest extends AbstractWinstoneTest {
    @Test
    public void mimeType() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("mimeTypes", "xxx=text/xxx");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/test.xxx"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("text/xxx", response.headers().firstValue("Content-Type").get());
        assertEquals("Hello", response.body());
    }

    @Test
    public void extraLibFolderDeprecation() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("extraLibFolder", "target/test-classes");
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("winstone");
        Filter orig = logger.getFilter();
        CapturingFilter filter = new CapturingFilter();
        logger.setFilter(filter);
        try {
            winstone = new Launcher(args);
            int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
            assertEquals(
                    "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n",
                    makeRequest("http://127.0.0.2:" + port + "/CountRequestsServlet"));
            assertThat(
                    filter.messages,
                    hasItem(
                            "You are using an extra library folder, support for which will end on or after January 1, 2023."));
        } finally {
            logger.setFilter(orig);
        }
    }

    static class CapturingFilter implements Filter {
        final Queue<String> messages = new ConcurrentLinkedQueue<>();

        @Override
        public boolean isLoggable(LogRecord record) {
            messages.offer(record.getMessage());
            return true;
        }
    }
}
