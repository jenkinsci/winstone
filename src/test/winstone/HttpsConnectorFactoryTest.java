package winstone;

import org.junit.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpsConnectorFactoryTest extends AbstractWinstoneTest {
    @Test
    public void testHttps() throws Exception {
        SSLSocketFactory d = HttpsURLConnection.getDefaultSSLSocketFactory();
        try {
            Map<String,String> args = new HashMap<String,String>();
            args.put("warfile", "target/test-classes/test.war");
            args.put("prefix", "/");
            args.put("httpPort", "-1");
            args.put("httpsPort", "59009");
            args.put("httpsListenAddress", "localhost");
            args.put("httpsPrivateKey", "src/ssl/server.key");
            args.put("httpsCertificate", "src/ssl/server.crt");
            winstone = new Launcher(args);

            new TrustManagerImpl().loadGlobally();
            new URL("https://localhost:59009/CountRequestsServlet").openConnection(); // not sure why this is needed

            assertConnectionRefused("127.0.0.2", 59009);
            makeRequest("https://localhost:59009/CountRequestsServlet");
        } finally {
            HttpsURLConnection.setDefaultSSLSocketFactory(d);
        }
    }

}
