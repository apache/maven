package org.apache.maven.artifact.deployer;

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
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactPathFormatException;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.apache.maven.wagon.TransferFailedException;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class DefaultArtifactDeployer
    implements ArtifactDeployer
{
    private WagonManager wagonManager;

    private ArtifactHandlerManager artifactHandlerManager;

    private List artifactTransformations;

    public void deploy( String basedir, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        File source = null;

        try
        {
            source = artifactHandlerManager.getArtifactHandler( artifact.getType() ).source( basedir, artifact );
        }
        catch ( ArtifactHandlerNotFoundException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }

        deploy( source, artifact, deploymentRepository, localRepository );
    }

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository,
                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        try
        {
            // TODO: better to have a transform manager, or reuse the handler manager again so we don't have these requirements duplicated all over?
            for ( Iterator i = artifactTransformations.iterator(); i.hasNext(); )
            {
                ArtifactTransformation transform = (ArtifactTransformation) i.next();
                transform.transformForDeployment( artifact, deploymentRepository );
            }

            wagonManager.putArtifact( source, artifact, deploymentRepository );

            // must be after the artifact is installed
            for ( Iterator i = artifact.getMetadataList().iterator(); i.hasNext(); )
            {
                ArtifactMetadata metadata = (ArtifactMetadata) i.next();
                metadata.storeInLocalRepository( localRepository );
                // TODO: shouldn't need to calculate this
                File f = new File( localRepository.getBasedir(), localRepository.pathOfMetadata( metadata ) );
                wagonManager.putArtifactMetadata( f, metadata, deploymentRepository );
            }
        }
        catch ( TransferFailedException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }
        catch ( ArtifactMetadataRetrievalException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }
        catch ( ArtifactPathFormatException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }
    }
}
