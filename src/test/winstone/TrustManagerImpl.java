package winstone;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
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

    public SSLContext createSSLContext() throws Exception {
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new X509TrustManager[] {this}, null);
        return ssl;
    }

    /**
     * Installs this trust manager globally to the VM as the default.
     */
    public void loadGlobally() throws Exception {
        HttpsURLConnection.setDefaultSSLSocketFactory(createSSLContext().getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });
    }
}