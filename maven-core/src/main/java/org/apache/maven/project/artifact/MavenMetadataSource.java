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
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
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
    
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        //
        // If we have a system scoped artifact then we do not want any searching in local or remote repositories
        // and we want artifact resolution to only return the system scoped artifact itself.
        //
        if ( artifact.getScope() != null && artifact.getScope().equals( Artifact.SCOPE_SYSTEM ) )
        {
            return new ResolutionGroup( null, null, null );
        }
        
        ResolutionGroup cached = cache.get( artifact, localRepository, remoteRepositories );

        if ( cached != null )
        {
            return cached;
        }

        List<Dependency> dependencies;

        Artifact pomArtifact;

        //TODO: Not even sure this is really required as the project will be cached in the builder, we'll see this
        // is currently the biggest hotspot
        if ( artifact instanceof ArtifactWithDependencies )
        {
            pomArtifact = artifact;

            dependencies = ((ArtifactWithDependencies)artifact).getDependencies();
        }
        else
        {
            ProjectRelocation rel = retrieveRelocatedProject( artifact, localRepository, remoteRepositories );
            
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
                dependencies = rel.project.getDependencies();
            }
        }

        Set<Artifact> artifacts = Collections.<Artifact>emptySet();       
        
        if ( !artifact.getArtifactHandler().isIncludesDependencies() )
        {
            artifacts = new LinkedHashSet<Artifact>();

            ArtifactFilter dependencyFilter = artifact.getDependencyFilter();

            for ( Dependency d : dependencies )
            {
                String effectiveScope = getEffectiveScope( d.getScope(), artifact.getScope() );

                if ( effectiveScope != null )
                {
                    Artifact dependencyArtifact;

                    VersionRange versionRange;
                    try
                    {
                        versionRange = VersionRange.createFromVersionSpec( d.getVersion() );
                    }
                    catch ( InvalidVersionSpecificationException e )
                    {
                        throw new ArtifactMetadataRetrievalException( "Invalid version for dependency "
                            + d.getManagementKey() + ": " + e.getMessage(), e, pomArtifact );
                    }

                    dependencyArtifact =
                        repositorySystem.createDependencyArtifact( d.getGroupId(), d.getArtifactId(), versionRange,
                                                                   d.getType(), d.getClassifier(), effectiveScope,
                                                                   d.isOptional() );

                    if ( dependencyFilter == null || dependencyFilter.include( dependencyArtifact ) )
                    {
                        dependencyArtifact.setOptional( d.isOptional() );

                        if ( Artifact.SCOPE_SYSTEM.equals( effectiveScope ) )
                        {
                            dependencyArtifact.setFile( new File( d.getSystemPath() ) );
                        }

                        ArtifactFilter newFilter = dependencyFilter;

                        if ( !d.getExclusions().isEmpty() )
                        {
                            List<String> exclusions = new ArrayList<String>();

                            for ( Exclusion e : d.getExclusions() )
                            {
                                exclusions.add( e.getGroupId() + ":" + e.getArtifactId() );
                            }

                            newFilter = new ExcludesArtifactFilter( exclusions );
                            if ( dependencyFilter != null )
                            {
                                newFilter = new AndArtifactFilter( Arrays.asList( dependencyFilter, newFilter ) );
                            }
                        }

                        dependencyArtifact.setDependencyFilter( newFilter );

                        artifacts.add( dependencyArtifact );
                    }
                }
            }
        }

        ResolutionGroup result = new ResolutionGroup( pomArtifact, artifacts, remoteRepositories );

        cache.put( artifact, localRepository, remoteRepositories, result );

        return result;
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
            throw new ArtifactMetadataRetrievalException( e.getMessage(), e, artifact );
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

    public Artifact retrieveRelocatedArtifact( Artifact artifact, ArtifactRepository localRepository,
                                               List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {

        ProjectRelocation rel = retrieveRelocatedProject( artifact, localRepository, remoteRepositories );

        if ( rel == null )
        {
            return artifact;
        }

        MavenProject project = rel.project;
        if ( project == null || getRelocationKey( artifact ).equals( getRelocationKey( project.getArtifact() ) ) )
        {
            return artifact;
        }

        // NOTE: Using artifact information here, since some POMs are deployed
        // to central with one version in the filename, but another in the <version> string!
        // Case in point: org.apache.ws.commons:XmlSchema:1.1:pom.
        //
        // Since relocation triggers a reconfiguration of the artifact's information
        // in retrieveRelocatedProject(..), this is safe to do.
        Artifact result = null;
        if ( artifact.getClassifier() != null )
        {
            result =
                repositorySystem.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                               artifact.getVersion(), artifact.getType(),
                                                               artifact.getClassifier() );
        }
        else
        {
            result =
                repositorySystem.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                 artifact.getVersion(), artifact.getScope(), artifact.getType() );
        }

        result.setResolved( artifact.isResolved() );
        result.setFile( artifact.getFile() );

        result.setScope( artifact.getScope() );
        result.setArtifactHandler( artifact.getArtifactHandler() );
        result.setDependencyFilter( artifact.getDependencyFilter() );
        result.setDependencyTrail( artifact.getDependencyTrail() );
        result.setOptional( artifact.isOptional() );
        result.setRelease( artifact.isRelease() );

        return result;
    }

    private String getRelocationKey( Artifact artifact )
    {
        return artifact.getGroupId() + ':' + artifact.getArtifactId() + ':' + artifact.getVersion();
    }

    private ProjectRelocation retrieveRelocatedProject( Artifact artifact, ArtifactRepository localRepository,
                                                        List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        MavenProject project = null;

        Artifact pomArtifact;
        boolean done = false;
        do
        {
            // TODO: can we just modify the original?
            pomArtifact =
                repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                        artifact.getVersion(), artifact.getScope() );

            if ( "pom".equals( artifact.getType() ) )
            {
                pomArtifact.setFile( artifact.getFile() );
            }

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                done = true;
            }
            else
            {
                try
                {

                    ProjectBuildingRequest configuration = new DefaultProjectBuildingRequest();
                    configuration.setLocalRepository( localRepository );
                    configuration.setRemoteRepositories( remoteRepositories );
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

                        artifact.setDownloadUrl( distMgmt.getDownloadUrl() );
                        pomArtifact.setDownloadUrl( distMgmt.getDownloadUrl() );
                    }

                    if ( relocation != null )
                    {
                        if ( relocation.getGroupId() != null )
                        {
                            artifact.setGroupId( relocation.getGroupId() );
                            project.setGroupId( relocation.getGroupId() );
                        }
                        if ( relocation.getArtifactId() != null )
                        {
                            artifact.setArtifactId( relocation.getArtifactId() );
                            project.setArtifactId( relocation.getArtifactId() );
                        }
                        if ( relocation.getVersion() != null )
                        {
                            // note: see MNG-3454. This causes a problem, but fixing it may break more.
                            artifact.setVersionRange( VersionRange.createFromVersion( relocation.getVersion() ) );
                            project.setVersion( relocation.getVersion() );
                        }

                        if ( artifact.getDependencyFilter() != null
                            && !artifact.getDependencyFilter().include( artifact ) )
                        {
                            return null;
                        }

                        // MNG-2861: the artifact data has changed. If the available versions where previously
                        // retrieved, we need to update it.
                        // TODO: shouldn't the versions be merged across relocations?
                        List<ArtifactVersion> available = artifact.getAvailableVersions();
                        if ( available != null && !available.isEmpty() )
                        {
                            artifact.setAvailableVersions( retrieveAvailableVersions( artifact, localRepository,
                                                                                      remoteRepositories ) );

                        }

                        String message =
                            "\n  This artifact has been relocated to " + artifact.getGroupId() + ":"
                                + artifact.getArtifactId() + ":" + artifact.getVersion() + ".\n";

                        if ( relocation.getMessage() != null )
                        {
                            message += "  " + relocation.getMessage() + "\n";
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

        return rel;
    }

    private static final class ProjectRelocation
    {
        private MavenProject project;

        private Artifact pomArtifact;
    }

}
