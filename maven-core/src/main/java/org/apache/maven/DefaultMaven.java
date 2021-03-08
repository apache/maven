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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.BuildResumptionAnalyzer;
import org.apache.maven.execution.BuildResumptionDataRepository;
import org.apache.maven.execution.BuildResumptionPersistenceException;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProfileActivation;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.graph.GraphBuilder;
import org.apache.maven.internal.aether.DefaultRepositorySystemSessionFactory;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleStarter;
import org.apache.maven.model.Model;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.superpom.SuperPomProvider;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.repository.LocalRepositoryNotAccessibleException;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.util.repository.ChainedWorkspaceReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

/**
 * @author Jason van Zyl
 */
@Named
@Singleton
public class DefaultMaven
    implements Maven
{

    @Inject
    private Logger logger;

    @Inject
    protected ProjectBuilder projectBuilder;

    @Inject
    private LifecycleStarter lifecycleStarter;

    @Inject
    protected PlexusContainer container;

    @Inject
    private ExecutionEventCatapult eventCatapult;

    @Inject
    private LegacySupport legacySupport;

    @Inject
    private SessionScope sessionScope;

    @Inject
    private DefaultRepositorySystemSessionFactory repositorySessionFactory;

    @Inject
    @Named( GraphBuilder.HINT )
    private GraphBuilder graphBuilder;

    @Inject
    private BuildResumptionAnalyzer buildResumptionAnalyzer;

    @Inject
    private BuildResumptionDataRepository buildResumptionDataRepository;

    @Inject
    private SuperPomProvider superPomProvider;

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
            // TODO Hack to make the cycle detection the same for the new graph builder
            if ( e.getCause() instanceof ProjectCycleException )
            {
                result = addExceptionToResult( new DefaultMavenExecutionResult(), e.getCause() );
            }
            else
            {
                result = addExceptionToResult( new DefaultMavenExecutionResult(),
                                               new InternalErrorException( "Internal error: " + e, e ) );
            }
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
    // This ensures that the projects passed into the ReactorReader are only those specified.
    //
    // 8) Create ReactorReader with the getProjectMap( projects ). NOTE that getProjectMap(projects) is the code that
    // checks for duplicate projects definitions in the build. Ideally this type of duplicate checking should be
    // part of getting the reactor projects in 6). The duplicate checking is conflated with getProjectMap(projects).
    //
    // 9) Execute AbstractLifecycleParticipant.afterProjectsRead(session)
    //
    // 10) Create ProjectDependencyGraph without trimming (as trimming was done in 7). A new topological sort is
    // required after the execution of 9) as the AbstractLifecycleParticipants are free to mutate the MavenProject
    // instances, which may change dependencies which can, in turn, affect the build order.
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
        // We enter the session scope right after the MavenSession creation and before any of the
        // AbstractLifecycleParticipant lookups
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
            afterSessionStart( session );
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }

        eventCatapult.fire( ExecutionEvent.Type.ProjectDiscoveryStarted, session, null );

        Result<? extends ProjectDependencyGraph> graphResult = buildGraph( session, result );

        if ( graphResult.hasErrors() )
        {
            return addExceptionToResult( result, graphResult.getProblems().iterator().next().getException() );
        }

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

        try
        {
            afterProjectsRead( session );
        }
        catch ( MavenExecutionException e )
        {
            return addExceptionToResult( result, e );
        }

        //
        // The projects need to be topologically after the participants have run their afterProjectsRead(session)
        // because the participant is free to change the dependencies of a project which can potentially change the
        // topological order of the projects, and therefore can potentially change the build order.
        //
        // Note that participants may affect the topological order of the projects but it is
        // not expected that a participant will add or remove projects from the session.
        //

        graphResult = buildGraph( session, result );

        if ( graphResult.hasErrors() )
        {
            return addExceptionToResult( result, graphResult.getProblems().iterator().next().getException() );
        }

        try
        {
            if ( result.hasExceptions() )
            {
                return result;
            }

            result.setTopologicallySortedProjects( session.getProjects() );

            result.setProject( session.getTopLevelProject() );

            validatePrerequisitesForNonMavenPluginProjects( session.getProjects() );

            validateRequiredProfiles( session, request.getProfileActivation() );
            if ( session.getResult().hasExceptions() )
            {
                return result;
            }

            validateOptionalProfiles( session, request.getProfileActivation() );

            lifecycleStarter.execute( session );

            validateOptionalProfiles( session, request.getProfileActivation() );

            if ( session.getResult().hasExceptions() )
            {
                addExceptionToResult( result, session.getResult().getExceptions().get( 0 ) );
                persistResumptionData( result, session );
                return result;
            }
            else
            {
                session.getAllProjects().stream()
                        .filter( MavenProject::isExecutionRoot )
                        .findFirst()
                        .ifPresent( buildResumptionDataRepository::removeResumptionData );
            }
        }
        finally
        {
            try
            {
                afterSessionEnd( session.getProjects(), session );
            }
            catch ( MavenExecutionException e )
            {
                return addExceptionToResult( result, e );
            }
        }

        return result;
    }

    private void afterSessionStart( MavenSession session )
        throws MavenExecutionException
    {
        // CHECKSTYLE_OFF: LineLength
        for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( Collections.<MavenProject>emptyList() ) )
        // CHECKSTYLE_ON: LineLength
        {
            listener.afterSessionStart( session );
        }
    }

    private void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            for ( AbstractMavenLifecycleParticipant listener : getLifecycleParticipants( session.getProjects() ) )
            {
                Thread.currentThread().setContextClassLoader( listener.getClass().getClassLoader() );

                listener.afterProjectsRead( session );
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( originalClassLoader );
        }
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

    private void persistResumptionData( MavenExecutionResult result, MavenSession session )
    {
        boolean hasLifecycleExecutionExceptions = result.getExceptions().stream()
                .anyMatch( LifecycleExecutionException.class::isInstance );

        if ( hasLifecycleExecutionExceptions )
        {
            MavenProject rootProject = session.getAllProjects().stream()
                    .filter( MavenProject::isExecutionRoot )
                    .findFirst()
                    .orElseThrow( () -> new IllegalStateException( "No project in the session is execution root" ) );

            buildResumptionAnalyzer.determineBuildResumptionData( result ).ifPresent( resumption ->
            {
                try
                {
                    buildResumptionDataRepository.persistResumptionData( rootProject, resumption );
                    result.setCanResume( true );
                }
                catch ( BuildResumptionPersistenceException e )
                {
                    logger.warn( "Could not persist build resumption data", e );
                }
            } );
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
        Collection<AbstractMavenLifecycleParticipant> lifecycleListeners = new LinkedHashSet<>();

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

            Collection<ClassLoader> scannedRealms = new HashSet<>();

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

    private void validatePrerequisitesForNonMavenPluginProjects( List<MavenProject> projects )
    {
        for ( MavenProject mavenProject : projects )
        {
            if ( !"maven-plugin".equals( mavenProject.getPackaging() ) )
            {
                Prerequisites prerequisites = mavenProject.getPrerequisites();
                if ( prerequisites != null && prerequisites.getMaven() != null )
                {
                    logger.warn( "The project " + mavenProject.getId() + " uses prerequisites"
                        + " which is only intended for maven-plugin projects "
                        + "but not for non maven-plugin projects. "
                        + "For such purposes you should use the maven-enforcer-plugin. "
                        + "See https://maven.apache.org/enforcer/enforcer-rules/requireMavenVersion.html" );
                }
            }
        }
    }

    /**
     * Get all profiles that are detected in the projects, any parent of the projects, or the settings.
     * @param session The Maven session
     * @return A {@link Set} of profile identifiers, never {@code null}.
     */
    private Set<String> getAllProfiles( MavenSession session )
    {
        final Model superPomModel = superPomProvider.getSuperModel( "4.0.0" );
        final Set<MavenProject> projectsIncludingParents = new HashSet<>();
        for ( MavenProject project : session.getProjects() )
        {
            boolean isAdded = projectsIncludingParents.add( project );
            MavenProject parent = project.getParent();
            while ( isAdded && parent != null )
            {
                isAdded = projectsIncludingParents.add( parent );
                parent = parent.getParent();
            }
        }

        final Stream<String> projectProfiles = projectsIncludingParents.stream()
                .map( MavenProject::getModel )
                .map( Model::getProfiles )
                .flatMap( Collection::stream )
                .map( Profile::getId );
        final Stream<String> settingsProfiles = session.getSettings().getProfiles().stream()
                .map( org.apache.maven.settings.Profile::getId );
        final Stream<String> superPomProfiles = superPomModel.getProfiles().stream()
                .map( Profile::getId );

        return Stream.of( projectProfiles, settingsProfiles, superPomProfiles )
                .flatMap( Function.identity() )
                .collect( toSet() );
    }

    /**
     * Check whether the required profiles were found in any of the projects we're building or the settings.
     * @param session the Maven session.
     * @param profileActivation the requested optional and required profiles.
     */
    private void validateRequiredProfiles( MavenSession session, ProfileActivation profileActivation )
    {
        final Set<String> allAvailableProfiles = getAllProfiles( session );

        final Set<String> requiredProfiles = new HashSet<>( );
        requiredProfiles.addAll( profileActivation.getRequiredActiveProfileIds() );
        requiredProfiles.addAll( profileActivation.getRequiredInactiveProfileIds() );

        // Check whether the required profiles were found in any of the projects we're building.
        final Set<String> notFoundRequiredProfiles = requiredProfiles.stream()
                .filter( rap -> !allAvailableProfiles.contains( rap ) )
                .collect( toSet() );

        if ( !notFoundRequiredProfiles.isEmpty() )
        {
            final String message = String.format(
                    "The requested profiles [%s] could not be activated or deactivated because they do not exist.",
                    String.join( ", ", notFoundRequiredProfiles )
            );
            addExceptionToResult( session.getResult(), new MissingProfilesException( message ) );
        }
    }

    /**
     * Check whether any of the requested optional profiles were not activated or deactivated.
     * @param session the Maven session.
     * @param profileActivation the requested optional and required profiles.
     */
    private void validateOptionalProfiles( MavenSession session, ProfileActivation profileActivation )
    {
        final Set<String> allAvailableProfiles = getAllProfiles( session );

        final Set<String> optionalProfiles = new HashSet<>( );
        optionalProfiles.addAll( profileActivation.getOptionalActiveProfileIds() );
        optionalProfiles.addAll( profileActivation.getOptionalInactiveProfileIds() );

        final Set<String> notFoundOptionalProfiles = optionalProfiles.stream()
                .filter( rap -> !allAvailableProfiles.contains( rap ) )
                .collect( toSet() );

        if ( !notFoundOptionalProfiles.isEmpty() )
        {
            final String message = String.format(
                    "The requested optional profiles [%s] could not be activated or deactivated because they "
                            + "do not exist.", String.join( ", ", notFoundOptionalProfiles )
            );
            logger.info( message );
        }
    }

    private Map<String, MavenProject> getProjectMap( Collection<MavenProject> projects )
        throws DuplicateProjectException
    {
        Map<String, MavenProject> index = new LinkedHashMap<>();
        Map<String, List<File>> collisions = new LinkedHashMap<>();

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
                    pomFiles = new ArrayList<>( Arrays.asList( collision.getFile(), project.getFile() ) );
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

    private Result<? extends ProjectDependencyGraph> buildGraph( MavenSession session, MavenExecutionResult result )
    {
        Result<? extends ProjectDependencyGraph> graphResult = graphBuilder.build( session );
        for ( ModelProblem problem : graphResult.getProblems() )
        {
            if ( problem.getSeverity() == ModelProblem.Severity.WARNING )
            {
                logger.warn( problem.toString() );
            }
            else
            {
                logger.error( problem.toString() );
            }
        }

        if ( !graphResult.hasErrors() )
        {
            ProjectDependencyGraph projectDependencyGraph = graphResult.get();
            session.setProjects( projectDependencyGraph.getSortedProjects() );
            session.setAllProjects( projectDependencyGraph.getAllProjects() );
            session.setProjectDependencyGraph( projectDependencyGraph );
        }

        return graphResult;
    }

    @Deprecated
    // 5 January 2014
    protected Logger getLogger()
    {
        return logger;
    }
}
