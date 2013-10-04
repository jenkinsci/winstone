/*
 * Copyright 2003-2006 Rick Knowles <winstone-devel at lists sourceforge net>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 */
package winstone.realm;

import org.eclipse.jetty.security.HashLoginService;
import winstone.AuthenticationRealm;
import winstone.Logger;
import winstone.WinstoneResourceBundle;
import winstone.cmdline.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static org.eclipse.jetty.util.security.Credential.*;

/**
 * Base class for authentication realms. Subclasses provide the source of
 * authentication roles, usernames, passwords, etc, and when asked for
 * validation respond with a role if valid, or null otherwise.
 * 
 * @author mailto: <a href="rick_knowles@hotmail.com">Rick Knowles</a>
 * @author Kohsuke Kawaguchi
 * @version $Id: ArgumentsRealm.java,v 1.4 2007/06/01 15:55:41 rickknowles Exp $
 */
public class ArgumentsRealm extends HashLoginService implements AuthenticationRealm {
    private static final WinstoneResourceBundle REALM_RESOURCES = new WinstoneResourceBundle("winstone.realm.LocalStrings");

    /**
     * Constructor - this sets up an authentication realm, using the arguments
     * supplied on the command line as a source of userNames/passwords/roles.
     */
    public ArgumentsRealm(Map args) {
        int count=0;
        for (Object o : args.keySet()) {
            String key = (String) o;
            if (key.startsWith(Option.ARGUMENTS_REALM_PASSWORD.name)) {
                String userName = key.substring(Option.ARGUMENTS_REALM_PASSWORD.name.length());
                String password = (String) args.get(key);

                String roleList = Option.stringArg(args, Option.ARGUMENTS_REALM_ROLES.name + userName, "");
                String[] roleArray = new String[0];
                if (roleList.equals("")) {
                    Logger.log(Logger.WARNING, REALM_RESOURCES, "ArgumentsRealm.UndeclaredRoles", userName);
                } else {
                    StringTokenizer st = new StringTokenizer(roleList, ",");
                    List<String> rl = new ArrayList<String>();
                    for (; st.hasMoreTokens(); ) {
                        String currentRole = st.nextToken();
                        rl.add(currentRole);
                    }
                    roleArray = rl.toArray(new String[rl.size()]);
                    Arrays.sort(roleArray);
                }
                putUser(userName, getCredential(password), roleArray);
                count++;
            }
        }

        Logger.log(Logger.DEBUG, REALM_RESOURCES, "ArgumentsRealm.Initialised",
                "" + count);
    }
}