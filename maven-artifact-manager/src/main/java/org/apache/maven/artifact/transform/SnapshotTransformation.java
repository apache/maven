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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.metadata.AbstractVersionArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private int deploymentBuildNumber = 1;

    private Map buildNumbers = new HashMap();

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
            SnapshotArtifactMetadata metadata = new SnapshotArtifactMetadata( artifact );
            metadata.storeInLocalRepository( localRepository );
        }
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository )
        throws ArtifactMetadataRetrievalException
    {
        if ( artifact.isSnapshot() )
        {
            SnapshotArtifactMetadata metadata;

            try
            {
                metadata = (SnapshotArtifactMetadata) retrieveFromRemoteRepository( artifact, remoteRepository, null,
                                                                                    ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );
                
                updateDeploymentBuildNumber( artifact, metadata.getTimestamp(), metadata.getBuildNumber() );
            }
            catch ( ResourceDoesNotExistException e )
            {
                getLogger().debug(
                                   "Snapshot version metadata for: " + artifact.getId()
                                       + " not found. Creating a new metadata instance.", e );
                
                // ignore. We'll be creating this metadata if it doesn't exist...
                metadata = (SnapshotArtifactMetadata) createMetadata( artifact );
            }

            metadata.setVersion( getDeploymentTimestamp(), deploymentBuildNumber );

            artifact.setResolvedVersion( metadata.constructVersion() );

            artifact.addMetadata( metadata );
        }
    }

    private void updateDeploymentBuildNumber( Artifact artifact, String timestamp, int buildNumberFromMetadata )
    {
        // we only have to handle bumping the build number if we're on the same timestamp, somehow...miraculously
        if ( deploymentTimestamp.equals( timestamp ) )
        {
            String artifactKey = ArtifactUtils.versionlessKey( artifact );
            
            Integer buildNum = (Integer) buildNumbers.get( artifactKey );
            
            if ( buildNum == null || buildNum.intValue() <= buildNumberFromMetadata )
            {
                buildNum = new Integer( buildNumberFromMetadata + 1 );
                
                buildNumbers.put( artifactKey, buildNum );
            }
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
    
    public int getDeploymentBuildNumber( Artifact artifact )
    {
        String artifactKey = ArtifactUtils.versionlessKey( artifact );
        
        Integer buildNum = (Integer) buildNumbers.get( artifactKey );
        
        if ( buildNum == null )
        {
            buildNum = new Integer( 1 );
            buildNumbers.put( artifactKey, buildNum );
        }
        
        return buildNum.intValue();
    }

    protected AbstractVersionArtifactMetadata createMetadata( Artifact artifact )
    {
        return new SnapshotArtifactMetadata( artifact );
    }

    public String getDeploymentVersion( Artifact artifact )
    {
        int buildnum = getDeploymentBuildNumber( artifact );
        
        SnapshotArtifactMetadata metadata = (SnapshotArtifactMetadata) createMetadata( artifact );
        
        metadata.setVersion( getDeploymentTimestamp(), buildnum );
        
        return metadata.constructVersion();
    }

}