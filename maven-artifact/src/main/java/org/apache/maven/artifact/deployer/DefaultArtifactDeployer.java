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

import org.apache.maven.artifact.AbstractArtifactComponent;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;

/**
 * @todo snapshot notions need to be dealt with in one place.
 */
public class DefaultArtifactDeployer
    extends AbstractArtifactComponent
    implements ArtifactDeployer
{
    private WagonManager wagonManager;

    public void deploy( String basedir, Artifact artifact, ArtifactRepository deploymentRepository )
        throws ArtifactDeploymentException
    {
        File source = null;

        try
        {
            source = getArtifactHandler( artifact.getType() ).source( basedir, artifact );
        }
        catch ( ArtifactHandlerNotFoundException e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }

        deploy( source, artifact, deploymentRepository );
    }

    public void deploy( File source, Artifact artifact, ArtifactRepository deploymentRepository )
        throws ArtifactDeploymentException
    {
        try
        {
            wagonManager.put( source, artifact, deploymentRepository );
        }
        catch ( Exception e )
        {
            throw new ArtifactDeploymentException( "Error deploying artifact: ", e );
        }
    }
}
