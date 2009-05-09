package org.apache.maven.project.artifact;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Jason van Zyl
 */
//TODO: we don't need the repository metadata thing really, we can retrieve files independendently and parse
@Component(role = ArtifactMetadataSource.class)
public class MavenMetadataSource
    implements ArtifactMetadataSource
{
    @Requirement
    private RepositoryMetadataManager repositoryMetadataManager;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private MavenProjectBuilder projectBuilder;

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        Artifact pomArtifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );

        if ( "pom".equals( artifact.getType() ) )
        {
            pomArtifact.setFile( artifact.getFile() );
        }

        Set<Artifact> artifacts = Collections.emptySet();

        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository( localRepository );
        configuration.setRemoteRepositories( remoteRepositories );

        MavenProject project;

        try
        {
            project = projectBuilder.buildFromRepository( pomArtifact, configuration );

            if ( !artifact.getArtifactHandler().isIncludesDependencies() )
            {                
                ArtifactFilter filter;
                if ( artifact.getScope() == null )
                {
                    filter = null;
                }
                else
                {
                    filter = new ScopeArtifactFilter( artifact.getScope() );
                }
                                
                artifacts = project.createArtifacts( filter );
                
                project.setArtifacts( artifacts );                
            }
        }
        catch ( ProjectBuildingException e )
        {
            // When this happens we have a Maven 1.x POM, or some invalid POM. There is still a pile of
            // shit in the Maven 2.x repository that should have never found its way into the repository
            // but it did.
        }

        return new ResolutionGroup( pomArtifact, artifacts, remoteRepositories );
    }

    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );

        try
        {
            repositoryMetadataManager.resolve( metadata, remoteRepositories, localRepository );
        }
        catch ( RepositoryMetadataResolutionException e )
        {
            throw new ArtifactMetadataRetrievalException( e.getMessage(), e );
        }

        return retrieveAvailableVersionsFromMetadata( metadata.getMetadata() );
    }

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact, ArtifactRepository localRepository, ArtifactRepository deploymentRepository )
        throws ArtifactMetadataRetrievalException
    {
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        try
        {
            repositoryMetadataManager.resolveAlways( metadata, localRepository, deploymentRepository );
        }
        catch ( RepositoryMetadataResolutionException e )
        {
            throw new ArtifactMetadataRetrievalException( e.getMessage(), e );
        }

        return retrieveAvailableVersionsFromMetadata( metadata.getMetadata() );
    }

    private List<ArtifactVersion> retrieveAvailableVersionsFromMetadata( Metadata repoMetadata )
    {
        List<ArtifactVersion> versions;
        if ( ( repoMetadata != null ) && ( repoMetadata.getVersioning() != null ) )
        {
            List<String> metadataVersions = repoMetadata.getVersioning().getVersions();
            versions = new ArrayList<ArtifactVersion>( metadataVersions.size() );
            for ( String version : metadataVersions )
            {
                versions.add( new DefaultArtifactVersion( version ) );
            }
        }
        else
        {
            versions = Collections.<ArtifactVersion> emptyList();
        }

        return versions;
    }
}
