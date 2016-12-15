package winstone;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest extends Assert {
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
        String s = IOUtils.toString(content);
        content.close();
        return s;
    }

    protected void assertConnectionRefused(String host, int port) throws IOException {
        try {
            new Socket(host, port);
            fail("shouldn't be listening on 127.0.0.1");
        } catch (ConnectException e) {
            // expected
        }
    }

    protected String responseHeader (String url, String header) throws IOException, SAXException {
        WebRequest wreq = new GetMethodWebRequest(url);
        WebResponse wresp = wc.getResponse(wreq);
        return wresp.getHeaderField(header);
    }
}
