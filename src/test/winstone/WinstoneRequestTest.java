package winstone;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import static org.junit.Assert.*;
import org.junit.Test;

public class WinstoneRequestTest {

    @SuppressWarnings("unchecked")
    @Test public void getHeader() throws Exception {
        WinstoneRequest r = new WinstoneRequest();
        r.parseHeaders(Arrays.asList("Foo: bar", "Baz-Quux: true", "include: stuff", "include: more stuff"));
        assertEquals("bar", r.getHeader("Foo"));
        assertEquals("bar", r.getHeader("foo"));
        assertEquals(null, r.getHeader("Foo:"));
        assertEquals(null, r.getHeader("Fooz"));
        assertEquals(null, r.getHeader("Fo"));
        assertEquals("true", r.getHeader("baz-quux"));
        Locale l = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            assertEquals("stuff", r.getHeader("Include"));
            assertEquals(Arrays.asList("stuff", "more stuff"), Collections.list(r.getHeaders("Include")));
        } finally {
            Locale.setDefault(l);
        }
        assertEquals(Collections.emptyList(), Collections.list(r.getHeaders("includ")));
        assertEquals(Collections.emptyList(), Collections.list(r.getHeaders("includes")));
    }

}
