package winstone;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import winstone.Launcher;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.jvnet.hudson.test.Issue;

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

    @Issue("JENKINS-60857")
    @Test
    public void wildcard() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "-1");
        args.put("httpsPort", "59009");
        args.put("httpsListenAddress", "localhost");
        args.put("httpsKeyStore", "src/ssl/wildcard.jks");
        args.put("httpsKeyStorePassword", "changeit");
        winstone = new Launcher(args);
        request(new TrustEveryoneManager());
    }

    @Test
    public void httpRedirect() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "59008");
        args.put("httpsPort", "59009");
        args.put("httpsRedirectHttp", "true");
        winstone = new Launcher(args);
        requestRedirect(new TrustEveryoneManager());

        // also verify that directly accessing the resource works.
        request(new TrustEveryoneManager());
    }

    private void requestRedirect(X509TrustManager tm) throws Exception {
        HttpURLConnection con = (HttpURLConnection)new URL("http://localhost:59008/CountRequestsServlet").openConnection();
        assertEquals(302, con.getResponseCode());
        assertTrue("Should have a Location header of the resource", con.getHeaderFields().containsKey("Location"));
        String newUrl = con.getHeaderField("Location");
        assertNotNull(newUrl);
        assertTrue(newUrl.contains("https"));
        assertTrue(newUrl.contains("59009"));
        HttpsURLConnection secureCon = (HttpsURLConnection)new URL(newUrl).openConnection();
        secureCon.setHostnameVerifier( ( s, sslSession ) -> true );
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new X509TrustManager[] {tm}, null);
        secureCon.setSSLSocketFactory(ssl.getSocketFactory());
        IOUtils.toString(secureCon.getInputStream());
    }

}
