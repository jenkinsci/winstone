package winstone.cmdline;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import winstone.Launcher;
import winstone.realm.ArgumentsRealm;

/**
 * Command line options used in {@link Launcher}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Option<T> {
    /**
     * List up all the known options.
     */
    public static List<Option<?>> all(Class<?> clazz) {
        List<Option<?>> r = new ArrayList<>();
        for (Field f : clazz.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Option.class.isAssignableFrom(f.getType())) {
                try {
                    r.add((Option<?>) f.get(null));
                } catch (IllegalAccessException e) {
                    throw (Error) new IllegalAccessError().initCause(e);
                }
            }
        }
        return r;
    }

    public static final OFile WEBROOT = file("webroot");
    public static final OFile WARFILE = file("warfile");
    public static final OFile JAVA_HOME = file("javaHome");
    public static final OFile CONFIG = file("config");
    public static final OString PREFIX = string("prefix", "");
    public static final OFile COMMON_LIB_FOLDER = file("commonLibFolder");
    public static final OFile LOGFILE = file("logfile");
    public static final OBoolean LOG_THROWING_LINE_NO = bool("logThrowingLineNo", false);
    public static final OBoolean LOG_THROWING_THREAD = bool("logThrowingThread", false);
    public static final OInt DEBUG = new ODebugInt("debug", 5);

    // these are combined with protocol to form options
    private static final OInt _PORT = integer("Port");
    private static final OString _LISTEN_ADDRESS = string("ListenAddress");
    private static final OString _UNIX_DOMAIN_PATH = string("UnixDomainPath");
    /**
     * Number of milliseconds for the HTTP keep-alive to hang around until the next request is sent.
     */
    private static final OInt _KEEP_ALIVE_TIMEOUT = integer("KeepAliveTimeout", 30000);

    public static final OInt HTTP_PORT = integer("http" + _PORT, 8080);
    public static final OString HTTP_LISTEN_ADDRESS = string("http" + _LISTEN_ADDRESS);
    public static final OString HTTP_UNIX_DOMAIN_PATH = string("http" + _UNIX_DOMAIN_PATH);
    public static final OInt HTTP_KEEP_ALIVE_TIMEOUT =
            integer("http" + _KEEP_ALIVE_TIMEOUT, _KEEP_ALIVE_TIMEOUT.defaultValue);

    public static final OInt HTTPS_PORT = integer("https" + _PORT, -1);
    public static final OString HTTPS_LISTEN_ADDRESS = string("https" + _LISTEN_ADDRESS);
    public static final OInt HTTPS_KEEP_ALIVE_TIMEOUT =
            integer("https" + _KEEP_ALIVE_TIMEOUT, _KEEP_ALIVE_TIMEOUT.defaultValue);
    public static final OFile HTTPS_KEY_STORE = file("httpsKeyStore");
    public static final OString HTTPS_KEY_STORE_PASSWORD = string("httpsKeyStorePassword");
    public static final OString HTTPS_PRIVATE_KEY_PASSWORD = string("httpsPrivateKeyPassword");
    public static final OString HTTPS_KEY_MANAGER_TYPE = string("httpsKeyManagerType", "SunX509");
    public static final OString HTTPS_VERIFY_CLIENT = string("httpsVerifyClient", "false");
    public static final OString HTTPS_CERTIFICATE_ALIAS = string("httpsCertificateAlias");
    public static final OString HTTPS_EXCLUDE_PROTOCOLS = string("excludeProtocols", "SSL, SSLv2, SSLv2Hello, SSLv3");
    public static final OString HTTPS_EXCLUDE_CIPHER_SUITES = string("excludeCipherSuites");
    public static final OBoolean HTTPS_REDIRECT_HTTP = bool("httpsRedirectHttp", false);
    public static final OBoolean HTTPS_SNI_HOST_CHECK = bool("httpsSniHostCheck", false);
    public static final OBoolean HTTPS_SNI_REQUIRED = bool("httpsSniRequired", false);

    public static final OString HTTP2_LISTEN_ADDRESS = string("http2" + _LISTEN_ADDRESS);
    public static final OInt HTTP2_PORT = integer("http2" + _PORT, -1);

    public static final OInt CONTROL_PORT = integer("controlPort", -1);

    public static final OBoolean USE_JMX = bool("useJmx", false);

    public static final OInt QTP_MAXTHREADS = integer("qtpMaxThreadsCount", -1);
    public static final OInt JETTY_ACCEPTORS = integer("jettyAcceptorsCount", -1);
    public static final OInt JETTY_SELECTORS = integer("jettySelectorsCount", 0);

    public static final OCompression COMPRESSION = new OCompression("compression", CompressionScheme.GZIP);
    public static final OString MIME_TYPES = string("mimeTypes");
    public static final OInt MAX_PARAM_COUNT = integer("maxParamCount", 10000);
    public static final OBoolean USAGE = bool("usage", false);
    public static final OInt SESSION_TIMEOUT = integer("sessionTimeout", -1);
    public static final OInt SESSION_EVICTION = integer("sessionEviction", 1800);
    public static final OInt REQUEST_HEADER_SIZE = integer("requestHeaderSize", 8192); // default for jetty 9
    public static final OInt RESPONSE_HEADER_SIZE = integer("responseHeaderSize", 8192); // default for jetty 9
    public static final OInt REQUEST_FORM_CONTENT_SIZE =
            integer("requestFormContentSize", -1); // no limit (compat with old winstone)
    public static final OBoolean HELP = bool("help", false);

    public static final OClass REALM_CLASS_NAME = clazz("realmClassName", ArgumentsRealm.class);

    public static final OString ARGUMENTS_REALM_PASSWORD = string("argumentsRealm.passwd.");
    public static final OString ARGUMENTS_REALM_ROLES = string("argumentsRealm.roles.");
    public static final OFile FILEREALM_CONFIGFILE = file("fileRealm.configFile");

    public static final OClass ACCESS_LOGGER_CLASSNAME = clazz("accessLoggerClassName", null);
    public static final OString SIMPLE_ACCESS_LOGGER_FORMAT = string("simpleAccessLogger.format", "combined");
    public static final OString SIMPLE_ACCESS_LOGGER_FILE =
            string("simpleAccessLogger.file", "logs/###host###/###webapp###_access.log");

    /**
     * Option name without the "--" prefix.
     */
    public final String name;

    /**
     * Expected type.
     */
    public final Class<T> type;

    public final T defaultValue;

    public Option(String name, Class<T> type, T defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public void remove(Map<String, String> args) {
        args.remove(name);
    }

    public void put(Map<String, String> args, String value) {
        args.put(name, value);
    }

    public boolean isIn(Map<String, String> args) {
        return args.containsKey(name);
    }

    /**
     * Indicates an option name that takes some argument.
     */
    public boolean isWildcard() {
        return name.endsWith(".");
    }

    @Override
    public String toString() {
        return name;
    }

    public static OString string(String name) {
        return new OString(name, null);
    }

    public static OString string(String name, String defaultValue) {
        return new OString(name, defaultValue);
    }

    public static OBoolean bool(String name, boolean defaultValue) {
        return new OBoolean(name, defaultValue);
    }

    public static OFile file(String name) {
        return new OFile(name);
    }

    public static OClass clazz(String name, Class defaultValue) {
        return new OClass(name, defaultValue);
    }

    public static OInt integer(String name) {
        return new OInt(name, -1);
    }

    public static OInt integer(String name, int defaultValue) {
        return new OInt(name, defaultValue);
    }

    public static class OBoolean extends Option<Boolean> {
        public OBoolean(String name, boolean defaultValue) {
            super(name, Boolean.class, defaultValue);
        }

        public boolean get(Map<String, String> args) {
            return get(args, defaultValue);
        }

        public boolean get(Map<String, String> args, boolean defaultValue) {
            return booleanArg(args, name, defaultValue);
        }
    }

    public static class OInt extends Option<Integer> {
        public OInt(String name, int defaultValue) {
            super(name, Integer.class, defaultValue);
        }

        public int get(Map<String, String> args) {
            return intArg(args, name, defaultValue);
        }

        public int get(Map<String, String> args, int defaultValue) {
            return intArg(args, name, defaultValue);
        }
    }

    public static class ODebugInt extends OInt {

        public ODebugInt(String name, int defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public int get(Map<String, String> args) {
            // before switching to java.util.Logging, winstone used a (1:9) range for log levels
            switch (super.get(args)) {
                case 1:
                    return Level.OFF.intValue();
                case 2:
                    return Level.SEVERE.intValue();
                case 3:
                    return Level.WARNING.intValue();
                case 4:
                    return Level.INFO.intValue();
                case 6:
                    return Level.FINE.intValue();
                case 7:
                    return Level.FINER.intValue();
                case 8:
                    return Level.FINEST.intValue();
                case 9:
                    return Level.ALL.intValue();
                case 5:
                default:
                    return Level.INFO.intValue();
            }
        }
    }

    public static class OString extends Option<String> {
        public OString(String name, String defaultValue) {
            super(name, String.class, defaultValue);
        }

        public String get(Map<String, String> args) {
            return get(args, defaultValue);
        }

        public char[] getCharArray(Map<String, String> args) {
            String v = get(args);
            return v != null ? v.toCharArray() : null;
        }

        public String get(Map<String, String> args, String defaultValue) {
            String v = args.get(name);
            return v != null ? v : defaultValue;
        }
    }

    public static class OFile extends Option<File> {
        public OFile(String name) {
            super(name, File.class, null);
        }

        @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "false positive, args come from command line")
        public File get(Map<String, String> args, File defaultValue) {
            String v = args.get(name);
            return v != null ? new File(v) : defaultValue;
        }

        public File get(Map<String, String> args) {
            return get(args, null);
        }
    }

    public static class OClass extends Option<Class> {
        public OClass(String name, Class defaultValue) {
            super(name, Class.class, defaultValue);
        }

        public <T> Class<? extends T> get(Map<String, String> args, Class<T> expectedType)
                throws ClassNotFoundException {
            return get(args, expectedType, getClass().getClassLoader());
        }

        public <T> Class<? extends T> get(Map<String, String> args, Class<T> expectedType, ClassLoader cl)
                throws ClassNotFoundException {
            String v = args.get(name);
            if (v == null) {
                return defaultValue;
            }

            v = v.trim();
            if (v.length() == 0) {
                return defaultValue;
            }

            Class<?> c = Class.forName(v, true, cl);
            if (!expectedType.isAssignableFrom(c)) {
                throw new ClassNotFoundException("Expected a subype of " + expectedType + " but got " + c + " instead");
            }

            return c.asSubclass(expectedType);
        }
    }

    public static class OCompression extends Option<CompressionScheme> {
        public OCompression(String name, CompressionScheme defaultValue) {
            super(name, CompressionScheme.class, defaultValue);
        }

        public CompressionScheme get(Map<String, String> args) {
            return get(args, defaultValue);
        }

        public CompressionScheme get(Map<String, String> args, CompressionScheme defaultValue) {
            String v = args.get(name);
            CompressionScheme compressionScheme;
            if (v == null) {
                compressionScheme = defaultValue;
            } else if (v.equalsIgnoreCase("gzip")) {
                compressionScheme = CompressionScheme.GZIP;
            } else if (v.equalsIgnoreCase("none")) {
                compressionScheme = CompressionScheme.NONE;
            } else {
                throw new IllegalArgumentException("Unexpected compression scheme: " + v);
            }
            return compressionScheme;
        }
    }

    //    static {
    //        String[] protocols = {"http","https"};
    //        for (int i=0; i<protocols.length; i++) {
    //            String protocol = protocols[i];
    //            String[]
    //        }
    //    }

    public static boolean booleanArg(Map<String, String> args, String name, boolean defaultTrue) {
        String value = args.get(name);
        if (defaultTrue) {
            return (value == null) || (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        } else {
            return (value != null) && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        }
    }

    public static String stringArg(Map<String, String> args, String name, String defaultValue) {
        return args.get(name) == null ? defaultValue : args.get(name);
    }

    public static int intArg(Map<String, String> args, String name, int defaultValue) {
        return Integer.parseInt(stringArg(args, name, "" + defaultValue));
    }
}
