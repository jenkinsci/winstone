/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import winstone.Launcher;
import winstone.Logger;

/**
 * Test case for the Http Connector to Winstone. Simulates a simple connect and
 * retrieve case, then a keep-alive connection case.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 */
class HttpConnectorTest {
    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    @Test
    void testSimpleConnection() throws URISyntaxException, IOException, InterruptedException {
        // Initialise container
        Map<String, String> args = new HashMap<>();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10003");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Level.FINEST, true);
        Launcher winstone = new Launcher(args);

        // Check for a simple connection
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:10003/examples/CountRequestsServlet"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\n", response.body());
        winstone.shutdown();
        Thread.sleep(500);
    }

    /**
     * Test the keep alive case
     */
    @Test
    void testWriteAfterServlet() throws URISyntaxException, IOException, InterruptedException {
        // Initialise container
        Map<String, String> args = new HashMap<>();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10005");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Level.FINEST, true);
        Launcher winstone = new Launcher(args);

        // Check for a simple connection
        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:10005/examples/TestWriteAfterServlet"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals(
                "<html><body>This servlet has been accessed via GET 1001 times</body></html>\r\nHello",
                response.body());
        winstone.shutdown();
        Thread.sleep(500);
    }
}
