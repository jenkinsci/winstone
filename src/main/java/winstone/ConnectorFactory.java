/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

/**
 * Interface that defines the necessary methods for being a connection listener
 * within winstone.
 *
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ConnectorFactory {
    /**
     * After the listener is loaded and initialized, this starts the thread
     * @return the Connector instance or <code>null</code> if not started
     */
    Connector start(Map<String, String> args, Server server) throws IOException;
}
