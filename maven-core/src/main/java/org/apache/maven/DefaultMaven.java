package org.apache.maven;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.execution.ProjectSorter;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author Jason van Zyl
 */
@Component(role = Maven.class)
public class DefaultMaven
    implements Maven
{

    @Requirement
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;

    @Requirement
    protected LifecycleExecutor lifecycleExecutor;

    @Requirement
    protected PlexusContainer container;
    
    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        //TODO: Need a general way to inject standard properties
        if ( request.getStartTime() != null )
        {
            request.getSystemProperties().put( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }        
        
        request.setStartTime( new Date() );
        
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        
        DelegatingLocalArtifactRepository delegatingLocalArtifactRepository = new DelegatingLocalArtifactRepository( request.getLocalRepository() );
        
        request.setLocalRepository( delegatingLocalArtifactRepository );        

        MavenSession session = new MavenSession( container, request, result);
        
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants() )
            {
                listener.afterSessionStart( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }

        //TODO: optimize for the single project or no project
        
        List<MavenProject> projects;
        try
        {
            projects = getProjectsForMavenReactor( request );                                                
        }
        catch ( ProjectBuildingException e )
        {
            return processResult( result, e );
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }

        session.setProjects( projects );

        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants() )
            {
                listener.afterProjectsRead( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }

        try
        {
            ProjectSorter projectSorter = new ProjectSorter( session.getProjects() );

            ProjectDependencyGraph projectDependencyGraph = createDependencyGraph( projectSorter, request );

            session.setProjects( projectDependencyGraph.getSortedProjects() );

            session.setProjectDependencyGraph( projectDependencyGraph );
        }
        catch ( CycleDetectedException e )
        {            
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            ProjectCycleException error = new ProjectCycleException( message, e );

            return processResult( result, error );
        }
        catch ( DuplicateProjectException e )
        {
            return processResult( result, e );
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }

        // Desired order of precedence for local artifact repositories
        //
        // Reactor
        // Workspace
        // User Local Repository
        try
        {
            delegatingLocalArtifactRepository.setBuildReactor( new ReactorArtifactRepository( getProjectMap( session.getProjects() ), session ) );
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }
        
        if ( result.hasExceptions() )
        {
            return result;
        }        

        lifecycleExecutor.execute( session );

        validateActivatedProfiles( session.getProjects(), request.getActiveProfiles() );

        if ( session.getResult().hasExceptions() )
        {        
            return processResult( result, session.getResult().getExceptions().get( 0 ) );
        }

        result.setTopologicallySortedProjects( session.getProjects() );
        
        result.setProject( session.getTopLevelProject() );

        return result;
    }

    private List<AbstractMavenLifecycleParticipant> getLifecycleParticipants()
    {
        // TODO injection of component lists does not work
        List<AbstractMavenLifecycleParticipant> lifecycleListeners;
        try
        {
            lifecycleListeners = container.lookupList( AbstractMavenLifecycleParticipant.class );
        }
        catch ( ComponentLookupException e1 )
        {
            // this is just silly, lookupList should return an empty list!
            lifecycleListeners = new ArrayList<AbstractMavenLifecycleParticipant>();
        }
        return lifecycleListeners;
    }

    private MavenExecutionResult processResult( MavenExecutionResult result, Exception e )
    {
        ExceptionHandler handler = new DefaultExceptionHandler();
        
        ExceptionSummary es = handler.handleException( e );                        

        if ( !result.getExceptions().contains( e ) )
        {
            result.addException( e );
        }
        
        result.setExceptionSummary( es );    
        
        return result;
    }
    
    private List<MavenProject> getProjectsForMavenReactor( MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        List<MavenProject> projects =  new ArrayList<MavenProject>();

        // We have no POM file.
        //
        if ( request.getPom() == null || !request.getPom().exists() )
        {
            MavenProject project = projectBuilder.buildStandaloneSuperProject( request.getProjectBuildingRequest() ).getProject(); 
            projects.add( project );
            request.setProjectPresent( false );
            return projects;
        }
        
        List<File> files = Arrays.asList( request.getPom().getAbsoluteFile() );        
        collectProjects( projects, files, request );
        return projects;
    }

    private Map<String, MavenProject> getProjectMap( List<MavenProject> projects )
        throws org.apache.maven.DuplicateProjectException
    {
        Map<String, MavenProject> index = new LinkedHashMap<String, MavenProject>();
        Map<String, List<File>> collisions = new LinkedHashMap<String, List<File>>();

        for ( MavenProject project : projects )
        {
            String projectId = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            MavenProject collision = index.get( projectId );

            if ( collision == null )
            {
                index.put( projectId, project );
            }
            else
            {
                List<File> pomFiles = collisions.get( projectId );

                if ( pomFiles == null )
                {
                    pomFiles = new ArrayList<File>( Arrays.asList( collision.getFile(), project.getFile() ) );
                    collisions.put( projectId, pomFiles );
                }
                else
                {
                    pomFiles.add( project.getFile() );
                }
            }
        }

        if ( !collisions.isEmpty() )
        {
            throw new org.apache.maven.DuplicateProjectException( "Two or more projects in the reactor"
                + " have the same identifier, please make sure that <groupId>:<artifactId>:<version>"
                + " is unique for each project: " + collisions, collisions );
        }

        return index;
    }

    private void collectProjects( List<MavenProject> projects, List<File> files, MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        /*
         * NOTE: We delay plugin configuration processing until a project is actually build to allow plugins to be
         * resolved from the reactor.
         */
        projectBuildingRequest.setProcessPluginConfiguration( false );

        List<ProjectBuildingResult> results = projectBuilder.build( files, request.isRecursive(), projectBuildingRequest );

        for ( ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );

            if ( !result.getProblems().isEmpty() && logger.isWarnEnabled() )
            {
                logger.warn( "" );
                logger.warn( "Some problems were encountered while building the effective model for "
                    + result.getProject().getId() );
                logger.warn( "" );

                for ( ModelProblem problem : result.getProblems() )
                {
                    logger.warn( problem.getMessage() + " @ " + problem.getSource() );
                }

                logger.warn( "" );
                logger.warn( "It is highly recommended to fix these problems"
                    + " because they threaten the stability of your build." );
                logger.warn( "" );
                logger.warn( "For this reason, future Maven versions might no"
                    + " longer support building such malformed projects." );
                logger.warn( "" );
            }
        }
    }

    private void validateActivatedProfiles( List<MavenProject> projects, List<String> activeProfileIds )
    {
        Collection<String> notActivatedProfileIds = new LinkedHashSet<String>( activeProfileIds );

        for ( MavenProject project : projects )
        {
            for ( List<String> profileIds : project.getInjectedProfileIds().values() )
            {
                notActivatedProfileIds.removeAll( profileIds );
            }
        }

        for ( String notActivatedProfileId : notActivatedProfileIds )
        {
            logger.warn( "Profile with id \"" + notActivatedProfileId + "\" has not been activated." );
        }
    }

    protected Logger getLogger()
    {
        return logger;
    }

    private ProjectDependencyGraph createDependencyGraph( ProjectSorter sorter, MavenExecutionRequest request )
        throws MavenExecutionException
    {
        ProjectDependencyGraph graph = new DefaultProjectDependencyGraph( sorter );

        Collection<MavenProject> activeProjects = sorter.getSortedProjects();

        File reactorDirectory = request.getPom().getAbsoluteFile().getParentFile();

        if ( !request.getSelectedProjects().isEmpty() )
        {
            List<MavenProject> selectedProjects = new ArrayList<MavenProject>( request.getSelectedProjects().size() );

            for ( String selectedProject : request.getSelectedProjects() )
            {
                MavenProject project = null;

                for ( MavenProject activeProject : activeProjects )
                {
                    if ( isMatchingProject( activeProject, selectedProject, reactorDirectory ) )
                    {
                        project = activeProject;
                        break;
                    }
                }

                if ( project != null )
                {
                    selectedProjects.add( project );
                }
                else
                {
                    throw new MavenExecutionException( "Could not find the selected project in the reactor: "
                        + selectedProject, request.getPom() );
                }
            }

            activeProjects = selectedProjects;

            boolean makeUpstream = false;
            boolean makeDownstream = false;

            if ( MavenExecutionRequest.REACTOR_MAKE_UPSTREAM.equals( request.getMakeBehavior() ) )
            {
                makeUpstream = true;
            }
            else if ( MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM.equals( request.getMakeBehavior() ) )
            {
                makeDownstream = true;
            }
            else if ( MavenExecutionRequest.REACTOR_MAKE_BOTH.equals( request.getMakeBehavior() ) )
            {
                makeUpstream = true;
                makeDownstream = true;
            }
            else if ( StringUtils.isNotEmpty( request.getMakeBehavior() ) )
            {
                throw new MavenExecutionException( "Invalid reactor make behavior: " + request.getMakeBehavior(),
                                                   request.getPom() );
            }

            if ( makeUpstream || makeDownstream )
            {
                activeProjects = new LinkedHashSet<MavenProject>( selectedProjects );

                for ( MavenProject selectedProject : selectedProjects )
                {
                    if ( makeUpstream )
                    {
                        activeProjects.addAll( graph.getUpstreamProjects( selectedProject, true ) );
                    }
                    if ( makeDownstream )
                    {
                        activeProjects.addAll( graph.getDownstreamProjects( selectedProject, true ) );
                    }
                }
            }
        }

        if ( StringUtils.isNotEmpty( request.getResumeFrom() ) )
        {
            String selectedProject = request.getResumeFrom();

            List<MavenProject> projects = new ArrayList<MavenProject>( activeProjects.size() );

            boolean resumed = false;

            for ( MavenProject project : activeProjects )
            {
                if ( !resumed && isMatchingProject( project, selectedProject, reactorDirectory ) )
                {
                    resumed = true;
                }

                if ( resumed )
                {
                    projects.add( project );
                }
            }

            if ( !resumed )
            {
                throw new MavenExecutionException( "Could not find project to resume reactor build from: "
                    + selectedProject + " vs " + activeProjects, request.getPom() );
            }

            activeProjects = projects;
        }

        if ( activeProjects.size() != sorter.getSortedProjects().size() )
        {
            graph = new FilteredProjectDependencyGraph( graph, activeProjects );
        }

        return graph;
    }

    private boolean isMatchingProject( MavenProject project, String selector, File reactorDirectory )
    {
        // [groupId]:artifactId
        if ( selector.indexOf( ':' ) >= 0 )
        {
            String id = ':' + project.getArtifactId();

            if ( id.equals( selector ) )
            {
                return true;
            }

            id = project.getGroupId() + id;

            if ( id.equals( selector ) )
            {
                return true;
            }
        }

        // relative path, e.g. "sub", "../sub" or "."
        else
        {
            File selectedProject = new File( new File( reactorDirectory, selector ).toURI().normalize() );

            if ( selectedProject.isFile() )
            {
                return selectedProject.equals( project.getFile() );
            }
            else if ( selectedProject.isDirectory() )
            {
                return selectedProject.equals( project.getBasedir() );
            }
        }

        return false;
    }

}
