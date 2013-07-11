/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.tools;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;

import winstone.Launcher;
import winstone.Logger;
import winstone.WebAppConfiguration;
import winstone.WinstoneResourceBundle;
import winstone.cmdline.CmdLineParser;
import winstone.cmdline.Option;
import winstone.cmdline.Option.OInt;
import winstone.cmdline.Option.OString;

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

    public static OInt CONTROL_PORT = Option.integer("controlPort");
    public static OInt PORT = Option.integer("port");
    public static OInt DEBUG = new OInt("debug", 5) {
        public int get(Map args) {
            switch(super.get(args)) {
                // before switching to java.util.Logging, winstone used a (1:9) range for log levels
                case 1: return Logger.MIN.intValue();
                case 2: return Logger.ERROR.intValue();
                case 3: return Logger.WARNING.intValue();
                case 4: return Logger.INFO.intValue();
                case 6: return Logger.SPEED.intValue();
                case 7: return Logger.DEBUG.intValue();
                case 8: return Logger.FULL_DEBUG.intValue();
                case 9: return Logger.MAX.intValue();
                case 5:
                default: return Logger.INFO.intValue();
    }}};
    public static OString HOST = Option.string("host", "localhost");


    /**
     * Parses command line parameters, and calls the appropriate method for
     * executing the winstone operation required.
     */
    public static void main(String argv[]) throws Exception {

        // Load args from the config file
        Map options = new CmdLineParser(Option.all(WinstoneControl.class)).parse(argv,"operation");
        String operation = (String) options.get("operation");

        if (operation.equals("")) {
            printUsage();
            return;
        }

        Logger.setCurrentDebugLevel(DEBUG.get(options));

        String host = HOST.get(options);
        int port = PORT.get(options, CONTROL_PORT.get(options));

        Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.UsingHostPort",host, port);

        // Check for shutdown
        if (operation.equalsIgnoreCase(OPERATION_SHUTDOWN)) {
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.SHUTDOWN_TYPE);
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ShutdownOK",host, port);
        }

        // check for reload
        else if (operation.toLowerCase().startsWith(OPERATION_RELOAD.toLowerCase())) {
            String webappName = operation.substring(OPERATION_RELOAD.length());
            Socket socket = new Socket(host, port);
            socket.setSoTimeout(TIMEOUT);
            OutputStream out = socket.getOutputStream();
            out.write(Launcher.RELOAD_TYPE);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeUTF(host);
            objOut.writeUTF(webappName);
            objOut.close();
            out.close();
            Logger.log(Logger.INFO, TOOLS_RESOURCES, "WinstoneControl.ReloadOK",host, port);
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
