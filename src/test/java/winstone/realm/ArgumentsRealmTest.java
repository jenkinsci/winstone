package winstone.realm;

import com.meterware.httpunit.AuthorizationRequiredException;
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
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "10059");
        args.put("argumentsRealm.passwd.joe","eoj");
        args.put("argumentsRealm.roles.joe","loginUser");
        winstone = new Launcher(args);

        try {
            makeRequest("http://localhost:10059/secure/secret.txt");
            fail("should require authentication");
        } catch (AuthorizationRequiredException e) {
            // expected
        }

        wc.setAuthorization("joe","eoj");
        assertEquals("diamond", makeRequest("http://localhost:10059/secure/secret.txt"));
    }
}
