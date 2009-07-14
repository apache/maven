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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

/**
 * @version $Id$
 */
@Component(role = ProjectBuilder.class)
public class DefaultProjectBuilder
    implements ProjectBuilder
{
    @Requirement
    private Logger logger;

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private ProjectBuildingHelper projectBuildingHelper;

    @Requirement
    private LifecycleExecutor lifecycle;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        return build( pomFile, true, configuration );
    }

    private MavenProject build( File pomFile, boolean localProject, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        ModelBuildingRequest request = getModelBuildingRequest( configuration );

        DefaultModelBuildingListener listener = new DefaultModelBuildingListener( projectBuildingHelper, configuration );
        request.setModelBuildingListeners( Arrays.asList( listener ) );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            ModelBuildingResult result;
            try
            {
                if ( localProject )
                {
                    result = modelBuilder.build( pomFile, request );
                }
                else
                {
                    result = modelBuilder.build( new FileModelSource( pomFile ), request );
                }
            }
            catch ( ModelBuildingException e )
            {
                throw new ProjectBuildingException( "[unknown]", "Failed to build project for " + pomFile, pomFile, e );
            }

            if ( localProject && !result.getProblems().isEmpty() && logger.isWarnEnabled() )
            {
                logger.warn( "" );
                logger.warn( "One or more problems were encountered while building the project's effective model:" );

                for ( ModelProblem problem : result.getProblems() )
                {
                    logger.warn( problem.getMessage() );
                }

                logger.warn( "" );
                logger.warn( "It is highly recommended to fix these problems"
                    + " because they threaten the stability of your build." );
                logger.warn( "" );
                logger.warn( "For this reason, future Maven versions will no"
                    + " longer support building such malformed projects." );
                logger.warn( "" );
            }

            Model model = result.getEffectiveModel();

            File parentPomFile = result.getRawModel( result.getModelIds().get( 1 ) ).getPomFile();
            MavenProject project = fromModelToMavenProject( model, parentPomFile, configuration, model.getPomFile() );

            project.setOriginalModel( result.getRawModel() );

            project.setRemoteArtifactRepositories( listener.getRemoteRepositories() );
            project.setPluginArtifactRepositories( listener.getPluginRepositories() );

            project.setClassRealm( listener.getProjectRealm() );

            try
            {
                if ( configuration.isProcessPlugins() )
                {
                    lifecycle.populateDefaultConfigurationForPlugins( model.getBuild().getPlugins(),
                                                                      configuration.getLocalRepository(),
                                                                      project.getPluginArtifactRepositories() );
                }
            }
            catch ( LifecycleExecutionException e )
            {
                throw new ProjectBuildingException( project.getId(), e.getMessage(), e );
            }

            Build build = project.getBuild();
            // NOTE: setting this script-source root before path translation, because
            // the plugin tools compose basedir and scriptSourceRoot into a single file.
            project.addScriptSourceRoot( build.getScriptSourceDirectory() );
            project.addCompileSourceRoot( build.getSourceDirectory() );
            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
            project.setFile( pomFile );

            List<Profile> activeProfiles = new ArrayList<Profile>();
            activeProfiles.addAll( result.getActivePomProfiles( result.getModelIds().get( 0 ) ) );
            activeProfiles.addAll( result.getActiveExternalProfiles() );
            project.setActiveProfiles( activeProfiles );

            project.setInjectedProfileIds( "external", getProfileIds( result.getActiveExternalProfiles() ) );
            for ( String modelId : result.getModelIds() )
            {
                project.setInjectedProfileIds( modelId, getProfileIds( result.getActivePomProfiles( modelId ) ) );
            }

            return project;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }
    }

    private List<String> getProfileIds( List<Profile> profiles )
    {
        List<String> ids = new ArrayList<String>( profiles.size() );

        for ( Profile profile : profiles )
        {
            ids.add( profile.getId() );
        }

        return ids;
    }

    private ModelBuildingRequest getModelBuildingRequest( ProjectBuildingRequest configuration )
    {
        ModelResolver resolver =
            new RepositoryModelResolver( repositorySystem, resolutionErrorHandler, configuration.getLocalRepository(),
                                         configuration.getRemoteRepositories() );

        ModelBuildingRequest request = new DefaultModelBuildingRequest();

        request.setValidationLevel( configuration.getValidationLevel() );
        request.setProcessPlugins( configuration.isProcessPlugins() );
        request.setProfiles( configuration.getProfiles() );
        request.setActiveProfileIds( configuration.getActiveProfileIds() );
        request.setInactiveProfileIds( configuration.getInactiveProfileIds() );
        request.setExecutionProperties( configuration.getExecutionProperties() );
        request.setBuildStartTime( configuration.getBuildStartTime() );
        request.setModelResolver( resolver );

        return request;
    }

    public MavenProject build( Artifact artifact, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        if ( !artifact.getType().equals( "pom" ) )
        {
            artifact = repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setLocalRepository( configuration.getLocalRepository() )
            .setRemoteRepostories( configuration.getRemoteRepositories() );
        
        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( artifact.getId(), "Error resolving project artifact.", e );
        }

        return build( artifact.getFile(), false, configuration );
    }

    /**
     * This is used for pom-less execution like running archetype:generate.
     * 
     * I am taking out the profile handling and the interpolation of the base directory until we
     * spec this out properly.
     */
    public MavenProject buildStandaloneSuperProject( ProjectBuildingRequest config )
        throws ProjectBuildingException
    {
        ModelBuildingRequest request = getModelBuildingRequest( config );

        ModelBuildingResult result;
        try
        {
            result = modelBuilder.build( new UrlModelSource( getClass().getResource( "standalone.xml" ) ), request );
        }
        catch ( ModelBuildingException e )
        {
            throw new ProjectBuildingException( "[standalone]", "Failed to build standalone project", e );
        }

        MavenProject standaloneProject;

        try
        {
            standaloneProject = new MavenProject( result.getEffectiveModel(), repositorySystem, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            throw new IllegalStateException( e );
        }

        standaloneProject.setActiveProfiles( result.getActiveExternalProfiles() );
        standaloneProject.setInjectedProfileIds( "external", getProfileIds( result.getActiveExternalProfiles() ) );

        standaloneProject.setExecutionRoot( true );

        return standaloneProject;
    }

    public MavenProjectBuildingResult buildProjectWithDependencies( File pomFile, ProjectBuildingRequest request )
        throws ProjectBuildingException
    {
        MavenProject project = build( pomFile, request );

        Artifact artifact = new ProjectArtifact( project );                     
        
        ArtifactResolutionRequest artifactRequest = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setResolveRoot( false )
            .setResolveTransitively( true )
            .setLocalRepository( request.getLocalRepository() )
            .setRemoteRepostories( project.getRemoteArtifactRepositories() )
            .setManagedVersionMap( project.getManagedVersionMap() );

        ArtifactResolutionResult result = repositorySystem.resolve( artifactRequest );

        if ( result.hasExceptions() )
        {
            Exception e = result.getExceptions().get( 0 );

            throw new ProjectBuildingException( safeVersionlessKey( project.getGroupId(), project.getArtifactId() ), "Unable to build project due to an invalid dependency version: " + e.getMessage(),
                                                pomFile, e );
        }

        project.setArtifacts( result.getArtifacts() );
        
        return new MavenProjectBuildingResult( project, result );
    }

    private MavenProject fromModelToMavenProject( Model model, File parentFile, ProjectBuildingRequest config, File projectDescriptor )
        throws InvalidProjectModelException
    {
        MavenProject project;

        try
        {
            project = new MavenProject( model, repositorySystem, this, config );

            Artifact projectArtifact = repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null, project.getPackaging() );
            project.setArtifact( projectArtifact );

            project.setParentFile( parentFile );
        }
        catch ( InvalidRepositoryException e )
        {
            String projectId = safeVersionlessKey( model.getGroupId(), model.getArtifactId() );
            throw new InvalidProjectModelException( projectId, e.getMessage(), projectDescriptor, e );
        }

        return project;
    }

    private static String safeVersionlessKey( String groupId, String artifactId )
    {
        String gid = groupId;

        if ( StringUtils.isEmpty( gid ) )
        {
            gid = "unknown";
        }

        String aid = artifactId;

        if ( StringUtils.isEmpty( aid ) )
        {
            aid = "unknown";
        }

        return ArtifactUtils.versionlessKey( gid, aid );
    }

}