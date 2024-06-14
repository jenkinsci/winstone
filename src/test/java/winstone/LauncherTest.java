package winstone;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
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
}
