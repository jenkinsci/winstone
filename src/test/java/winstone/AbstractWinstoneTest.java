package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.After;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest {
    protected Launcher winstone;

    @After
    public void tearDown() {
        if (winstone!=null)
            winstone.shutdown();
    }

    public String makeRequest(String url)
            throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(new URI(url)).GET().build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        return response.body();
    }

    protected void assertConnectionRefused(String host, int port) {
        assertThrows(ConnectException.class, () -> {
            try (Socket s = new Socket(host, port)) {
                // shouldn't be listening on 127.0.0.1
            }
        });
    }
}
