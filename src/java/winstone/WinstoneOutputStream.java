/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;

import javax.servlet.http.Cookie;

/**
 * Matches the socket output stream to the servlet output.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneOutputStream.java,v 1.19 2007/10/14 14:48:14 rickknowles Exp $
 */
public class WinstoneOutputStream extends javax.servlet.ServletOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final byte[] CR_LF = "\r\n".getBytes();
    protected OutputStream outStream;
    protected long bufferSize;
    protected long bufferPosition;
    protected long bytesCommitted;
    protected ByteArrayOutputStream buffer;
    protected boolean committed;
    protected boolean bodyOnly;
    protected WinstoneResponse owner;
    protected boolean disregardMode = false;
    protected boolean closed = false;
    protected Stack includeByteStreams;
    
    /**
     * Constructor
     */
    public WinstoneOutputStream(OutputStream out, boolean bodyOnlyForInclude) {
        this.outStream = new ClientOutputStream(out);
        this.bodyOnly = bodyOnlyForInclude;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.committed = false;
        // this.headersWritten = false;
        this.buffer = new ByteArrayOutputStream();
    }

    public void setResponse(WinstoneResponse response) {
        this.owner = response;
    }

    public long getBufferSize() {
        return this.bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        if (this.owner.isCommitted()) {
            throw new IllegalStateException(Launcher.RESOURCES.getString(
                    "WinstoneOutputStream.AlreadyCommitted"));
        }
        this.bufferSize = bufferSize;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public long getOutputStreamLength() {
        return this.bytesCommitted + this.bufferPosition;
    }

    public long getBytesCommitted() {
        return this.bytesCommitted;
    }
    
    public void setDisregardMode(boolean disregard) {
        this.disregardMode = disregard;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void write(int oneChar) throws IOException {
        if (this.disregardMode || this.closed) {
            return;
        }
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) && 
                (this.bytesCommitted >= Long.parseLong(contentLengthHeader))) {
            return;
        }
//        System.out.println("Out: " + this.bufferPosition + " char=" + (char)oneChar);
        this.buffer.write(oneChar);
        commit(contentLengthHeader, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (this.disregardMode || this.closed) {
            return;
        }
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if ((contentLengthHeader != null) &&
                (this.bytesCommitted+len > Long.parseLong(contentLengthHeader))) {
            return;
        }

        this.buffer.write(b,off,len);
        commit(contentLengthHeader,len);
    }
    
    private void commit(String contentLengthHeader, int len) throws IOException {
        this.bufferPosition+= len;
        // if (this.headersWritten)
        if (this.bufferPosition >= this.bufferSize) {
            commit();
        } else if ((contentLengthHeader != null) &&
                ((this.bufferPosition + this.bytesCommitted)
                        >= Long.parseLong(contentLengthHeader))) {
            commit();
        }
    }

    public void commit() throws IOException {
        this.buffer.flush();

        // If we haven't written the headers yet, write them out
        if (!this.committed && !this.bodyOnly) {
            this.owner.validateHeaders();
            this.committed = true;

            Logger.log(Logger.DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.CommittingOutputStream");
            
            int statusCode = this.owner.getStatus();
            String reason = Launcher.RESOURCES.getString("WinstoneOutputStream.reasonPhrase." + statusCode);
            String statusLine = this.owner.getProtocol() + " " + statusCode + " " + 
                    (reason == null ? "No reason" : reason);
            OutputStream o = new BufferedOutputStream(outStream);
            o.write(statusLine.getBytes("8859_1"));
            o.write(CR_LF);
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResponseStatus", statusLine);

            // Write headers and cookies
            for (String header : this.owner.getHeaders()) {
                o.write(URIUtil.noCRLF(header).getBytes("8859_1"));
                o.write(CR_LF);
                Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                        "WinstoneOutputStream.Header", header);
            }

            if (!this.owner.getHeaders().isEmpty()) {
                for (Object o1 : this.owner.getCookies()) {
                    Cookie cookie = (Cookie) o1;
                    String cookieText = this.owner.writeCookie(cookie);
                    o.write(cookieText.getBytes("8859_1"));
                    o.write(CR_LF);
                    Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                            "WinstoneOutputStream.Header", cookieText);
                }
            }
            o.write(CR_LF);
            o.flush();
            // Logger.log(Logger.FULL_DEBUG,
            // Launcher.RESOURCES.getString("HttpProtocol.OutHeaders") + out.toString());
        }
        byte content[] = this.buffer.toByteArray();
        this.buffer.reset();
        this.bufferPosition = 0;

//        winstone.ajp13.Ajp13Listener.packetDump(content, content.length);
//        this.buffer.writeTo(this.outStream);
        long commitLength = content.length;
        String contentLengthHeader = this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER);
        if (contentLengthHeader != null) {
            commitLength = Math.min (Long.parseLong(contentLengthHeader)
                    - this.bytesCommitted, (long)content.length);
        }
        if (commitLength > 0) {
            this.outStream.write(content, 0, (int)commitLength);
        }
        this.outStream.flush();

        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                "WinstoneOutputStream.CommittedBytes", 
                "" + (this.bytesCommitted + commitLength));

        this.bytesCommitted += commitLength;
    }

    public void reset() {
        if (isCommitted())
            throw new IllegalStateException(Launcher.RESOURCES
                    .getString("WinstoneOutputStream.AlreadyCommitted"));
        else {
            Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES,
                    "WinstoneOutputStream.ResetBuffer", this.bufferPosition
                            + "");
            this.buffer.reset();
            this.bufferPosition = 0;
            this.bytesCommitted = 0;
        }
    }

    public void finishResponse() throws IOException {
        this.outStream.flush();
        this.outStream = null;
    }

    public void flush() throws IOException {
        if (this.disregardMode) {
            return;
        }
        Logger.log(Logger.FULL_DEBUG, Launcher.RESOURCES, "WinstoneOutputStream.Flushing");
        this.buffer.flush();
        this.commit();
    }

    public void close() throws IOException {
        if (!isCommitted() && !this.disregardMode && !this.closed &&
                (this.owner.getHeader(WinstoneResponse.CONTENT_LENGTH_HEADER) == null)) {
            if ((this.owner != null) && !this.bodyOnly) {
                this.owner.setContentLength((int)getOutputStreamLength());
            }
        }
        flush();
    }

    // Include related buffering
    public boolean isIncluding() {
        return (this.includeByteStreams != null && !this.includeByteStreams.isEmpty());
    }
    
    public void startIncludeBuffer() {
        synchronized (this.buffer) {
            if (this.includeByteStreams == null) {
                this.includeByteStreams = new Stack();
            }
        }
        this.includeByteStreams.push(new ByteArrayOutputStream());
    }
    
    public void finishIncludeBuffer() throws IOException {
        if (isIncluding()) {
            ByteArrayOutputStream body = (ByteArrayOutputStream) this.includeByteStreams.pop();
            OutputStream topStream = this.outStream;
            if (!this.includeByteStreams.isEmpty()) {
                topStream = (OutputStream) this.includeByteStreams.peek();
            }
            byte bodyArr[] = body.toByteArray();
            if (bodyArr.length > 0) {
                topStream.write(bodyArr);
            }
            body.close();
        }
    }
    
    public void clearIncludeStackForForward() throws IOException {
        if (isIncluding()) {
            for (Object includeByteStream : this.includeByteStreams) {
                ((ByteArrayOutputStream) includeByteStream).close();
            }
            this.includeByteStreams.clear();
        }
    }
}
