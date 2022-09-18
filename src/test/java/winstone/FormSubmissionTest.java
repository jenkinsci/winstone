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
import org.jvnet.hudson.test.Issue;

public class FormSubmissionTest extends AbstractWinstoneTest {

    @Issue("JENKINS-60409")
    @Test
    public void largeForm() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpListenAddress", "127.0.0.2");
        /* To see it fail:
        args.put("requestFormContentSize", "999");
        */
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        for (int size = 1; size <= 9_999_999; size *= 3) {
            System.out.println("trying size " + size);
            HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/AcceptFormServlet"))
                    .POST(HttpRequest.BodyPublishers.ofString("x=" + ".".repeat(size)))
                    .build();
            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
            assertEquals("correct response at size " + size, "received " + (size + "x=".length()) + " bytes", response.body());
        }
    }

}
