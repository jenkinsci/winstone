/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import winstone.cmdline.Option;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Holds the object pooling code for Winstone. Presently this is only responses
 * and requests, but may increase.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ObjectPool.java,v 1.9 2006/11/18 14:56:59 rickknowles Exp $
 */
public class ObjectPool implements Runnable {
    private static final long FLUSH_PERIOD = 60000L;
    
    private int STARTUP_REQUEST_HANDLERS_IN_POOL = 5;
    private int MAX_IDLE_REQUEST_HANDLERS_IN_POOL = 50;
    private int MAX_REQUEST_HANDLERS_IN_POOL = 1000;
    private long RETRY_PERIOD = 1000;
    private int START_REQUESTS_IN_POOL = 10;
    private int MAX_REQUESTS_IN_POOL = 1000;
    private int START_RESPONSES_IN_POOL = 10;
    private int MAX_RESPONSES_IN_POOL = 1000;
    private List unusedRequestHandlerThreads;
    private List usedRequestHandlerThreads;
    private List unusedRequestPool;
    private List unusedResponsePool;
    private Object requestHandlerSemaphore = true;
    private Object requestPoolSemaphore = true;
    private Object responsePoolSemaphore = true;
    private int threadIndex = 0;
    private boolean simulateModUniqueId;
    private boolean saveSessions;

    private Thread thread;
    
