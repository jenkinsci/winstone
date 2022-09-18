package winstone.realm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.meterware.httpunit.AuthorizationRequiredException;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import winstone.AbstractWinstoneTest;
import winstone.Launcher;

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
        assertThrows(AuthorizationRequiredException.class, () -> makeRequest("http://localhost:" + port + "/secure/secret.txt"));

        wc.setAuthorization("joe","eoj");
        assertEquals("diamond", makeRequest("http://localhost:"+port+"/secure/secret.txt"));
    }
}
