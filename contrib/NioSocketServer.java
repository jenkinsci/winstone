package winstone.nio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: NioSocketServer.java,v 1.1 2006/08/27 14:22:32 rickknowles Exp $
 */
public class NioSocketServer implements Runnable {

    private final static int LISTEN_PORT = 6475;
    
    private Thread thread;
    private Selector selector;
    
    private ServerSocket serverSocket;
    
    public NioSocketServer(boolean useNIO) throws IOException {
        if (useNIO) {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket ss = ssc.socket();
            ss.bind(new InetSocketAddress(LISTEN_PORT));
            
            this.selector = Selector.open();
            ssc.register(this.selector, SelectionKey.OP_ACCEPT);
        } else {
            this.serverSocket = new ServerSocket(LISTEN_PORT);
            this.serverSocket.setSoTimeout(500);
        }

        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();
    }
    
    public void run() {
        boolean interrupted = false;
        while (!interrupted) {
            try {
                if (this.selector != null) {
                    nioLoop();
                } else {
                    jioLoop();
                }
                interrupted = Thread.interrupted();
            } catch (IOException err) {
                err.printStackTrace();
                interrupted = true;
            }
        }
        this.thread = null;
    }

    private void nioLoop() throws IOException {
        this.selector.select(500);
        Set selectedKeys = this.selector.selectedKeys();
        Iterator i = selectedKeys.iterator();
        while (i.hasNext()) {
            SelectionKey key = (SelectionKey) i.next();
            if (key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(this.selector, SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(10);
                buffer.clear();
                sc.read(buffer);
                buffer.flip();
                sc.write(buffer);
                sc.close();
            }
            i.remove();
        }
    }
    
    private void jioLoop() throws IOException {
        Socket socket = null;
        try {
            socket = this.serverSocket.accept();
        } catch (SocketTimeoutException err) {
        }
        if (socket != null) {
            InputStream in = socket.getInputStream();
            int pos = 0;
            int read = 0;
            byte buffer[] = new byte[10];
            while ((pos < buffer.length) && ((read = in.read(buffer, pos, buffer.length - pos)) != -1)){
                pos += read;
            }
            OutputStream out = socket.getOutputStream();
            out.write(buffer, 0, pos);
            in.close();
            out.close();
            socket.close();
        }
    }
    
    public void stop() {
        this.thread.interrupt();
    }
    
    public static void main(String argv[]) throws Exception {
        
        String iterArg = argv.length > 1 ? argv[1] : "1000";
        int ITERATION_COUNT = Integer.parseInt(iterArg);
        boolean useNIO = argv.length > 0 && argv[0].equals("nio"); 
        
        InetAddress LOCATION = InetAddress.getLocalHost();
        System.out.println("Address: " + LOCATION);
        
        NioSocketServer server = new NioSocketServer(useNIO);
        Thread.sleep(1000);
        
        long startTime = System.currentTimeMillis();
        
        byte TEST_ARRAY[] = "1234567890".getBytes();
        for (int n = 0; n < ITERATION_COUNT; n++) {
            byte buffer[] = new byte[TEST_ARRAY.length];
            Socket socket = new Socket(LOCATION, LISTEN_PORT);
            socket.setSoTimeout(50);
            OutputStream out = socket.getOutputStream();
            out.write(TEST_ARRAY);
            
            InputStream in = socket.getInputStream();
            int read = 0;
            int pos = 0;
            while ((pos < buffer.length) && ((read = in.read(buffer, pos, buffer.length - pos)) != -1)){
                pos += read;
            }
            in.close();
            out.close();
            socket.close();
//            if (!Arrays.equals(TEST_ARRAY, buffer)) {
//                throw new RuntimeException("in and out arrays are not equal");
//            }
            if (n % 500 == 0) {
                System.out.println("Completed " + n + " iterations in " + 
                        (System.currentTimeMillis() - startTime) + "ms");
            }
        }
        System.out.println("Completed " + ITERATION_COUNT + " iterations in " + 
                (System.currentTimeMillis() - startTime) + "ms");
        server.stop();
    }
}
