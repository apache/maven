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

import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactInstallerRequest;
import org.apache.maven.api.services.ArtifactManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.metadata.Metadata;

public class DefaultArtifactInstaller implements ArtifactInstaller
{

    private final RepositorySystem repositorySystem;

    DefaultArtifactInstaller( @Nonnull RepositorySystem repositorySystem )
    {
        this.repositorySystem = Objects.requireNonNull( repositorySystem );
    }

    @Override
    public void install( ArtifactInstallerRequest request ) throws ArtifactInstallerException, IllegalArgumentException
    {

        /*
        // prepare installRequest
        InstallRequest request = new InstallRequest();

        // transform artifacts
        for ( org.apache.maven.artifact.Artifact mavenArtifact : mavenArtifacts )
        {
            Artifact mainArtifact = RepositoryUtils.toArtifact( mavenArtifact );
            request.addArtifact( mainArtifact );

            for ( ArtifactMetadata metadata : mavenArtifact.getMetadataList() )
            {
                if ( metadata instanceof ProjectArtifactMetadata )
                {
                    Artifact pomArtifact = new SubArtifact( mainArtifact, "", "pom" );
                    pomArtifact = pomArtifact.setFile( ( (ProjectArtifactMetadata) metadata ).getFile() );
                    request.addArtifact( pomArtifact );
                }
                else if ( // metadata instanceof SnapshotArtifactRepositoryMetadata ||
                        metadata instanceof ArtifactRepositoryMetadata )
                {
                    // eaten, handled by repo system
                }
                else if ( metadata instanceof org.apache.maven.shared.transfer.metadata.ArtifactMetadata )
                {
                    org.apache.maven.shared.transfer.metadata.ArtifactMetadata transferMetadata =
                            (org.apache.maven.shared.transfer.metadata.ArtifactMetadata) metadata;

                    request.addMetadata( new Maven31MetadataBridge( metadata ).setFile( transferMetadata.getFile() ) );
                }
            }
        }
        */

        DefaultSession session = ( DefaultSession ) request.getSession();
        try
        {
            ArtifactManager artifactManager = session.getService( ArtifactManager.class );
            List<Metadata> metadatas = request.getArtifacts().stream()
                    .map( artifactManager::getAttachedMetadatas )
                    .flatMap( Collection::stream )
                    .map( session::toMetadata )
                    .collect( Collectors.toList() );
            InstallRequest installRequest = new InstallRequest()
                    .setArtifacts( session.toArtifacts( request.getArtifacts() ) )
                    .setMetadata( metadatas );

            InstallResult result = repositorySystem.install( session.getSession(), installRequest );
        }
        catch ( InstallationException e )
        {
            throw new ArtifactInstallerException( e.getMessage(), e );
        }
    }
}
