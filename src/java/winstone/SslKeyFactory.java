package winstone;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;

public interface SslKeyFactory {
    
    public PrivateKey getPrivateKey(byte[] data) throws IOException, GeneralSecurityException;

    public KeyStore createKeyStore(char[] password) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, IOException, CertificateException,
            SignatureException, KeyStoreException;
    
    public String getHttpsKeyManagerType();
}
