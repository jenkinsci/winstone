/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.testCase;

import junit.framework.TestCase;
import winstone.WinstoneResourceBundle;

/**
 * Simple tests for the string replacer
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: WinstoneResourceBundleTest.java,v 1.1 2006/11/09 05:39:43 rickknowles Exp $
 */
public class WinstoneResourceBundleTest extends TestCase {

    public void testGlobalReplace() {
        assertEquals(
                "One token",
                "Foo = bar squared",
                WinstoneResourceBundle.globalReplace("Foo = [#0] squared", "[#0]", "bar"));
        assertEquals(
                "Repeated token",
                "Foo = bar bar squared",
                WinstoneResourceBundle.globalReplace("Foo = [#0] [#0] squared", "[#0]", "bar"));
        assertEquals(
                "Two tokens",
                "Foo = blah bar squared",
                WinstoneResourceBundle.globalReplace(
                        "Foo = [#1] [#0] squared", new String[][] {{"[#0]", "bar"}, {"[#1]", "blah"}}));
    }

    //    public static void testSpeed() throws Exception {
    //        String tokens[][] = new String[20][2];
    //        for (int n = 0; n < tokens.length; n++) {
    //            tokens[n] = new String[] {"[#" + n + "]", "token" + n};
    //        }
    //        Random rnd = new Random();
    //        String inputs[] = new String[5000];
    //        for (int n = 0; n < inputs.length; n++) {
    //            inputs[n] = "";
    //            for (int k = 0; k < tokens.length; k++) {
    //                inputs[n] += "[#" + (rnd.nextInt() % tokens.length) + "] abc";
    //            }
    //        }
    //
    //        long startTime = System.currentTimeMillis();
    //        for (int n = 0; n < inputs.length; n++) {
    //            WinstoneResourceBundle.globalReplace(inputs[n], tokens);
    //        }
    //        System.out.println("Replaced " + tokens.length + " tokens in " + inputs.length + " strings in " +
    //                (System.currentTimeMillis() - startTime) + "ms");
    //    }
}
