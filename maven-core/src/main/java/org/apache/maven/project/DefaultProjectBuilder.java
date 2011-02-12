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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
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
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.WorkspaceRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.SubArtifact;

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
    private ModelProcessor modelProcessor;

    @Requirement
    private ProjectBuildingHelper projectBuildingHelper;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement
    private RemoteRepositoryManager repositoryManager;

    @Requirement
    private ProjectDependenciesResolver dependencyResolver;

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
                request.setLocationTracking( true );
    
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

                initProject( project, result, new HashMap<File, Boolean>() );
            }
            else if ( configuration.isResolveDependencies() )
            {
                projectBuildingHelper.selectProjectRealm( project );
            }

            DependencyResolutionResult resolutionResult = null;

            if ( configuration.isResolveDependencies() )
            {
                try
                {
                    DefaultDependencyResolutionRequest resolution =
                        new DefaultDependencyResolutionRequest( project, configuration.getRepositorySession() );
                    resolutionResult = dependencyResolver.resolve( resolution );
                }
                catch ( DependencyResolutionException e )
                {
                    resolutionResult = e.getResult();
                }

                Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
                if ( resolutionResult.getDependencyGraph() != null )
                {
                    RepositoryUtils.toArtifacts( artifacts, resolutionResult.getDependencyGraph().getChildren(),
                                                 Collections.singletonList( project.getArtifact().getId() ), null );

                    // Maven 2.x quirk: an artifact always points at the local repo, regardless whether resolved or not
                    LocalRepositoryManager lrm = configuration.getRepositorySession().getLocalRepositoryManager();
                    for ( Artifact artifact : artifacts )
                    {
                        if ( !artifact.isResolved() )
                        {
                            String path = lrm.getPathForLocalArtifact( RepositoryUtils.toArtifact( artifact ) );
                            artifact.setFile( new File( lrm.getRepository().getBasedir(), path ) );
                        }
                    }
                }
                project.setResolvedArtifacts( artifacts );
                project.setArtifacts( artifacts );
            }

            return new DefaultProjectBuildingResult( project, modelProblems, resolutionResult );
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
                                                          ReactorModelPool modelPool )
    {
        ModelResolver resolver =
            new ProjectModelResolver( configuration.getRepositorySession(), artifactResolver, repositoryManager,
                                      RepositoryUtils.toRepos( configuration.getRemoteRepositories() ),
                                      configuration.getRepositoryMerging(), modelPool );

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
        org.sonatype.aether.artifact.Artifact pomArtifact = RepositoryUtils.toArtifact( artifact );
        if ( !pomArtifact.getExtension().equals( "pom" ) )
        {
            pomArtifact = new SubArtifact( pomArtifact, "", "pom" );
        }

        ArtifactResult result;
        try
        {
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact( pomArtifact );
            request.setRepositories( RepositoryUtils.toRepos( configuration.getRemoteRepositories() ) );
            result = artifactResolver.resolveArtifact( configuration.getRepositorySession(), request );
            pomArtifact = result.getArtifact();
        }
        catch ( org.sonatype.aether.resolution.ArtifactResolutionException e )
        {
            if ( e.getResults().get( 0 ).isMissing() && allowStubModel )
            {
                return build( null, createStubModelSource( artifact ), configuration );
            }
            throw new ProjectBuildingException( artifact.getId(),
                                                "Error resolving project artifact: " + e.getMessage(), e );
        }

        boolean localProject = result.getRepository() instanceof WorkspaceRepository;

        File pomFile = pomArtifact.getFile();

        if ( "pom".equals( artifact.getType() ) )
        {
            artifact.selectVersion( pomArtifact.getVersion() );
            artifact.setFile( pomFile );
            artifact.setResolved( true );
        }

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

        ReactorModelPool modelPool = new ReactorModelPool();

        ReactorModelCache modelCache = new ReactorModelCache();

        boolean noErrors =
            build( results, interimResults, pomFiles, new LinkedHashSet<File>(), true, recursive, config, modelPool,
                   modelCache );

        populateReactorModelPool( modelPool, interimResults );

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            noErrors =
                build( results, new ArrayList<MavenProject>(), interimResults, config, new HashMap<File, Boolean>() )
                    && noErrors;
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
                           List<File> pomFiles, Set<File> aggregatorFiles, boolean isRoot, boolean recursive,
                           ProjectBuildingRequest config, ReactorModelPool reactorModelPool,
                           ReactorModelCache modelCache )
    {
        boolean noErrors = true;

        for ( File pomFile : pomFiles )
        {
            aggregatorFiles.add( pomFile );

            if ( !build( results, interimResults, pomFile, aggregatorFiles, isRoot, recursive, config,
                         reactorModelPool, modelCache ) )
            {
                noErrors = false;
            }

            aggregatorFiles.remove( pomFile );
        }

        return noErrors;
    }

    private boolean build( List<ProjectBuildingResult> results, List<InterimResult> interimResults, File pomFile,
                           Set<File> aggregatorFiles, boolean isRoot, boolean recursive, ProjectBuildingRequest config,
                           ReactorModelPool reactorModelPool, ReactorModelCache modelCache )
    {
        boolean noErrors = true;

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

                    if ( aggregatorFiles.contains( moduleFile ) )
                    {
                        StringBuilder buffer = new StringBuilder( 256 );
                        for ( File aggregatorFile : aggregatorFiles )
                        {
                            buffer.append( aggregatorFile ).append( " -> " );
                        }
                        buffer.append( moduleFile );

                        ModelProblem problem =
                            new DefaultModelProblem( "Child module " + moduleFile + " of " + pomFile
                                + " forms aggregation cycle " + buffer, ModelProblem.Severity.ERROR, model, -1, -1,
                                                     null );
                        result.getProblems().add( problem );

                        noErrors = false;

                        continue;
                    }

                    moduleFiles.add( moduleFile );
                }

                interimResult.modules = new ArrayList<InterimResult>();

                if ( !build( results, interimResult.modules, moduleFiles, aggregatorFiles, false, recursive, config,
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
                           List<InterimResult> interimResults, ProjectBuildingRequest config, Map<File, Boolean> profilesXmls )
    {
        boolean noErrors = true;

        for ( InterimResult interimResult : interimResults )
        {
            try
            {
                ModelBuildingResult result = modelBuilder.build( interimResult.request, interimResult.result );

                MavenProject project = interimResult.listener.getProject();
                initProject( project, result, profilesXmls );

                List<MavenProject> modules = new ArrayList<MavenProject>();
                noErrors = build( results, modules, interimResult.modules, config, profilesXmls ) && noErrors;

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

    private void initProject( MavenProject project, ModelBuildingResult result, Map<File, Boolean> profilesXmls )
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

        String modelId = findProfilesXml( result, profilesXmls );
        if ( modelId != null )
        {
            ModelProblem problem =
                new DefaultModelProblem( "Detected profiles.xml alongside " + modelId
                    + ", this file is no longer supported and was ignored" + ", please use the settings.xml instead",
                                         ModelProblem.Severity.WARNING, model, -1, -1, null );
            result.getProblems().add( problem );
        }
    }

    private String findProfilesXml( ModelBuildingResult result, Map<File, Boolean> profilesXmls )
    {
        for ( String modelId : result.getModelIds() )
        {
            Model model = result.getRawModel( modelId );

            File basedir = model.getProjectDirectory();
            if ( basedir == null )
            {
                break;
            }

            Boolean profilesXml = profilesXmls.get( basedir );
            if ( profilesXml == null )
            {
                profilesXml = Boolean.valueOf( new File( basedir, "profiles.xml" ).exists() );
                profilesXmls.put( basedir, profilesXml );
            }
            if ( profilesXml.booleanValue() )
            {
                return modelId;
            }
        }

        return null;
    }

}
