package org.apache.maven.lifecycle.internal;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.caching.CacheController;
import org.apache.maven.caching.CacheResult;
import org.apache.maven.caching.MojoExecutionManager;
import org.apache.maven.caching.MojoParametersListener;
import org.apache.maven.caching.xml.BuildInfo;
import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.caching.xml.CacheState;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MissingProjectException;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecution.Source;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginIncompatibleException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.maven.caching.ProjectUtils.isLaterPhase;
import static org.apache.maven.caching.checksum.KeyUtils.getVersionlessProjectKey;
import static org.apache.maven.caching.xml.CacheState.DISABLED;
import static org.apache.maven.caching.xml.CacheState.INITIALIZED;


/**
 * <p>
 * Executes an individual mojo
 * </p>
 * <strong>NOTE:</strong> This class is not part of any public api and can be changed or deleted without prior notice.
 *
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 * @since 3.0
 */
@Component( role = MojoExecutor.class )
public class MojoExecutor
{

    @Requirement
    private Logger logger;

    @Requirement
    private BuildPluginManager pluginManager;

    @Requirement
    private MavenPluginManager mavenPluginManager;

    @Requirement
    private LifecycleDependencyResolver lifeCycleDependencyResolver;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    @Requirement
    private CacheController cacheController;

    @Requirement
    private CacheConfig cacheConfig;

    @Requirement( role = MojoExecutionListener.class, hint = "MojoParametersListener" )
    private MojoParametersListener mojoListener;

    public MojoExecutor()
    {
    }

    public DependencyContext newDependencyContext( MavenSession session, List<MojoExecution> mojoExecutions )
    {
        Set<String> scopesToCollect = new TreeSet<>();
        Set<String> scopesToResolve = new TreeSet<>();

        collectDependencyRequirements( scopesToResolve, scopesToCollect, mojoExecutions );

        return new DependencyContext( session.getCurrentProject(), scopesToCollect, scopesToResolve );
    }

