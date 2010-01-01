package org.apache.maven.project;

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
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.wagon.events.TransferListener;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @version $Id$
 */
@Component(role = MavenProjectBuilder.class)
@Deprecated
public class DefaultMavenProjectBuilder
    implements MavenProjectBuilder
{

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private LegacySupport legacySupport;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pomFile, ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        return projectBuilder.build( pomFile, configuration ).getProject();
    }

    public MavenProject buildFromRepository( Artifact artifact, ProjectBuilderConfiguration configuration, boolean allowStubModel )
        throws ProjectBuildingException
    {
        normalizeToArtifactRepositories( configuration );

        return projectBuilder.build( artifact, allowStubModel, configuration ).getProject();
    }

    private void normalizeToArtifactRepositories( ProjectBuilderConfiguration configuration )
        throws ProjectBuildingException
    {
        /*
         * This provides backward-compat with 2.x that allowed plugins like the maven-remote-resources-plugin:1.0 to
         * populate the builder configuration with model repositories instead of artifact repositories.
         */

        List<?> repositories = configuration.getRemoteRepositories();

        if ( repositories != null )
        {
            boolean normalized = false;

            List<ArtifactRepository> repos = new ArrayList<ArtifactRepository>( repositories.size() );

            for ( Object repository : repositories )
            {
                if ( repository instanceof Repository )
                {
                    try
                    {
                        ArtifactRepository repo = repositorySystem.buildArtifactRepository( (Repository) repository );
                        repositorySystem.injectMirror( Arrays.asList( repo ), configuration.getMirrors() );
                        repositorySystem.injectProxy( Arrays.asList( repo ), configuration.getProxies() );
                        repositorySystem.injectAuthentication( Arrays.asList( repo ), configuration.getServers() );
                        repos.add( repo );
                    }
                    catch ( InvalidRepositoryException e )
                    {
                        throw new ProjectBuildingException( "", "Invalid remote repository " + repository, e );
                    }
                    normalized = true;
                }
                else
                {
                    repos.add( (ArtifactRepository) repository );
                }
            }

            if ( normalized )
            {
                configuration.setRemoteRepositories( repos );
            }
        }
    }

    // This is used by the SITE plugin.
    public MavenProject build( File project, ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration()
            .setLocalRepository( localRepository );

        return build( project, configuration );
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                                             ArtifactRepository localRepository, boolean allowStubModel )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository( localRepository );
        configuration.setRemoteRepositories( remoteRepositories );
        configuration.setProcessPlugins( false );
        configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

        MavenSession session = legacySupport.getSession();
        if ( session != null )
        {
            configuration.setSystemProperties( session.getSystemProperties() );
            configuration.setUserProperties( session.getUserProperties() );
        }
        else
        {
            configuration.setSystemProperties( System.getProperties() );
        }

        try
        {
            return buildFromRepository( artifact, configuration, allowStubModel );
        }
        catch ( ProjectBuildingException e )
        {
            if ( e.getCause() instanceof ModelBuildingException )
            {
                throw new InvalidProjectModelException( e.getProjectId(), e.getMessage(), e.getPomFile() );
            }

            throw e;
        }
    }

    public MavenProject buildFromRepository( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                                             ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return buildFromRepository( artifact, remoteRepositories, localRepository, true );
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we
     * spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuilderConfiguration config )
        throws ProjectBuildingException
    {
        ModelSource modelSource = new UrlModelSource( getClass().getResource( "standalone.xml" ) );
        MavenProject project = projectBuilder.build( modelSource, config ).getProject();
        project.setExecutionRoot( true );
        return project;
    }

    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return buildStandaloneSuperProject( localRepository, null );
    }

    public MavenProject buildStandaloneSuperProject( ArtifactRepository localRepository, ProfileManager profileManager )
        throws ProjectBuildingException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();
        configuration.setLocalRepository( localRepository );
        configuration.setProcessPlugins( false );
        configuration.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

        if ( profileManager != null )
        {
            configuration.setActiveProfileIds( profileManager.getExplicitlyActivatedIds() );
            configuration.setInactiveProfileIds( profileManager.getExplicitlyDeactivatedIds() );
        }

        return buildStandaloneSuperProject( configuration );
    }

    public MavenProject buildWithDependencies( File project, ArtifactRepository localRepository,
                                               ProfileManager profileManager, TransferListener transferListener )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        ProjectBuilderConfiguration configuration = new DefaultProjectBuilderConfiguration();

        configuration.setLocalRepository( localRepository );

        if ( profileManager != null )
        {
            configuration.setActiveProfileIds( profileManager.getExplicitlyActivatedIds() );
            configuration.setInactiveProfileIds( profileManager.getExplicitlyDeactivatedIds() );
        }

        configuration.setResolveDependencies( true );

        return build( project, configuration );
    }

    public MavenProject buildWithDependencies( File project, ArtifactRepository localRepository,
                                               ProfileManager profileManager )
        throws ProjectBuildingException, ArtifactResolutionException, ArtifactNotFoundException
    {
        return buildWithDependencies( project, localRepository, profileManager, null );
    }

}
