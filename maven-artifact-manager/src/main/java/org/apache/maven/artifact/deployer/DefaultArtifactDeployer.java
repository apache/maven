package org.apache.maven.artifact.deployer;

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
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataDeploymentException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.transform.ArtifactTransformationManager;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class DefaultArtifactDeployer
    extends AbstractLogEnabled
    implements ArtifactDeployer
{
    private WagonManager wagonManager;

    private ArtifactTransformationManager transformationManager;

    private RepositoryMetadataManager repositoryMetadataManager;

    /**
     * @deprecated we want to use the artifact method only, and ensure artifact.file is set correctly.
     */
    public void deploy( String basedir, String finalName, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        String extension = artifact.getArtifactHandler().getExtension();
        File source = new File( basedir, finalName + "." + extension );
        deploy( source, artifact, deploymentRepository, localRepository );
    }

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        if ( !wagonManager.isOnline() )
        {
            // deployment shouldn't silently fail when offline
            throw new ArtifactDeploymentException( "System is offline. Cannot deploy artifact: " + artifact + "." );
        }

        // If we're installing the POM, we need to transform it first. The source file supplied for 
        // installation here may be the POM, but that POM may not be set as the file of the supplied
        // artifact. Since the transformation only has access to the artifact and not the supplied
        // source file, we have to use the Artifact.setFile(..) and Artifact.getFile(..) methods
        // to shunt the POM file into the transformation process.
        // Here, we also set a flag indicating that the POM has been shunted through the Artifact,
        // and to expect the transformed version to be available in the Artifact afterwards...
        boolean useArtifactFile = false;
        File oldArtifactFile = artifact.getFile();
        if ( "pom".equals( artifact.getType() ) )
        {
            artifact.setFile( source );
            useArtifactFile = true;
        }
        
        try
        {
            transformationManager.transformForDeployment( artifact, deploymentRepository, localRepository );

            // If we used the Artifact shunt to transform a POM source file, we need to install
            // the transformed version, not the supplied version. Therefore, we need to replace
            // the supplied source POM with the one from Artifact.getFile(..).
            if ( useArtifactFile )
            {
                source = artifact.getFile();
                artifact.setFile( oldArtifactFile );
            }

            // FIXME: Why oh why are we re-installing the artifact in the local repository? Isn't this
            // the responsibility of the ArtifactInstaller??
            
            // Copy the original file to the new one if it was transformed
            File artifactFile = new File( localRepository.getBasedir(), localRepository.pathOf( artifact ) );
            if ( !artifactFile.equals( source ) )
            {
                FileUtils.copyFile( source, artifactFile );
            }

            wagonManager.putArtifact( source, artifact, deploymentRepository );

            // must be after the artifact is installed
            for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
            {
                ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                repositoryMetadataManager.deploy( metadata, localRepository, deploymentRepository );
            }
            // TODO: would like to flush this, but the plugin metadata is added in advance, not as an install/deploy transformation
            // This would avoid the need to merge and clear out the state during deployment
//            artifact.getMetadataList().clear();
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: " + e.getMessage(), e );
        }
        catch ( RepositoryMetadataDeploymentException e )
        {
            throw new ArtifactDeploymentException( "Error installing artifact's metadata: " + e.getMessage(), e );
        }
    }
}
