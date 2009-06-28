package org.apache.maven.repository.security;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.Authenticator;

@Component(role=RepositorySystemSecurityManager.class)
public class DefaultRepositorySystemSecurityManager
    implements RepositorySystemSecurityManager
{
    @Requirement
    private Authenticator authenticator;
    
    public AuthenticationInfo authenticate( AuthenticationToken token )
        throws AuthenticationException
    {
        AuthenticationInfo authenticationInfo = authenticator.authenticate( token );
                        
        return authenticationInfo;
    }

}
