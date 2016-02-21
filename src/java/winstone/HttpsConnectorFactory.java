/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.B64Code;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;
import winstone.cmdline.Option;

import javax.net.ssl.KeyManagerFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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

/**
 * Implements the main listener daemon thread. This is the class that gets
 * launched by the command line, and owns the server socket, etc.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpsConnectorFactory.java,v 1.10 2007/06/13 15:27:35 rickknowles Exp $
 */
public class HttpsConnectorFactory implements ConnectorFactory {
    private static final WinstoneResourceBundle SSL_RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    private KeyStore keystore;
    private String keystorePassword;

    public boolean start(Map args, Server server) throws IOException {
        int listenPort = Option.HTTPS_PORT.get(args);
        String listenAddress = Option.HTTPS_LISTEN_ADDRESS.get(args);
        int keepAliveTimeout = Option.HTTPS_KEEP_ALIVE_TIMEOUT.get(args);

        if (listenPort<0) {
            // not running HTTPS listener
            return false;
        }

        try {
            File opensslCert = Option.HTTPS_CERTIFICATE.get(args);
            File opensslKey =  Option.HTTPS_PRIVATE_KEY.get(args);
            File keyStore =    Option.HTTPS_KEY_STORE.get(args);
            String pwd =       Option.HTTPS_KEY_STORE_PASSWORD.get(args);

            if ((opensslCert!=null ^ opensslKey!=null))
                throw new WinstoneException(MessageFormat.format("--{0} and --{1} need to be used together", Option.HTTPS_CERTIFICATE, Option.HTTPS_PRIVATE_KEY));
            if (keyStore!=null && opensslKey!=null)
                throw new WinstoneException(MessageFormat.format("--{0} and --{1} are mutually exclusive", Option.HTTPS_KEY_STORE, Option.HTTPS_PRIVATE_KEY));

            if (keyStore!=null) {
                // load from Java style JKS
                if (!keyStore.exists() || !keyStore.isFile())
                    throw new WinstoneException(SSL_RESOURCES.getString(
                            "HttpsListener.KeyStoreNotFound", keyStore.getPath()));

                this.keystorePassword = pwd;

                keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream(keyStore), this.keystorePassword.toCharArray());
            } else if (opensslCert!=null) {
                // load from openssl style key files
                CertificateFactory cf = CertificateFactory.getInstance("X509");
                Certificate cert = cf.generateCertificate(new FileInputStream(opensslCert));
                PrivateKey key = readPEMRSAPrivateKey(new FileReader(opensslKey));

                this.keystorePassword = "changeit";
                keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
                keystore.setKeyEntry("hudson", key, keystorePassword.toCharArray(), new Certificate[]{cert});
            } else {
                // use self-signed certificate
                this.keystorePassword = "changeit";
                System.out.println("Using one-time self-signed certificate");

                CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
                ckg.generate(1024);
                PrivateKey privKey = ckg.getPrivateKey();

                X500Name xn = new X500Name("Test site", "Unknown", "Unknown", "Unknown");
                X509Certificate cert = ckg.getSelfCertificate(xn, 3650L * 24 * 60 * 60);

                keystore = KeyStore.getInstance("JKS");
                keystore.load(null);
                keystore.setKeyEntry("hudson", privKey, keystorePassword.toCharArray(), new Certificate[]{cert});
            }
        } catch (GeneralSecurityException e) {
            throw (IOException)new IOException("Failed to handle keys").initCause(e);
        }

        ServerConnector connector = createConnector(server,args);
        connector.setPort(listenPort);
        connector.setHost(listenAddress);
        connector.setIdleTimeout(keepAliveTimeout);

        HttpConfiguration config = connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        config.addCustomizer(new ForwardedRequestCustomizer());
        config.setRequestHeaderSize(Option.REQUEST_HEADER_SIZE.get(args));

        server.addConnector(connector);

        return true;
    }

    private ServerConnector createConnector(Server server, Map args) {
        SslContextFactory sslcf = getSSLContext(args);
        if (Option.HTTPS_SPDY.get(args)) {// based on http://wiki.eclipse.org/Jetty/Feature/SPDY
            try {
                sslcf.setIncludeProtocols("TLSv1");
                return (ServerConnector)Class.forName("org.eclipse.jetty.spdy.server.http.HTTPSPDYServerConnector")
                        .getConstructor(Server.class, SslContextFactory.class)
                        .newInstance(server,sslcf);
            } catch (NoClassDefFoundError e) {
                if (e.getMessage().contains("org/eclipse/jetty/npn")) {
                    // a typically error is to forget to run NPN
                    throw new WinstoneException(SSL_RESOURCES.getString("HttpsListener.MissingNPN"), e);
                }
                throw e;
            } catch (Exception e) {
                throw new Error("Failed to enable SPDY connector",e);
            }
        } else
            return new ServerConnector(server,sslcf);
    }

    private static PrivateKey readPEMRSAPrivateKey(Reader reader) throws IOException, GeneralSecurityException {
        // TODO: should have more robust format error handling
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BufferedReader r = new BufferedReader(reader);
            String line;
            boolean in = false;
            while ((line=r.readLine())!=null) {
                if (line.startsWith("-----")) {
                    in = !in;
                    continue;
                }
                if (in) {
                    baos.write(B64Code.decode(line));
                }
            }
        } finally {
            reader.close();
        }


        DerInputStream dis = new DerInputStream(baos.toByteArray());
        DerValue[] seq = dis.getSequence(0);

        // int v = seq[0].getInteger();
        BigInteger mod = seq[1].getBigInteger();
        // pubExpo
        BigInteger privExpo = seq[3].getBigInteger();
        // p1, p2, exp1, exp2, crtCoef

        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate (new RSAPrivateKeySpec(mod,privExpo));
    }

    /**
     * Used to get the base ssl context in which to create the server socket.
     * This is basically just so we can have a custom location for key stores.
     */
    SslContextFactory getSSLContext(Map args) {
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
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(Option.HTTPS_KEY_MANAGER_TYPE.get(args));

            // In case the KeyStore password and the KeyPassword are not the same,
            // the KeyManagerFactory needs the KeyPassword because it will access the individual key(s)
            kmf.init(keystore, keystorePassword.toCharArray());
            Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                    "HttpsListener.KeyCount", keystore.size() + "");
            for (Enumeration e = keystore.aliases(); e.hasMoreElements();) {
                String alias = (String) e.nextElement();
                Logger.log(Logger.FULL_DEBUG, SSL_RESOURCES,
                        "HttpsListener.KeyFound", alias,
                        keystore.getCertificate(alias) + "");
            }

            SslContextFactory ssl = new SslContextFactory();

            ssl.setKeyStore(keystore);
            ssl.setKeyStorePassword(keystorePassword);
            ssl.setKeyManagerPassword(privateKeyPassword);
            ssl.setSslKeyManagerFactoryAlgorithm(Option.HTTPS_KEY_MANAGER_TYPE.get(args));
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
