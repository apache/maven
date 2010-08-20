package org.apache.maven.project;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.DefaultModelProblem;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;

/**
 * @version $Id$
 */
@Component( role = ProjectBuilder.class )
public class DefaultProjectBuilder
    implements ProjectBuilder
{

    @Requirement
    private Logger logger;

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private ModelProcessor modelProcessor;

    @Requirement
    private ProjectBuildingHelper projectBuildingHelper;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ResolutionErrorHandler resolutionErrorHandler;

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public ProjectBuildingResult build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        return build( pomFile, new FileModelSource( pomFile ), configuration );
    }

    public ProjectBuildingResult build( ModelSource modelSource, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        return build( null, modelSource, configuration );
    }

    private ProjectBuildingResult build( File pomFile, ModelSource modelSource, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            MavenProject project = configuration.getProject();

            List<ModelProblem> modelProblems = null;

            if ( project == null )
            {
                ModelBuildingRequest request = getModelBuildingRequest( configuration, null );

                project = new MavenProject( repositorySystem, this, configuration, logger );

                DefaultModelBuildingListener listener =
                    new DefaultModelBuildingListener( project, projectBuildingHelper, configuration );
                request.setModelBuildingListener( listener );

                request.setPomFile( pomFile );
                request.setModelSource( modelSource );
                request.setLocationTracking( pomFile != null );

                ModelBuildingResult result;
                try
                {
                    result = modelBuilder.build( request );
                }
                catch ( ModelBuildingException e )
                {
                    throw new ProjectBuildingException( e.getModelId(), e.getMessage(), pomFile, e );
                }

                modelProblems = result.getProblems();

                initProject( project, result );
            }
            else if ( configuration.isResolveDependencies() )
            {
                projectBuildingHelper.selectProjectRealm( project );
            }

            ArtifactResolutionResult artifactResult = null;

            if ( configuration.isResolveDependencies() )
            {
                Artifact artifact = new ProjectArtifact( project );

                ArtifactResolutionRequest artifactRequest = new ArtifactResolutionRequest()
                    .setArtifact( artifact )
                    .setResolveRoot( false )
                    .setResolveTransitively( true )
                    .setCache( configuration.getRepositoryCache() )
                    .setLocalRepository( configuration.getLocalRepository() )
                    .setRemoteRepositories( project.getRemoteArtifactRepositories() )
                    .setServers( configuration.getServers() )
                    .setMirrors( configuration.getMirrors() )
                    .setProxies( configuration.getProxies() )
                    .setOffline( configuration.isOffline() )
                    .setForceUpdate( configuration.isForceUpdate() )
                    .setManagedVersionMap( project.getManagedVersionMap() );
                artifactRequest.setTransferListener( configuration.getTransferListener() );

                artifactResult = repositorySystem.resolve( artifactRequest );

                project.setResolvedArtifacts( artifactResult.getArtifacts() );
                project.setArtifacts( artifactResult.getArtifacts() );
            }

            return new DefaultProjectBuildingResult( project, modelProblems, artifactResult );
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

    private ModelBuildingRequest getModelBuildingRequest( ProjectBuildingRequest configuration,
                                                          ReactorModelPool reactorModelPool )
    {
        ModelResolver resolver =
            new RepositoryModelResolver( repositorySystem, resolutionErrorHandler, configuration, reactorModelPool );

        ModelBuildingRequest request = new DefaultModelBuildingRequest();

        request.setValidationLevel( configuration.getValidationLevel() );
        request.setProcessPlugins( configuration.isProcessPlugins() );
        request.setProfiles( configuration.getProfiles() );
        request.setActiveProfileIds( configuration.getActiveProfileIds() );
        request.setInactiveProfileIds( configuration.getInactiveProfileIds() );
        request.setSystemProperties( configuration.getSystemProperties() );
        request.setUserProperties( configuration.getUserProperties() );
        request.setBuildStartTime( configuration.getBuildStartTime() );
        request.setModelResolver( resolver );

        return request;
    }

    public ProjectBuildingResult build( Artifact artifact, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        return build( artifact, false, configuration );
    }

    public ProjectBuildingResult build( Artifact artifact, boolean allowStubModel, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        if ( !artifact.getType().equals( "pom" ) )
        {
            artifact =
                repositorySystem.createProjectArtifact( artifact.getGroupId(), artifact.getArtifactId(),
                                                        artifact.getVersion() );
        }

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
            .setArtifact( artifact )
            .setCache( configuration.getRepositoryCache() )
            .setLocalRepository( configuration.getLocalRepository() )
            .setRemoteRepositories( configuration.getRemoteRepositories() )
            .setOffline( configuration.isOffline() )
            .setForceUpdate( configuration.isForceUpdate() );
        request.setTransferListener( configuration.getTransferListener() );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        if ( result.hasMissingArtifacts() && allowStubModel )
        {
            return build( null, createStubModelSource( artifact ), configuration );
        }

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( artifact.getId(),
                                                "Error resolving project artifact: " + e.getMessage(), e );
        }

        boolean localProject = artifact.getRepository() != null && artifact.getRepository().isProjectAware();

        File pomFile = artifact.getFile();

        return build( localProject ? pomFile : null, new FileModelSource( pomFile ), configuration );
    }

    private ModelSource createStubModelSource( Artifact artifact )
    {
        StringBuilder buffer = new StringBuilder( 1024 );

        buffer.append( "<?xml version='1.0'?>" );
        buffer.append( "<project>" );
        buffer.append( "<modelVersion>4.0.0</modelVersion>" );
        buffer.append( "<groupId>" ).append( artifact.getGroupId() ).append( "</groupId>" );
        buffer.append( "<artifactId>" ).append( artifact.getArtifactId() ).append( "</artifactId>" );
        buffer.append( "<version>" ).append( artifact.getBaseVersion() ).append( "</version>" );
        buffer.append( "<packaging>" ).append( artifact.getType() ).append( "</packaging>" );
        buffer.append( "</project>" );

        return new StringModelSource( buffer, artifact.getId() );
    }

    public List<ProjectBuildingResult> build( List<File> pomFiles, boolean recursive, ProjectBuildingRequest config )
        throws ProjectBuildingException
    {
        List<ProjectBuildingResult> results = new ArrayList<ProjectBuildingResult>();

        List<InterimResult> interimResults = new ArrayList<InterimResult>();

        ReactorModelPool reactorModelPool = new ReactorModelPool();

        ReactorModelCache modelCache = new ReactorModelCache();

        boolean noErrors =
            build( results, interimResults, pomFiles, true, recursive, config, reactorModelPool, modelCache );

        populateReactorModelPool( reactorModelPool, interimResults );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            noErrors = build( results, new ArrayList<MavenProject>(), interimResults, config ) && noErrors;
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldContextClassLoader );
        }

        if ( !noErrors )
        {
            throw new ProjectBuildingException( results );
        }

        return results;
    }

    private boolean build( List<ProjectBuildingResult> results, List<InterimResult> interimResults,
                           List<File> pomFiles, boolean isRoot, boolean recursive, ProjectBuildingRequest config,
                           ReactorModelPool reactorModelPool, ReactorModelCache modelCache )
    {
        boolean noErrors = true;

        for ( File pomFile : pomFiles )
        {
            ModelBuildingRequest request = getModelBuildingRequest( config, reactorModelPool );

            MavenProject project = new MavenProject( repositorySystem, this, config, logger );

            request.setPomFile( pomFile );
            request.setTwoPhaseBuilding( true );
            request.setLocationTracking( true );
            request.setModelCache( modelCache );

            DefaultModelBuildingListener listener =
                new DefaultModelBuildingListener( project, projectBuildingHelper, config );
            request.setModelBuildingListener( listener );

            try
            {
                ModelBuildingResult result = modelBuilder.build( request );

                Model model = result.getEffectiveModel();

                InterimResult interimResult = new InterimResult( pomFile, request, result, listener, isRoot );
                interimResults.add( interimResult );

                if ( recursive && !model.getModules().isEmpty() )
                {
                    File basedir = pomFile.getParentFile();

                    List<File> moduleFiles = new ArrayList<File>();

                    for ( String module : model.getModules() )
                    {
                        if ( StringUtils.isEmpty( module ) )
                        {
                            continue;
                        }

                        module = module.replace( '\\', File.separatorChar ).replace( '/', File.separatorChar );

                        File moduleFile = new File( basedir, module );

                        if ( moduleFile.isDirectory() )
                        {
                            moduleFile = modelProcessor.locatePom( moduleFile );
                        }

                        if ( !moduleFile.isFile() )
                        {
                            ModelProblem problem =
                                new DefaultModelProblem( "Child module " + moduleFile + " of " + pomFile
                                    + " does not exist", ModelProblem.Severity.ERROR, model, -1, -1, null );
                            result.getProblems().add( problem );

                            noErrors = false;

                            continue;
                        }

                        if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
                        {
                            // we don't canonicalize on unix to avoid interfering with symlinks
                            try
                            {
                                moduleFile = moduleFile.getCanonicalFile();
                            }
                            catch ( IOException e )
                            {
                                moduleFile = moduleFile.getAbsoluteFile();
                            }
                        }
                        else
                        {
                            moduleFile = new File( moduleFile.toURI().normalize() );
                        }

                        moduleFiles.add( moduleFile );
                    }

                    interimResult.modules = new ArrayList<InterimResult>();

                    if ( !build( results, interimResult.modules, moduleFiles, false, recursive, config,
                                reactorModelPool, modelCache ) )
                    {
                        noErrors = false;
                    }
                }
            }
            catch ( ModelBuildingException e )
            {
                results.add( new DefaultProjectBuildingResult( e.getModelId(), pomFile, e.getProblems() ) );

                noErrors = false;
            }
        }

        return noErrors;
    }

    static class InterimResult
    {

        File pomFile;

        ModelBuildingRequest request;

        ModelBuildingResult result;

        DefaultModelBuildingListener listener;

        boolean root;

        List<InterimResult> modules = Collections.emptyList();

        InterimResult( File pomFile, ModelBuildingRequest request, ModelBuildingResult result,
                       DefaultModelBuildingListener listener, boolean root )
        {
            this.pomFile = pomFile;
            this.request = request;
            this.result = result;
            this.listener = listener;
            this.root = root;
        }

    }

    private void populateReactorModelPool( ReactorModelPool reactorModelPool, List<InterimResult> interimResults )
    {
        for ( InterimResult interimResult : interimResults )
        {
            Model model = interimResult.result.getEffectiveModel();
            reactorModelPool.put( model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPomFile() );

            populateReactorModelPool( reactorModelPool, interimResult.modules );
        }
    }

    private boolean build( List<ProjectBuildingResult> results, List<MavenProject> projects,
                           List<InterimResult> interimResults, ProjectBuildingRequest config )
    {
        boolean noErrors = true;

        for ( InterimResult interimResult : interimResults )
        {
            try
            {
                ModelBuildingResult result = modelBuilder.build( interimResult.request, interimResult.result );

                MavenProject project = interimResult.listener.getProject();
                initProject( project, result );

                List<MavenProject> modules = new ArrayList<MavenProject>();
                noErrors = build( results, modules, interimResult.modules, config ) && noErrors;

                projects.addAll( modules );
                projects.add( project );

                project.setExecutionRoot( interimResult.root );
                project.setCollectedProjects( modules );

                results.add( new DefaultProjectBuildingResult( project, result.getProblems(), null ) );
            }
            catch ( ModelBuildingException e )
            {
                results.add( new DefaultProjectBuildingResult( e.getModelId(), interimResult.pomFile, e.getProblems() ) );

                noErrors = false;
            }
        }

        return noErrors;
    }

    private void initProject( MavenProject project, ModelBuildingResult result )
    {
        Model model = result.getEffectiveModel();

        project.setModel( model );
        project.setOriginalModel( result.getRawModel() );

        project.setFile( model.getPomFile() );

        File parentPomFile = result.getRawModel( result.getModelIds().get( 1 ) ).getPomFile();
        project.setParentFile( parentPomFile );

        Artifact projectArtifact =
            repositorySystem.createArtifact( project.getGroupId(), project.getArtifactId(), project.getVersion(), null,
                                             project.getPackaging() );
        project.setArtifact( projectArtifact );

        if ( project.getFile() != null )
        {
            Build build = project.getBuild();
            project.addScriptSourceRoot( build.getScriptSourceDirectory() );
            project.addCompileSourceRoot( build.getSourceDirectory() );
            project.addTestCompileSourceRoot( build.getTestSourceDirectory() );
        }

        List<Profile> activeProfiles = new ArrayList<Profile>();
        activeProfiles.addAll( result.getActivePomProfiles( result.getModelIds().get( 0 ) ) );
        activeProfiles.addAll( result.getActiveExternalProfiles() );
        project.setActiveProfiles( activeProfiles );

        project.setInjectedProfileIds( "external", getProfileIds( result.getActiveExternalProfiles() ) );
        for ( String modelId : result.getModelIds() )
        {
            project.setInjectedProfileIds( modelId, getProfileIds( result.getActivePomProfiles( modelId ) ) );
        }
    }

}
