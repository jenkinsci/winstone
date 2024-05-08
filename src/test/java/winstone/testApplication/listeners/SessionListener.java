/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testApplication.listeners;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Logs messages when any session event is received
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: SessionListener.java,v 1.2 2006/02/28 07:32:46 rickknowles Exp $
 */
public class SessionListener
        implements HttpSessionListener, HttpSessionAttributeListener, HttpSessionActivationListener {
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session Created - id=" + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session Destroyed - id=" + se.getSession().getId());
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session Attribute added (session id="
                        + se.getSession().getId() + ") " + se.getName() + "="
                        + se.getValue());
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session Attribute removed (session id="
                        + se.getSession().getId() + ") " + se.getName() + "="
                        + se.getValue());
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session Attribute replaced (session id="
                        + se.getSession().getId() + ") " + se.getName() + "="
                        + se.getValue());
    }

    @Override
    public void sessionDidActivate(HttpSessionEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session activated - id=" + se.getSession().getId());
    }

    @Override
    public void sessionWillPassivate(HttpSessionEvent se) {
        se.getSession()
                .getServletContext()
                .log("Session passivating - id=" + se.getSession().getId());
    }
}
