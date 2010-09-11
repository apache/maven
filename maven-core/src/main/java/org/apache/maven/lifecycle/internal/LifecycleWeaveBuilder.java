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
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.MavenExecutionPlan;
import org.apache.maven.lifecycle.Schedule;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Builds the full lifecycle in weave-mode (phase by phase as opposed to project-by-project)
 * <p/>
 * NOTE: Weave mode is still experimental. It may be either promoted to first class citizen
 * at some later point in time, and it may also be removed entirely. Weave mode has much more aggressive
 * concurrency behaviour than regular threaded mode, and as such is still under test wrt cross platform stability.
 * <p/>
 * To remove weave mode from m3, the following should be removed:
 * ExecutionPlanItem.schedule w/setters and getters
 * DefaultLifeCycles.getScheduling() and all its use
 * ReactorArtifactRepository has a reference to isWeave too.
 * This class and its usage
 * 
 * @since 3.0
 * @author Kristian Rosenvold
 *         Builds one or more lifecycles for a full module
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component( role = LifecycleWeaveBuilder.class )
public class LifecycleWeaveBuilder
{

    @Requirement
    private MojoExecutor mojoExecutor;

    @Requirement
    private BuilderCommon builderCommon;

    @Requirement
    private Logger logger;

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    private Map<MavenProject, MavenExecutionPlan> executionPlans = new HashMap<MavenProject, MavenExecutionPlan>();


    @SuppressWarnings( { "UnusedDeclaration" } )
    public LifecycleWeaveBuilder()
    {
    }

    public LifecycleWeaveBuilder( MojoExecutor mojoExecutor, BuilderCommon builderCommon, Logger logger,
                                  ExecutionEventCatapult eventCatapult )
    {
        this.mojoExecutor = mojoExecutor;
        this.builderCommon = builderCommon;
        this.logger = logger;
        this.eventCatapult = eventCatapult;
    }

    public void build( ProjectBuildList projectBuilds, ReactorContext buildContext, List<TaskSegment> taskSegments,
                       MavenSession session, ExecutorService executor, ReactorBuildStatus reactorBuildStatus )
        throws ExecutionException, InterruptedException
    {
        ConcurrentBuildLogger concurrentBuildLogger = new ConcurrentBuildLogger();
        CompletionService<ProjectSegment> service = new ExecutorCompletionService<ProjectSegment>( executor );

        try
        {
            for ( MavenProject mavenProject : session.getProjects() )
            {
                Artifact mainArtifact = mavenProject.getArtifact();
                if ( mainArtifact != null && !( mainArtifact instanceof ThreadLockedArtifact ) )
                {
                    ThreadLockedArtifact threadLockedArtifact = new ThreadLockedArtifact( mainArtifact );
                    mavenProject.setArtifact( threadLockedArtifact );
                }
            }

            final List<Future<ProjectSegment>> futures = new ArrayList<Future<ProjectSegment>>();
            final Map<ProjectSegment, Future<MavenExecutionPlan>> plans =
                new HashMap<ProjectSegment, Future<MavenExecutionPlan>>();

            for ( TaskSegment taskSegment : taskSegments )
            {
                ProjectBuildList segmentChunks = projectBuilds.getByTaskSegment( taskSegment );
                Set<Artifact> projectArtifacts = new HashSet<Artifact>();
                for ( ProjectSegment segmentChunk : segmentChunks )
                {
                    Artifact artifact = segmentChunk.getProject().getArtifact();
                    if ( artifact != null )
                    {
                        projectArtifacts.add( artifact );
                    }
                }
                for ( ProjectSegment projectBuild : segmentChunks )
                {
                    plans.put( projectBuild, executor.submit( createEPFuture( projectBuild, projectArtifacts ) ) );
                }

                for ( ProjectSegment projectSegment : plans.keySet() )
                {
                    executionPlans.put( projectSegment.getProject(), plans.get( projectSegment ).get() );

                }
                for ( ProjectSegment projectBuild : segmentChunks )
                {
                    try
                    {
                        final MavenExecutionPlan executionPlan = plans.get( projectBuild ).get();

                        DependencyContext dependencyContext =
                            mojoExecutor.newDependencyContext( session, executionPlan.getMojoExecutions() );

                        final Callable<ProjectSegment> projectBuilder =
                            createCallableForBuildingOneFullModule( buildContext, session, reactorBuildStatus,
                                                                    executionPlan, projectBuild, dependencyContext,
                                                                    concurrentBuildLogger );

                        futures.add( service.submit( projectBuilder ) );
                    }
                    catch ( Exception e )
                    {
                        throw new ExecutionException( e );
                    }
                }

                for ( Future<ProjectSegment> buildFuture : futures )
                {
                    buildFuture.get();  // At this point, this build *is* finished.
                    // Do not leak threads past here or evil gremlins will get you!
                }
                futures.clear();
            }
        }
        finally
        {
            projectBuilds.closeAll();
        }
        logger.info( concurrentBuildLogger.toString() );
    }

