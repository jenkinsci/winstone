package winstone.testApplication.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class WriteAfterServletFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {}
    public void destroy() {}

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
        ServletOutputStream os = response.getOutputStream();
        os.print("Hello");
    }

}
