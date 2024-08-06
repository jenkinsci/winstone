package winstone;

import java.io.IOException;
import java.io.InputStream;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class HostConfigurationTest {

    @Rule
    public ErrorCollector errors = new ErrorCollector();

    @Test
    public void mimeTypes() throws IOException {
        NavigableMap<String, String> jetty = loadMimeTypes("/org/eclipse/jetty/http/mime.properties");
        NavigableMap<String, String> winstone = loadMimeTypes("/winstone/mime.properties");
        for (String key : winstone.keySet()) {
            if (jetty.containsKey(key)) {
                errors.addError(new AssertionError(String.format(
                        "Attempting to add %s=%s but Jetty already defines %s=%s",
                        key, winstone.get(key), key, jetty.get(key))));
            }
        }
    }

    private static NavigableMap<String, String> loadMimeTypes(String name) throws IOException {
        Properties props = new Properties();
        try (InputStream in = HostConfigurationTest.class.getResourceAsStream(name)) {
            props.load(in);
        }
        NavigableMap<String, String> result = new TreeMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }
}
