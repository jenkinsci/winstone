/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase.load;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import winstone.Logger;
import winstone.WinstoneResourceBundle;

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
    private HttpClient client;
    private Thread thread;
    private boolean interrupted;
    private LoadTestThread next;

    public LoadTestThread(String url, LoadTest loadTest,
            WinstoneResourceBundle resources, HttpClient client,
            int delayedThreads) {
        this.resources = resources;
        this.url = url;
        this.loadTest = loadTest;
        this.client = client;
        this.delayBeforeStarting = 1000L * delayedThreads;
        this.interrupted = false;
        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();

        // Launch the next second's getter
        if (delayedThreads > 0)
            this.next = new LoadTestThread(url, loadTest, resources, client,
                    delayedThreads - 1);
    }

    @Override
    public void run() {
        if (this.delayBeforeStarting > 0)
            try {
                Thread.sleep(this.delayBeforeStarting);
            } catch (InterruptedException err) {
            }

        long startTime = System.currentTimeMillis();

        try {
            if (this.client == null) {
                this.client = HttpClient.newHttpClient();
            }

            // Access the URL
            HttpRequest request = HttpRequest.newBuilder(new URI(this.url)).GET().build();
            HttpResponse<String> response =
                    this.client.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();
            if (responseCode >= 400)
                throw new IOException("Failed with status " + responseCode);
            if (this.interrupted) {
                return;
            }
            this.loadTest.incTimeTotal(System.currentTimeMillis() - startTime);
            this.loadTest.incSuccessCount();
        } catch (IOException | InterruptedException | URISyntaxException err) {
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
