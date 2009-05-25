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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
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
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Jason van Zyl
 */
@Component(role = ArtifactMetadataSource.class)
public class MavenMetadataSource
    implements ArtifactMetadataSource
{
    @Requirement
    private RepositoryMetadataManager repositoryMetadataManager;

    //TODO: this will also cause a cycle so we need to refactor some code
    @Requirement
    private ArtifactFactory repositorySystem;

    //TODO: This prevents a cycle in the composition which shows us another problem we need to deal with. 
    //@Requirement
    private MavenProjectBuilder projectBuilder;

    @Requirement
    private PlexusContainer container;
    
    @Requirement
    private Logger logger;

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
        configuration.setLenientValidation( true );
        // We don't care about processing plugins here, all we're interested in is the dependencies.
        configuration.setProcessPlugins( false );
        // FIXME: We actually need the execution properties here...
        configuration.setExecutionProperties( System.getProperties() );

        MavenProject project;

        try
        {
            project = getProjectBuilder().buildFromRepository( pomArtifact, configuration );

            if ( !artifact.getArtifactHandler().isIncludesDependencies() )
            {
                artifacts = new LinkedHashSet<Artifact>();

                for ( Dependency d : project.getDependencies() )
                {
                    String effectiveScope = getEffectiveScope( d.getScope(), artifact.getScope() );

                    if ( effectiveScope != null )
                    {
                        Artifact dependencyArtifact;
                        
                        //TODO: deal with this in a unified way, probably just looking at the dependency.                        
                        if ( d.getClassifier() != null )
                        {
                            dependencyArtifact = repositorySystem.createArtifactWithClassifier( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), d.getClassifier() );                            
                        }
                        else
                        {
                            dependencyArtifact = repositorySystem.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(), effectiveScope, d.getType() );
                        }

                        dependencyArtifact.setOptional( d.isOptional() );

                        if ( Artifact.SCOPE_SYSTEM.equals( effectiveScope ) )
                        {
                            dependencyArtifact.setFile( new File( d.getSystemPath() ) );
                        }

                        artifacts.add( dependencyArtifact );
                    }
                }
            }
        }
        catch ( ProjectBuildingException e )
        {
            // When this happens we have a Maven 1.x POM, or some invalid POM. There is still a pile of
            // shit in the Maven 2.x repository that should have never found its way into the repository
            // but it did.
            logger.debug( "Failed to resolve artifact dependencies: " + e.getMessage() );
        }

        return new ResolutionGroup( pomArtifact, artifacts, remoteRepositories );
    }

    private String getEffectiveScope( String originalScope, String inheritedScope )
    {
        String effectiveScope = Artifact.SCOPE_RUNTIME;

        if ( originalScope == null )
        {
            originalScope = Artifact.SCOPE_COMPILE;
        }

        if ( inheritedScope == null )
        {
            // direct dependency retains its scope
            effectiveScope = originalScope;
        }
        else if ( Artifact.SCOPE_TEST.equals( originalScope ) || Artifact.SCOPE_PROVIDED.equals( originalScope ) )
        {
            // test and provided are not transitive, so exclude them
            effectiveScope = null;
        }
        else if ( Artifact.SCOPE_SYSTEM.equals( originalScope ) )
        {
            // system scope come through unchanged...
            effectiveScope = Artifact.SCOPE_SYSTEM;
        }
        else if ( Artifact.SCOPE_COMPILE.equals( originalScope ) && Artifact.SCOPE_COMPILE.equals( inheritedScope ) )
        {
            // added to retain compile scope. Remove if you want compile inherited as runtime
            effectiveScope = Artifact.SCOPE_COMPILE;
        }
        else if ( Artifact.SCOPE_TEST.equals( inheritedScope ) )
        {
            effectiveScope = Artifact.SCOPE_TEST;
        }
        else if ( Artifact.SCOPE_PROVIDED.equals( inheritedScope ) )
        {
            effectiveScope = Artifact.SCOPE_PROVIDED;
        }

        return effectiveScope;
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

    // USED BY MAVEN ASSEMBLY PLUGIN                                                                                                                                                                                                    
    @Deprecated                                                                                                                                                                                                                         
    public static Set<Artifact> createArtifacts( ArtifactFactory artifactFactory, List<Dependency> dependencies, String inheritedScope, ArtifactFilter dependencyFilter, MavenProject project )                                                                                                                                                                 
        throws InvalidDependencyVersionException                                                                                                                                                                                        
    {             
        return createArtifacts( artifactFactory, dependencies, dependencyFilter );
    }
    
    private static Set<Artifact> createArtifacts( ArtifactFactory factory, List<Dependency> dependencies, ArtifactFilter filter )
    {
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();

        for ( Dependency d : dependencies )
        {
            Artifact dependencyArtifact = factory.createArtifact( d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType() );

            if ( filter.include( dependencyArtifact ) )
            {
                artifacts.add( dependencyArtifact );
            }
        }
        
        return artifacts;
    }    
    
    public MavenProjectBuilder getProjectBuilder()
    {
        if ( projectBuilder != null )
        {
            return projectBuilder;
        }
        
        try
        {
            projectBuilder = container.lookup( MavenProjectBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
            // Won't happen
        }
        
        return projectBuilder;
    }    
}
