package winstone;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class HostConfigurationTest {

    @Test
    void mimeTypes() throws IOException {
        NavigableMap<String, String> jetty = loadMimeTypes("/org/eclipse/jetty/http/mime.properties");
        NavigableMap<String, String> winstone = loadMimeTypes("/winstone/mime.properties");

        List<Executable> failures = new ArrayList<>();
        for (String key : winstone.keySet()) {
            if (jetty.containsKey(key)) {
                String message = String.format(
                        "Attempting to add %s=%s but Jetty already defines %s=%s",
                        key, winstone.get(key), key, jetty.get(key));
                failures.add(() -> fail(message));
            }
        }

        assertAll("Winstone MIME types must not duplicate Jetty MIME types", failures);
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
