package org.apache.maven.artifact.repository;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.model.Repository;
import org.apache.maven.model.user.ServerProfile;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.model.user.UserModelUtils;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

/**
 * @author jdcasey
 */
public class DefaultArtifactRepositoryFactory
    extends AbstractLogEnabled
    implements ArtifactRepositoryFactory
{

    public ArtifactRepository createArtifactRepository( Repository modelRepository, UserModel userModel )
    {
        ServerProfile repoProfile = null;

        String repoId = modelRepository.getId();

        if ( repoId != null && repoId.length() > 0 )
        {
            repoProfile = UserModelUtils.getServerProfile( userModel, modelRepository.getId() );
        }
        else
        {
            Logger logger = getLogger();
            if ( logger != null )
            {
                logger.warn( "Cannot associate authentication to repository with null id. The offending repository's URL is: "
                    + modelRepository.getUrl() );
            }
        }

        ArtifactRepository repo = null;

        if ( repoProfile != null )
        {
            AuthenticationInfo authInfo = new AuthenticationInfo();

            authInfo.setUserName( repoProfile.getUsername() );

            authInfo.setPassword( repoProfile.getPassword() );

            authInfo.setPrivateKey( repoProfile.getPrivateKey() );

            authInfo.setPassphrase( repoProfile.getPassphrase() );

            repo = new ArtifactRepository( modelRepository.getId(), modelRepository.getUrl(), authInfo );
        }
        else
        {
            repo = new ArtifactRepository( modelRepository.getId(), modelRepository.getUrl() );
        }

        return repo;
    }

}