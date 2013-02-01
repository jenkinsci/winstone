package winstone;

import org.junit.Test;

import javax.servlet.http.Cookie;

import static org.junit.Assert.assertTrue;

public class WinstoneResponseTest {

    @SuppressWarnings("unchecked")
    @Test public void testCookieExpires() throws Exception {
        WinstoneResponse response = new WinstoneResponse();

        Cookie cookie = new Cookie("testcookie", "cookievalue");
        cookie.setMaxAge(1000);
        String cookieAsString = response.writeCookie(cookie);

        assertTrue("expires date format", cookieAsString.matches(".*Expires=\\w{3}, \\d{1,2}-\\w{3}-\\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} GMT.*"));
    }

}
