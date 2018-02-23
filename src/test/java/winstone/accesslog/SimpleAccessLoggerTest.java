package winstone.accesslog;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import winstone.AbstractWinstoneTest;
import winstone.Launcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class SimpleAccessLoggerTest extends AbstractWinstoneTest {
    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    @Test
    public void testSimpleConnection() throws Exception {
        File logFile = new File("target/test.log");
        logFile.delete();

        // Initialise container
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/examples");
        args.put("httpPort", "10003");
        args.put("accessLoggerClassName",SimpleAccessLogger.class.getName());
        args.put("simpleAccessLogger.file",logFile.getAbsolutePath());
        args.put("simpleAccessLogger.format","###ip### - ###user### ###uriLine### ###status###");
        winstone = new Launcher(args);

        // make a request
        makeRequest("http://localhost:10003/examples/CountRequestsServlet");

        // check the log file
        String text = FileUtils.readFileToString(logFile);
        assertEquals(String.format("127.0.0.1 - - GET /examples/CountRequestsServlet HTTP/1.1 200%n"),text);
    }

}
