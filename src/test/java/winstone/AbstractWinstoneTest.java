package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Path;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.After;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest {
    protected Launcher winstone;

    @After
    public void tearDown() {
        if (winstone != null) {
            winstone.shutdown();
        }
    }

    enum Protocol {
        HTTP_1,
        HTTP_2;
    }

    /**
     * please use {@link #makeRequest(String, String, Protocol)}
     */
    @Deprecated
    public String makeRequest(String url) throws Exception {
        return makeRequest(null, url, Protocol.HTTP_1);
    }

    /**
     * please use {@link #makeRequest(String, String, Protocol)}
     */
    @Deprecated
    public String makeRequest(String path, String url) throws Exception {
        return makeRequest(path, url, Protocol.HTTP_1);
    }

    /**
     *
     * @param path path to unix domain socket (can be null if not using unix domain socket)
     * @param url the URL to request
     * @param protocol see #Protocol
     * @return the response
     * @throws Exception
     */
    public String makeRequest(String path, String url, Protocol protocol) throws Exception {
        return makeRequest(path, url, HttpStatus.OK_200, protocol);
    }

    /**
     *
     * @param path path to unix domain socket (can be null if not using unix domain socket)
     * @param url the URL to request
     * @param expectedHttpStatus the expected http response code from the server
     * @param protocol see #Protocol
     * @return the response
     * @throws Exception
     */
    public String makeRequest(String path, String url, int expectedHttpStatus, Protocol protocol) throws Exception {

        HttpClient httpClient = getHttpClient(path);

        Request request = httpClient.newRequest(url);

        switch (protocol) {
            case HTTP_1 -> request.version(HttpVersion.HTTP_1_1);
            case HTTP_2 -> request.version(HttpVersion.HTTP_2);
            default -> throw new Exception("Unsupported Http Version: " + protocol);
        }

        ContentResponse response = request.send();

        httpClient.stop();

        assertEquals(expectedHttpStatus, response.getStatus());
        return response.getContentAsString();
    }

    protected HttpClient getHttpClient(String path) throws Exception {
        HttpClient httpClient;

        ClientConnector connector;

        if (path != null) {
            Path unixDomainPath = Path.of(path);
            connector = ClientConnector.forUnixDomain(unixDomainPath);
        } else {
            SslContextFactory.Client sslContextFactory = new SslContextFactory.Client(true);
            sslContextFactory.setHostnameVerifier((hostname, session) -> true);

            connector = new ClientConnector();
            connector.setSslContextFactory(sslContextFactory);
        }

        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        HTTP2Client http2Client = new HTTP2Client(connector);
        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);

        HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector, http1, http2);
        httpClient = new HttpClient(transport);
        httpClient.setFollowRedirects(false);
        httpClient.start();
        return httpClient;
    }

    protected void assertConnectionRefused(String host, int port) {
        assertThrows(ConnectException.class, () -> {
            try (Socket s = new Socket(host, port)) {
                // shouldn't be listening on 127.0.0.1
            }
        });
    }
}
