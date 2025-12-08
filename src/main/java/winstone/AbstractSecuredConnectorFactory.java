/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */

package winstone;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import winstone.cmdline.Option;

/**
 *
 */
public abstract class AbstractSecuredConnectorFactory implements ConnectorFactory {
    protected static final WinstoneResourceBundle SSL_RESOURCES = new WinstoneResourceBundle("winstone.LocalStrings");
    protected KeyStore keystore;
    protected String keystorePassword;

    protected void configureSsl(Map<String, String> args, Server server) throws IOException {
        try {
            File keyStore = Option.HTTPS_KEY_STORE.get(args);
            String pwd = Option.HTTPS_KEY_STORE_PASSWORD.get(args);

            if (keyStore != null) {
                // load from default Keystore
                if (!keyStore.exists() || !keyStore.isFile()) {
                    throw new WinstoneException(
                            SSL_RESOURCES.getString("HttpsListener.KeyStoreNotFound", keyStore.getPath()));
                }

                this.keystorePassword = pwd;

                String keyStoreType = Option.HTTPS_KEY_STORE_TYPE.get(args, KeyStore.getDefaultType());
                keystore = KeyStore.getInstance(keyStoreType);
                try (InputStream inputStream = new FileInputStream(keyStore)) {
                    keystore.load(inputStream, this.keystorePassword.toCharArray());
                }
            } else {
                throw new WinstoneException(MessageFormat.format("Please set --{0}", Option.HTTPS_KEY_STORE));
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to handle keys", e);
        }
    }

    /**
     * Used to get the base ssl context in which to create the server socket.
     * This is basically just so we can have a custom location for key stores.
     */
    protected SslContextFactory.Server getSSLContext(Map<String, String> args) {
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
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(Option.HTTPS_KEY_MANAGER_TYPE.get(args));

            // In case the KeyStore password and the KeyPassword are not the same,
            // the KeyManagerFactory needs the KeyPassword because it will access the
            // individual key(s)
            kmf.init(keystore, keystorePassword.toCharArray());
            Logger.log(Level.FINEST, SSL_RESOURCES, "HttpsListener.KeyCount", keystore.size() + "");
            for (Enumeration<String> e = keystore.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                Logger.log(
                        Level.FINEST,
                        SSL_RESOURCES,
                        "HttpsListener.KeyFound",
                        alias,
                        keystore.getCertificate(alias) + "");
            }

            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setKeyStore(keystore);
            ssl.setKeyStorePassword(keystorePassword);
            ssl.setKeyManagerPassword(privateKeyPassword);
            ssl.setKeyManagerFactoryAlgorithm(Option.HTTPS_KEY_MANAGER_TYPE.get(args));
            ssl.setCertAlias(Option.HTTPS_CERTIFICATE_ALIAS.get(args));
            String excludeProtos = Option.HTTPS_EXCLUDE_PROTOCOLS.get(args);
            if (excludeProtos != null && excludeProtos.length() > 0) {
                String[] protos =
                        Stream.of(excludeProtos.split(",")).map(String::trim).toArray(String[]::new);
                ssl.setExcludeProtocols(protos);
            }
            String excludeCiphers = Option.HTTPS_EXCLUDE_CIPHER_SUITES.get(args);
            if (excludeCiphers != null && excludeCiphers.length() > 0) {
                String[] cipherSuites = excludeCiphers.split(",");
                ssl.setExcludeCipherSuites(cipherSuites);
            }
            Logger.log(
                    Level.INFO,
                    SSL_RESOURCES, //
                    "HttpsListener.ExcludeProtocols", //
                    Arrays.asList(ssl.getExcludeProtocols()));
            Logger.log(
                    Level.INFO,
                    SSL_RESOURCES, //
                    "HttpsListener.ExcludeCiphers", //
                    Arrays.asList(ssl.getExcludeCipherSuites()));

            switch (Option.HTTPS_VERIFY_CLIENT.get(args).toLowerCase(Locale.ROOT)) {
                case "yes":
                case "true":
                    ssl.setNeedClientAuth(true);
                    break;
                case "optional":
                    ssl.setWantClientAuth(true);
                    break;
                default:
                    ssl.setNeedClientAuth(false);
                    break;
            }
            return ssl;
        } catch (Throwable err) {
            throw new WinstoneException(SSL_RESOURCES.getString("HttpsListener.ErrorGettingContext"), err);
        }
    }
}
