/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Automated tests for the JNDI provider component of Winstone
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: NamingTest.java,v 1.2 2006/02/28 07:32:49 rickknowles Exp $
 */
public class NamingTest extends TestCase {
    public static Test suite() {
        return (new TestSuite(NamingTest.class));
    }

    private InitialContext ic;

    /**
     * Constructor for the junit test class for the JNDI service.
     * 
     * @param name
     *            The name of the test case
     */
    public NamingTest(String name) {
        super(name);
    }

    /**
     * Begins the setup of the test case
     */
    public void setUp() throws NamingException {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "winstone.jndi.java.javaURLContextFactory");
        env.put(Context.URL_PKG_PREFIXES, "winstone.jndi");
        this.ic = new InitialContext(env);
    }

    /**
     * Undoes any setup work for the test case
     */
    public void tearDown() throws NamingException {
        this.ic.close();
        this.ic = null;
    }

    /**
     * Performs an absolute context lookup
     */
    public void testAbsoluteContextLookup() throws NamingException {
        Object context1 = this.ic.lookup("java:/comp/env");
        assertNotNull("Lookup on java:/comp/env must be non-null", context1);
        assertTrue("Lookup on java:/comp/env must be a Context",
                context1 instanceof Context);

        Object context2 = this.ic.lookup("java:/comp/env/");
        assertNotNull("Lookup on java:/comp/env/ must be non-null", context2);
        assertTrue("Lookup on java:/comp/env/ must be a Context",
                context2 instanceof Context);
    }

    /**
     * Performs an absolute lookup on the context
     */
    public void testAbsoluteLookup() throws NamingException {
        Object value = this.ic.lookup("java:/comp/env");
        assertNotNull("Lookup on java:/comp/env must be non-null", value);
    }

    /**
     * Performs a relative lookup on the context
     */
    public void testRelativeLookup() throws NamingException {
        Object value = this.ic.lookup("");
        assertNotNull("Lookup on \"\" must be non-null", value);
    }

    /**
     * Performs a relative list on the context
     */
    public void testRelativeList() throws NamingException {
        NamingEnumeration listing = this.ic.list("");
        assertNotNull("Listing of current context must be non-null", listing);
        listing.close();
    }

    /**
     * Performs an absolute list on the context
     */
    public void testAbsoluteList() throws NamingException {
        NamingEnumeration listing1 = this.ic.list("java:/comp/env");
        assertNotNull("Listing of java:/comp/env must be non-null", listing1);
        listing1.close();
        NamingEnumeration listing2 = this.ic.list("java:/comp/env/");
        assertNotNull("Listing of java:/comp/env/ must be non-null", listing2);
        listing2.close();
    }

    /**
     * Performs an absolute list on the context
     */
    public void testCreateDestroyContexts() throws NamingException {
        Context child = this.ic.createSubcontext("TestChildContext");
        assertNotNull("Created subcontext TestChildContext must not be null",
                child);
        NamingEnumeration listing = child.list("");
        assertTrue("Listing on new child context is empty", !listing
                .hasMoreElements());
        listing.close();
        this.ic.destroySubcontext("java:/comp/env/TestChildContext");
    }

    /**
     * Attempts a simple bind
     */
    public void testSimpleBind() throws NamingException {
        Context child = this.ic.createSubcontext("TestBindContext");
        assertNotNull("Created subcontext TestBindContext must not be null",
                child);
        child.bind("bindInteger", 80);
        Object lookupInt = this.ic.lookup("TestBindContext/bindInteger");
        assertNotNull(
                "java:/comp/env/TestBindContext/bindInteger should be non-null",
                lookupInt);
        assertEquals("java:/comp/env/TestBindContext/bindInteger", lookupInt,
                80);
        this.ic.destroySubcontext("java:/comp/env/TestBindContext");
    }

    /**
     * Attempts a rebind
     */
    public void testSimpleRebind() throws NamingException {
        Context child = this.ic.createSubcontext("TestRebindContext");
        assertNotNull("Created subcontext TestRebindContext must not be null",
                child);
        Context rebindChild = child.createSubcontext("ChildRebind");
        assertNotNull("Created subcontext rebindChild must not be null",
                rebindChild);
        rebindChild.rebind(
                "java:/comp/env/TestRebindContext/ChildRebind/integer",
                25);
        rebindChild.close();
        child.close();

        Object lookupInt = this.ic
                .lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
        assertNotNull(
                "java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null",
                lookupInt);
        assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer",
                lookupInt, 25);

        this.ic
                .rebind("TestRebindContext/ChildRebind/integer",
                        new Integer(40));
        Object lookupInt2 = this.ic
                .lookup("TestRebindContext/ChildRebind/integer");
        assertNotNull(
                "TestRebindContext/ChildRebind/integer should be non-null",
                lookupInt2);
        assertEquals("TestRebindContext/ChildRebind/integer", lookupInt2,
                40);
        Object lookupInt3 = this.ic
                .lookup("java:/comp/env/TestRebindContext/ChildRebind/integer");
        assertNotNull(
                "java:/comp/env/TestRebindContext/ChildRebind/integer should be non-null",
                lookupInt3);
        assertEquals("java:/comp/env/TestRebindContext/ChildRebind/integer",
                lookupInt3, 40);

        this.ic
                .destroySubcontext("java:/comp/env/TestRebindContext/ChildRebind");
        this.ic.destroySubcontext("java:/comp/env/TestRebindContext");
    }
}
