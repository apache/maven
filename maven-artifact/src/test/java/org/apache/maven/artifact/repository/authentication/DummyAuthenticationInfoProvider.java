package org.apache.maven.artifact.repository.authentication;

import org.apache.maven.wagon.repository.Repository;

/**
 * @author jdcasey
 */
public class DummyAuthenticationInfoProvider
    implements AuthenticationInfoProvider
{

    public void configureAuthenticationInfo( Repository wagonRepository ) throws Exception
    {
    }

}
