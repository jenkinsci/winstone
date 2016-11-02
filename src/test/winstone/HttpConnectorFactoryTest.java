package winstone;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpConnectorFactoryTest extends AbstractWinstoneTest {

    @Test
    public void testListenAddress() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "59009");
        args.put("httpListenAddress", "127.0.0.2");
        winstone = new Launcher(args);

        assertConnectionRefused("127.0.0.1",59009);

        makeRequest("http://127.0.0.2:59009/CountRequestsServlet");
    }

    @Test
    public void sendServerVersion() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "59009");
        args.put("httpListenAddress", "127.0.0.1");
        args.put("sendServerVersion", "false");
        winstone = new Launcher(args);

        String header = responseHeader("http://127.0.0.1:59009/CountRequestsServlet", "Server");

        assertNull("Server header should not be returned", header);
    }
}
