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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Holds the object pooling code for Winstone. Presently this is only responses
 * and requests, but may increase.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: ObjectPool.java,v 1.9 2006/11/18 14:56:59 rickknowles Exp $
 */
public class ObjectPool {
    private static final long FLUSH_PERIOD = 60000L;
    
    private final int maxIdleRequestHandlersInPool;
    private final int maxConcurrentRequests;
    private long RETRY_PERIOD = 1000;
    private int START_REQUESTS_IN_POOL = 10;
    private int MAX_REQUESTS_IN_POOL = 1000;
    private int START_RESPONSES_IN_POOL = 10;
    private int MAX_RESPONSES_IN_POOL = 1000;

    private final ExecutorService requestHandler;

    private List<WinstoneRequest> unusedRequestPool;
    private List<WinstoneResponse> unusedResponsePool;
    private final Object requestPoolSemaphore = new Object();
    private final Object responsePoolSemaphore = new Object();
    private boolean simulateModUniqueId;
    private boolean saveSessions;

    /**
     * Constructs an instance of the object pool, including handlers, requests
     * and responses
     */
    public ObjectPool(Map args) throws IOException {
        this.simulateModUniqueId = Option.SIMULATE_MOD_UNIQUE_ID.get(args);
        this.saveSessions = Option.USE_SAVED_SESSIONS.get(args);
//        this.STARTUP_REQUEST_HANDLERS_IN_POOL = Option.HANDLER_COUNT_STARTUP.get(args);
        this.maxConcurrentRequests = Option.HANDLER_COUNT_MAX.get(args);
        this.maxIdleRequestHandlersInPool = Option.HANDLER_COUNT_MAX_IDLE.get(args);

        ExecutorService es = new ThreadPoolExecutor(maxIdleRequestHandlersInPool, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS, // idle thread will only hang around for 60 secs
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    private int threadIndex;
                    public synchronized Thread newThread(Runnable r) {
                        String threadName = Launcher.RESOURCES.getString(
                                "RequestHandlerThread.ThreadName", "" + (++threadIndex));
                
                        // allocate a thread to run on this object
                        Thread thread = new Thread(r, threadName);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        requestHandler = new BoundedExecutorService(es, maxConcurrentRequests);

        // Build the request/response pools
        this.unusedRequestPool = new ArrayList();
        this.unusedResponsePool = new ArrayList();

        // Initialise the request/response pools
        for (int n = 0; n < START_REQUESTS_IN_POOL; n++) {
            this.unusedRequestPool.add(new WinstoneRequest());
        }
        for (int n = 0; n < START_RESPONSES_IN_POOL; n++) {
            this.unusedResponsePool.add(new WinstoneResponse());
        }
    }

    public void destroy() {
        requestHandler.shutdown();
    }

    /**
     * Once the socket request comes in, this method is called. It reserves a
     * request handler, then delegates the socket to that class. When it
     * finishes, the handler is released back into the pool.
     */
    public void handleRequest(Socket socket, Listener listener) throws IOException, InterruptedException {
        try {
            requestHandler.submit(new RequestHandlerThread(this.simulateModUniqueId,this.saveSessions,socket,listener));
        } catch (RejectedExecutionException e) {
            Logger.log(Logger.WARNING, Launcher.RESOURCES,
                    "ObjectPool.NoRHPoolThreads");
            socket.close();
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
                req = this.unusedRequestPool.remove(unused - 1);
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
                rsp = this.unusedResponsePool.remove(unused - 1);
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
