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
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.metadata.VersionArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 * @version $Id: SnapshotTransformation.java,v 1.1 2005/03/03 15:37:25
 *          jvanzyl Exp $
 */
public class SnapshotTransformation
    extends AbstractVersionTransformation
{
    public static final String SNAPSHOT_VERSION = "SNAPSHOT";

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        Matcher m = SnapshotArtifactMetadata.VERSION_FILE_PATTERN.matcher( artifact.getBaseVersion() );
        if ( m.matches() )
        {
            // This corrects the base version, but ensure it is not resolved again
            artifact.setBaseVersion( m.group( 1 ) + "-" + SNAPSHOT_VERSION );
        }
        else if ( isSnapshot( artifact ) )
        {
            String version = resolveVersion( artifact, localRepository, remoteRepositories );
            artifact.updateVersion( version, localRepository );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        Matcher m = SnapshotArtifactMetadata.VERSION_FILE_PATTERN.matcher( artifact.getBaseVersion() );
        if ( m.matches() )
        {
            artifact.setBaseVersion( m.group( 1 ) + "-" + SNAPSHOT_VERSION );
        }
        else if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata metadata = new SnapshotArtifactMetadata( artifact );
            metadata.storeInLocalRepository( localRepository );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        Matcher m = SnapshotArtifactMetadata.VERSION_FILE_PATTERN.matcher( artifact.getBaseVersion() );
        if ( m.matches() )
        {
            // This corrects the base version, but ensure it is not updated again
            artifact.setBaseVersion( m.group( 1 ) + "-" + SNAPSHOT_VERSION );
        }
        else if ( isSnapshot( artifact ) )
        {
            SnapshotArtifactMetadata metadata = null;
            metadata = (SnapshotArtifactMetadata) retrieveFromRemoteRepository( artifact, remoteRepository, null );
            metadata.update();

            artifact.setVersion( metadata.constructVersion() );

            artifact.addMetadata( metadata );
        }
    }

    private static boolean isSnapshot( Artifact artifact )
    {
        return artifact.getVersion().endsWith( SNAPSHOT_VERSION );
    }

    protected VersionArtifactMetadata retrieveFromRemoteRepository( Artifact artifact,
                                                                    ArtifactRepository remoteRepository,
                                                                    VersionArtifactMetadata localMetadata )
        throws ArtifactMetadataRetrievalException
    {
        SnapshotArtifactMetadata metadata = new SnapshotArtifactMetadata( artifact );
        try
        {
            metadata.retrieveFromRemoteRepository( remoteRepository, wagonManager );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // No problem...
            // this just means that there is no snapshot version file, so we keep timestamp = null, build = 0
        }
        return metadata;
    }

    protected VersionArtifactMetadata readFromLocalRepository( Artifact artifact, ArtifactRepository localRepository )
        throws IOException, ArtifactPathFormatException
    {
        SnapshotArtifactMetadata metadata = new SnapshotArtifactMetadata( artifact );
        metadata.readFromLocalRepository( localRepository );
        return metadata;
    }
}