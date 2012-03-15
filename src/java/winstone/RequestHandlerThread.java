/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * The threads to which incoming requests get allocated.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: RequestHandlerThread.java,v 1.21 2007/04/23 02:55:35 rickknowles Exp $
 */
public class RequestHandlerThread implements Runnable {
    private WinstoneInputStream inData;
    private WinstoneOutputStream outData;
    private WinstoneRequest req;
    private WinstoneResponse rsp;
    private Listener listener;
    private Socket socket;
    private long requestStartTime;
    private boolean simulateModUniqueId;
    private boolean saveSessions;
//    private Object processingMonitor = new Boolean(true);

    /**
     * Constructor - this is called by the handler pool, and just sets up for
     * when a real request comes along.
     */
    public RequestHandlerThread(boolean simulateModUniqueId, boolean saveSessions, Socket socket, Listener listener) {
        this.simulateModUniqueId = simulateModUniqueId;
        this.saveSessions = saveSessions;

        this.socket = socket;
        this.listener = listener;
    }

    /**
     * The main thread execution code.
     */
    public void run() {
        // Start request processing
        InputStream inSocket = null;
        OutputStream outSocket = null;
        boolean iAmFirst = true;
        try {
            // Get input/output streams
            inSocket = socket.getInputStream();
            outSocket = socket.getOutputStream();

            // The keep alive loop - exiting from here means the connection has closed
            boolean continueFlag = true;
            while (continueFlag) {
                try {
                    long requestId = System.currentTimeMillis();
                    this.listener.allocateRequestResponse(socket, inSocket,
                            outSocket, this, iAmFirst);
                    if (this.req == null) {
                        // Dead request - happens sometimes with ajp13 - discard
                        this.listener.deallocateRequestResponse(this, req,
                                rsp, inData, outData);
                        continue;
                    }
                    String servletURI = this.listener.parseURI(this,
                            this.req, this.rsp, this.inData, this.socket,
                            iAmFirst);
                    if (servletURI == null) {
                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                                "RequestHandlerThread.KeepAliveTimedOut", Thread.currentThread().getName());

                        // Keep alive timed out - deallocate and go into wait state
                        this.listener.deallocateRequestResponse(this, req,
                                rsp, inData, outData);
                        continueFlag = false;
                        continue;
                    }

                    if (this.simulateModUniqueId) {
                        req.setAttribute("UNIQUE_ID", "" + requestId);
                    }
                    long headerParseTime = getRequestProcessTime();
                    iAmFirst = false;

                    HostConfiguration hostConfig = req.getHostGroup().getHostByName(req.getServerName());
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "RequestHandlerThread.StartRequest",
                            "" + requestId, hostConfig.getHostname());

                    // Get the URI from the request, check for prefix, then
                    // match it to a requestDispatcher
                    WebAppConfiguration webAppConfig = hostConfig.getWebAppByURI(servletURI);
                    if (webAppConfig == null) {
                        webAppConfig = hostConfig.getWebAppByURI("/");
                    }
                    if (webAppConfig == null) {
                        Logger.log(Logger.WARNING, Launcher.RESOURCES,
                                "RequestHandlerThread.UnknownWebapp",
                                servletURI);
                        rsp.sendError(WinstoneResponse.SC_NOT_FOUND,
                                Launcher.RESOURCES.getString("RequestHandlerThread.UnknownWebappPage", servletURI));
                        rsp.flushBuffer();
                        req.discardRequestBody();
                        writeToAccessLog(servletURI, req, rsp, null);

                        // Process keep-alive
                        continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);
                        this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "RequestHandlerThread.FinishRequest",
                                "" + requestId);
                        Logger.log(Logger.SPEED, Launcher.RESOURCES, "RequestHandlerThread.RequestTime",
                                servletURI, "" + headerParseTime, "" + getRequestProcessTime());
                        continue;
                    }
                    req.setWebAppConfig(webAppConfig);

