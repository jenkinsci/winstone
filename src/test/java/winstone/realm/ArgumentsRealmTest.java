package winstone.realm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import winstone.AbstractWinstoneTest;
import winstone.Launcher;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class ArgumentsRealmTest extends AbstractWinstoneTest {
    @Test
    public void realm() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("argumentsRealm.passwd.joe","eoj");
        args.put("argumentsRealm.roles.joe","loginUser");
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request =
                HttpRequest.newBuilder(new URI("http://localhost:" + port + "/secure/secret.txt")).GET().build();
        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.statusCode());
        assertNotEquals("diamond", response.body());

        HttpClient client = HttpClient.newBuilder().authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("joe", "eoj".toCharArray());
            }
        }).build();
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("diamond", response.body());
    }
}
