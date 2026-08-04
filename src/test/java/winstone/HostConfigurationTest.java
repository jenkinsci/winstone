package winstone;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.function.Executable;

class HostConfigurationTest {

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

    @Test
    void deleteRecursiveDoesNotFollowSymlinks(@TempDir Path tempDir) throws Exception {
        // Create a directory structure with a symlink pointing outside
        Path webroot = tempDir.resolve("webroot");
        Files.createDirectories(webroot);
        Files.writeString(webroot.resolve("file.txt"), "hello");

        // Create an external directory that should NOT be deleted
        Path externalDir = tempDir.resolve("external");
        Files.createDirectories(externalDir);
        Files.writeString(externalDir.resolve("important.txt"), "do not delete");

        // Create a symlink inside webroot pointing to the external directory
        Path symlink = webroot.resolve("link-to-external");
        Files.createSymbolicLink(symlink, externalDir);

        // Invoke deleteRecursive via reflection since it is private
        HostConfiguration config = null; // static context not needed, method only uses the parameter
        Method deleteRecursive = HostConfiguration.class.getDeclaredMethod("deleteRecursive", java.io.File.class);
        deleteRecursive.setAccessible(true);
        deleteRecursive.invoke(config, webroot.toFile());

        // The webroot should be deleted
        assertFalse(Files.exists(webroot), "webroot should be deleted");

        // The external directory should NOT be deleted (symlink was not followed)
        assertEquals("do not delete", Files.readString(externalDir.resolve("important.txt")),
                "external directory should not be deleted when symlink is not followed");
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
