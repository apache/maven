package org.apache.maven.artifact.repository.authentication;

import org.apache.maven.model.user.ServerProfile;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.util.UserModelUtils;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author jdcasey
 */
public class MavenAuthenticationInfoProvider
    implements AuthenticationInfoProvider
{

    public void configureAuthenticationInfo( Repository repo ) throws Exception
    {
        UserModel userModel = UserModelUtils.getUserModel();

        String repoId = repo.getId();
        if ( !StringUtils.isEmpty( repoId ) )
        {
            ServerProfile serverProfile = UserModelUtils.getServerProfile( userModel, repo.getId() );

            AuthenticationInfo info = new AuthenticationInfo();
            if ( serverProfile != null )
            {
                info.setUserName( serverProfile.getUsername() );
                info.setPassword( serverProfile.getPassword() );
                info.setPrivateKey( serverProfile.getPrivateKey() );
                info.setPassphrase( serverProfile.getPassphrase() );
            }

            repo.setAuthenticationInfo( info );
        }
    }

}