/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.Map;

/**
 * Interface that defines the necessary methods for being a connection listener
 * within winstone.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 */
public interface ConnectorFactory {
    /**
     * After the listener is loaded and initialized, this starts the thread
     * @param args
     * @param server
     * @return Result
     */
    boolean start(Map args, Server server) throws IOException;
}
