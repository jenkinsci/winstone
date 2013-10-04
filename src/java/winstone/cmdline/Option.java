package winstone.cmdline;

import winstone.Launcher;
import winstone.WebAppConfiguration;
import winstone.classLoader.WebappClassLoader;
import winstone.realm.ArgumentsRealm;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<Option<?>> r = new ArrayList<Option<?>>();
        for (Field f : clazz.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Option.class.isAssignableFrom(f.getType())) {
                try {
                    r.add((Option<?>) f.get(null));
                } catch (IllegalAccessException e) {
                    throw (Error)new IllegalAccessError().initCause(e);
                }
            }
        }
        return r;
    }
    
    public static final OFile WEBROOT=file("webroot");
    public static final OFile WARFILE=file("warfile");
    public static final OFile WEBAPPS_DIR=file("webappsDir");
    public static final OFile JAVA_HOME=file("javaHome");
    public static final OFile TOOLS_JAR=file("toolsJar");
    public static final OFile CONFIG=file("config");
    public static final OString PREFIX=string("prefix","");
    public static final OFile COMMON_LIB_FOLDER=file("commonLibFolder");
    public static final OFile LOGFILE=file("logfile");
    public static final OBoolean LOG_THROWING_LINE_NO=bool("logThrowingLineNo",false);
    public static final OBoolean LOG_THROWING_THREAD=bool("logThrowingThread",false);
    public static final OBoolean DEBUG=bool("debug",false);

    // these are combined with protocol to form options
    public static final OInt _PORT = integer("Port");
    public static final OString _LISTEN_ADDRESS = string("ListenAddress");
    public static final OBoolean _DO_HOSTNAME_LOOKUPS = bool("DoHostnameLookups",false);
    /**
     * Number of milliseconds for the HTTP keep-alive to hang around until the next request is sent.
     */
    public static final OInt _KEEP_ALIVE_TIMEOUT = integer("KeepAliveTimeout",5000);


    public static final OInt HTTP_PORT=integer("http"+_PORT);
    public static final OString HTTP_LISTEN_ADDRESS=string("http"+ _LISTEN_ADDRESS);
    public static final OBoolean HTTP_DO_HOSTNAME_LOOKUPS=bool("http"+ _DO_HOSTNAME_LOOKUPS,false);
    public static final OInt HTTP_KEEP_ALIVE_TIMEOUT=integer("http" + _KEEP_ALIVE_TIMEOUT, _KEEP_ALIVE_TIMEOUT.defaultValue);

    public static final OInt HTTPS_PORT=integer("https"+_PORT);
    public static final OString HTTPS_LISTEN_ADDRESS=string("https"+_LISTEN_ADDRESS);
    public static final OBoolean HTTPS_DO_HOSTNAME_LOOKUPS=bool("https"+ _DO_HOSTNAME_LOOKUPS,false);
    public static final OInt HTTPS_KEEP_ALIVE_TIMEOUT=integer("https" + _KEEP_ALIVE_TIMEOUT, _KEEP_ALIVE_TIMEOUT.defaultValue);
    public static final OFile HTTPS_KEY_STORE=file("httpsKeyStore");
    public static final OString HTTPS_KEY_STORE_PASSWORD=string("httpsKeyStorePassword");
    public static final OString HTTPS_KEY_MANAGER_TYPE=string("httpsKeyManagerType","SunX509");
    public static final OBoolean HTTPS_VERIFY_CLIENT=bool("httpsVerifyClient",false);
    public static final OFile HTTPS_CERTIFICATE=file("httpsCertificate");
    public static final OFile HTTPS_PRIVATE_KEY=file("httpsPrivateKey");

    public static final OInt AJP13_PORT=integer("ajp13"+_PORT,-1);
    public static final OString AJP13_LISTEN_ADDRESS=string("ajp13"+_LISTEN_ADDRESS);

    public static final OInt CONTROL_PORT=integer("controlPort",-1);

    /**
     * Currently unused.
     */
    public static final OInt HANDLER_COUNT_STARTUP =integer("handlerCountStartup",5);
    /**
     * How many requests do we handle concurrently?
     *
     * If the system gets really loaded, too many concurrent threads will create vicious cycles
     * and make everyone slow (or worst case choke every request by OOME), so better to err
     * on the conservative side (and have inbound connections wait in the queue)
     */
    public static final OInt HANDLER_COUNT_MAX     =integer("handlerCountMax",40);
    /**
     * Leave this number of request handler threads in the pool even when they are idle.
     * Other threads are destroyed when they are idle to free up resources.
     */
    public static final OInt HANDLER_COUNT_MAX_IDLE=integer("handlerCountMaxIdle",5);

    public static final OBoolean DIRECTORY_LISTINGS=bool("directoryListings",true);
    public static final OBoolean USE_JASPER=bool("useJasper",false);
    public static final OBoolean USE_SERVLET_RELOADING=bool("useServletReloading",false);
    public static final OClass PREFERRED_CLASS_LOADER=clazz("preferredClassLoader", WebappClassLoader.class);
    public static final OBoolean USE_INVOKER=bool("useInvoker",false);
    public static final OString INVOKER_PREFIX=string("invokerPrefix","/servlet/");
    public static final OBoolean SIMULATE_MOD_UNIQUE_ID=bool("simulateModUniqueId",false);
    public static final OBoolean USE_SAVED_SESSIONS=bool("useSavedSessions",false);
    public static final OString MIME_TYPES=string("mimeTypes");
    public static final OInt MAX_PARAM_COUNT=integer("maxParamCount",-1);
    public static final OBoolean USAGE=bool("usage",false);
    public static final OInt SESSION_TIMEOUT=integer("sessionTimeout",-1);
    public static final OBoolean HELP=bool("help",false);

    public static final OClass REALM_CLASS_NAME=clazz("realmClassName", ArgumentsRealm.class);

    public static final OString ARGUMENTS_REALM_PASSWORD=string("argumentsRealm.passwd.");
    public static final OString ARGUMENTS_REALM_ROLES=string("argumentsRealm.roles.");
    public static final OFile FILEREALM_CONFIGFILE=file("fileRealm.configFile");

    public static final OClass ACCESS_LOGGER_CLASSNAME=clazz("accessLoggerClassName",null);
    public static final OString SIMPLE_ACCESS_LOGGER_FORMAT=string("simpleAccessLogger.format","combined");
    public static final OString SIMPLE_ACCESS_LOGGER_FILE=string("simpleAccessLogger.file","logs/###host###/###webapp###_access.log");


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
    
    public void remove(Map args) {
        args.remove(name);
    }

    public void put(Map args, String value) {
        args.put(name, value);
    }
    
    public boolean isIn(Map args) {
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
        return new OString(name,null);
    }
    
    public static OString string(String name, String defaultValue) {
        return new OString(name,defaultValue);
    }
    
    public static OBoolean bool(String name, boolean defaultValue) {
        return new OBoolean(name,defaultValue);
    }

    public static OFile file(String name) {
        return new OFile(name);
    }

    public static OClass clazz(String name, Class defaultValue) {
        return new OClass(name,defaultValue);
    }

    public static OInt integer(String name) {
        return new OInt(name,-1);
    }
    
    public static OInt integer(String name,int defaultValue) {
        return new OInt(name,defaultValue);
    }

    public static class OBoolean extends Option<Boolean> {
        public OBoolean(String name, boolean defaultValue) {
            super(name, Boolean.class, defaultValue);
        }
        
        public boolean get(Map args) {
            return get(args,defaultValue);
        }

        public boolean get(Map args, boolean defaultValue) {
            return WebAppConfiguration.booleanArg(args, name, defaultValue);
        }
    }
    
    public static class OInt extends Option<Integer> {
        public OInt(String name, int defaultValue) {
            super(name, Integer.class, defaultValue);
        }

        public int get(Map args) {
            return WebAppConfiguration.intArg(args, name, defaultValue);
        }

        public int get(Map args, int defaultValue) {
            return WebAppConfiguration.intArg(args, name, defaultValue);
        }
    }

    public static class OString extends Option<String> {
        public OString(String name, String defaultValue) {
            super(name, String.class, defaultValue);
        }
        
        public String get(Map args) {
            String v = (String)args.get(name);
            return v!=null ? v : defaultValue;
        }
    }
    
    public static class OFile extends Option<File> {
        public OFile(String name) {
            super(name, File.class,null);
        }
        
        public File get(Map args, File defaultValue) {
            String v = (String)args.get(name);
            return v!=null ? new File(v) : defaultValue;
        }

        public File get(Map args) {
            return get(args,null);
        }
    }
    
    public static class OClass extends Option<Class> {
        public OClass(String name,Class defaultValue) {
            super(name, Class.class, defaultValue);
        }

        public <T> Class<? extends T> get(Map args, Class<T> expectedType) throws ClassNotFoundException {
            return get(args,expectedType,getClass().getClassLoader());
        }
        
        public <T> Class<? extends T> get(Map args, Class<T> expectedType, ClassLoader cl) throws ClassNotFoundException {
            String v = (String)args.get(name);
            if (v==null) return defaultValue;
            
            v=v.trim();
            if (v.length()==0)  return defaultValue;

            Class<?> c = Class.forName(v, true, cl);
            if (!expectedType.isAssignableFrom(c))
                throw new ClassNotFoundException("Expected a subype of "+expectedType+" but got "+c+" instead");

            return c.asSubclass(expectedType);
        }
    }
    
    //    static {
//        String[] protocols = {"http","https","ajp13"};
//        for (int i=0; i<protocols.length; i++) {
//            String protocol = protocols[i];
//            String[]
//        }
//    }
}
