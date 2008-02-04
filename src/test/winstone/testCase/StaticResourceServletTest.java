/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import winstone.StaticResourceServlet;

/**
 * Automated tests for the url security check inside the static resource servlet
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: StaticResourceServletTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class StaticResourceServletTest extends TestCase {
    
    /**
     * Constructor
     */
    public StaticResourceServletTest(String name) {
        super(name);
    }
    
    public void testIsDescendant() throws IOException {
        File webroot = new File("src/testwebapp");
        File webinf = new File(webroot, "WEB-INF");
        assertTrue("Direct subfolder", StaticResourceServlet.isDescendant(webroot, webinf, webroot));
        assertTrue("Self is a descendent of itself", StaticResourceServlet.isDescendant(webinf, webinf, webroot));
        assertTrue("Direct subfile", StaticResourceServlet.isDescendant(webinf, new File(webinf, "web.xml"), webroot));
        assertTrue("Indirect subfile", StaticResourceServlet.isDescendant(webroot, new File(webinf, "web.xml"), webroot));
        assertTrue("Backwards iterations", !StaticResourceServlet.isDescendant(webinf, new File(webinf, ".."), webroot));
    }
    
    public void testCanonicalVersion() throws IOException {
        File webroot = new File("src/testwebapp");
        File webinf = new File(webroot, "WEB-INF");
        File webxml = new File(webinf, "web.xml");
        assertTrue("Simplest case", 
                StaticResourceServlet.constructOurCanonicalVersion(
                        webxml, webroot).equals("/WEB-INF/web.xml"));
        assertTrue("One back step", 
                StaticResourceServlet.constructOurCanonicalVersion(
                        new File(webroot, "/test/../WEB-INF/web.xml"), webroot)
                .equals("/WEB-INF/web.xml"));
    }
}
