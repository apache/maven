package org.apache.maven.impl;

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

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectDeployer;
import org.apache.maven.api.services.ProjectDeployerException;
import org.apache.maven.api.services.ProjectDeployerRequest;
import org.apache.maven.api.services.ProjectManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.metadata.Metadata;

@Named
public class DefaultProjectDeployer implements ProjectDeployer
{

    private final RepositorySystem repositorySystem;

    @Inject
    DefaultProjectDeployer( @Nonnull RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void deploy( ProjectDeployerRequest request ) throws ProjectDeployerException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            ArtifactManager artifactManager = session.getService( ArtifactManager.class );
            Collection<Artifact> artifacts = session.getService( ProjectManager.class )
                    .getAttachedArtifacts( request.getProject() );
            List<Metadata> metadatas = artifacts.stream()
                    .map( artifactManager::getAttachedMetadatas )
                    .flatMap( Collection::stream )
                    .map( session::toMetadata )
                    .collect( Collectors.toList() );
            DeployRequest deployRequest = new DeployRequest()
                    .setRepository( session.toRepository( request.getRepository() ) )
                    .setArtifacts( session.toArtifacts( artifacts ) )
                    .setMetadata( metadatas );

            DeployResult result = repositorySystem.deploy( session.getSession(), deployRequest );
        }
        catch ( DeploymentException e )
        {
            throw new ProjectDeployerException( "Unable to deploy project", e );
        }
    }

    void dumb()
    {
        /*
        Artifact artifact = projectDeployerRequest.getProject().getArtifact();
        String packaging = projectDeployerRequest.getProject().getPackaging();
        File pomFile = projectDeployerRequest.getProject().getFile();

        List<Artifact> attachedArtifacts = projectDeployerRequest.getProject().getAttachedArtifacts();

        // Deploy the POM
        boolean isPomArtifact = "pom".equals( packaging );
        if ( isPomArtifact )
        {
            artifact.setFile( pomFile );
        }
        else
        {
            ProjectArtifactMetadata metadata = new ProjectArtifactMetadata( artifact, pomFile );
            artifact.addMetadata( metadata );
        }

        // What consequence does this have?
        // artifact.setRelease( true );

        artifact.setRepository( artifactRepository );

        int retryFailedDeploymentCount = projectDeployerRequest.getRetryFailedDeploymentCount();

        List<Artifact> deployableArtifacts = new ArrayList<>();
        if ( isPomArtifact )
        {
            deployableArtifacts.add( artifact );
        }
        else
        {
            File file = artifact.getFile();

            if ( file != null && file.isFile() )
            {
                deployableArtifacts.add( artifact );
            }
            else if ( !attachedArtifacts.isEmpty() )
            {
                // TODO: Reconsider this exception? Better Exception type?
                throw new NoFileAssignedException( "The packaging plugin for this project did not assign "
                    + "a main file to the project but it has attachments. Change packaging to 'pom'." );
            }
            else
            {
                // TODO: Reconsider this exception? Better Exception type?
                throw new NoFileAssignedException( "The packaging for this project did not assign "
                    + "a file to the build artifact" );
            }
        }

        deployableArtifacts.addAll( attachedArtifacts );

        deploy( buildingRequest, deployableArtifacts, artifactRepository, retryFailedDeploymentCount );

         */
    }
}
