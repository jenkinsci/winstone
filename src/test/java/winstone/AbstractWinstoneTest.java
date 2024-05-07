package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Path;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ClientConnector;
import org.junit.After;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest {
    protected Launcher winstone;

    @After
    public void tearDown() {
        if (winstone != null) winstone.shutdown();
    }

    public String makeRequest(String url) throws Exception {
        return makeRequest(null, url);
    }

    public String makeRequest(String path, String url) throws Exception {

        HttpClient httpClient;

        if (path != null) {
            Path unixDomainPath = Path.of(path);
            ClientConnector clientConnector = ClientConnector.forUnixDomain(unixDomainPath);
            httpClient = new HttpClient(new HttpClientTransportDynamic(clientConnector));
        } else {
            httpClient = new HttpClient();
        }
        httpClient.start();

        ContentResponse response = httpClient.GET(url);

        httpClient.stop();

        assertEquals(HttpStatus.OK_200, response.getStatus());
        return response.getContentAsString();
    }

    protected void assertConnectionRefused(String host, int port) {
        assertThrows(ConnectException.class, () -> {
            try (Socket s = new Socket(host, port)) {
                // shouldn't be listening on 127.0.0.1
            }
        });
    }
}
