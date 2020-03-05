package winstone;

import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

public class FormSubmissionTest extends AbstractWinstoneTest {

    @Issue("JENKINS-60409")
    @Test
    public void largeForm() throws Exception {
        Map<String,String> args = new HashMap<String,String>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "59009");
        args.put("httpListenAddress", "127.0.0.2");
        /* To see it fail:
        args.put("requestFormContentSize", "999");
        */
        winstone = new Launcher(args);

        for (int size = 1; size <= 9_999_999; size *= 3) {
            System.out.println("trying size " + size);
            WebRequest wreq = new PostMethodWebRequest("http://127.0.0.2:59009/AcceptFormServlet");
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < size; i++) {
                b.append('.');
            }
            wreq.setParameter("x", b.toString());
            WebResponse wresp = wc.getResponse(wreq);
            try (InputStream content = wresp.getInputStream()) {
                assertTrue("Loading AcceptFormServlet at size " + size, content.available() > 0);
                assertEquals("correct response at size " + size, "received " + (size + "x=".length()) + " bytes", IOUtils.toString(content, StandardCharsets.US_ASCII));
            }
        }
    }

}
