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
import org.apache.maven.artifact.metadata.LegacyArtifactMetadata;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:mmaczka@interia.pl">Michal Maczka</a>
 * @version $Id: SnapshotTransformation.java,v 1.1 2005/03/03 15:37:25
 *          jvanzyl Exp $
 */
public class SnapshotTransformation
    extends AbstractVersionTransformation
{
    private String deploymentTimestamp;

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( artifact.isSnapshot() )
        {
            String version = resolveVersion( artifact, localRepository, remoteRepositories );
            artifact.updateVersion( version, localRepository );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( artifact.isSnapshot() )
        {
            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            metadata.getMetadata().getVersioning().getSnapshot().setLocalCopy( true );

            artifact.addMetadata( metadata );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( artifact.isSnapshot() )
        {
            int buildNumber = resolveLatestSnapshotBuildNumber( artifact, localRepository, remoteRepository );

            Snapshot snapshot = new Snapshot();
            snapshot.setTimestamp( getDeploymentTimestamp() );
            snapshot.setBuildNumber( buildNumber + 1 );

            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );

            artifact.setResolvedVersion(
                constructVersion( metadata.getMetadata().getVersioning(), artifact.getBaseVersion() ) );

            artifact.addMetadata( metadata );
        }
    }

    public String getDeploymentTimestamp()
    {
        if ( deploymentTimestamp == null )
        {
            deploymentTimestamp = SnapshotArtifactMetadata.getUtcDateFormatter().format( new Date() );
        }
        return deploymentTimestamp;
    }

    protected LegacyArtifactMetadata createLegacyMetadata( Artifact artifact )
    {
        return new SnapshotArtifactMetadata( artifact );
    }

    protected String constructVersion( Versioning versioning, String baseVersion )
    {
        String version = null;
        Snapshot snapshot = versioning.getSnapshot();
        if ( snapshot != null )
        {
            if ( snapshot.isLocalCopy() )
            {
                version = baseVersion;
            }
            else if ( snapshot.getTimestamp() != null && snapshot.getBuildNumber() > 0 )
            {
                String newVersion = snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
                version = StringUtils.replace( baseVersion, "SNAPSHOT", newVersion );
            }
        }
        return version;
    }

    private int resolveLatestSnapshotBuildNumber( Artifact artifact, ArtifactRepository localRepository,
                                                  ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );

        getLogger().info( "Retrieving previous build number from " + remoteRepository.getId() );
        repositoryMetadataManager.resolveAlways( metadata, localRepository, remoteRepository );

        int buildNumber = 0;
        Metadata repoMetadata = metadata.getMetadata();
        if ( repoMetadata != null )
        {
            if ( repoMetadata.getVersioning() != null && repoMetadata.getVersioning().getSnapshot() != null )
            {
                buildNumber = repoMetadata.getVersioning().getSnapshot().getBuildNumber();
            }
        }
        else
        {
            try
            {
                SnapshotArtifactMetadata snapshotMetadata = new SnapshotArtifactMetadata( artifact );
                snapshotMetadata.retrieveFromRemoteRepository( remoteRepository, wagonManager,
                                                               ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );
                getLogger().warn( "Using old-style versioning metadata from remote repo for " + artifact );

                buildNumber = snapshotMetadata.getBuildNumber();
            }
            catch ( ResourceDoesNotExistException e1 )
            {
                // safe to ignore, use default snapshot data
                getLogger().debug( "Unable to find legacy metadata - ignoring" );
            }
        }
        return buildNumber;
    }

}
