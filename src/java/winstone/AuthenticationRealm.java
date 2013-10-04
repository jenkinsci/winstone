/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone;

import org.eclipse.jetty.security.LoginService;

/**
 * Interface for authentication realms.
 * 
 * @author <a href="mailto:rick_knowles@hotmail.com">Rick Knowles</a>
 * @version $Id: AuthenticationRealm.java,v 1.3 2006/12/09 03:56:41 rickknowles Exp $
 */
public interface AuthenticationRealm extends LoginService {
}
