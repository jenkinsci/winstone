package winstone;

import java.io.IOException;

/**
 * Indicates an I/O exception writing to client.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClientSocketException extends IOException {
    public ClientSocketException(Throwable cause) {
        super("Failed to write to client");
        initCause(cause);
    }
}
