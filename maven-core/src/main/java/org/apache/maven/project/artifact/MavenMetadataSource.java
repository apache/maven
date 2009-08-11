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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataResolutionException;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.legacy.metadata.DefaultMetadataResolutionRequest;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;
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

    @Requirement
    private ArtifactFactory repositorySystem;

    //TODO: This prevents a cycle in the composition which shows us another problem we need to deal with. 
    //@Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private PlexusContainer container;

    @Requirement
    private Logger logger;

    @Requirement
    private MavenMetadataCache cache;    

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        return retrieve( artifact, localRepository, remoteRepositories, false );
    }

    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository,
                                     List<ArtifactRepository> remoteRepositories, boolean resolveManagedVersions )
        throws ArtifactMetadataRetrievalException
    {
        MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        request.setArtifact( artifact );
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        request.setResolveManagedVersions( resolveManagedVersions );
        return retrieve( request );
    }

    public ResolutionGroup retrieve( MetadataResolutionRequest request )
        throws ArtifactMetadataRetrievalException
    {
        Artifact artifact = request.getArtifact();

        //
        // If we have a system scoped artifact then we do not want any searching in local or remote repositories
        // and we want artifact resolution to only return the system scoped artifact itself.
        //
        if ( artifact.getScope() != null && artifact.getScope().equals( Artifact.SCOPE_SYSTEM ) )
        {
            return new ResolutionGroup( null, null, null );
        }

        ResolutionGroup cached =
            cache.get( artifact, request.isResolveManagedVersions(), request.getLocalRepository(),
                       request.getRemoteRepositories() );

        if ( cached != null )
        {
            return cached;
        }

        List<Dependency> dependencies;

        List<Dependency> managedDependencies = null;

        Artifact pomArtifact;

        Artifact relocatedArtifact = null;

        //TODO: Not even sure this is really required as the project will be cached in the builder, we'll see this
        // is currently the biggest hotspot
        if ( artifact instanceof ArtifactWithDependencies )
        {
            pomArtifact = artifact;

            dependencies = ( (ArtifactWithDependencies) artifact ).getDependencies();

            managedDependencies = ( (ArtifactWithDependencies) artifact ).getManagedDependencies();
        }
        else
        {
            ProjectRelocation rel = retrieveRelocatedProject( artifact, request );
            
            if ( rel == null )
            {
                return null;
            }

            pomArtifact = rel.pomArtifact;

            if ( rel.project == null )
            {
                // When this happens we have a Maven 1.x POM, or some invalid POM. There is still a pile of
                // shit in the Maven 2.x repository that should have never found its way into the repository
                // but it did.
                dependencies = Collections.emptyList();
            }
            else
            {
                relocatedArtifact = rel.relocatedArtifact;

                dependencies = rel.project.getDependencies();

                DependencyManagement depMngt = rel.project.getDependencyManagement();
                managedDependencies = ( depMngt != null ) ? depMngt.getDependencies() : null;
            }
        }

        Set<Artifact> artifacts = Collections.<Artifact>emptySet();       
        
        if ( !artifact.getArtifactHandler().isIncludesDependencies() )
        {
            artifacts = new LinkedHashSet<Artifact>();

            for ( Dependency dependency : dependencies )
            {
                Artifact dependencyArtifact = createDependencyArtifact( dependency, artifact, pomArtifact );

                if ( dependencyArtifact != null )
                {
                    artifacts.add( dependencyArtifact );
                }
            }
        }

        Map<String, Artifact> managedVersions = null;

        if ( managedDependencies != null && request.isResolveManagedVersions() )
        {
            managedVersions = new HashMap<String, Artifact>();

            for ( Dependency managedDependency : managedDependencies )
            {
                Artifact managedArtifact = createDependencyArtifact( managedDependency, null, pomArtifact );

                managedVersions.put( managedDependency.getManagementKey(), managedArtifact );
            }
        }

        ResolutionGroup result =
            new ResolutionGroup( pomArtifact, relocatedArtifact, artifacts, managedVersions, request.getRemoteRepositories() );

        cache.put( artifact, request.isResolveManagedVersions(), request.getLocalRepository(),
                   request.getRemoteRepositories(), result );

        return result;
    }

    private Artifact createDependencyArtifact( Dependency dependency, Artifact owner, Artifact pom )
        throws ArtifactMetadataRetrievalException
    {
        String effectiveScope = getEffectiveScope( dependency.getScope(), ( owner != null ) ? owner.getScope() : null );

        if ( effectiveScope == null )
        {
            return null;
        }

        VersionRange versionRange;
        try
        {
            versionRange = VersionRange.createFromVersionSpec( dependency.getVersion() );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new ArtifactMetadataRetrievalException( "Invalid version for dependency "
                + dependency.getManagementKey() + ": " + e.getMessage(), e, pom );
        }

        Artifact dependencyArtifact =
            repositorySystem.createDependencyArtifact( dependency.getGroupId(), dependency.getArtifactId(),
                                                       versionRange, dependency.getType(), dependency.getClassifier(),
                                                       effectiveScope, dependency.isOptional() );

        ArtifactFilter dependencyFilter = ( owner != null ) ? owner.getDependencyFilter() : null;

        if ( dependencyFilter != null && !dependencyFilter.include( dependencyArtifact ) )
        {
            return null;
        }

        if ( Artifact.SCOPE_SYSTEM.equals( effectiveScope ) )
        {
            dependencyArtifact.setFile( new File( dependency.getSystemPath() ) );
        }

        dependencyArtifact.setDependencyFilter( createDependencyFilter( dependency, dependencyFilter ) );

        return dependencyArtifact;
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

    private ArtifactFilter createDependencyFilter( Dependency dependency, ArtifactFilter inheritedFilter )
    {
        ArtifactFilter effectiveFilter = inheritedFilter;

        if ( !dependency.getExclusions().isEmpty() )
        {
            List<String> exclusions = new ArrayList<String>();

            for ( Exclusion e : dependency.getExclusions() )
            {
                exclusions.add( e.getGroupId() + ':' + e.getArtifactId() );
            }

            effectiveFilter = new ExcludesArtifactFilter( exclusions );

            if ( inheritedFilter != null )
            {
                effectiveFilter = new AndArtifactFilter( Arrays.asList( inheritedFilter, effectiveFilter ) );
            }
        }

        return effectiveFilter;
    }

    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        MetadataResolutionRequest request = new DefaultMetadataResolutionRequest();
        request.setArtifact( artifact );
        request.setLocalRepository( localRepository );
        request.setRemoteRepositories( remoteRepositories );
        return retrieveAvailableVersions( request );
    }

    public List<ArtifactVersion> retrieveAvailableVersions( MetadataResolutionRequest request )
        throws ArtifactMetadataRetrievalException
    {
        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( request.getArtifact() );

        try
        {
            repositoryMetadataManager.resolve( metadata, request );
        }
        catch ( RepositoryMetadataResolutionException e )
        {
            throw new ArtifactMetadataRetrievalException( e.getMessage(), e, request.getArtifact() );
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
            throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
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

    private ProjectBuilder getProjectBuilder()
    {
        if ( projectBuilder != null )
        {
            return projectBuilder;
        }

        try
        {
            projectBuilder = container.lookup( ProjectBuilder.class );
        }
        catch ( ComponentLookupException e )
        {
            // Won't happen
        }

        return projectBuilder;
    }

    private ProjectRelocation retrieveRelocatedProject( Artifact artifact, RepositoryRequest repositoryRequest )
        throws ArtifactMetadataRetrievalException
    {
        MavenProject project = null;

        Artifact pomArtifact;
        Artifact relocatedArtifact = artifact;
        boolean done = false;
        do
        {
            pomArtifact =
                repositorySystem.createProjectArtifact( relocatedArtifact.getGroupId(),
                                                        relocatedArtifact.getArtifactId(),
                                                        relocatedArtifact.getVersion(), relocatedArtifact.getScope() );

            if ( "pom".equals( relocatedArtifact.getType() ) )
            {
                pomArtifact.setFile( relocatedArtifact.getFile() );
            }

            if ( Artifact.SCOPE_SYSTEM.equals( relocatedArtifact.getScope() ) )
            {
                done = true;
            }
            else
            {
                try
                {
                    ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
                    configuration.setRepositoryCache( repositoryRequest.getCache() );
                    configuration.setLocalRepository( repositoryRequest.getLocalRepository() );
                    configuration.setRemoteRepositories( repositoryRequest.getRemoteRepositories() );
                    configuration.setOffline( repositoryRequest.isOffline() );
                    configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );
                    configuration.setProcessPlugins( false );
                    configuration.setSystemProperties( System.getProperties() );

                    project = getProjectBuilder().build( pomArtifact, configuration );
                }
                catch ( ProjectBuildingException e )
                {
                    // bad/incompatible POM
                    logger.debug( "Invalid artifact metadata for " + artifact.getId() + ": " + e.getMessage() );
                }

                if ( project != null )
                {
                    Relocation relocation = null;

                    DistributionManagement distMgmt = project.getDistributionManagement();
                    if ( distMgmt != null )
                    {
                        relocation = distMgmt.getRelocation();

                        relocatedArtifact.setDownloadUrl( distMgmt.getDownloadUrl() );
                        pomArtifact.setDownloadUrl( distMgmt.getDownloadUrl() );
                    }

                    if ( relocation != null )
                    {
                        if ( relocatedArtifact == artifact )
                        {
                            relocatedArtifact = ArtifactUtils.copyArtifact( artifact );
                        }

                        if ( relocation.getGroupId() != null )
                        {
                            relocatedArtifact.setGroupId( relocation.getGroupId() );
                            project.setGroupId( relocation.getGroupId() );
                        }
                        if ( relocation.getArtifactId() != null )
                        {
                            relocatedArtifact.setArtifactId( relocation.getArtifactId() );
                            project.setArtifactId( relocation.getArtifactId() );
                        }
                        if ( relocation.getVersion() != null )
                        {
                            // note: see MNG-3454. This causes a problem, but fixing it may break more.
                            relocatedArtifact.setVersionRange( VersionRange.createFromVersion( relocation.getVersion() ) );
                            project.setVersion( relocation.getVersion() );
                        }

                        if ( artifact.getDependencyFilter() != null
                            && !artifact.getDependencyFilter().include( relocatedArtifact ) )
                        {
                            return null;
                        }

                        // MNG-2861: the artifact data has changed. If the available versions where previously
                        // retrieved, we need to update it.
                        // TODO: shouldn't the versions be merged across relocations?
                        List<ArtifactVersion> available = artifact.getAvailableVersions();
                        if ( available != null && !available.isEmpty() )
                        {
                            MetadataResolutionRequest metadataRequest =
                                new DefaultMetadataResolutionRequest( repositoryRequest );
                            metadataRequest.setArtifact( relocatedArtifact );
                            available = retrieveAvailableVersions( metadataRequest );
                            relocatedArtifact.setAvailableVersions( available );
                        }

                        String message =
                            "\n  This artifact has been relocated to " + relocatedArtifact.getGroupId() + ":"
                                + relocatedArtifact.getArtifactId() + ":" + relocatedArtifact.getVersion() + ".\n";

                        if ( relocation.getMessage() != null )
                        {
                            message += "  " + relocation.getMessage() + "\n";
                        }

                        if ( artifact.getDependencyTrail() != null && artifact.getDependencyTrail().size() == 1 )
                        {
                            logger.warn( "While downloading " + pomArtifact.getGroupId() + ":"
                                + pomArtifact.getArtifactId() + ":" + pomArtifact.getVersion() + message + "\n" );
                        }
                        else
                        {
                            logger.debug( "While downloading " + pomArtifact.getGroupId() + ":"
                                + pomArtifact.getArtifactId() + ":" + pomArtifact.getVersion() + message + "\n" );
                        }
                    }
                    else
                    {
                        done = true;
                    }
                }
                else
                {
                    done = true;
                }
            }
        }
        while ( !done );

        ProjectRelocation rel = new ProjectRelocation();
        rel.project = project;
        rel.pomArtifact = pomArtifact;
        rel.relocatedArtifact = ( relocatedArtifact == artifact ) ? null : relocatedArtifact;

        return rel;
    }

    private static final class ProjectRelocation
    {
        private MavenProject project;

        private Artifact pomArtifact;

        private Artifact relocatedArtifact;
    }

}
