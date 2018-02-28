package winstone;

import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * {@link X509TrustManager} that only recognizes our self-signed certificate.
 *
 * @author Kohsuke Kawaguchi
 */
public class TrustManagerImpl implements X509TrustManager {
    private X509Certificate cert;

    public TrustManagerImpl() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        cert = (X509Certificate) cf.generateCertificate(new FileInputStream("src/ssl/server.crt"));
    }

    public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        throw new UnsupportedOperationException("Client trust not supported");
    }

    public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        for (X509Certificate x509Certificate : xcs) {
            System.out.println("certificate: " + x509Certificate.getIssuerX500Principal().getName());
            if (cert.getSubjectX500Principal().equals(x509Certificate.getIssuerX500Principal()))
                return;
        }

        throw new CertificateException("Untrusted certificate?");
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[]{cert};
    }
}