    private Callable<MavenExecutionPlan> createEPFuture( final ProjectSegment projectSegment,
                                                         final Set<Artifact> projectArtifacts )
    {
        return new Callable<MavenExecutionPlan>()
        {
            public MavenExecutionPlan call()
                throws Exception
            {
                return builderCommon.resolveBuildPlan( projectSegment.getSession(), projectSegment.getProject(),
                                                       projectSegment.getTaskSegment(), projectArtifacts );
            }
        };
    }


    private Callable<ProjectSegment> createCallableForBuildingOneFullModule( final ReactorContext reactorContext,
                                                                             final MavenSession rootSession,
                                                                             final ReactorBuildStatus reactorBuildStatus,
                                                                             final MavenExecutionPlan executionPlan,
                                                                             final ProjectSegment projectBuild,
                                                                             final DependencyContext dependencyContext,
                                                                             final ConcurrentBuildLogger concurrentBuildLogger )
    {
        return new Callable<ProjectSegment>()
        {
            public ProjectSegment call()
                throws Exception
            {
                Iterator<ExecutionPlanItem> planItems = executionPlan.iterator();
                ExecutionPlanItem current = planItems.hasNext() ? planItems.next() : null;
                ThreadLockedArtifact threadLockedArtifact = (ThreadLockedArtifact)projectBuild.getProject().getArtifact();
                if ( threadLockedArtifact != null )
                {
                    threadLockedArtifact.attachToThread();
                }
                long buildStartTime = System.currentTimeMillis();

                //muxer.associateThreadWithProjectSegment( projectBuild );

                if ( reactorBuildStatus.isHaltedOrBlacklisted( projectBuild.getProject() ) )
                {
                    eventCatapult.fire( ExecutionEvent.Type.ProjectSkipped, projectBuild.getSession(), null );
                    return null;
                }

                eventCatapult.fire( ExecutionEvent.Type.ProjectStarted, projectBuild.getSession(), null );

                Collection<ArtifactLink> dependencyLinks = getUpstreamReactorDependencies( projectBuild );

                try
                {
                    PhaseRecorder phaseRecorder = new PhaseRecorder( projectBuild.getProject() );
                    long totalMojoTime = 0;
                    long mojoStart;
                    while ( current != null && !reactorBuildStatus.isHaltedOrBlacklisted( projectBuild.getProject() ) )
                    {

                        BuildLogItem builtLogItem =
                            concurrentBuildLogger.createBuildLogItem( projectBuild.getProject(), current );
                        final Schedule schedule = current.getSchedule();

                        mojoStart = System.currentTimeMillis();
                        buildExecutionPlanItem( current, phaseRecorder, schedule, reactorContext, projectBuild,
                                                dependencyContext );
                        totalMojoTime += ( System.currentTimeMillis() - mojoStart );

                        current.setComplete();
                        builtLogItem.setComplete();

                        ExecutionPlanItem nextPlanItem = planItems.hasNext() ? planItems.next() : null;
                        if ( nextPlanItem != null && phaseRecorder.isDifferentPhase( nextPlanItem.getMojoExecution() ) )
                        {

                            final Schedule scheduleOfNext = nextPlanItem.getSchedule();
                            if ( scheduleOfNext == null || !scheduleOfNext.isParallel() )
                            {
                                waitForAppropriateUpstreamExecutionsToFinish( builtLogItem, nextPlanItem, projectBuild,
                                                                              scheduleOfNext );
                            }

                            for ( ArtifactLink dependencyLink : dependencyLinks )
                            {
                                dependencyLink.resolveFromUpstream();
                            }
                        }
                        current = nextPlanItem;
                    }

                    final BuildSuccess summary =
                        new BuildSuccess( projectBuild.getProject(), totalMojoTime ); // - waitingTime
                    reactorContext.getResult().addBuildSummary( summary );
                    eventCatapult.fire( ExecutionEvent.Type.ProjectSucceeded, projectBuild.getSession(), null );
                }
                catch ( Exception e )
                {
                    builderCommon.handleBuildError( reactorContext, rootSession, projectBuild.getProject(), e,
                                                    buildStartTime );
                }
                finally
                {
                    if ( current != null )
                    {
                        executionPlan.forceAllComplete();
                    }
                    // muxer.setThisModuleComplete( projectBuild );
                }
                return null;
            }

        };
    }

