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

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ProjectInstaller;
import org.apache.maven.api.services.ProjectInstallerException;
import org.apache.maven.api.services.ProjectInstallerRequest;
import org.apache.maven.api.services.ProjectManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.metadata.Metadata;

@Named
public class DefaultProjectInstaller implements ProjectInstaller
{
    private final RepositorySystem repositorySystem;

    @Inject
    DefaultProjectInstaller( RepositorySystem repositorySystem )
    {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public void install( ProjectInstallerRequest request ) throws ProjectInstallerException, IllegalArgumentException
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
            InstallRequest installRequest = new InstallRequest()
                    .setArtifacts( session.toArtifacts( artifacts ) )
                    .setMetadata( metadatas );

            InstallResult result = repositorySystem.install( session.getSession(), installRequest );
        }
        catch ( InstallationException e )
        {
            throw new ProjectInstallerException( "Unable to install project", e );
        }
    }
}
