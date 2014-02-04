package winstone;

/**
 * Expose the property to remain compatible with Winstone 1.0
 *
 * @author Kohsuke Kawaguchi
 */
public class WinstoneSession {
    /**
     * Name of the cookie that stores HTTP session ID.
     */
    public static String SESSION_COOKIE_NAME = "JSESSIONID";
}
