package winstone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import winstone.cmdline.Option;

/**
 * @author Kohsuke Kawaguchi
 */
class LauncherTest extends AbstractWinstoneTest {
    @Test
    void implicitGzip() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        verifyGzip(true);
    }

    @Test
    void explicitGzip() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("compression", "gzip");
        winstone = new Launcher(args);
        verifyGzip(true);
    }

    @Test
    void noCompression() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("compression", "none");
        winstone = new Launcher(args);
        verifyGzip(false);
    }

    private void verifyGzip(boolean gzip) throws Exception {
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/lipsum.txt"))
                .GET()
                .header("Accept-Encoding", "gzip")
                .build();
        HttpResponse<InputStream> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("text/plain", response.headers().firstValue("Content-Type").orElseThrow());
        InputStream is;
        if (gzip) {
            assertEquals(
                    "gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
            is = new GZIPInputStream(response.body());
        } else {
            assertFalse(response.headers().firstValue("Content-Encoding").isPresent());
            is = response.body();
        }
        try {
            assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), startsWith("Lorem ipsum dolor sit amet"));
        } finally {
            is.close();
        }
    }

    @Test
    void mimeType() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("mimeTypes", "xxx=text/xxx");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/test.xxx"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals("text/xxx", response.headers().firstValue("Content-Type").get());
        assertEquals("Hello", response.body());
    }

    @Issue("JENKINS-60409")
    @Test
    void doubleGzip() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        winstone = new Launcher(args);
        int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
        HttpRequest request = HttpRequest.newBuilder(new URI("http://127.0.0.2:" + port + "/lipsum.tar.gz"))
                .header("Accept-Encoding", "gzip")
                .GET()
                .build();
        HttpResponse<byte[]> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        assertEquals(
                "application/gzip",
                response.headers().firstValue("Content-Type").orElseThrow());
        assertFalse(response.headers().firstValue("Content-Encoding").isPresent());
        assertEquals(1345, response.body().length);
    }

    @Test
    void testHttpsKeyStoreTypeOption() {
        Map<String, String> args = new HashMap<>();

        String defaultType = Option.HTTPS_KEY_STORE_TYPE.get(args, KeyStore.getDefaultType());
        assertEquals(KeyStore.getDefaultType(), defaultType);

        args.put("httpsKeyStoreType", "PKCS12");
        String customType = Option.HTTPS_KEY_STORE_TYPE.get(args, KeyStore.getDefaultType());
        assertEquals("PKCS12", customType);
    }

    @Issue("https://github.com/jenkinsci/winstone/issues/554")
    @Test
    void testEmptyWebRootArgument() throws Exception {
        // Check that empty string is handled the same as null
        //
        // Treat an empty string first argument the same as a null first argument to fix
        // https://github.com/jenkinsci/winstone/issues/554
        //
        // Java 25 returns File("").exists() as true (intentionally)
        // Java 21 returns File("").exists() as false
        // https://bugs.openjdk.org/browse/JDK-8024695
        String[] argv = {""};
        Map<String, String> args = Launcher.getArgsFromCommandLine(argv);
        assertThat(args, is(anEmptyMap()));
    }

    @Test
    void testZeroLengthArrayWebRootArgument() throws Exception {
        String[] argv = {};
        Map<String, String> args = Launcher.getArgsFromCommandLine(argv);
        assertThat(args, is(anEmptyMap()));
    }

    @Test
    void testBlankWarFileArgument() throws Exception {
        // Check that a single argument that is an existing file is assigned to the warfile entry in the Map
        String fileName = "a-file-name";
        Path spaceFile = Path.of(fileName);
        Files.createFile(spaceFile);
        String[] argv = {fileName};
        Map<String, String> args = Launcher.getArgsFromCommandLine(argv);
        Files.deleteIfExists(spaceFile); // Delete before assertion to avoid try / catch
        assertThat(args, is(aMapWithSize(1)));
        assertThat(args, hasEntry("warfile", fileName));
    }

    @Test
    void testBlankWebRootArgument() throws Exception {
        // Check that a single argument that is an existing directory is assigned to the webroot entry in the Map
        String dirName = "a-directory-name";
        Path spaceDirectory = Path.of(dirName);
        Files.createDirectory(spaceDirectory);
        String[] argv = {dirName};
        Map<String, String> args = Launcher.getArgsFromCommandLine(argv);
        Files.deleteIfExists(spaceDirectory); // Delete before assertion to avoid try / catch
        assertThat(args, is(aMapWithSize(1)));
        assertThat(args, hasEntry("webroot", dirName));
    }
}
