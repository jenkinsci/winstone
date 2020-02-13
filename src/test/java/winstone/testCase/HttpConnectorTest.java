/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.xml.sax.SAXException;

import winstone.Launcher;
import winstone.Logger;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebImage;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Test case for the Http Connector to Winstone. Simulates a simple connect and
 * retrieve case, then a keep-alive connection case.
 *
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: HttpConnectorTest.java,v 1.8 2007/04/23 15:06:22 rickknowles Exp $
 */
public class HttpConnectorTest extends TestCase {
    public static Test suite() {
        return (new TestSuite(HttpConnectorTest.class));
    }

    /**
     * Constructor
     */
    public HttpConnectorTest(String name) {
        super(name);
    }

    /**
     * Test the simple case of connecting, retrieving and disconnecting
     */
    public void testSimpleConnection() throws IOException, SAXException,
            InterruptedException {
        // Initialise container
        Map<String,String> args = new HashMap<>();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10003");
        args.put("ajp13Port", "-1");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Logger.FULL_DEBUG, System.out, true);
        Launcher winstone = new Launcher(args);

        // Check for a simple connection
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10003/examples/CountRequestsServlet");
        WebResponse wresp = wc.getResponse(wreq);
        InputStream content = wresp.getInputStream();
        assertTrue("Loading CountRequestsServlet", content.available() > 0);
        content.close();
        winstone.shutdown();
        Thread.sleep(500);
    }

    /**
     * Test the keep alive case
     */
    public void testKeepAliveConnection() throws IOException,
            InterruptedException, SAXException {
        // Initialise container
        Map<String,String> args = new HashMap<>();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10004");
        args.put("ajp13Port", "-1");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Logger.FULL_DEBUG, System.out, true);
        Launcher winstone = new Launcher(args);

        // Check for a simple connection
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10004/examples/CountRequestsServlet");
        WebResponse wresp1 = wc.getResponse(wreq);
        WebImage img[] = wresp1.getImages();
        for (WebImage anImg : img) wc.getResponse(anImg.getRequest());
        // Thread.sleep(2000);
        // WebResponse wresp2 = wc.getResponse(wreq);
        // Thread.sleep(2000);
        //WebResponse wresp3 = wc.getResponse(wreq);
        InputStream content = wresp1.getInputStream();
        assertTrue("Loading CountRequestsServlet + child images", content
                .available() > 0);
        content.close();
        winstone.shutdown();
        Thread.sleep(500);
    }

    /**
     * Test the keep alive case
     */
    public void testWriteAfterServlet() throws IOException,
            InterruptedException, SAXException {
        // Initialise container
        Map<String,String> args = new HashMap<>();
        args.put("webroot", "target/testwebapp");
        args.put("prefix", "/examples");
        args.put("httpPort", "10005");
        args.put("ajp13Port", "-1");
        args.put("controlPort", "-1");
        args.put("debug", "8");
        args.put("logThrowingLineNo", "true");
        Logger.init(Logger.FULL_DEBUG, System.out, true);
        Launcher winstone = new Launcher(args);

        // Check for a simple connection
        WebConversation wc = new WebConversation();
        WebRequest wreq = new GetMethodWebRequest(
                "http://localhost:10005/examples/TestWriteAfterServlet");
        WebResponse wresp1 = wc.getResponse(wreq);
        Logger.logDirectMessage(Logger.INFO, "log", "Output: " + wresp1.getText(), null);
        assertTrue(wresp1.getText().endsWith("Hello"));
        winstone.shutdown();
        Thread.sleep(500);
    }
}
