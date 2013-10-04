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

    private final ExecutorService requestHandler;

    /**
     * Constructs an instance of the object pool, including handlers, requests
     * and responses
     */
    public ObjectPool(Map args) throws IOException {
//        this.STARTUP_REQUEST_HANDLERS_IN_POOL = Option.HANDLER_COUNT_STARTUP.get(args);
        int maxConcurrentRequests = Option.HANDLER_COUNT_MAX.get(args);
        int maxIdleRequestHandlersInPool = Option.HANDLER_COUNT_MAX_IDLE.get(args);

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
    }

    public ExecutorService getRequestHandler() {
        return requestHandler;
    }

    public void destroy() {
        requestHandler.shutdown();
    }
}
