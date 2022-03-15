package org.apache.maven.internal.impl;

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

import org.apache.maven.api.annotations.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactDeployerRequest;
import org.apache.maven.api.services.ArtifactManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.metadata.Metadata;

@Named
public class DefaultArtifactDeployer implements ArtifactDeployer
{
    private final RepositorySystem repositorySystem;

    @Inject
    DefaultArtifactDeployer( @Nonnull RepositorySystem repositorySystem )
    {
        this.repositorySystem = Objects.requireNonNull( repositorySystem );
    }

    @Override
    public void deploy( ArtifactDeployerRequest request ) throws ArtifactDeployerException, IllegalArgumentException
    {
        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            ArtifactManager artifactManager = session.getService( ArtifactManager.class );
            List<Metadata> metadatas = request.getArtifacts().stream()
                    .map( artifactManager::getAttachedMetadatas )
                    .flatMap( Collection::stream )
                    .map( session::toMetadata )
                    .collect( Collectors.toList() );
            DeployRequest deployRequest = new DeployRequest()
                    .setRepository( session.toRepository( request.getRepository() ) )
                    .setArtifacts( session.toArtifacts( request.getArtifacts() ) )
                    .setMetadata( metadatas );

            DeployResult result = repositorySystem.deploy( session.getSession(), deployRequest );
        }
        catch ( DeploymentException e )
        {
            throw new ArtifactDeployerException( "Unable to deploy artifacts", e );
        }
    }
}
