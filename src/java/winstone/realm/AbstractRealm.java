/*
 * The MIT License
 *
 * Copyright (c) 2017, Olivier Lamy, Webtide Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package winstone.realm;

import org.eclipse.jetty.security.AbstractLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.util.security.Credential;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class AbstractRealm
    extends AbstractLoginService
{
    protected final ConcurrentMap<String, UserIdentity> knownUserIdentities = new ConcurrentHashMap<>();

    /* ------------------------------------------------------------ */
    @Override
    protected String[] loadRoleInfo( UserPrincipal user )
    {
        UserIdentity userIdentity = knownUserIdentities.get( user.getName() );
        if ( userIdentity == null )
        {
            return null;
        }
        Set<RolePrincipal> roles = userIdentity.getSubject().getPrincipals( RolePrincipal.class );
        if ( roles == null )
        {
            return null;
        }
        List<String> list = new ArrayList<>( roles.size() );
        for ( RolePrincipal r : roles )
        {
            list.add( r.getName() );
        }
        return list.toArray( new String[roles.size()] );
    }


    /* ------------------------------------------------------------ */
    @Override
    protected UserPrincipal loadUserInfo( String userName )
    {
        UserIdentity userIdentity = knownUserIdentities.get( userName );
        if ( userIdentity != null )
        {
            return (UserPrincipal) userIdentity.getUserPrincipal();
        }

        return null;
    }


    public synchronized UserIdentity putUser( String userName, Credential credential, String[] roles )
    {
        Principal userPrincipal = new UserPrincipal( userName, credential );
        Subject subject = new Subject();
        subject.getPrincipals().add( userPrincipal );
        subject.getPrivateCredentials().add( credential );

        if ( roles != null )
        {
            for ( String role : roles )
            {
                subject.getPrincipals().add( new RolePrincipal( role ) );
            }
        }

        subject.setReadOnly();
        UserIdentity identity = _identityService.newUserIdentity( subject, userPrincipal, roles );
        knownUserIdentities.put( userName, identity );
        return identity;
    }


}
