package org.apache.maven.artifact.transform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.codehaus.plexus.util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );

    private static final String UTC_TIMESTAMP_PATTERN = "yyyyMMdd.HHmmss";

    public void transformForResolve( Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException
    {
        // Only select snapshots that are unresolved (eg 1.0-SNAPSHOT, not 1.0-20050607.123456)
        if ( artifact.isSnapshot() && artifact.getBaseVersion().equals( artifact.getVersion() ) )
        {
            try
            {
                String version = resolveVersion( artifact, localRepository, remoteRepositories );
                artifact.updateVersion( version, localRepository );
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                throw new ArtifactResolutionException( e.getMessage(), artifact, e );
            }
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
    {
        if ( artifact.isSnapshot() )
        {
            Snapshot snapshot = new Snapshot();
            snapshot.setLocalCopy( true );
            RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );

            artifact.addMetadata( metadata );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        if ( artifact.isSnapshot() )
        {
            Snapshot snapshot = new Snapshot();
            if ( remoteRepository.isUniqueVersion() )
            {
                snapshot.setTimestamp( getDeploymentTimestamp() );
            }

            // we update the build number anyway so that it doesn't get lost. It requires the timestamp to take effect
            try
            {
                int buildNumber = resolveLatestSnapshotBuildNumber( artifact, localRepository, remoteRepository );

                snapshot.setBuildNumber( buildNumber + 1 );
            }
            catch ( RepositoryMetadataResolutionException e )
            {
                throw new ArtifactDeploymentException( "Error retrieving previous build number for artifact '" +
                    artifact.getDependencyConflictId() + "': " + e.getMessage(), e );
            }

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
            deploymentTimestamp = getUtcDateFormatter().format( new Date() );
        }
        return deploymentTimestamp;
    }

    protected String constructVersion( Versioning versioning, String baseVersion )
    {
        String version = null;
        Snapshot snapshot = versioning.getSnapshot();
        if ( snapshot != null )
        {
            if ( snapshot.getTimestamp() != null && snapshot.getBuildNumber() > 0 )
            {
                String newVersion = snapshot.getTimestamp() + "-" + snapshot.getBuildNumber();
                version = StringUtils.replace( baseVersion, "SNAPSHOT", newVersion );
            }
            else
            {
                version = baseVersion;
            }
        }
        return version;
    }

    private int resolveLatestSnapshotBuildNumber( Artifact artifact, ArtifactRepository localRepository,
                                                  ArtifactRepository remoteRepository )
        throws RepositoryMetadataResolutionException
    {
        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );

        if ( !wagonManager.isOnline() )
        {
            // build number is a required feature for metadata consistency
            throw new RepositoryMetadataResolutionException(
                "System is offline. Cannot resolve metadata:\n" + metadata.extendedToString() + "\n\n" );
        }

        getLogger().info( "Retrieving previous build number from " + remoteRepository.getId() );
        repositoryMetadataManager.resolveAlways( metadata, localRepository, remoteRepository );

        int buildNumber = 0;
        Metadata repoMetadata = metadata.getMetadata();
        if ( ( repoMetadata != null )
            && ( repoMetadata.getVersioning() != null && repoMetadata.getVersioning().getSnapshot() != null ) )
        {
            buildNumber = repoMetadata.getVersioning().getSnapshot().getBuildNumber();
        }
        return buildNumber;
    }

    public static DateFormat getUtcDateFormatter()
    {
        DateFormat utcDateFormatter = new SimpleDateFormat( UTC_TIMESTAMP_PATTERN );
        utcDateFormatter.setTimeZone( UTC_TIME_ZONE );
        return utcDateFormatter;
    }
}
