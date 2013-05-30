package winstone;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

public class WinstoneResponseTest {

    @SuppressWarnings("unchecked")
    @Test public void testCookieExpires() throws Exception {
        WinstoneResponse response = new WinstoneResponse();

        Cookie cookie = new Cookie("testcookie", "cookievalue");
        cookie.setMaxAge(1000);
        String cookieAsString = response.writeCookie(cookie);

        assertTrue("expires date format", cookieAsString.matches(".*Expires=\\w{3}, \\d{1,2}-\\w{3}-\\d{4} \\d{1,2}:\\d{1,2}:\\d{1,2} GMT.*"));
    }

	@Test
	public void testValidateHeadersWhenOk() throws Exception {
		WinstoneResponse response = setupResponse(HttpServletResponse.SC_OK);

		response.validateHeaders();
		String contentType = response.getHeader(WinstoneResponse.CONTENT_TYPE_HEADER);

		assertThat(contentType, containsString("text/html"));
	}

	@Test
	public void testValidateHeadersWhenNotModified() throws Exception {
		WinstoneResponse response = setupResponse(HttpServletResponse.SC_NOT_MODIFIED);

		response.validateHeaders();
		String contentType = response.getHeader(WinstoneResponse.CONTENT_TYPE_HEADER);

		assertThat(contentType, is(nullValue()));
	}

	private WinstoneResponse setupResponse(int statusCode) {
		WinstoneRequest request = new WinstoneRequest();
		WinstoneResponse response = new WinstoneResponse();

		response.setOutputStream(new WinstoneOutputStream(null, false));
		response.setRequest(request);
		response.setProtocol("HTTP/1.1");
		response.setStatus(statusCode);

		return response;
	}

}
