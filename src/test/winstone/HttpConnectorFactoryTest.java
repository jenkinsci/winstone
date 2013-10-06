package winstone;

import org.junit.Test;

import java.net.Socket;
import java.net.ConnectException;
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

        try {
            new Socket("127.0.0.1",59009);
            fail("shouldn't be listening on 127.0.0.1");
        } catch (ConnectException e) {
            // expected
        }

        makeRequest("http://127.0.0.2:59009/CountRequestsServlet");
    }
}