    private void waitForAppropriateUpstreamExecutionsToFinish( BuildLogItem builtLogItem,
                                                               ExecutionPlanItem nextPlanItem,
                                                               ProjectSegment projectBuild, Schedule scheduleOfNext )
        throws InterruptedException
    {
        for ( MavenProject upstreamProject : projectBuild.getImmediateUpstreamProjects() )
        {
            final MavenExecutionPlan upstreamPlan = executionPlans.get( upstreamProject );
            final String nextPhase = scheduleOfNext != null && scheduleOfNext.hasUpstreamPhaseDefined()
                ? scheduleOfNext.getUpstreamPhase()
                : nextPlanItem.getLifecyclePhase();
            final ExecutionPlanItem upstream = upstreamPlan.findLastInPhase( nextPhase );

            if ( upstream != null )
            {
                long startWait = System.currentTimeMillis();
                upstream.waitUntilDone();
                builtLogItem.addWait( upstreamProject, upstream, startWait );
            }
            else if ( !upstreamPlan.containsPhase( nextPhase ) )
            {
                // Still a bit of a kludge; if we cannot connect in a sensible way to
                // the upstream build plan we just revert to waiting for it all to
                // complete. Real problem is per-mojo phase->lifecycle mapping
                builtLogItem.addDependency( upstreamProject, "No phase tracking possible " );
                upstreamPlan.waitUntilAllDone();
            }
            else
            {
                builtLogItem.addDependency( upstreamProject, "No schedule" );
            }
        }
    }

    private Collection<ArtifactLink> getUpstreamReactorDependencies( ProjectSegment projectBuild )
    {
        Collection<ArtifactLink> result = new ArrayList<ArtifactLink>();
        for ( MavenProject upstreamProject : projectBuild.getTransitiveUpstreamProjects() )
        {
            Artifact upStreamArtifact = upstreamProject.getArtifact();
            if ( upStreamArtifact != null )
            {
                Artifact dependencyArtifact = findDependency( projectBuild.getProject(), upStreamArtifact );
                if ( dependencyArtifact != null )
                {
                    result.add( new ArtifactLink( dependencyArtifact, upStreamArtifact ) );
                }
            }

            Artifact upStreamTestScopedArtifact = findTestScopedArtifact( upstreamProject );
            if ( upStreamTestScopedArtifact != null )
            {
                Artifact dependencyArtifact = findDependency( projectBuild.getProject(), upStreamArtifact );
                if ( dependencyArtifact != null )
                {
                    result.add( new ArtifactLink( dependencyArtifact, upStreamTestScopedArtifact ) );
                }
            }
        }
        return result;
    }


