/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */

package winstone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import javax.net.ssl.KeyManagerFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.RSAPrivateKeySpec;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 */
public abstract class AbstractSecuredConnectorFactory implements ConnectorFactory
{
    protected static final WinstoneResourceBundle SSL_RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    protected KeyStore keystore;
    protected String keystorePassword;

    protected void configureSsl( Map<String, String> args, Server server ) throws IOException
    {
        try {
            File opensslCert = Option.HTTPS_CERTIFICATE.get( args);
            File opensslKey =  Option.HTTPS_PRIVATE_KEY.get(args);
            File keyStore =    Option.HTTPS_KEY_STORE.get(args);
            String pwd =       Option.HTTPS_KEY_STORE_PASSWORD.get(args);

            if ((opensslCert!=null ^ opensslKey!=null))
                throw new WinstoneException(
                    MessageFormat.format( "--{0} and --{1} need to be used together", Option.HTTPS_CERTIFICATE, Option.HTTPS_PRIVATE_KEY));
            if (keyStore!=null && opensslKey!=null)
                throw new WinstoneException(MessageFormat.format("--{0} and --{1} are mutually exclusive", Option.HTTPS_KEY_STORE, Option.HTTPS_PRIVATE_KEY));

            if (keyStore!=null) {
                // load from default Keystore
                if (!keyStore.exists() || !keyStore.isFile())
                    throw new WinstoneException(SSL_RESOURCES.getString(
                        "HttpsListener.KeyStoreNotFound", keyStore.getPath()));

                this.keystorePassword = pwd;

                keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                try(InputStream inputStream = new FileInputStream(keyStore)){
                    keystore.load( inputStream, this.keystorePassword.toCharArray());
                }
            } else if (opensslCert!=null) {
                // load from openssl style key files
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                try (InputStream inputStream = new FileInputStream(opensslCert)) {
                    Certificate cert = cf.generateCertificate(inputStream);
                    PrivateKey key = readPEMRSAPrivateKey(opensslKey);

                    this.keystorePassword = "changeit";
                    keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                    keystore.load( null );
                    keystore.setKeyEntry( "hudson", key, keystorePassword.toCharArray(), new Certificate[]{ cert } );
                }
            } else {
                throw new WinstoneException(MessageFormat.format("Please set --{0}", Option.HTTPS_KEY_STORE));
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to handle keys", e);
        }
    }


    private static PrivateKey readPEMRSAPrivateKey(File opensslKey) throws IOException, GeneralSecurityException {
        // TODO: should have more robust format error handling
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (BufferedReader r = Files.newBufferedReader(opensslKey.toPath(), StandardCharsets.US_ASCII)) {
                String line;
                boolean in = false;
                while ( ( line = r.readLine() ) != null ) {
                    if ( line.startsWith( "-----" ) ) {
                        in = !in;
                        continue;
                    }
                    if ( in ) {
                        baos.write(Base64.getDecoder().decode(line.trim()));
                    }
                }
            } catch (InvalidPathException e) {
                throw new IOException(e);
            }

            BigInteger mod, privExpo;
            try {
                Class<?> disC = Class.forName( "sun.security.util.DerInputStream" );
                Object dis = disC.getConstructor( byte[].class ).newInstance( (Object) baos.toByteArray() );
                Object[] seq = (Object[]) disC.getMethod( "getSequence", int.class ).invoke( dis, 0 );
                Method getBigInteger = seq[0].getClass().getMethod( "getBigInteger" );
                // int v = seq[0].getInteger();
                mod = (BigInteger) getBigInteger.invoke( seq[1] );
                // pubExpo
                // p1, p2, exp1, exp2, crtCoef
                privExpo = (BigInteger) getBigInteger.invoke( seq[3] );
            } catch ( Exception x ) {
                throw new WinstoneException( SSL_RESOURCES.getString( "HttpsConnectorFactory.LoadPrivateKeyError" ), x );
            }
            Logger.log( Level.WARNING, SSL_RESOURCES, "HttpsConnectorFactory.LoadPrivateKey" );

            KeyFactory kf = KeyFactory.getInstance( "RSA" );
            return kf.generatePrivate( new RSAPrivateKeySpec( mod, privExpo ) );
        }
    }

    /**
     * Used to get the base ssl context in which to create the server socket.
     * This is basically just so we can have a custom location for key stores.
     */
    protected SslContextFactory.Server getSSLContext( Map<String, String> args) {
        try {
            String privateKeyPassword;

            // There are many legacy setups in which the KeyStore password and the
            // key password are identical and people will not even be aware that these
            // are two different things
            // Therefore if no httpsPrivateKeyPassword is explicitly set we try to
            // use the KeyStore password also for the key password not to break
            // backward compatibility
            // Otherwise the following code will completely break the startup of
            // Jenkins in case the --httpsPrivateKeyPassword parameter is not set
            privateKeyPassword = Option.HTTPS_PRIVATE_KEY_PASSWORD.get(args, keystorePassword);

            // Dump the content of the keystore if log level is FULL_DEBUG
            // Note: The kmf is instantiated here only to access the keystore,
            // the SslContextFactory will instantiate its own KeyManager
            KeyManagerFactory kmf = KeyManagerFactory.getInstance( Option.HTTPS_KEY_MANAGER_TYPE.get( args));

            // In case the KeyStore password and the KeyPassword are not the same,
            // the KeyManagerFactory needs the KeyPassword because it will access the individual key(s)
            kmf.init(keystore, keystorePassword.toCharArray());
            Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                       "HttpsListener.KeyCount", keystore.size() + "");
            for ( Enumeration<String> e = keystore.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                           "HttpsListener.KeyFound", alias,
                           keystore.getCertificate(alias) + "");
            }

            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setKeyStore(keystore);
            ssl.setKeyStorePassword(keystorePassword);
            ssl.setKeyManagerPassword(privateKeyPassword);
            ssl.setKeyManagerFactoryAlgorithm(Option.HTTPS_KEY_MANAGER_TYPE.get(args));
            ssl.setCertAlias(Option.HTTPS_CERTIFICATE_ALIAS.get(args));
            ssl.setExcludeProtocols("SSLv3", "SSLv2", "SSLv2Hello");
            String excludeCiphers = Option.HTTPS_EXCLUDE_CIPHER_SUITES.get(args);
            if(excludeCiphers!=null&&excludeCiphers.length()>0) {
                String[] cipherSuites = excludeCiphers.split(",");
                ssl.setExcludeCipherSuites(cipherSuites);
            }
            Logger.log( Logger.INFO, SSL_RESOURCES, //
                        "HttpsListener.ExcludeCiphers", //
                        Arrays.asList(ssl.getExcludeCipherSuites()));

            /*
             * If true, request the client certificate ala "SSLVerifyClient require" Apache directive.
             * If false, which is the default, don't do so.
             * Technically speaking, there's the equivalent of "SSLVerifyClient optional", but IE doesn't
             * recognize it and it always prompt the certificate chooser dialog box, so in practice
             * it's useless.
             * <p>
             * See http://hudson.361315.n4.nabble.com/winstone-container-and-ssl-td383501.html for this failure mode in IE.
             */
            ssl.setNeedClientAuth(Option.HTTPS_VERIFY_CLIENT.get(args));
            return ssl;
        } catch (Throwable err) {
            throw new WinstoneException(SSL_RESOURCES
                                            .getString("HttpsListener.ErrorGettingContext"), err);
        }
    }

}
