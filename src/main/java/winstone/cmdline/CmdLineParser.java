package winstone.cmdline;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import winstone.Launcher;
import winstone.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Command line argument parser, Winstone style.
 *
 * @author Kohsuke Kawaguchi
 */
public class CmdLineParser {
    private final List<Option<?>> options;

    public CmdLineParser(List<Option<?>> options) {
        this.options = options;
    }

    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "TODO needs triage")
    public Map<String,String> parse(String[] argv, String nonSwitchArgName) throws IOException {
        Map<String,String> args = new HashMap<>();

        // Load embedded properties file
        String embeddedPropertiesFilename = Launcher.RESOURCES.getString(
                "Launcher.EmbeddedPropertiesFile");

        try (InputStream embeddedPropsStream = Launcher.class.getResourceAsStream(
                embeddedPropertiesFilename)) {
            if ( embeddedPropsStream != null ) {
                loadPropsFromStream( embeddedPropsStream, args );
            }
        }

        // Get command line args
        String configFilename = Launcher.RESOURCES.getString("Launcher.DefaultPropertyFile");
        for (String option : argv) {
            if (option.startsWith("--")) {
                int equalPos = option.indexOf('=');
                String paramName = option.substring(2,
                        equalPos == -1 ? option.length() : equalPos);
                Option<?> opt = toOption(paramName);
                if (opt == null)
                    throw new IllegalArgumentException(Launcher.RESOURCES.getString("CmdLineParser.UnrecognizedOption", option));

                if (equalPos != -1) {
                    args.put(paramName, option.substring(equalPos + 1));
                } else {
                    if (opt.type == Boolean.class)
                        args.put(paramName, "true");
                    else
                        throw new IllegalArgumentException(Launcher.RESOURCES.getString("CmdLineParser.OperandExpected", option));
                }
                if (paramName.equals(Option.CONFIG.name)) {
                    configFilename = args.get(paramName);
                }
            } else {
                if (args.containsKey(nonSwitchArgName))
                    throw new IllegalArgumentException(Launcher.RESOURCES.getString("CmdLineParser.MultipleArgs", option));
                args.put(nonSwitchArgName, option);
            }
        }

        // Load default props if available
        File configFile = new File(configFilename);
        if (configFile.exists() && configFile.isFile()) {
            try (InputStream inConfig = new FileInputStream(configFile)) {
                loadPropsFromStream( inConfig, args );
                inConfig.close();
                Launcher.initLogger( args );
                Logger.log( Logger.DEBUG, Launcher.RESOURCES, "Launcher.UsingPropertyFile", configFilename );
            }
        } else {
            Launcher.initLogger(args);
        }
        return args;
    }

    private static void loadPropsFromStream(InputStream inConfig, Map<String,String> args) throws IOException {
        Properties props = new Properties();
        props.load(inConfig);
        for (Object o : props.keySet()) {
            String key = (String) o;
            if (!args.containsKey(key.trim())) {
                args.put(key.trim(), props.getProperty(key).trim());
            }
        }
        props.clear();
    }

    private Option<?> toOption(String paramName) {
        for (Option<?> o : options) {
            if (o.isWildcard() && paramName.startsWith(o.name))
                return o;
            if (!o.isWildcard() && paramName.equals(o.name))
                return o;
        }
        return null;
    }
}
