package winstone;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * @author Kohsuke Kawaguchi
 */
public class TrustEveryoneManager implements X509TrustManager {
    public TrustEveryoneManager() throws Exception {}

    @Override
    public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
