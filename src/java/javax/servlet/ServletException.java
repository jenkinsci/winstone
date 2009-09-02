/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package javax.servlet;

import java.io.PrintWriter;
import java.io.PrintStream;

/**
 * Generic servlet exception
 * 
 * @author Rick Knowles
 */
public class ServletException extends java.lang.Exception {
    private Throwable rootCause;

    public ServletException() {
        super();
    }

    public ServletException(String message) {
        super(message);
    }

    public ServletException(String message, Throwable rootCause) {
        super(message,rootCause);
        this.rootCause = rootCause;
    }

    public ServletException(Throwable rootCause) {
        super(rootCause);
        this.rootCause = rootCause;
    }

    public Throwable getRootCause() {
        return this.rootCause;
    }
}
