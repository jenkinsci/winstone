package winstone;

import java.io.IOException;
import java.io.InputStream;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
class HostConfigurationTest {

    @InjectSoftAssertions
    private SoftAssertions softly;

    @Test
    void mimeTypes() throws IOException {
        NavigableMap<String, String> jetty = loadMimeTypes("/org/eclipse/jetty/http/mime.properties");
        NavigableMap<String, String> winstone = loadMimeTypes("/winstone/mime.properties");
        for (String key : winstone.keySet()) {
            softly.assertThat(jetty)
                    .as(
                            "Attempting to add %s=%s but Jetty already defines %s=%s",
                            key, winstone.get(key), key, jetty.get(key))
                    .doesNotContainKey(key);
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
