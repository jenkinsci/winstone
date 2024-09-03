/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testApplication.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Simple timing and request dumping test filter
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: TimingFilter.java,v 1.2 2006/02/28 07:32:50 rickknowles Exp $
 */
public class TimingFilter implements Filter {
    private boolean dumpRequestParams;

    private ServletContext context;

    @Override
    public void init(FilterConfig config) {
        String dumpRequestParams = config.getInitParameter("dumpRequestParameters");
        this.dumpRequestParams = ((dumpRequestParams != null) && dumpRequestParams.equalsIgnoreCase("true"));
        this.context = config.getServletContext();
    }

    @Override
    public void destroy() {
        this.context = null;
    }

    /**
     * Times the execution of the rest of the filter chain, optionally dumping
     * the request parameters to the servlet context log
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (this.dumpRequestParams) {
            for (Enumeration<String> paramNames = request.getParameterNames(); paramNames.hasMoreElements(); ) {
                String name = paramNames.nextElement();
                this.context.log("Request parameter: " + name + "=" + request.getParameter(name));
            }
        }

        long startTime = System.currentTimeMillis();
        chain.doFilter(request, response);
        this.context.log("Filter chain executed in " + (System.currentTimeMillis() - startTime) + "ms");
    }
}
