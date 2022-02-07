/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase.load;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

import winstone.Logger;
import winstone.WinstoneResourceBundle;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * A single worked thread in the load testing program
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: LoadTestThread.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class LoadTestThread implements Runnable {
    private WinstoneResourceBundle resources;
    private String url;
    private long delayBeforeStarting;
    private LoadTest loadTest;
    private WebConversation webConv;
    private Thread thread;
    private boolean interrupted;
    private LoadTestThread next;

    public LoadTestThread(String url, LoadTest loadTest,
            WinstoneResourceBundle resources, WebConversation webConv,
            int delayedThreads) {
        this.resources = resources;
        this.url = url;
        this.loadTest = loadTest;
        this.webConv = webConv;
        this.delayBeforeStarting = 1000 * delayedThreads;
        this.interrupted = false;
        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();

        // Launch the next second's getter
        if (delayedThreads > 0)
            this.next = new LoadTestThread(url, loadTest, resources, webConv,
                    delayedThreads - 1);
    }

    public void run() {
        if (this.delayBeforeStarting > 0)
            try {
                Thread.sleep(this.delayBeforeStarting);
            } catch (InterruptedException err) {
            }

        long startTime = System.currentTimeMillis();

        try {
            if (this.webConv == null)
                this.webConv = new WebConversation();

            // Access the URL
            WebRequest wreq = new GetMethodWebRequest(this.url);
            WebResponse wresp = this.webConv.getResponse(wreq);
            int responseCode = wresp.getResponseCode();
            if (responseCode >= 400)
                throw new IOException("Failed with status " + responseCode);
            InputStream inContent = wresp.getInputStream();
            int contentLength = wresp.getContentLength();
            byte[] content = new byte[contentLength == -1 ? 100 * 1024
                    : contentLength];
            int position = 0;
            int value = inContent.read();
            while ((value != -1)
                    && (((contentLength >= 0) && (position < contentLength)) || (contentLength < 0))) {
                content[position++] = (byte) value;
                value = inContent.read();
            }
            inContent.close();

            // Confirm the result is the same size the content-length said it
            // was
            if ((position == contentLength) || (contentLength == -1)) {
                if (this.interrupted)
                    return;
                this.loadTest.incTimeTotal(System.currentTimeMillis()
                        - startTime);
                this.loadTest.incSuccessCount();
            } else
                throw new IOException("Only downloaded " + position + " of "
                        + contentLength + " bytes");
        } catch (IOException | SAXException err) {
            Logger.log(Logger.DEBUG, resources, "LoadTestThread.Error", err);
        }
    }

    public void destroy() {
        this.interrupted = true;
        this.thread.interrupt();
        if (this.next != null)
            this.next.destroy();
    }
}
