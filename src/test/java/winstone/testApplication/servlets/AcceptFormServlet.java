package winstone.testApplication.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AcceptFormServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        try (PrintWriter pw = resp.getWriter()) {
            try {
                req.getParameterNames();
            } catch (Exception x) {
                x.printStackTrace(pw);
                return;
            }
            pw.print("received " + req.getContentLength() + " bytes");
        }
    }

}
