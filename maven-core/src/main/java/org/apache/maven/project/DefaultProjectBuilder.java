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
import java.io.IOException;
import java.util.Date;

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
import org.apache.maven.model.DefaultModelBuildingRequest;
import org.apache.maven.model.FileModelSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBuilder;
import org.apache.maven.model.ModelBuildingException;
import org.apache.maven.model.ModelBuildingRequest;
import org.apache.maven.model.ModelBuildingResult;
import org.apache.maven.model.io.ModelReader;
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
    private ModelReader modelReader;

    @Requirement
    private LifecycleExecutor lifecycle;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;
    
    private MavenProject superProject;

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
        ModelResolver resolver =
            new RepositoryModelResolver( repositorySystem, resolutionErrorHandler, configuration.getLocalRepository(),
                                         configuration.getRemoteRepositories() );

        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setLenientValidation( configuration.istLenientValidation() );
        request.setProcessPlugins( configuration.isProcessPlugins() );
        request.setProfiles( configuration.getProfiles() );
        request.setActiveProfileIds( configuration.getActiveProfileIds() );
        request.setInactiveProfileIds( configuration.getInactiveProfileIds() );
        request.setExecutionProperties( configuration.getExecutionProperties() );
        request.setBuildStartTime( configuration.getBuildStartTime() );
        request.setModelResolver( resolver );

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
        
        Model model = result.getEffectiveModel();

        MavenProject project = fromModelToMavenProject( model, result.getRawModels().get( 1 ).getPomFile(), configuration, model.getPomFile() );

        project.setOriginalModel( result.getRawModel() );
     
        try
        {
            if ( configuration.isProcessPlugins() )
            {
                lifecycle.populateDefaultConfigurationForPlugins( model.getBuild().getPlugins(), configuration.getLocalRepository(), project.getPluginArtifactRepositories() );
            }
        }
        catch ( LifecycleExecutionException e )
        {
            throw new ProjectBuildingException( "", e.getMessage(), e );
        }

        Build build = project.getBuild();
        // NOTE: setting this script-source root before path translation, because
        // the plugin tools compose basedir and scriptSourceRoot into a single file.
        project.addScriptSourceRoot( build.getScriptSourceDirectory() );
        project.addCompileSourceRoot( build.getSourceDirectory() );
        project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
        project.setFile( pomFile );
        project.setActiveProfiles( result.getActiveProfiles( result.getRawModel() ) );
                
        return project;
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
        if ( superProject != null )
        {
            return superProject;
        }

        Model superModel = getSuperModel();

        try
        {
            superProject = new MavenProject( superModel, repositorySystem, this, config );
        }
        catch ( InvalidRepositoryException e )
        {
            // Not going to happen.
        }

        superProject.setExecutionRoot( true );

        return superProject;
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

    // Super Model Handling

    private static final String MAVEN_MODEL_VERSION = "4.0.0";

    private Model superModel;

    protected Model getSuperModel()
    {
        if ( superModel != null )
        {
            return superModel;
        }

        String superPomResource = "/org/apache/maven/project/pom-" + MAVEN_MODEL_VERSION + ".xml";

        try
        {
            superModel = modelReader.read( getClass().getResourceAsStream( superPomResource ), null );
        }
        catch ( IOException e )
        {
            throw new IllegalStateException( "The super POM is damaged"
                + ", please verify the integrity of your Maven installation", e );
        }

        return superModel;
    }

}