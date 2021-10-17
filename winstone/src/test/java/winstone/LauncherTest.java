package winstone;

import com.meterware.httpunit.WebResponse;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import winstone.Launcher;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class LauncherTest extends AbstractWinstoneTest {
    @Test
    public void mimeType() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("mimeTypes", "xxx=text/xxx");
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        WebResponse r = wc.getResponse("http://127.0.0.2:"+port+"/test.xxx");
        assertEquals("text/xxx",r.getContentType());
        assertEquals("Hello",r.getText());
    }

}