    /**
     * Constructs an instance of the object pool, including handlers, requests
     * and responses
     */
    public ObjectPool(Map args) throws IOException {
        this.simulateModUniqueId = Option.SIMULATE_MOD_UNIQUE_ID.get(args);
        this.saveSessions = Option.USE_SAVED_SESSIONS.get(args);

        // Build the initial pool of handler threads
        this.unusedRequestHandlerThreads = new ArrayList();
        this.usedRequestHandlerThreads = new ArrayList();

        // Build the request/response pools
        this.unusedRequestPool = new ArrayList();
        this.unusedResponsePool = new ArrayList();

        // Get handler pool options
        STARTUP_REQUEST_HANDLERS_IN_POOL = Option.HANDLER_COUNT_STARTUP.get(args);
        MAX_REQUEST_HANDLERS_IN_POOL     = Option.HANDLER_COUNT_MAX.get(args);
        MAX_IDLE_REQUEST_HANDLERS_IN_POOL = Option.HANDLER_COUNT_MAX_IDLE.get(args);

        // Start the base set of handler threads
        for (int n = 0; n < STARTUP_REQUEST_HANDLERS_IN_POOL; n++) {
            this.unusedRequestHandlerThreads
                    .add(new RequestHandlerThread(this, 
                            this.threadIndex++, this.simulateModUniqueId,
                            this.saveSessions));
        }

        // Initialise the request/response pools
        for (int n = 0; n < START_REQUESTS_IN_POOL; n++) {
            this.unusedRequestPool.add(new WinstoneRequest());
        }
        for (int n = 0; n < START_RESPONSES_IN_POOL; n++) {
            this.unusedResponsePool.add(new WinstoneResponse());
        }
        
        this.thread = new Thread(this, "WinstoneObjectPoolMgmt");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                Thread.sleep(FLUSH_PERIOD);
                removeUnusedRequestHandlers();
            } catch (InterruptedException err) {
                interrupted = true;
            }
        }
        this.thread = null;
    }
    
    private void removeUnusedRequestHandlers() {
        // Check max idle requestHandler count
        synchronized (this.requestHandlerSemaphore) {
            // If we have too many idle request handlers
            while (this.unusedRequestHandlerThreads.size() > MAX_IDLE_REQUEST_HANDLERS_IN_POOL) {
                RequestHandlerThread rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.get(0);
                rh.destroy();
                this.unusedRequestHandlerThreads.remove(rh);
            }
        }
    }

    public void destroy() {
        synchronized (this.requestHandlerSemaphore) {
            Collection usedHandlers = new ArrayList(this.usedRequestHandlerThreads);
            for (Object usedHandler : usedHandlers) releaseRequestHandler((RequestHandlerThread) usedHandler);
            Collection unusedHandlers = new ArrayList(this.unusedRequestHandlerThreads);
            for (Object unusedHandler : unusedHandlers) ((RequestHandlerThread) unusedHandler).destroy();
            this.unusedRequestHandlerThreads.clear();
        }
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

    /**
     * Once the socket request comes in, this method is called. It reserves a
     * request handler, then delegates the socket to that class. When it
     * finishes, the handler is released back into the pool.
     */
    public void handleRequest(Socket socket, Listener listener)
            throws IOException, InterruptedException {
        RequestHandlerThread rh = null;
        synchronized (this.requestHandlerSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedRequestHandlerThreads.size();
            if (unused > 0) {
                rh = (RequestHandlerThread) this.unusedRequestHandlerThreads.remove(unused - 1);
                this.usedRequestHandlerThreads.add(rh);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "ObjectPool.UsingRHPoolThread", "" + this.usedRequestHandlerThreads.size(),
                        "" + this.unusedRequestHandlerThreads.size());
            }

            // If we are out (and not over our limit), allocate a new one
            else if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL) {
                rh = new RequestHandlerThread(this,
                        this.threadIndex++, this.simulateModUniqueId,
                        this.saveSessions);
                this.usedRequestHandlerThreads.add(rh);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "ObjectPool.NewRHPoolThread", "" + this.usedRequestHandlerThreads.size(),
                        "" + this.unusedRequestHandlerThreads.size());
            }

            // otherwise throw fail message - we've blown our limit
            else {
                // Possibly insert a second chance here ? Delay and one retry ?
                // Remember to release the lock first
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                        "ObjectPool.NoRHPoolThreadsRetry");
                // socket.close();
                // throw new UnavailableException("NoHandlersAvailable");
            }
        }

        if (rh != null)
            rh.commenceRequestHandling(socket, listener);
        else {
            // Sleep for a set period and try again from the pool
            Thread.sleep(RETRY_PERIOD);

            synchronized (this.requestHandlerSemaphore) {
                if (this.usedRequestHandlerThreads.size() < MAX_REQUEST_HANDLERS_IN_POOL) {
                    rh = new RequestHandlerThread(this, 
                            this.threadIndex++, this.simulateModUniqueId,
                            this.saveSessions);
                    this.usedRequestHandlerThreads.add(rh);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "ObjectPool.NewRHPoolThread", "" + this.usedRequestHandlerThreads.size(),
                            "" + this.unusedRequestHandlerThreads.size());
                }
            }
            if (rh != null)
                rh.commenceRequestHandling(socket, listener);
            else {
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                        "ObjectPool.NoRHPoolThreads");
                socket.close();
            }
        }
    }

    /**
     * Release the handler back into the pool
     */
    public void releaseRequestHandler(RequestHandlerThread rh) {
        synchronized (this.requestHandlerSemaphore) {
            this.usedRequestHandlerThreads.remove(rh);
            this.unusedRequestHandlerThreads.add(rh);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "ObjectPool.ReleasingRHPoolThread", "" + this.usedRequestHandlerThreads.size(),
                    "" + this.unusedRequestHandlerThreads.size());
        }
    }

    /**
     * An attempt at pooling request objects for reuse.
     */
    public WinstoneRequest getRequestFromPool() throws IOException {
        WinstoneRequest req;
        synchronized (this.requestPoolSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedRequestPool.size();
            if (unused > 0) {
                req = (WinstoneRequest) this.unusedRequestPool.remove(unused - 1);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "ObjectPool.UsingRequestFromPool", ""
                                + this.unusedRequestPool.size());
            }
            // If we are out, allocate a new one
            req = new WinstoneRequest();
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "ObjectPool.NewRequestForPool");
        }
        return req;
    }

    public void releaseRequestToPool(WinstoneRequest req) {
        req.cleanUp();
        synchronized (this.requestPoolSemaphore) {
            if(this.unusedRequestPool.size() < MAX_REQUESTS_IN_POOL)
                this.unusedRequestPool.add(req);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "ObjectPool.RequestReleased", ""
                            + this.unusedRequestPool.size());
        }
    }

    /**
     * An attempt at pooling request objects for reuse.
     */
    public WinstoneResponse getResponseFromPool() {
        WinstoneResponse rsp;
        synchronized (this.responsePoolSemaphore) {
            // If we have any spare, get it from the pool
            int unused = this.unusedResponsePool.size();
            if (unused > 0) {
                rsp = (WinstoneResponse) this.unusedResponsePool.remove(unused - 1);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "ObjectPool.UsingResponseFromPool", ""
                                + this.unusedResponsePool.size());
            }
            // If we are out, allocate a new one
            rsp = new WinstoneResponse();
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "ObjectPool.NewResponseForPool");
        }
        return rsp;
    }

    public void releaseResponseToPool(WinstoneResponse rsp) {
        rsp.cleanUp();
        synchronized (this.responsePoolSemaphore) {
            if(this.unusedResponsePool.size() < MAX_RESPONSES_IN_POOL)
                this.unusedResponsePool.add(rsp);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "ObjectPool.ResponseReleased", ""
                            + this.unusedResponsePool.size());
        }
    }

}
