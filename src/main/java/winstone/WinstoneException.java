/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

/**
 * Master exception within the servlet container. This is thrown whenever a
 * non-recoverable error occurs that we want to throw to the top of the
 * application.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneException.java,v 1.1 2004/03/08 15:27:21 rickknowles
 *          Exp $
 */
public class WinstoneException extends RuntimeException {
    /**
     * Create an exception with a useful message for the system administrator.
     * 
     * @param pMsg
     *            Error message for to be used for administrative
     *            troubleshooting
     */
    public WinstoneException(String pMsg) {
        super(pMsg);
    }

    /**
     * Create an exception with a useful message for the system administrator
     * and a nested throwable object.
     * 
     * @param pMsg
     *            Error message for administrative troubleshooting
     * @param pError
     *            The actual exception that occurred
     */
    public WinstoneException(String pMsg, Throwable pError) {
        super(pMsg,pError);
    }

    /**
     * Get the nested error or exception
     * 
     * @return The nested error or exception
     */
    public Throwable getNestedError() {
        return getCause();
    }

}
