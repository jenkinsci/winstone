package winstone;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.After;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest {
    protected Launcher winstone;
    protected WebConversation wc = new WebConversation();

    @After
    public void tearDown() {
        if (winstone!=null)
            winstone.shutdown();
    }

    public String makeRequest(String url) throws IOException, SAXException {
        WebRequest wreq = new GetMethodWebRequest(url);
        WebResponse wresp = wc.getResponse(wreq);
        InputStream content = wresp.getInputStream();
        assertTrue("Loading CountRequestsServlet", content.available() > 0);
        String s = new String(content.readAllBytes(), StandardCharsets.UTF_8);
        content.close();
        return s;
    }

    protected void assertConnectionRefused(String host, int port) throws IOException {
        try (Socket s = new Socket(host,port)) {
            fail("shouldn't be listening on 127.0.0.1");
        } catch (ConnectException e) {
            // expected
        }
    }
}
