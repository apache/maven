package org.apache.maven.artifact.repository.authentication;

import org.apache.maven.wagon.repository.Repository;

/**
 * @author jdcasey
 */
public interface AuthenticationInfoProvider
{
    public static final String ROLE = AuthenticationInfoProvider.class.getName();

    // TODO: do not throw Exception.
    void configureAuthenticationInfo( Repository wagonRepository ) throws Exception;

}