/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */

package winstone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

import javax.net.ssl.KeyManagerFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.text.MessageFormat;
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

    protected void configureSsl( Map args, Server server ) throws IOException
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
                // load from Java style JKS
                if (!keyStore.exists() || !keyStore.isFile())
                    throw new WinstoneException(SSL_RESOURCES.getString(
                        "HttpsListener.KeyStoreNotFound", keyStore.getPath()));

                this.keystorePassword = pwd;

                keystore = KeyStore.getInstance("JKS");
                keystore.load( new FileInputStream( keyStore), this.keystorePassword.toCharArray());
            } else if (opensslCert!=null) {
                // load from openssl style key files
                CertificateFactory cf = CertificateFactory.getInstance( "X509");
                Certificate cert = cf.generateCertificate( new FileInputStream( opensslCert));
                PrivateKey key = readPEMRSAPrivateKey( new FileReader( opensslKey));

                this.keystorePassword = "changeit";
                keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
                keystore.setKeyEntry("hudson", key, keystorePassword.toCharArray(), new Certificate[]{cert});
            } else {
                // use self-signed certificate
                this.keystorePassword = "changeit";
                System.out.println("Using one-time self-signed certificate");

                X509Certificate cert;
                PrivateKey privKey;
                Object ckg;

                try { // TODO switch to (shaded?) Bouncy Castle
                    // TODO: Cleanup when JDK 7 support is removed.
                    try {
                        ckg = Class.forName("sun.security.x509.CertAndKeyGen").getDeclaredConstructor(String.class, String.class, String.class).newInstance("RSA", "SHA1WithRSA", null);
                    } catch (ClassNotFoundException cnfe) {
                        // Java 8
                        ckg = Class.forName("sun.security.tools.keytool.CertAndKeyGen").getDeclaredConstructor(String.class, String.class, String.class).newInstance("RSA", "SHA1WithRSA", null);
                    }
                    ckg.getClass().getDeclaredMethod("generate", int.class).invoke(ckg, 1024);
                    privKey = (PrivateKey) ckg.getClass().getMethod("getPrivateKey").invoke(ckg);
                    Class<?> x500Name = Class.forName("sun.security.x509.X500Name");
                    Object xn = x500Name.getConstructor(String.class, String.class, String.class, String.class).newInstance("Test site", "Unknown", "Unknown", "Unknown");
                    cert = (X509Certificate) ckg.getClass().getMethod("getSelfCertificate", x500Name, long.class).invoke(ckg, xn, 3650L * 24 * 60 * 60);
                } catch (Exception x) {
                    throw new WinstoneException(SSL_RESOURCES.getString("HttpsConnectorFactory.SelfSignedError"), x);
                }
                Logger.log( Level.WARNING, SSL_RESOURCES, "HttpsConnectorFactory.SelfSigned");

                keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
                keystore.setKeyEntry("hudson", privKey, keystorePassword.toCharArray(), new Certificate[]{cert});
            }
        } catch (GeneralSecurityException e) {
            throw (IOException)new IOException("Failed to handle keys").initCause(e);
        }
    }


    private static PrivateKey readPEMRSAPrivateKey(Reader reader) throws IOException, GeneralSecurityException {
        // TODO: should have more robust format error handling
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            try (BufferedReader r = new BufferedReader( reader ))
            {
                String line;
                boolean in = false;
                while ( ( line = r.readLine() ) != null )
                {
                    if ( line.startsWith( "-----" ) )
                    {
                        in = !in;
                        continue;
                    }
                    if ( in )
                    {
                        baos.write( B64Code.decode( line ) );
                    }
                }
            }
            finally
            {
                reader.close();
            }

            BigInteger mod, privExpo;
            try
            {
                Class<?> disC = Class.forName( "sun.security.util.DerInputStream" );
                Object dis = disC.getConstructor( byte[].class ).newInstance( (Object) baos.toByteArray() );
                Object[] seq = (Object[]) disC.getMethod( "getSequence", int.class ).invoke( dis, 0 );
                Method getBigInteger = seq[0].getClass().getMethod( "getBigInteger" );
                // int v = seq[0].getInteger();
                mod = (BigInteger) getBigInteger.invoke( seq[1] );
                // pubExpo
                // p1, p2, exp1, exp2, crtCoef
                privExpo = (BigInteger) getBigInteger.invoke( seq[3] );
            }
            catch ( Exception x )
            {
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
    protected SslContextFactory getSSLContext( Map args) {
        try {
            String privateKeyPassword;

            // There are many legacy setups in which the KeyStore password and the
            // key password are identical and people will not even be aware that these
            // are two different things
            // Therefore if no httpsPrivateKeyPassword is explicitely set we try to
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
            for ( Enumeration e = keystore.aliases(); e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                           "HttpsListener.KeyFound", alias,
                           keystore.getCertificate(alias) + "");
            }

            SslContextFactory ssl = new SslContextFactory();

            ssl.setKeyStore(keystore);
            ssl.setKeyStorePassword(keystorePassword);
            ssl.setKeyManagerPassword(privateKeyPassword);
            ssl.setKeyManagerFactoryAlgorithm(Option.HTTPS_KEY_MANAGER_TYPE.get(args));
            ssl.setCertAlias(Option.HTTPS_CERTIFICATE_ALIAS.get(args));
            ssl.setExcludeProtocols("SSLv3", "SSLv2", "SSLv2Hello");


            /**
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