    private void collectDependencyRequirements( Set<String> scopesToResolve, Set<String> scopesToCollect,
                                                Collection<MojoExecution> mojoExecutions )
    {
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            scopesToResolve.addAll( toScopes( mojoDescriptor.getDependencyResolutionRequired() ) );

            scopesToCollect.addAll( toScopes( mojoDescriptor.getDependencyCollectionRequired() ) );
        }
    }

    private Collection<String> toScopes( String classpath )
    {
        Collection<String> scopes = Collections.emptyList();

        if ( StringUtils.isNotEmpty( classpath ) )
        {
            if ( Artifact.SCOPE_COMPILE.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED );
            }
            else if ( Artifact.SCOPE_RUNTIME.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
                        Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME );
            }
            else if ( Artifact.SCOPE_TEST.equals( classpath ) )
            {
                scopes = Arrays.asList( Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
                        Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST );
            }
        }
        return Collections.unmodifiableCollection( scopes );
    }

    public void execute( MavenSession session, List<MojoExecution> mojoExecutions, ProjectIndex projectIndex )
            throws LifecycleExecutionException
    {
        DependencyContext dependencyContext = newDependencyContext( session, mojoExecutions );

        PhaseRecorder phaseRecorder = new PhaseRecorder( session.getCurrentProject() );

        final MavenProject project = session.getCurrentProject();
        final Source source = getSource( mojoExecutions );

        // execute clean bound goals before restoring to not interfere/slowdown clean
        CacheState cacheState = DISABLED;
        CacheResult result = CacheResult.empty();
        if ( source == Source.LIFECYCLE )
        {
            List<MojoExecution> cleanPhase = getCleanPhase( mojoExecutions );
            for ( MojoExecution mojoExecution : cleanPhase )
            {
                execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
            }
            cacheState = cacheConfig.initialize( project, session );
            if ( cacheState == INITIALIZED )
            {
                result = cacheController.findCachedBuild( session, project, projectIndex, mojoExecutions );
            }
        }

        boolean restorable = result.isSuccess() || result.isPartialSuccess();
        boolean restored = result.isSuccess(); // if partially restored need to save increment
        if ( restorable )
        {
            restored &= restoreProject( result, mojoExecutions, projectIndex, dependencyContext, phaseRecorder );
        }
        else
        {
            for ( MojoExecution mojoExecution : mojoExecutions )
            {
                if ( source == Source.CLI || isLaterPhase( mojoExecution.getLifecyclePhase(), "post-clean" ) )
                {
                    execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
                }
            }
        }

        if ( cacheState == INITIALIZED && ( !restorable || !restored ) )
        {
            final Map<String, MojoExecutionEvent> executionEvents = mojoListener.getProjectExecutions( project );
            cacheController.save( result, mojoExecutions, executionEvents );
        }

        if ( cacheConfig.isFailFast() && !result.isSuccess() )
        {
            throw new LifecycleExecutionException(
                    "Failed to restore project[" + getVersionlessProjectKey( project ) + "] from cache, failing build.",
                    project );
        }
    }

    private Source getSource( List<MojoExecution> mojoExecutions )
    {
        if ( mojoExecutions == null || mojoExecutions.isEmpty() )
        {
            return null;
        }
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( mojoExecution.getSource() == Source.CLI )
            {
                return Source.CLI;
            }
        }
        return Source.LIFECYCLE;
    }

    private boolean restoreProject( CacheResult cacheResult,
                                    List<MojoExecution> mojoExecutions,
                                    ProjectIndex projectIndex,
                                    DependencyContext dependencyContext,
                                    PhaseRecorder phaseRecorder ) throws LifecycleExecutionException
    {

        final BuildInfo buildInfo = cacheResult.getBuildInfo();
        final MavenProject project = cacheResult.getContext().getProject();
        final MavenSession session = cacheResult.getContext().getSession();
        final List<MojoExecution> cachedSegment = buildInfo.getCachedSegment( mojoExecutions );

        boolean restored = cacheController.restoreProjectArtifacts( cacheResult );
        if ( !restored )
        {
            logger.info(
                    "[CACHE][" + project.getArtifactId()
                            + "] Cannot restore project artifacts, continuing with non cached build" );
            return false;
        }

        for ( MojoExecution cacheCandidate : cachedSegment )
        {

            if ( cacheController.isForcedExecution( project, cacheCandidate ) )
            {
                logger.info(
                        "[CACHE][" + project.getArtifactId() + "] Mojo execution is forced by project property: "
                                + cacheCandidate.getMojoDescriptor().getFullGoalName() );
                execute( session, cacheCandidate, projectIndex, dependencyContext, phaseRecorder );
            }
            else
            {
                restored = verifyCacheConsistency( cacheCandidate, buildInfo, project, session, projectIndex,
                        dependencyContext, phaseRecorder );
                if ( !restored )
                {
                    break;
                }
            }
        }

        if ( !restored )
        {
            // cleanup partial state
            project.getArtifact().setFile( null );
            project.getArtifact().setResolved( false );
            project.getAttachedArtifacts().clear();
            mojoListener.remove( project );
            // build as usual
            for ( MojoExecution mojoExecution : cachedSegment )
            {
                execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
            }
        }

        for ( MojoExecution mojoExecution : buildInfo.getPostCachedSegment( mojoExecutions ) )
        {
            execute( session, mojoExecution, projectIndex, dependencyContext, phaseRecorder );
        }
        return restored;
    }

    private boolean verifyCacheConsistency( MojoExecution cacheCandidate,
                                            BuildInfo cachedBuildInfo,
                                            MavenProject project,
                                            MavenSession session,
                                            ProjectIndex projectIndex,
                                            DependencyContext dependencyContext,
                                            PhaseRecorder phaseRecorder ) throws LifecycleExecutionException
    {

        AtomicBoolean consistent = new AtomicBoolean( true );
        final MojoExecutionManager mojoChecker = new MojoExecutionManager( project, cacheController, cachedBuildInfo,
                consistent, logger, cacheConfig );

        if ( mojoChecker.needCheck( cacheCandidate, session ) )
        {
            try
            {
                // actual execution will not happen (if not forced). decision delayed to execution time
                // then all properties are resolved.
                cacheCandidate.setMojoExecutionManager( mojoChecker );
                IDependencyContext nop = new NoResolutionContext( dependencyContext );
                execute( session, cacheCandidate, projectIndex, nop, phaseRecorder );
            }
            finally
            {
                cacheCandidate.setMojoExecutionManager( null );
            }
        }
        else
        {
            logger.info(
                    "[CACHE][" + project.getArtifactId() + "] Skipping plugin execution (cached): "
                            + cacheCandidate.getMojoDescriptor().getFullGoalName() );
        }

        return consistent.get();
    }

    private List<MojoExecution> getCleanPhase( List<MojoExecution> mojoExecutions )
    {
        List<MojoExecution> list = new ArrayList<>();
        for ( MojoExecution mojoExecution : mojoExecutions )
        {
            if ( isLaterPhase( mojoExecution.getLifecyclePhase(), "post-clean" ) )
            {
                break;
            }
            list.add( mojoExecution );
        }
        return list;
    }

    public void execute( MavenSession session,
                         MojoExecution mojoExecution,
                         ProjectIndex projectIndex,
                         IDependencyContext dependencyContext,
                         PhaseRecorder phaseRecorder ) throws LifecycleExecutionException
    {
        execute( session, mojoExecution, projectIndex, dependencyContext );
        phaseRecorder.observeExecution( mojoExecution );
    }

    private void execute( MavenSession session,
                          MojoExecution mojoExecution,
                          ProjectIndex projectIndex,
                          IDependencyContext dependencyContext ) throws LifecycleExecutionException
    {
        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

        try
        {
            mavenPluginManager.checkRequiredMavenVersion( mojoDescriptor.getPluginDescriptor() );
        }
        catch ( PluginIncompatibleException e )
        {
            throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
        }

        if ( mojoDescriptor.isProjectRequired() && !session.getRequest().isProjectPresent() )
        {
            Throwable cause = new MissingProjectException(
                    "Goal requires a project to execute" + " but there is no POM in this directory ("
                            + session.getExecutionRootDirectory() + ")."
                            + " Please verify you invoked Maven from the correct directory." );
            throw new LifecycleExecutionException( mojoExecution, null, cause );
        }

        if ( mojoDescriptor.isOnlineRequired() && session.isOffline() )
        {
            if ( Source.CLI.equals( mojoExecution.getSource() ) )
            {
                Throwable cause = new IllegalStateException(
                        "Goal requires online mode for execution" + " but Maven is currently offline." );
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), cause );
            }
            else
            {
                eventCatapult.fire( ExecutionEvent.Type.MojoSkipped, session, mojoExecution );

                return;
            }
        }

        List<MavenProject> forkedProjects = executeForkedExecutions( mojoExecution, session, projectIndex );

        ensureDependenciesAreResolved( mojoDescriptor, session, dependencyContext );

        eventCatapult.fire( ExecutionEvent.Type.MojoStarted, session, mojoExecution );

        try
        {
            try
            {
                pluginManager.executeMojo( session, mojoExecution );
            }
            catch ( MojoFailureException
                    | PluginManagerException
                    | PluginConfigurationException
                    | MojoExecutionException e )
            {
                throw new LifecycleExecutionException( mojoExecution, session.getCurrentProject(), e );
            }

            eventCatapult.fire( ExecutionEvent.Type.MojoSucceeded, session, mojoExecution );
        }
        catch ( LifecycleExecutionException e )
        {
            eventCatapult.fire( ExecutionEvent.Type.MojoFailed, session, mojoExecution, e );

            throw e;
        }
        finally
        {
            for ( MavenProject forkedProject : forkedProjects )
            {
                forkedProject.setExecutionProject( null );
            }
        }
    }

    public void ensureDependenciesAreResolved( MojoDescriptor mojoDescriptor,
                                               MavenSession session,
                                               IDependencyContext dependencyContext ) throws LifecycleExecutionException

    {
        MavenProject project = dependencyContext.getProject();
        boolean aggregating = mojoDescriptor.isAggregator();

        if ( dependencyContext.isResolutionRequiredForCurrentProject() )
        {
            Collection<String> scopesToCollect = dependencyContext.getScopesToCollectForCurrentProject();
            Collection<String> scopesToResolve = dependencyContext.getScopesToResolveForCurrentProject();

            lifeCycleDependencyResolver.resolveProjectDependencies( project, scopesToCollect, scopesToResolve, session,
                    aggregating, Collections.<Artifact>emptySet() );

            dependencyContext.synchronizeWithProjectState();
        }

        if ( aggregating )
        {
            Collection<String> scopesToCollect = toScopes( mojoDescriptor.getDependencyCollectionRequired() );
            Collection<String> scopesToResolve = toScopes( mojoDescriptor.getDependencyResolutionRequired() );

            if ( dependencyContext.isResolutionRequiredForAggregatedProjects( scopesToCollect, scopesToResolve ) )
            {
                for ( MavenProject aggregatedProject : session.getProjects() )
                {
                    if ( aggregatedProject != project )
                    {
                        lifeCycleDependencyResolver.resolveProjectDependencies( aggregatedProject, scopesToCollect,
                                scopesToResolve, session, aggregating, Collections.<Artifact>emptySet() );
                    }
                }
            }
        }

        ArtifactFilter artifactFilter = getArtifactFilter( mojoDescriptor );
        List<MavenProject> projectsToResolve = LifecycleDependencyResolver.getProjects( session.getCurrentProject(),
                session, mojoDescriptor.isAggregator() );
        for ( MavenProject projectToResolve : projectsToResolve )
        {
            projectToResolve.setArtifactFilter( artifactFilter );
        }
    }

    private ArtifactFilter getArtifactFilter( MojoDescriptor mojoDescriptor )
    {
        String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
        String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();

        List<String> scopes = new ArrayList<>( 2 );
        if ( StringUtils.isNotEmpty( scopeToCollect ) )
        {
            scopes.add( scopeToCollect );
        }
        if ( StringUtils.isNotEmpty( scopeToResolve ) )
        {
            scopes.add( scopeToResolve );
        }

        if ( scopes.isEmpty() )
        {
            return null;
        }
        else
        {
            return new CumulativeScopeArtifactFilter( scopes );
        }
    }

    public List<MavenProject> executeForkedExecutions( MojoExecution mojoExecution,
                                                       MavenSession session,
                                                       ProjectIndex projectIndex ) throws LifecycleExecutionException
    {
        List<MavenProject> forkedProjects = Collections.emptyList();

        Map<String, List<MojoExecution>> forkedExecutions = mojoExecution.getForkedExecutions();

        if ( !forkedExecutions.isEmpty() )
        {
            eventCatapult.fire( ExecutionEvent.Type.ForkStarted, session, mojoExecution );

            MavenProject project = session.getCurrentProject();

            forkedProjects = new ArrayList<>( forkedExecutions.size() );

            try
            {
                for ( Map.Entry<String, List<MojoExecution>> fork : forkedExecutions.entrySet() )
                {
                    String projectId = fork.getKey();

                    int index = projectIndex.getIndices().get( projectId );

                    MavenProject forkedProject = projectIndex.getProjects().get( projectId );

                    forkedProjects.add( forkedProject );

                    MavenProject executedProject = forkedProject.clone();

                    forkedProject.setExecutionProject( executedProject );

                    List<MojoExecution> mojoExecutions = fork.getValue();

                    if ( mojoExecutions.isEmpty() )
                    {
                        continue;
                    }

                    try
                    {
                        session.setCurrentProject( executedProject );
                        session.getProjects().set( index, executedProject );
                        projectIndex.getProjects().put( projectId, executedProject );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectStarted, session, mojoExecution );

                        execute( session, mojoExecutions, projectIndex );

                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectSucceeded, session, mojoExecution );
                    }
                    catch ( LifecycleExecutionException e )
                    {
                        eventCatapult.fire( ExecutionEvent.Type.ForkedProjectFailed, session, mojoExecution, e );

                        throw e;
                    }
                    finally
                    {
                        projectIndex.getProjects().put( projectId, forkedProject );
                        session.getProjects().set( index, forkedProject );
                        session.setCurrentProject( project );
                    }
                }

                eventCatapult.fire( ExecutionEvent.Type.ForkSucceeded, session, mojoExecution );
            }
            catch ( LifecycleExecutionException e )
            {
                eventCatapult.fire( ExecutionEvent.Type.ForkFailed, session, mojoExecution, e );

                throw e;
            }
        }

        return forkedProjects;
    }
}
