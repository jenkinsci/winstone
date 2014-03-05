package winstone;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import dynamiccompilation.DynamicInMemoryCompileHelper;

public class SslKeyFactoryForIbmJdkDelegateImpl implements SslKeyFactory {
    
    private SslKeyFactory  instance;
    
    public SslKeyFactoryForIbmJdkDelegateImpl(){
        final InputStream javacodeSrc = SslKeyFactory.class.getResourceAsStream("/winstone/SslKeyFactoryForIbmJdkImpl.javasrc");
         try {
            instance = (SslKeyFactory)DynamicInMemoryCompileHelper.compileAndNewInstance("winstone.SslKeyFactoryForIbmJdkImpl", javacodeSrc);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e.getMessage(),e);
        }
    }

    public PrivateKey getPrivateKey(byte[] data) throws IOException, GeneralSecurityException {
        return instance.getPrivateKey(data);
    }

    public KeyStore createKeyStore(char[] password) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidKeyException, IOException, CertificateException,
            SignatureException, KeyStoreException {
        return instance.createKeyStore(password);
    }

    public String getHttpsKeyManagerType() {
        return instance.getHttpsKeyManagerType();
    };

}
