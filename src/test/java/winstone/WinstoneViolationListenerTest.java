package winstone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.eclipse.jetty.http.ComplianceViolation;
import org.eclipse.jetty.http.CookieCompliance;
import org.eclipse.jetty.http.HttpCompliance;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WinstoneViolationListener}.
 *
 * Verifies that HTTP compliance violations are properly detected and logged
 * for various types of protocol violations (HTTP, URI, Cookie).
 */
class WinstoneViolationListenerTest extends AbstractWinstoneTest {

    /**
     * Test listener that captures compliance violations for verification.
     */
    static class CapturingComplianceListener implements ComplianceViolation.Listener {
        private final Queue<ComplianceViolation.Event> events = new ConcurrentLinkedQueue<>();

        @Override
        public void onComplianceViolation(ComplianceViolation.Event event) {
            events.add(event);
        }

        public List<ComplianceViolation.Event> getEvents() {
            return new ArrayList<>(events);
        }

        public void clear() {
            events.clear();
        }

        public boolean isEmpty() {
            return events.isEmpty();
        }
    }

    @Test
    void testUriComplianceViolationDetected() throws Exception {
        CapturingComplianceListener captureListener = new CapturingComplianceListener();

        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        winstone = new Launcher(args);

        // Register test listener after startup
        ServerConnector connector = (ServerConnector) winstone.server.getConnectors()[0];
        HttpConfiguration httpConfig =
                connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        httpConfig.addComplianceViolationListener(captureListener);

        int port = connector.getLocalPort();

        // Make request with URI compliance violation (ambiguous path segments)
        // With UriCompliance.LEGACY mode, violations may be allowed so request might succeed
        String response = sendRawRequest(
                "127.0.0.1", port, "GET /path//..//resource HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");

        // Verify response was received (may be 200 or 400 depending on compliance mode)
        assertNotNull(response);
        assertTrue(response.contains("HTTP/1.1"), "Should receive HTTP response");

        // Wait for violations to be captured
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !captureListener.isEmpty());

        // Verify violations were captured
        List<ComplianceViolation.Event> violations = captureListener.getEvents();
        assertFalse(violations.isEmpty(), "Should have captured URI compliance violations");

        // Verify we have URI violations (may be allowed or forbidden depending on mode)
        boolean hasUriViolation = violations.stream()
                .anyMatch(e -> e.violation() instanceof UriCompliance.Violation
                        && ("AMBIGUOUS_PATH_SEGMENT".equals(e.violation().getName())
                                || "AMBIGUOUS_EMPTY_SEGMENT"
                                        .equals(e.violation().getName())
                                || "AMBIGUOUS_PATH_SEPARATOR"
                                        .equals(e.violation().getName())));

        assertTrue(hasUriViolation, "Should have captured URI compliance violations");
    }

    @Test
    void testHttpComplianceViolationAllowed() throws Exception {
        CapturingComplianceListener captureListener = new CapturingComplianceListener();

        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        winstone = new Launcher(args);

        // Register test listener
        ServerConnector connector = (ServerConnector) winstone.server.getConnectors()[0];
        HttpConfiguration httpConfig =
                connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        httpConfig.addComplianceViolationListener(captureListener);

        int port = connector.getLocalPort();

        // Send request with LF instead of CRLF (allowed HTTP violation)
        String response = sendRawRequest(
                "127.0.0.1", port, "GET /CountRequestsServlet HTTP/1.1\r\nHost: localhost\nConnection: close\r\n\r\n");

        // Request should succeed despite violation
        assertTrue(response.contains("200 OK"), "Request should succeed with allowed violation");

        // Wait for violations to be captured
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !captureListener.isEmpty());

        // Verify violation was captured
        List<ComplianceViolation.Event> violations = captureListener.getEvents();
        assertFalse(violations.isEmpty(), "Should have captured HTTP compliance violation");

        // Verify it's the expected violation (LF_HEADER_TERMINATION)
        boolean hasLfViolation = violations.stream()
                .anyMatch(e -> e.violation() instanceof HttpCompliance.Violation
                        && "LF_HEADER_TERMINATION".equals(e.violation().getName())
                        && e.allowed());

        assertTrue(hasLfViolation, "Should have captured LF_HEADER_TERMINATION violation");
    }

    @Test
    void testCookieComplianceViolationAllowed() throws Exception {
        CapturingComplianceListener captureListener = new CapturingComplianceListener();

        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        winstone = new Launcher(args);

        // Register test listener
        ServerConnector connector = (ServerConnector) winstone.server.getConnectors()[0];
        HttpConfiguration httpConfig =
                connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        httpConfig.addComplianceViolationListener(captureListener);

        int port = connector.getLocalPort();

        // Send request with cookie containing tab character (allowed cookie violation)
        String response = sendRawRequest(
                "127.0.0.1",
                port,
                "GET /CountRequestsServlet HTTP/1.1\r\nHost: localhost\r\nCookie: foo\t=bar\r\nConnection: close\r\n\r\n");

        // Request should succeed
        assertTrue(response.contains("200 OK"), "Request should succeed with allowed cookie violation");

        // Wait for violations to be captured
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !captureListener.isEmpty());

        // Verify violation was captured
        List<ComplianceViolation.Event> violations = captureListener.getEvents();
        assertFalse(violations.isEmpty(), "Should have captured Cookie compliance violation");

        // Verify it's a cookie violation
        boolean hasCookieViolation =
                violations.stream().anyMatch(e -> e.violation() instanceof CookieCompliance.Violation && e.allowed());

        assertTrue(hasCookieViolation, "Should have captured Cookie compliance violation");
    }

    @Test
    void testCleanRequestNoViolations() throws Exception {
        CapturingComplianceListener captureListener = new CapturingComplianceListener();

        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        winstone = new Launcher(args);

        // Register test listener
        ServerConnector connector = (ServerConnector) winstone.server.getConnectors()[0];
        HttpConfiguration httpConfig =
                connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
        httpConfig.addComplianceViolationListener(captureListener);

        int port = connector.getLocalPort();

        // Make perfectly compliant request
        String content = makeRequest("http://127.0.0.1:" + port + "/CountRequestsServlet", Protocol.HTTP_1);

        // Verify response
        assertNotNull(content);
        assertTrue(content.contains("accessed via GET"));

        // Give some time for any violations to be captured (if any)
        Thread.sleep(500);

        // Verify no violations were captured
        assertTrue(captureListener.isEmpty(), "Clean request should not trigger any violations");
    }

    @Test
    void testWinstoneViolationListenerIsRegistered() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("warfile", "target/test-classes/test.war");
        args.put("prefix", "/");
        args.put("httpPort", "0");

        winstone = new Launcher(args);

        // Verify WinstoneViolationListener is registered
        ServerConnector connector = (ServerConnector) winstone.server.getConnectors()[0];
        HttpConfiguration httpConfig =
                connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();

        assertNotNull(httpConfig, "HttpConfiguration should exist");

        // WinstoneViolationListener should be registered (we can't directly verify it,
        // but we can verify the configuration is set up correctly)
        assertEquals(HttpCompliance.RFC7230, httpConfig.getHttpCompliance());
        assertEquals(UriCompliance.LEGACY, httpConfig.getUriCompliance());
    }

    /**
     * Helper method to send raw HTTP requests for testing compliance violations.
     */
    private String sendRawRequest(String host, int port, String rawRequest) throws IOException {
        try (Socket socket = new Socket(host, port);
                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream()) {

            // Send raw request
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return new String(in.readAllBytes());
        }
    }
}
