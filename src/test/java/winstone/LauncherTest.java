package winstone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

/**
 * @author Kohsuke Kawaguchi
 */
public class LauncherTest extends AbstractWinstoneTest {
    @Test
    public void implicitGzip() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        verifyGzip(true);
    }

    @Test
    public void explicitGzip() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("compression", "gzip");
        winstone = new Launcher(args);
        verifyGzip(true);
    }

    @Test
    public void noCompression() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("compression", "none");
        winstone = new Launcher(args);
        verifyGzip(false);
    }

    private void verifyGzip(boolean gzip) throws Exception {
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/lipsum.txt"))
                .GET()
                .header("Accept-Encoding", "gzip")
                .build();
        HttpResponse<InputStream> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElseThrow());
        InputStream is;
        if (gzip) {
            assertEquals(
                    "gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
            is = new GZIPInputStream(response.body());
        } else {
            assertFalse(response.headers().firstValue("Content-Encoding").isPresent());
            is = response.body();
        }
        try {
            assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), startsWith("Lorem ipsum dolor sit amet"));
        } finally {
            is.close();
        }
    }

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