                    // Now we've verified it's in the right webapp, send
                    // request in scope notify
                    ServletRequestListener reqLsnrs[] = webAppConfig.getRequestListeners();
                    for (ServletRequestListener reqLsnr1 : reqLsnrs) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                        reqLsnr1.requestInitialized(new ServletRequestEvent(webAppConfig, req));
                        Thread.currentThread().setContextClassLoader(cl);
                    }

                    // Lookup a dispatcher, then process with it
                    processRequest(webAppConfig, req, rsp,
                            webAppConfig.getServletURIFromRequestURI(servletURI));
                    writeToAccessLog(servletURI, req, rsp, webAppConfig);

                    this.outData.finishResponse();
                    this.inData.finishRequest();

                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "RequestHandlerThread.FinishRequest",
                            "" + requestId);

                    // Process keep-alive
                    continueFlag = this.listener.processKeepAlive(req, rsp, inSocket);

                    // Set last accessed time on session as start of this
                    // request
                    req.markSessionsAsRequestFinished(this.requestStartTime, this.saveSessions);

                    // send request listener notifies
                    for (ServletRequestListener reqLsnr : reqLsnrs) {
                        ClassLoader cl = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(webAppConfig.getLoader());
                        reqLsnr.requestDestroyed(new ServletRequestEvent(webAppConfig, req));
                        Thread.currentThread().setContextClassLoader(cl);
                    }

                    req.setWebAppConfig(null);
                    rsp.setWebAppConfig(null);
                    req.setRequestAttributeListeners(null);

                    this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
                    Logger.log(Logger.SPEED, Launcher.RESOURCES, "RequestHandlerThread.RequestTime",
                            servletURI, "" + headerParseTime,
                            "" + getRequestProcessTime());
                } catch (InterruptedIOException errIO) {
                    continueFlag = false;
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "RequestHandlerThread.SocketTimeout", errIO);
                } catch (SocketException errIO) {
                    continueFlag = false;
                }
            }
            this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
            this.listener.releaseSocket(this.socket, inSocket, outSocket); // shut sockets
        } catch (Throwable err) {
            try {
                this.listener.deallocateRequestResponse(this, req, rsp, inData, outData);
            } catch (Throwable errClose) {
            }
            try {
                this.listener.releaseSocket(this.socket, inSocket,
                        outSocket); // shut sockets
            } catch (Throwable errClose) {
            }
            if (!(err instanceof ClientSocketException)) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES,
                        "RequestHandlerThread.RequestError", err);
            }
        }
    }

    /**
     * Actually process the request. This takes the request and response, and feeds
     * them to the desired servlet, which then processes them or throws them off to
     * another servlet.
     */
    private void processRequest(WebAppConfiguration webAppConfig, WinstoneRequest req, 
            WinstoneResponse rsp, String path) throws IOException {
        RequestDispatcher rd;
        javax.servlet.RequestDispatcher rdError = null;
        try {
            rd = webAppConfig.getInitialDispatcher(path, req, rsp);

            // Null RD means an error or we have been redirected to a welcome page
            if (rd != null) {
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "RequestHandlerThread.HandlingRD", rd.getName());
                rd.forward(req, rsp);
            }
            // if null returned, assume we were redirected
        } catch (ClientSocketException err) {
            // ignore this error. caused by a browser shutting down the connection
        } catch (Throwable err) {
            boolean ignore = false;
            for(Throwable t=err; t!=null; t=t.getCause()) {
                if (t instanceof ClientSocketException) {
                    ignore = true;
                    break;
                }
            }
            if(!ignore) {
                Logger.log(Logger.WARNING, Launcher.RESOURCES,
                        "RequestHandlerThread.UntrappedError", err);
                rdError = webAppConfig.getErrorDispatcherByClass(err);
            }
        }

        // If there was any kind of error, execute the error dispatcher here
        if (rdError != null) {
            try {
                if (rsp.isCommitted()) {
                    rdError.include(req, rsp);
                } else {
                    rsp.resetBuffer();
                    rdError.forward(req, rsp);
                }
            } catch (Throwable err) {
                Logger.log(Logger.ERROR, Launcher.RESOURCES, "RequestHandlerThread.ErrorInErrorServlet", err);
            }
//            rsp.sendUntrappedError(err, req, rd != null ? rd.getName() : null);
        }
        rsp.flushBuffer();
        rsp.getWinstoneOutputStream().setClosed(true);
        req.discardRequestBody();
    }

    public void setRequest(WinstoneRequest request) {
        this.req = request;
    }

    public void setResponse(WinstoneResponse response) {
        this.rsp = response;
    }

    public void setInStream(WinstoneInputStream inStream) {
        this.inData = inStream;
    }

    public void setOutStream(WinstoneOutputStream outStream) {
        this.outData = outStream;
    }

    public void setRequestStartTime() {
        this.requestStartTime = System.currentTimeMillis();
    }

    public long getRequestProcessTime() {
        return System.currentTimeMillis() - this.requestStartTime;
    }

    protected void writeToAccessLog(String originalURL, WinstoneRequest request, WinstoneResponse response,
            WebAppConfiguration webAppConfig) {
        if (webAppConfig != null) {
            // Log a row containing appropriate data
            AccessLogger logger = webAppConfig.getAccessLogger();
            if (logger != null) {
                logger.log(originalURL, request, response);
            }
        }
    }
}
