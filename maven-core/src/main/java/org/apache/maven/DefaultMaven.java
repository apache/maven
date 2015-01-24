package org.apache.maven;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemUtils;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.LocalRepositoryNotAccessibleException;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;

/**
 * @author Jason van Zyl
 */
@Component( role = Maven.class )
public class DefaultMaven
    implements Maven
{

    @Requirement
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;

    @Requirement
    private LifecycleStarter lifecycleStarter;

    @Requirement
    protected PlexusContainer container;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private SessionScope sessionScope;

    @Requirement
    private DefaultRepositorySystemSessionFactory repositorySessionFactory;

    @Override
    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        MavenExecutionResult result;

        try
        {
            result = doExecute( request );
        }
        catch ( OutOfMemoryError e )
        {
            result = addExceptionToResult( new DefaultMavenExecutionResult(), e );
        }
        catch ( RuntimeException e )
        {
            result =
                addExceptionToResult( new DefaultMavenExecutionResult(), new InternalErrorException( "Internal error: "
                    + e, e ) );
        }
        finally
        {
            legacySupport.setSession( null );
        }

        return result;
    }

    //
    // 1) Setup initial properties.
    //
    // 2) Validate local repository directory is accessible.
    //
    // 3) Create RepositorySystemSession.
    //
    // 4) Create MavenSession.
    //
    // 5) Execute AbstractLifecycleParticipant.afterSessionStart(session)
    //
    // 6) Get reactor projects looking for general POM errors
    //
    // 7) Create ProjectDependencyGraph using trimming which takes into account --projects and reactor mode.
    //    This ensures that the projects passed into the ReactorReader are only those specified.
    //
    // 8) Create ReactorReader with the getProjectMap( projects ). NOTE that getProjectMap(projects) is the code that
    //    checks for duplicate projects definitions in the build. Ideally this type of duplicate checking should be
    //    part of getting the reactor projects in 6). The duplicate checking is conflated with getProjectMap(projects).
    //
    // 9) Execute AbstractLifecycleParticipant.afterProjectsRead(session)
    //
    // 10) Create ProjectDependencyGraph without trimming (as trimming was done in 7). A new topological sort is
    //     required after the execution of 9) as the AbstractLifecycleParticipants are free to mutate the MavenProject
    //     instances, which may change dependencies which can, in turn, affect the build order.
    //
    // 11) Execute LifecycleStarter.start()
    //
    @SuppressWarnings( "checkstyle:methodlength" )
    private MavenExecutionResult doExecute( MavenExecutionRequest request )
    {        
        request.setStartTime( new Date() );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        try
        {
            validateLocalRepository( request );
        }
        catch ( LocalRepositoryNotAccessibleException e )
        {
            return addExceptionToResult( result, e );
        }

        //
        // We enter the session scope right after the MavenSession creation and before any of the AbstractLifecycleParticipant lookups
        // so that @SessionScoped components can be @Injected into AbstractLifecycleParticipants.
        //
        sessionScope.enter();
        try
        {
            DefaultRepositorySystemSession repoSession =
                (DefaultRepositorySystemSession) newRepositorySession( request );
            MavenSession session = new MavenSession( container, repoSession, request, result );

            sessionScope.seed( MavenSession.class, session );

            legacySupport.setSession( session );

            return doExecute( request, session, result, repoSession );
        }
        finally
        {
            sessionScope.exit();
        }
    }

    private MavenExecutionResult doExecute( MavenExecutionRequest request, MavenSession session,
                                            MavenExecutionResult result, DefaultRepositorySystemSession repoSession )
    {
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( Collections.<MavenProject>emptyList() ) )
            {
                listener.afterSessionStart( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }

        eventCatapult.fire( ExecutionEvent.Type.ProjectDiscoveryStarted, session, null );

        List<MavenProject> projects;
        try
        {
            projects = getProjectsForMavenReactor( session );
            //
            // Capture the full set of projects before any potential constraining is performed by --projects
            //
            session.setAllProjects( projects );
        }
        catch ( ProjectBuildingException e )
        {
            return addExceptionToResult( result, e );
        }

        validateProjects( projects );

        //
        // This creates the graph and trims the projects down based on the user request using something like:
        //
        // -pl project0,project2 eclipse:eclipse
        //
        ProjectDependencyGraph projectDependencyGraph = createProjectDependencyGraph( projects, request, result, true );

        if ( result.hasExceptions() )
        {
            return result;
        }

        session.setProjects( projectDependencyGraph.getSortedProjects() );

        try
        {
            session.setProjectMap( getProjectMap( session.getProjects() ) );
        }
        catch ( DuplicateProjectException e )
        {
            return addExceptionToResult( result, e );
        }

        WorkspaceReader reactorWorkspace;
        try
        {
            reactorWorkspace = container.lookup( WorkspaceReader.class, ReactorReader.HINT );
        }
        catch ( ComponentLookupException e )
        {
            return addExceptionToResult( result, e );
        }

        //
        // Desired order of precedence for local artifact repositories
        //
        // Reactor
        // Workspace
        // User Local Repository
        //
        repoSession.setWorkspaceReader( ChainedWorkspaceReader.newInstance( reactorWorkspace,
                                                                            repoSession.getWorkspaceReader() ) );

        repoSession.setReadOnly();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( projects ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterProjectsRead( session );
            }
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

        //
        // The projects need to be topologically after the participants have run their afterProjectsRead(session)
        // because the participant is free to change the dependencies of a project which can potentially change the
        // topological order of the projects, and therefore can potentially change the build order.
        //
        // Note that participants may affect the topological order of the projects but it is
        // not expected that a participant will add or remove projects from the session.
        //
        projectDependencyGraph = createProjectDependencyGraph( session.getProjects(), request, result, false );

        try
        {
            if ( result.hasExceptions() )
            {
                return result;
            }

            session.setProjects( projectDependencyGraph.getSortedProjects() );

            session.setProjectDependencyGraph( projectDependencyGraph );

            result.setTopologicallySortedProjects( session.getProjects() );

            result.setProject( session.getTopLevelProject() );

            lifecycleStarter.execute( session );

            validateActivatedProfiles( session.getProjects(), request.getActiveProfiles() );

            if ( session.getResult().hasExceptions() )
            {
                return addExceptionToResult( result, session.getResult().getExceptions().get( 0 ) );
            }
        }
        finally
        {
            try
            {
                afterSessionEnd( projects, session );
            }
            catch ( MavenExecutionException e )
            {
                return addExceptionToResult( result, e );
            }
        }

        return result;
    }

    private void afterSessionEnd( Collection<MavenProject> projects, MavenSession session )
        throws MavenExecutionException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( projects ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterSessionEnd( session );
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }
    }

    public RepositorySystemSession newRepositorySession( MavenExecutionRequest request )
    {
        return repositorySessionFactory.newRepositorySession( request );
    }

    private void validateLocalRepository( MavenExecutionRequest request )
        throws LocalRepositoryNotAccessibleException
    {
        File localRepoDir = request.getLocalRepositoryPath();

        logger.debug( "Using local repository at " + localRepoDir );

        localRepoDir.mkdirs();

        if ( !localRepoDir.isDirectory() )
        {
            throw new LocalRepositoryNotAccessibleException( "Could not create local repository at " + localRepoDir );
        }
    }

    private Collection<AbstractMavenLifecycleParticipant> getLifecycleParticipants( Collection<MavenProject> projects )
    {
        Collection<AbstractMavenLifecycleParticipant> lifecycleListeners =
            new LinkedHashSet<AbstractMavenLifecycleParticipant>();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            try
            {
                lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
            }
            catch ( ComponentLookupException e )
            {
                // this is just silly, lookupList should return an empty list!
                logger.warn( "Failed to lookup lifecycle participants: " + e.getMessage() );
            }

            Collection<ClassLoader> scannedRealms = new HashSet<ClassLoader>();

            for ( MavenProject project : projects )
            {
                ClassLoader projectRealm = project.getClassRealm();

                if ( projectRealm != null && scannedRealms.add( projectRealm ) )
                {
                    Thread.currentThread().setContextClassLoader( projectRealm );

                    try
                    {
                        lifecycleListeners.addAll( container.lookupList( AbstractMavenLifecycleParticipant.class ) );
                    }
                    catch ( ComponentLookupException e )
                    {
                        // this is just silly, lookupList should return an empty list!
                        logger.warn( "Failed to lookup lifecycle participants: " + e.getMessage() );
                    }
                }
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }

        return lifecycleListeners;
    }

    private MavenExecutionResult addExceptionToResult( MavenExecutionResult result, Throwable e )
    {
        if ( !result.getExceptions().contains( e ) )
        {
            result.addException( e );
        }

        return result;
    }

    private List<MavenProject> getProjectsForMavenReactor( MavenSession session )
        throws ProjectBuildingException
    {
        MavenExecutionRequest request = session.getRequest();

        request.getProjectBuildingRequest().setRepositorySession( session.getRepositorySession() );

        List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        //
        if ( request.getPom() == null )
        {
            ModelSource modelSource = new UrlModelSource( DefaultMaven.class.getResource( "project/standalone.xml" ) );
            MavenProject project =
                projectBuilder.build( modelSource, request.getProjectBuildingRequest() ).getProject();
            project.setExecutionRoot( true );
            projects.add( project );
            request.setProjectPresent( false );
            return projects;
        }

        List<File> files = Arrays.asList( request.getPom().getAbsoluteFile() );
        collectProjects( projects, files, request );
        return projects;
    }

    private void collectProjects( List<MavenProject> projects, List<File> files, MavenExecutionRequest request )
        throws ProjectBuildingException
    {
        ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        List<ProjectBuildingResult> results =
            projectBuilder.build( files, request.isRecursive(), projectBuildingRequest );

        boolean problems = false;

        for ( ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );

            if ( !result.getProblems().isEmpty() && logger.isWarnEnabled() )
            {
                logger.warn( "" );
                logger.warn( "Some problems were encountered while building the effective model for "
                    + result.getProject().getId() );

                for ( ModelProblem problem : result.getProblems() )
                {
                    String loc = ModelProblemUtils.formatLocation( problem, result.getProjectId() );
                    logger.warn( problem.getMessage() + ( StringUtils.isNotEmpty( loc ) ? " @ " + loc : "" ) );
                }

                problems = true;
            }
        }

        if ( problems )
        {
            logger.warn( "" );
            logger.warn( "It is highly recommended to fix these problems"
                + " because they threaten the stability of your build." );
            logger.warn( "" );
            logger.warn( "For this reason, future Maven versions might no"
                + " longer support building such malformed projects." );
            logger.warn( "" );
        }
    }

    private Map<String, MavenProject> getProjectMap( Collection<MavenProject> projects )
        throws DuplicateProjectException
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
            throw new DuplicateProjectException( "Two or more projects in the reactor"
                + " have the same identifier, please make sure that <groupId>:<artifactId>:<version>"
                + " is unique for each project: " + collisions, collisions );
        }

        return index;
    }

    private void validateProjects( List<MavenProject> projects )
    {
        Map<String, MavenProject> projectsMap = new HashMap<String, MavenProject>();

        for ( MavenProject p : projects )
        {
            String projectKey = ArtifactUtils.key( p.getGroupId(), p.getArtifactId(), p.getVersion() );

            projectsMap.put( projectKey, p );
        }

        for ( MavenProject project : projects )
        {
            // MNG-1911 / MNG-5572: Building plugins with extensions cannot be part of reactor
            for ( Plugin plugin : project.getBuildPlugins() )
            {
                if ( plugin.isExtensions() )
                {
                    String pluginKey =
                        ArtifactUtils.key( plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion() );

                    if ( projectsMap.containsKey( pluginKey ) )
                    {
                        logger.warn( project.getName() + " uses " + plugin.getKey()
                            + " as extensions, which is not possible within the same reactor build. "
                            + "This plugin was pulled from the local repository!" );
                    }
                }
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
            logger.warn( "The requested profile \"" + notActivatedProfileId
                + "\" could not be activated because it does not exist." );
        }
    }

    @Deprecated // 5 January 2014
    protected Logger getLogger()
    {
        return logger;
    }

    private ProjectDependencyGraph createProjectDependencyGraph( Collection<MavenProject> projects,
                                                                 MavenExecutionRequest request,
                                                                 MavenExecutionResult result, boolean trimming )
    {
        ProjectDependencyGraph projectDependencyGraph = null;

        try
        {
            projectDependencyGraph = new DefaultProjectDependencyGraph( projects );

            if ( trimming )
            {
                List<MavenProject> activeProjects = projectDependencyGraph.getSortedProjects();

                activeProjects = trimSelectedProjects( activeProjects, projectDependencyGraph, request );
                activeProjects = trimExcludedProjects( activeProjects,  request );
                activeProjects = trimResumedProjects( activeProjects, request );

                if ( activeProjects.size() != projectDependencyGraph.getSortedProjects().size() )
                {
                    projectDependencyGraph =
                        new FilteredProjectDependencyGraph( projectDependencyGraph, activeProjects );
                }
            }
        }
        catch ( CycleDetectedException e )
        {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            ProjectCycleException error = new ProjectCycleException( message, e );

            addExceptionToResult( result, error );
        }
        catch ( org.apache.maven.project.DuplicateProjectException e )
        {
            addExceptionToResult( result, e );
        }
        catch ( MavenExecutionException e )
        {
            addExceptionToResult( result, e );
        }

        return projectDependencyGraph;
    }

    private List<MavenProject> trimSelectedProjects( List<MavenProject> projects, ProjectDependencyGraph graph,
                                                     MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( !request.getSelectedProjects().isEmpty() )
        {
            File reactorDirectory = null;
            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            Collection<MavenProject> selectedProjects = new LinkedHashSet<MavenProject>( projects.size() );

            for ( String selector : request.getSelectedProjects() )
            {
                MavenProject selectedProject = null;

                for ( MavenProject project : projects )
                {
                    if ( isMatchingProject( project, selector, reactorDirectory ) )
                    {
                        selectedProject = project;
                        break;
                    }
                }

                if ( selectedProject != null )
                {
                    selectedProjects.add( selectedProject );
                }
                else
                {
                    throw new MavenExecutionException( "Could not find the selected project in the reactor: "
                        + selector, request.getPom() );
                }
            }

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
                for ( MavenProject selectedProject : new ArrayList<MavenProject>( selectedProjects ) )
                {
                    if ( makeUpstream )
                    {
                        selectedProjects.addAll( graph.getUpstreamProjects( selectedProject, true ) );
                    }
                    if ( makeDownstream )
                    {
                        selectedProjects.addAll( graph.getDownstreamProjects( selectedProject, true ) );
                    }
                }
            }

            result = new ArrayList<MavenProject>( selectedProjects.size() );

            for ( MavenProject project : projects )
            {
                if ( selectedProjects.contains( project ) )
                {
                    result.add( project );
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimExcludedProjects( List<MavenProject> projects, MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( !request.getExcludedProjects().isEmpty() )
        {
            File reactorDirectory = null;

            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            Collection<MavenProject> excludedProjects = new LinkedHashSet<MavenProject>( projects.size() );

            for ( String selector : request.getExcludedProjects() )
            {
                MavenProject excludedProject = null;

                for ( MavenProject project : projects )
                {
                    if ( isMatchingProject( project, selector, reactorDirectory ) )
                    {
                        excludedProject = project;
                        break;
                    }
                }

                if ( excludedProject != null )
                {
                    excludedProjects.add( excludedProject );
                }
                else
                {
                    throw new MavenExecutionException( "Could not find the selected project in the reactor: "
                        + selector, request.getPom() );
                }
            }

            result = new ArrayList<MavenProject>( projects.size() );
            for ( MavenProject project : projects )
            {
                if ( !excludedProjects.contains( project ) )
                {
                    result.add( project );
                }
            }
        }

        return result;
    }

    private List<MavenProject> trimResumedProjects( List<MavenProject> projects, MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> result = projects;

        if ( StringUtils.isNotEmpty( request.getResumeFrom() ) )
        {
            File reactorDirectory = null;
            if ( request.getBaseDirectory() != null )
            {
                reactorDirectory = new File( request.getBaseDirectory() );
            }

            String selector = request.getResumeFrom();

            result = new ArrayList<MavenProject>( projects.size() );

            boolean resumed = false;

            for ( MavenProject project : projects )
            {
                if ( !resumed && isMatchingProject( project, selector, reactorDirectory ) )
                {
                    resumed = true;
                }

                if ( resumed )
                {
                    result.add( project );
                }
            }

            if ( !resumed )
            {
                throw new MavenExecutionException( "Could not find project to resume reactor build from: " + selector
                    + " vs " + projects, request.getPom() );
            }
        }

        return result;
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
        else if ( reactorDirectory != null )
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
