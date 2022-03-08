package winstone;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static winstone.Launcher.WINSTONE_PORT_FILE_NAME_PROPERTY;

/**
 * @author Kohsuke Kawaguchi
 */
public class HttpConnectorFactoryTest extends AbstractWinstoneTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testListenAddress() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpListenAddress", "127.0.0.2");
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        assertConnectionRefused("127.0.0.1",port);

        makeRequest("http://127.0.0.2:"+port+"/CountRequestsServlet");

        LowResourceMonitor lowResourceMonitor = winstone.server.getBean(LowResourceMonitor.class);
        assertNotNull(lowResourceMonitor);
        assertFalse(lowResourceMonitor.isLowOnResources());
        assertTrue(lowResourceMonitor.isAcceptingInLowResources());
    }

    @Test
    public void writePortInFile() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        Path portFile = Paths.get(tmp.getRoot().getAbsolutePath(), "subdir/jenkins.port");
        System.setProperty(WINSTONE_PORT_FILE_NAME_PROPERTY, portFile.toString());
        try {
            winstone = new Launcher(args);
            int port = ((ServerConnector) winstone.server.getConnectors()[0]).getLocalPort();
            try (BufferedReader reader = Files.newBufferedReader(portFile, StandardCharsets.UTF_8)) {
                String portInFile = reader.readLine();
                assertEquals(Integer.toString(port), portInFile);
            }
        } finally {
            System.clearProperty(WINSTONE_PORT_FILE_NAME_PROPERTY);
        }
    }
}
