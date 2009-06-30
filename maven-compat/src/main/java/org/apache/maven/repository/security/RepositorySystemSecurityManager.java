package org.apache.maven.repository.security;

import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationInfo;
import org.jsecurity.authc.AuthenticationToken;

public interface RepositorySystemSecurityManager
{
    public AuthenticationInfo authenticate( AuthenticationToken token )
        throws AuthenticationException;
}
