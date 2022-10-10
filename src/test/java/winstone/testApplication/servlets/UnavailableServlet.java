/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testApplication.servlets;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Used to test the unavailable exception processing
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: UnavailableServlet.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class UnavailableServlet extends HttpServlet {
    protected boolean errorAtInit;

    @Override
    public void init() throws ServletException {
        String errorTime = getServletConfig().getInitParameter("errorTime");
        this.errorAtInit = ((errorTime == null) || errorTime.equals("init"));
        if (this.errorAtInit)
            throw new UnavailableException(
                    "Error thrown deliberately during init");
    }

    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (!this.errorAtInit)
            throw new UnavailableException(
                    "Error thrown deliberately during get");

        try (Writer out = response.getWriter()) {
            out.write("This should not be shown, because we've thrown unavailable exceptions");
        }
    }

}
