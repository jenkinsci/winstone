package winstone.accesslog;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import winstone.Launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class SimpleAccessLoggerTest extends Assert {
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
        Launcher winstone = new Launcher(args);

        // make a request
        makeRequest();
        winstone.shutdown();

        // check the log file
        String text = FileUtils.readFileToString(logFile);
        assertEquals("127.0.0.1 - - GET /examples/CountRequestsServlet HTTP/1.1 200\n",text);
    }

    private void makeRequest() throws IOException, SAXException {
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10003/examples/CountRequestsServlet");
        WebResponse wresp = wc.getResponse(wreq);
        InputStream content = wresp.getInputStream();
        assertTrue("Loading CountRequestsServlet", content.available() > 0);
        content.close();
    }
}
