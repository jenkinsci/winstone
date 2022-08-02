package winstone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class FormSubmissionTest extends AbstractWinstoneTest {

    @Issue("JENKINS-60409")
    @Test
    public void largeForm() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");
        args.put("httpListenAddress", "127.0.0.2");
        /* To see it fail:
        args.put("requestFormContentSize", "999");
        */
        winstone = new Launcher(args);
        int port = ((ServerConnector)winstone.server.getConnectors()[0]).getLocalPort();
        for (int size = 1; size <= 9_999_999; size *= 3) {
            System.out.println("trying size " + size);
            WebRequest wreq = new PostMethodWebRequest("http://127.0.0.2:"+port+"/AcceptFormServlet");
            wreq.setParameter("x", ".".repeat(size));
            WebResponse wresp = wc.getResponse(wreq);
            try (InputStream content = wresp.getInputStream()) {
                assertTrue("Loading AcceptFormServlet at size " + size, content.available() > 0);
                assertEquals("correct response at size " + size, "received " + (size + "x=".length()) + " bytes", IOUtils.toString(content, StandardCharsets.US_ASCII));
            }
        }
    }

}
