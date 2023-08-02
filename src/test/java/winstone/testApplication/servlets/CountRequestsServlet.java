/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testApplication.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simple test servlet that counts the number of times it has been requested,
 * and returns that number in the response.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: CountRequestsServlet.java,v 1.3 2006/02/28 07:32:49 rickknowles Exp $
 */
public class CountRequestsServlet extends HttpServlet {
    private int numberOfGets;

    @Override
    public void init() {
        String offset = getServletConfig().getInitParameter("offset");
        numberOfGets = offset == null ? 0 : Integer.parseInt(offset);
    }

    /**
     * Get implementation - increments and shows the access count
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        numberOfGets++;
        ServletOutputStream out = response.getOutputStream();
        out.println("<html><body>This servlet has been accessed via GET "
                + numberOfGets + " times</body></html>");
        out.flush();
    }
}
