package org.apache.maven.artifact.ant;

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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * Deploy task, using maven-artifact.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DeployTask
    extends AbstractArtifactTask
{
    private RemoteRepository remoteRepository;

    private File file;

    public void execute()
    {
        ArtifactRepository localRepo = createLocalArtifactRepository();
        MavenProjectBuilder builder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );

        Pom pom = buildPom( builder, localRepo );

        if ( pom == null )
        {
            throw new BuildException( "A POM element is required to deploy to the repository" );
        }

        if ( remoteRepository == null )
        {
            if ( pom.getDistributionManagement() == null || pom.getDistributionManagement().getRepository() == null )
            {
                throw new BuildException( "A distributionManagement element is required in your POM to deploy" );
            }

            remoteRepository = createAntRemoteRepository( pom.getDistributionManagement().getRepository() );
        }

        ArtifactRepository deploymentRepository = createRemoteArtifactRepository( remoteRepository );

        // Deploy the POM
        Artifact artifact = new DefaultArtifact( pom.getGroupId(), pom.getArtifactId(), pom.getVersion(),
                                                 pom.getPackaging() );
        boolean isPomArtifact = "pom".equals( pom.getPackaging() );
        if ( !isPomArtifact )
        {
            ArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pom.getFile() );
            artifact.addMetadata( metadata );
        }

        log( "Deploying to " + remoteRepository.getUrl() );
        ArtifactDeployer deployer = (ArtifactDeployer) lookup( ArtifactDeployer.ROLE );
        try
        {
            if ( !isPomArtifact )
            {
                deployer.deploy( file, artifact, deploymentRepository, localRepo );
            }
            else
            {
                deployer.deploy( pom.getFile(), artifact, deploymentRepository, localRepo );
            }
        }
        catch ( ArtifactDeploymentException e )
        {
            // TODO: deployment exception that does not give a trace
            throw new BuildException( "Error deploying artifact", e );
        }
    }

    public RemoteRepository getRemoteRepository()
    {
        return remoteRepository;
    }

    public void addRemoteRepository( RemoteRepository remoteRepository )
    {
        this.remoteRepository = remoteRepository;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile( File file )
    {
        this.file = file;
    }
}
