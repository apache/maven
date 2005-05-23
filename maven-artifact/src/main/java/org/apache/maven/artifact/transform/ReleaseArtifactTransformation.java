package org.apache.maven.artifact.transform;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractVersionArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ReleaseArtifactMetadata;
import org.apache.maven.artifact.metadata.VersionArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.IOException;
import java.util.List;

/**
 * Change the version <code>RELEASE</code> to the appropriate release version from the remote repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class ReleaseArtifactTransformation
    extends AbstractVersionTransformation
{
    public static final String RELEASE_VERSION = "RELEASE";

    private static boolean isRelease( Artifact artifact )
    {
        return artifact.getVersion().equals( RELEASE_VERSION );
    }

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( isRelease( artifact ) )
        {
            String version = resolveVersion( artifact, localRepository, remoteRepositories );
            if ( !version.equals( artifact.getVersion() ) )
            {
                artifact.setBaseVersion( version );
                artifact.updateVersion( version, localRepository );
            }
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        // metadata is added at install time
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        // metadata is added at deploy time
    }

    protected VersionArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                    ArtifactRepository remoteRepository,
                                                                    VersionArtifactMetadata localMetadata )
        throws ArtifactMetadataRetrievalException
    {
        AbstractVersionArtifactMetadata metadata = new ReleaseArtifactMetadata( artifact );
        try
        {
            metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager );
        }
        catch ( ResourceDoesNotExistException e )
        {
            if ( localMetadata.constructVersion() == null )
            {
                throw new ArtifactMetadataRetrievalException( "Unable to find release for artifact " + artifact, e );
            }
            // otherwise, ignore - use the local one
        }
        return metadata;
    }

    protected VersionArtifactMetadata readFromLocalRepository( Artifact artifact, ArtifactRepository localRepository )
        throws IOException, ArtifactPathFormatException
    {
        AbstractVersionArtifactMetadata metadata = new ReleaseArtifactMetadata( artifact );
        metadata.readFromLocalRepository( localRepository );
        return metadata;
    }
}
