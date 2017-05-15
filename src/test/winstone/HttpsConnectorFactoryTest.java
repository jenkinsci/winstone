package winstone;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpsConnectorFactoryTest extends AbstractWinstoneTest {
    @Test
    public void testHttps() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "59009");
        args.put("httpsListenAddress", "localhost");
        args.put("httpsPrivateKey", "src/ssl/server.key");
        args.put("httpsCertificate", "src/ssl/server.crt");
        winstone = new Launcher(args);

        assertConnectionRefused("127.0.0.2", 59009);

        request(new TrustManagerImpl());
    }

    private void request(X509TrustManager tm) throws Exception {
        HttpsURLConnection con = (HttpsURLConnection)new URL("https://localhost:59009/CountRequestsServlet").openConnection();
        con.setHostnameVerifier( ( s, sslSession ) -> true );
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new X509TrustManager[] {tm}, null);
        con.setSSLSocketFactory(ssl.getSocketFactory());
        IOUtils.toString(con.getInputStream());
    }

    /**
     * Without specifying the certificate and key, it uses the random key
     */
    @Test
    public void testHttpsRandomCert() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "59009");
        winstone = new Launcher(args);


        try {
            request(new TrustManagerImpl());
            fail("we should have generated a unique key");
        } catch (SSLHandshakeException e) {
            // expected
        }

        request(new TrustEveryoneManager());
    }
}