    private Artifact findTestScopedArtifact( MavenProject upstreamProject )
    {
        if ( upstreamProject == null )
        {
            return null;
        }

        List<Artifact> artifactList = upstreamProject.getAttachedArtifacts();
        for ( Artifact artifact : artifactList )
        {
            if ( Artifact.SCOPE_TEST.equals( artifact.getScope() ) )
            {
                return artifact;
            }
        }
        return null;
    }

    private static boolean isThreadLockedAndEmpty(Artifact artifact){
        return artifact instanceof  ThreadLockedArtifact && !((ThreadLockedArtifact) artifact).hasReal();
    }

    private static Artifact findDependency( MavenProject project, Artifact upStreamArtifact )
    {
        if ( upStreamArtifact == null || isThreadLockedAndEmpty(upStreamArtifact))
        {
            return null;
        }

        String key = ArtifactUtils.key( upStreamArtifact.getGroupId(), upStreamArtifact.getArtifactId(),
                                        upStreamArtifact.getVersion() );
        final Set<Artifact> deps = project.getDependencyArtifacts();
        for ( Artifact dep : deps )
        {
            String depKey = ArtifactUtils.key( dep.getGroupId(), dep.getArtifactId(), dep.getVersion() );
            if ( key.equals( depKey ) )
            {
                return dep;
            }
        }
        return null;

    }

    private void buildExecutionPlanItem( ExecutionPlanItem current, PhaseRecorder phaseRecorder, Schedule schedule,
                                         ReactorContext reactorContext, ProjectSegment projectBuild,
                                         DependencyContext dependencyContext )
        throws LifecycleExecutionException
    {
        if ( schedule != null && schedule.isMojoSynchronized() )
        {
            synchronized ( current.getPlugin() )
            {
                buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext, phaseRecorder );
            }
        }
        else
        {
            buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext, phaseRecorder );
        }
    }


    private void buildExecutionPlanItem( ReactorContext reactorContext, ExecutionPlanItem node,
                                         ProjectSegment projectBuild, DependencyContext dependencyContext,
                                         PhaseRecorder phaseRecorder )
        throws LifecycleExecutionException
    {

        MavenProject currentProject = projectBuild.getProject();

        long buildStartTime = System.currentTimeMillis();

        CurrentPhaseForThread.setPhase(  node.getLifecyclePhase() );

        MavenSession sessionForThisModule = projectBuild.getSession();
        try
        {

            if ( reactorContext.getReactorBuildStatus().isHaltedOrBlacklisted( currentProject ) )
            {
                return;
            }

            BuilderCommon.attachToThread( currentProject );

            mojoExecutor.execute( sessionForThisModule, node.getMojoExecution(), reactorContext.getProjectIndex(),
                                  dependencyContext, phaseRecorder );

            final BuildSuccess summary =
                new BuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );
            reactorContext.getResult().addBuildSummary( summary );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( reactorContext.getOriginalContextClassLoader() );
        }
    }

    public static boolean isWeaveMode( MavenExecutionRequest request )
    {
        return "true".equals( request.getUserProperties().getProperty( "maven3.weaveMode" ) );
    }

    public static void setWeaveMode( Properties properties )
    {
        properties.setProperty( "maven3.weaveMode", "true" );
    }

    static class ArtifactLink
    {
        private final Artifact artifactInThis;

        private final Artifact upstream;

        ArtifactLink( Artifact artifactInThis, Artifact upstream )
        {
            this.artifactInThis = artifactInThis;
            this.upstream = upstream;
        }

        public void resolveFromUpstream()
        {
            artifactInThis.setFile( upstream.getFile() );
            artifactInThis.setRepository( upstream.getRepository() );
            artifactInThis.setResolved( true ); // Or maybe upstream.isResolved()....

        }
    }

}