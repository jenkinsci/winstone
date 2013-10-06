package winstone;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import org.junit.After;
import org.junit.Assert;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractWinstoneTest extends Assert {
    protected Launcher winstone;

    @After
    public void tearDown() {
        if (winstone!=null)
            winstone.shutdown();
    }

    public void makeRequest(String url) throws IOException, SAXException {
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(url);
        WebResponse wresp = wc.getResponse(wreq);
        InputStream content = wresp.getInputStream();
        assertTrue("Loading CountRequestsServlet", content.available() > 0);
        content.close();
    }
}
