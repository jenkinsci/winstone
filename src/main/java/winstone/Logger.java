/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.PrintStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

/**
 * A utility class for logging event and status messages.
 *
 * <p>{@link Launcher#initLogger} examines the parsed {@code --logfile} option and, if present,
 * creates a new {@link PrintStream} and sets standard output (stdout) and standard error (stderr)
 * to that new stream with {@link System#setOut} and {@link System#setErr}. If this takes place
 * after JUL has initialized the root logger, the root logger will not be aware of the switch
 * because its {@link ConsoleHandler} will have already read the old value of {@link System#err}.
 * For this reason, parsing the {@code --logfile} option and redirecting the standard output
 * (stdout) and standard error (stderr) streams must take place prior to JUL initialization.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: Logger.java,v 1.8 2006/11/09 06:01:43 rickknowles Exp $
 */
public class Logger {

    private final static Object semaphore = new Object();
    static boolean initialised = false;
    static boolean showThrowingThread;

    /**
     * Initialises default streams
     */
    public static void init(Level level) {
        init(level, false);
    }

    public static void init(int level) {
        init(Level.parse(String.valueOf(level)));
    }

    /**
     * Initialize default streams
     */
    public static void init(
            Level level,
            boolean showThrowingThreadArg) {
        synchronized (semaphore) {
            if (!initialised) { // recheck in case we were blocking on another init
                LOGGER.setLevel(level);
                showThrowingThread = showThrowingThreadArg;
                initialised = true;
            }
        }
    }

    public static void setCurrentDebugLevel(int level) {
        if (!initialised) {
            init(level);
        } else synchronized (semaphore) {
            LOGGER.setLevel(Level.parse(String.valueOf(level)));
        }
    }

    /**
     * Writes a log message to the requested stream, and immediately flushes
     * the contents of the stream.
     */
    private static void logInternal(Level level, String message, Throwable error) {
        if (!initialised) {
            init(Level.INFO);
        }

        String msg = "";
        if (showThrowingThread) {
            msg = "["+Thread.currentThread().getName()+"] - ";
        }
        msg += message;

        LOGGER.log(level,msg,error);
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object param) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, param), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object... params) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), null);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object param, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, param), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String messageKey, Object[] params, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), error);
        }
    }

    public static void log(Level level, WinstoneResourceBundle resources,
            String streamName, String messageKey, Object[] params, Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, resources.getString(messageKey, params), error);
        }
    }

    public static void logDirectMessage(Level level, String streamName, String message,
            Throwable error) {
        if (!LOGGER.isLoggable(level)) {
            return;
        } else {
            logInternal(level, message, error);
        }
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("winstone");
}
