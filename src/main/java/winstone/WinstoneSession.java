package winstone;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Expose the property to remain compatible with Winstone 1.0
 *
 * @author Kohsuke Kawaguchi
 */
public class WinstoneSession {
    /**
     * Name of the cookie that stores HTTP session ID.
     */
    @SuppressFBWarnings(value = "MS_SHOULD_BE_FINAL", justification = "mutated by jenkinsci/jenkins/war/src/main/java/executable/Main.java")
    public static String SESSION_COOKIE_NAME = "JSESSIONID";

    private WinstoneSession() {}

}
