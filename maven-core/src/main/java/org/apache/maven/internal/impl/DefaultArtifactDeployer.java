/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactDeployerRequest;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * Implementation of {@link ArtifactDeployer} service.
 */
@Named
@Singleton
public class DefaultArtifactDeployer
    implements ArtifactDeployer
{
    private final @Nonnull RepositorySystem repositorySystem;

    @Inject
    DefaultArtifactDeployer( @Nonnull
    RepositorySystem repositorySystem )
    {
        this.repositorySystem = nonNull( repositorySystem, "repositorySystem can not be null" );
    }

    @Override
    public void deploy( @Nonnull
    ArtifactDeployerRequest request )
    {
        nonNull( request, "request can not be null" );
        DefaultSession session =
            cast( DefaultSession.class, request.getSession(), "request.session should be a " + DefaultSession.class );
        Collection<Artifact> artifacts = nonNull( request.getArtifacts(), "request.artifacts can not be null" );
        RemoteRepository repository = nonNull( request.getRepository(), "request.repository can not be null" );
        try
        {
            DeployRequest deployRequest =
                new DeployRequest().setRepository( session.toRepository( repository ) ).setArtifacts( session.toArtifacts( artifacts ) );

            DeployResult result = repositorySystem.deploy( session.getSession(), deployRequest );
        }
        catch ( DeploymentException e )
        {
            throw new ArtifactDeployerException( "Unable to deploy artifacts", e );
        }
    }
}
