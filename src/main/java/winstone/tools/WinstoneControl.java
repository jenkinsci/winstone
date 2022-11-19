/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.tools;

import winstone.Launcher;
import winstone.Logger;
import winstone.WinstoneResourceBundle;
import winstone.cmdline.CmdLineParser;
import winstone.cmdline.Option;
import winstone.cmdline.Option.ODebugInt;
import winstone.cmdline.Option.OInt;
import winstone.cmdline.Option.OString;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;

/**
 * Included so that we can control winstone from the command line a little more
 * easily.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneControl.java,v 1.6 2006/03/13 15:37:29 rickknowles Exp $
 */
public class WinstoneControl {
    private final static WinstoneResourceBundle TOOLS_RESOURCES = new WinstoneResourceBundle("winstone.tools.LocalStrings");

    final static String OPERATION_SHUTDOWN = "shutdown";
    final static String OPERATION_RELOAD = "reload:";
    static int TIMEOUT = 10000;

    public final static OInt CONTROL_PORT = Option.integer("controlPort");
    public final static OInt PORT = Option.integer("port");
    public final static OInt DEBUG = new ODebugInt("debug", 5);
    public final static OString HOST = Option.string("host", "localhost");


    /**
     * Parses command line parameters, and calls the appropriate method for
     * executing the winstone operation required.
     */
    public static void main(String[] argv) throws Exception {

        // Load args from the config file
        Map<String, String> options = new CmdLineParser(Option.all(WinstoneControl.class)).parse(argv,"operation");
        String operation = options.get("operation");

        if (operation.equals("")) {
            printUsage();
            return;
        }

        Logger.setCurrentDebugLevel(DEBUG.get(options));

        String host = HOST.get(options);
        int port = PORT.get(options, CONTROL_PORT.get(options));

        Logger.log(Level.INFO, TOOLS_RESOURCES, "WinstoneControl.UsingHostPort",host, port);

        // Check for shutdown
        if (operation.equalsIgnoreCase(OPERATION_SHUTDOWN)) {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(TIMEOUT);
            try(OutputStream out = socket.getOutputStream()){
                out.write( Launcher.SHUTDOWN_TYPE );
                Logger.log(Level.INFO, TOOLS_RESOURCES, "WinstoneControl.ShutdownOK", host, port );
            }
        }

        // check for reload
        else if (operation.toLowerCase().startsWith(OPERATION_RELOAD.toLowerCase())) {
            String webappName = operation.substring(OPERATION_RELOAD.length());
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(TIMEOUT);
            try(OutputStream out = socket.getOutputStream(); //
                ObjectOutputStream objOut = new ObjectOutputStream( out )) {
                out.write( Launcher.RELOAD_TYPE );
                objOut.writeUTF( host );
                objOut.writeUTF( webappName );
            }
            Logger.log(Level.INFO, TOOLS_RESOURCES, "WinstoneControl.ReloadOK",host, port);
        }
        else {
            printUsage();
        }
    }

    /**
     * Displays the usage message
     */
    private static void printUsage() {
        System.out.println(TOOLS_RESOURCES.getString("WinstoneControl.Usage"));
    }
}
