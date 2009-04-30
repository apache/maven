package org.apache.maven.project.artifact;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Relocation;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
@Component(role = ArtifactMetadataSource.class )
public class MavenMetadataSource
    extends AbstractLogEnabled
    implements ArtifactMetadataSource
{
    public static final String ROLE_HINT = "default";

    @Requirement
    private ArtifactFactory artifactFactory;

    @Requirement
    private RepositoryMetadataManager repositoryMetadataManager;
    
    // lazily instantiated and cached.
    private MavenProject superProject;

    @Requirement
    private PlexusContainer container;

    //!! not injected which is a problem
    private MavenProjectBuilder mavenProjectBuilder;

    /**
     * Unfortunately we have projects that are still sending us JARs without the accompanying POMs.
     */
    private boolean strictlyEnforceThePresenceOfAValidMavenPOM = false;

    /**
     * Resolve all relocations in the POM for this artifact, and return the new artifact coordinate.
     */
    public Artifact retrieveRelocatedArtifact( Artifact artifact, ArtifactRepository localRepository,
                                               List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        if ( artifact instanceof ActiveProjectArtifact )
        {
            return artifact;
        }

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
            result = artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact.getArtifactId(),
                                                                   artifact.getVersion(), artifact.getType(),
                                                                   artifact.getClassifier() );
        }
        else
        {
            result = artifactFactory.createArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                     artifact.getVersion(), artifact.getScope(), artifact.getType() );
        }

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
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }
    
    private ProjectRelocation retrieveRelocatedProject( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = Collections.emptyList();
        }

        try
        {
            loadProjectBuilder();
        }
        catch ( ComponentLookupException e )
        {
            throw new ArtifactMetadataRetrievalException(
                "Cannot lookup MavenProjectBuilder component instance: " + e.getMessage(), e );
        }

        MavenProject project = null;
        Artifact pomArtifact;

        boolean done = false;
        do
        {
            // TODO: can we just modify the original?
            pomArtifact = artifactFactory.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getScope() );

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                done = true;
            }
            else
            {
                try
                {
                    project = mavenProjectBuilder.buildFromRepository( pomArtifact, new DefaultProjectBuilderConfiguration( localRepository, remoteRepositories ) );
                }
                catch ( InvalidProjectModelException e )
                {
                    handleInvalidOrMissingMavenPOM( artifact, e );

                    if ( getLogger().isDebugEnabled() )
                    {
                        getLogger().debug( "Reason: " + e.getMessage() );

                        ModelValidationResult validationResult = e.getValidationResult();

                        if ( validationResult != null )
                        {
                            getLogger().debug( "\nValidation Errors:" );
                            for ( Iterator i = validationResult.getMessages().iterator(); i.hasNext(); )
                            {
                                getLogger().debug( i.next().toString() );
                            }
                            getLogger().debug( "\n" );
                        }
                        else
                        {
                            getLogger().debug( "", e );
                        }
                    }

                    throw new ArtifactMetadataRetrievalException(
                            "Cannot validate pom " + e.getMessage(), e );
                }
                catch ( ProjectBuildingException e )
                {
                    handleInvalidOrMissingMavenPOM( artifact, e );

                    throw new ArtifactMetadataRetrievalException(
                            "Cannot build project: " + e.getMessage(), e );
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
                            artifact.setVersionRange( VersionRange.createFromVersion( relocation.getVersion() ) );
                            project.setVersion( relocation.getVersion() );
                        }

                        if ( ( artifact.getDependencyFilter() != null ) &&
                            !artifact.getDependencyFilter().include( artifact ) )
                        {
                            return null;
                        }

                        //MNG-2861: the artifact data has changed. If the available versions where previously retrieved,
                        //we need to update it. TODO: shouldn't the versions be merged across relocations?
                        List available = artifact.getAvailableVersions();
                        if ( available != null && !available.isEmpty() )
                        {
                            artifact.setAvailableVersions(
                                retrieveAvailableVersions( artifact, localRepository, remoteRepositories ) );

                        }

                        String message = "\n  This artifact has been relocated to " + artifact.getGroupId() + ":" +
                            artifact.getArtifactId() + ":" + artifact.getVersion() + ".\n";

                        if ( relocation.getMessage() != null )
                        {
                            message += "  " + relocation.getMessage() + "\n";
                        }

                        if ( ( artifact.getDependencyTrail() != null ) &&
                            ( artifact.getDependencyTrail().size() == 1 ) )
                        {
                            getLogger().warn( "While downloading " + artifact.getGroupId() + ":" +
                                artifact.getArtifactId() + ":" + artifact.getVersion() + message + "\n" );
                        }
                        else
                        {
                            getLogger().debug( "While downloading " + artifact.getGroupId() + ":" +
                                artifact.getArtifactId() + ":" + artifact.getVersion() + message + "\n" );
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

        ProjectRelocation res = new ProjectRelocation();
        res.project = project;
        res.pomArtifact = pomArtifact;

        return res;
    }

    /**
     * Retrieve the metadata for the project from the repository.
     * Uses the ProjectBuilder, to enable post-processing and inheritance calculation before retrieving the
     * associated artifacts.
     */
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        ProjectRelocation res = retrieveRelocatedProject( artifact, localRepository, remoteRepositories );
        MavenProject project = res.project;
        Artifact pomArtifact = res.pomArtifact;

        // last ditch effort to try to get this set...
        if ( artifact.getDownloadUrl() == null )
        {
            // TODO: this could come straight from the project, negating the need to set it in the project itself?
            artifact.setDownloadUrl( pomArtifact.getDownloadUrl() );
        }

        ResolutionGroup result;

        if ( project == null )
        {
            // if the project is null, we encountered an invalid model (read: m1 POM)
            // we'll just return an empty resolution group.
            // or used the inherited scope (should that be passed to the buildFromRepository method above?)
            result = new ResolutionGroup( pomArtifact, Collections.EMPTY_SET, Collections.EMPTY_LIST );
        }
        else
        {
            Set artifacts = Collections.EMPTY_SET;
            if ( !artifact.getArtifactHandler().isIncludesDependencies() )
            {
                // TODO: we could possibly use p.getDependencyArtifacts instead of this call, but they haven't been filtered
                // or used the inherited scope (should that be passed to the buildFromRepository method above?)
                try
                {
                    artifacts =
                        project.createArtifacts( artifactFactory, artifact.getScope(), artifact.getDependencyFilter() );
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new ArtifactMetadataRetrievalException( "Error in metadata for artifact '" +
                        artifact.getDependencyConflictId() + "': " + e.getMessage(), e );
                }
            }

            //List repositories = aggregateRepositoryLists( remoteRepositories, project.getRemoteArtifactRepositories() );

            result = new ResolutionGroup( pomArtifact, artifacts, remoteRepositories );
        }

        return result;
    }

    private void handleInvalidOrMissingMavenPOM( Artifact artifact, ProjectBuildingException e )
        throws ArtifactMetadataRetrievalException
    {
        if ( strictlyEnforceThePresenceOfAValidMavenPOM )
        {
            throw new ArtifactMetadataRetrievalException(
                "Invalid POM file for artifact: '" + artifact.getDependencyConflictId() + "': " + e.getMessage(), e,
                artifact );
        }
        else
        {
            getLogger().debug( "\n\tDEPRECATION: The POM for the artifact '" + artifact.getDependencyConflictId() +
                "' was invalid or not found on any repositories.\n" +
                "\tThis may not be supported by future versions of Maven and should be corrected as soon as possible.\n" +
                "\tError given: " + e.getMessage() + "\n" );
        }
    }

    private void loadProjectBuilder()
        throws ComponentLookupException
    {
        if ( mavenProjectBuilder == null )
        {
            mavenProjectBuilder = (MavenProjectBuilder) container.lookup( MavenProjectBuilder.class );
        }
    }

    public List<ArtifactVersion> retrieveAvailableVersions( Artifact artifact, ArtifactRepository localRepository,
                                                            List<ArtifactRepository> remoteRepositories )
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

    public List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository( Artifact artifact,
                                                                                    ArtifactRepository localRepository,
                                                                                    ArtifactRepository deploymentRepository )
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
            versions = Collections.<ArtifactVersion>emptyList();
        }

        return versions;
    }

    private static final class ProjectRelocation
    {
        private MavenProject project;

        private Artifact pomArtifact;
    }

    // USED BY MAVEN ASSEMBLY PLUGIN
    @Deprecated
    public static Set<Artifact> createArtifacts( ArtifactFactory artifactFactory, List<Dependency> dependencies,
                                                 String inheritedScope, ArtifactFilter dependencyFilter,
                                                 MavenProject project )
        throws InvalidDependencyVersionException
    {
        try
        {
            return LegacyRepositorySystem.createArtifacts( artifactFactory, dependencies, inheritedScope,
                                                                dependencyFilter, project );
        }
        catch ( VersionNotFoundException e )
        {
            throw new InvalidDependencyVersionException( e.getProjectId(), e.getDependency(), e.getPomFile(),
                                                         e.getCauseException() );
        }
    }

}
