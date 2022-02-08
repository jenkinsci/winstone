package winstone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpConnectorFactoryTest extends AbstractWinstoneTest {

    @Test
    public void testListenAddress() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        // see README development section for getting this to work on macOS
        args.put("httpListenAddress", "127.0.0.2");
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        assertConnectionRefused("127.0.0.1",port);

        makeRequest("http://127.0.0.2:"+port+"/CountRequestsServlet");

        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }
}
