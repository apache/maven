package org.apache.maven.artifact.repository;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.settings.MavenSettingsBuilder;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;

/**
 * @author jdcasey
 */
public class DefaultArtifactRepositoryFactory
    extends AbstractLogEnabled
    implements ArtifactRepositoryFactory
{
    // TODO: use settings?
    private String globalSnapshotPolicy = null;

    // TODO: make this a store once object?
    private MavenSettingsBuilder settingsBuilder;

    public ArtifactRepository createArtifactRepository( String id, String url,
                                                        ArtifactRepositoryLayout repositoryLayout,
                                                        String snapshotPolicy )
    {
        AuthenticationInfo authInfo = null;

        if ( id != null && id.length() > 0 )
        {
            Settings settings = null;
            try
            {
                settings = settingsBuilder.buildSettings();
            }
            catch ( IOException e )
            {
                getLogger().warn( "Error reading settings", e );
            }
            catch ( XmlPullParserException e )
            {
                getLogger().warn( "Error reading settings", e );
            }

            Server repoProfile = settings.getServer( id );

            if ( repoProfile != null )
            {
                authInfo = new AuthenticationInfo();

                authInfo.setUserName( repoProfile.getUsername() );

                authInfo.setPassword( repoProfile.getPassword() );

                authInfo.setPrivateKey( repoProfile.getPrivateKey() );

                authInfo.setPassphrase( repoProfile.getPassphrase() );
            }

        }
        else
        {
            Logger logger = getLogger();
            if ( logger != null )
            {
                logger.warn( "Cannot associate authentication to repository with null id. The offending repository's URL is: " +
                             url );
            }
        }

        ArtifactRepository repo = null;

        if ( globalSnapshotPolicy != null )
        {
            snapshotPolicy = globalSnapshotPolicy;
        }

        if ( authInfo != null )
        {
            repo = new ArtifactRepository( id, url, authInfo, repositoryLayout, snapshotPolicy );
        }
        else
        {
            repo = new ArtifactRepository( id, url, repositoryLayout, snapshotPolicy );
        }

        return repo;
    }

    public void setGlobalSnapshotPolicy( String snapshotPolicy )
    {
        this.globalSnapshotPolicy = snapshotPolicy;
    }
}
