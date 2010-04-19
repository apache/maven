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
package org.apache.maven.lifecycle.internal;

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
 * @author Kristian Rosenvold
 *         Builds one or more lifecycles for a full module
 *         <p/>
 *         NOTE: This class is not part of any public api and can be changed or deleted without prior notice.
 */
@Component(role = LifecycleWeaveBuilder.class)
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

    private Map<MavenProject, MavenExecutionPlan> executionPlans = new HashMap<MavenProject, MavenExecutionPlan>( );


    @SuppressWarnings({"UnusedDeclaration"})
    public LifecycleWeaveBuilder()
    {
    }

    public LifecycleWeaveBuilder(MojoExecutor mojoExecutor, BuilderCommon builderCommon, Logger logger,
                                 ExecutionEventCatapult eventCatapult)
    {
        this.mojoExecutor = mojoExecutor;
        this.builderCommon = builderCommon;
        this.logger = logger;
        this.eventCatapult = eventCatapult;
    }

    public void build( ProjectBuildList projectBuilds, ReactorContext buildContext, List<TaskSegment> taskSegments,
                       MavenSession session, CompletionService<ProjectSegment> service,
                       ReactorBuildStatus reactorBuildStatus )
        throws ExecutionException, InterruptedException
    {
        ConcurrentBuildLogger concurrentBuildLogger = new ConcurrentBuildLogger();
        try
        {
            final List<Future<ProjectSegment>> futures = new ArrayList<Future<ProjectSegment>>();

            for ( TaskSegment taskSegment : taskSegments )
            {
                ProjectBuildList segmentChunks = projectBuilds.getByTaskSegment( taskSegment );
                    ThreadOutputMuxer muxer = null;  // new ThreadOutputMuxer( segmentChunks, System.out );
                Set<String> projectArtifacts = new HashSet<String>();
                Set<Artifact> projectArtifactsA = new HashSet<Artifact>();
                for (ProjectSegment segmentChunk : segmentChunks) {
                    Artifact artifact = segmentChunk.getProject().getArtifact();
                    if (artifact != null) {
                        projectArtifacts.add( ArtifactUtils.key(artifact));
                        projectArtifactsA.add( artifact);
                    }
                }
                for ( ProjectSegment projectBuild : segmentChunks )
                {
                    try
                    {
                        MavenExecutionPlan executionPlan =
                            builderCommon.resolveBuildPlan( projectBuild.getSession(), projectBuild.getProject(),
                                                            projectBuild.getTaskSegment(), projectArtifactsA );
                        for (Artifact dependency : projectBuild.getProject().getDependencyArtifacts()) {
                            String s = ArtifactUtils.key(dependency);
                            if ( projectArtifacts.contains(s)){
                                dependency.setFile( null);
                                dependency.setResolved( false);
                                dependency.setRepository( null);
                            }
                        }
                        
                        executionPlans.put( projectBuild.getProject(), executionPlan );
                        DependencyContext dependencyContext =
                            new DependencyContext( executionPlan, projectBuild.getTaskSegment().isAggregating() );

                        final Callable<ProjectSegment> projectBuilder =
                            createCallableForBuildingOneFullModule( buildContext, session, reactorBuildStatus,
                                                                    executionPlan, projectBuild, muxer,
                                                                    dependencyContext, concurrentBuildLogger,
                                                                    projectBuilds );

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

    private Callable<ProjectSegment> createCallableForBuildingOneFullModule( final ReactorContext reactorContext,
                                                                             final MavenSession rootSession,
                                                                             final ReactorBuildStatus reactorBuildStatus,
                                                                             final MavenExecutionPlan executionPlan,
                                                                             final ProjectSegment projectBuild,
                                                                             final ThreadOutputMuxer muxer,
                                                                             final DependencyContext dependencyContext,
                                                                             final ConcurrentBuildLogger concurrentBuildLogger,
                                                                             final ProjectBuildList projectBuilds )
    {
        return new Callable<ProjectSegment>()
        {
            public ProjectSegment call()
                throws Exception
            {
                Iterator<ExecutionPlanItem> planItems = executionPlan.iterator();
                ExecutionPlanItem current = planItems.hasNext() ? planItems.next() : null;
                long buildStartTime = System.currentTimeMillis();

                //muxer.associateThreadWithProjectSegment( projectBuild );

                if ( reactorBuildStatus.isHaltedOrBlacklisted( projectBuild.getProject() ) )
                {
                    eventCatapult.fire( ExecutionEvent.Type.ProjectSkipped, projectBuild.getSession(), null );
                    return null;
                }

                eventCatapult.fire( ExecutionEvent.Type.ProjectStarted, projectBuild.getSession(), null );

                try
                {
                    while (current != null && !reactorBuildStatus.isHaltedOrBlacklisted( projectBuild.getProject() ))
                    {
                        PhaseRecorder phaseRecorder = new PhaseRecorder( projectBuild.getProject() );

                        BuildLogItem builtLogItem =
                            concurrentBuildLogger.createBuildLogItem( projectBuild.getProject(), current );
                        final Schedule schedule = current.getSchedule();

                        buildExecutionPlanItem(current, phaseRecorder, schedule, reactorContext, projectBuild, dependencyContext);

                        current.setComplete();
                        builtLogItem.setComplete();

                        ExecutionPlanItem nextPlanItem = planItems.hasNext() ? planItems.next() : null;
                        if ( nextPlanItem != null )
                        {

                            final Schedule scheduleOfNext = nextPlanItem.getSchedule();
                            if ( scheduleOfNext == null || !scheduleOfNext.isParallel() )
                            {
                                waitForAppropriateUpstreamExecutionsToFinish(builtLogItem, nextPlanItem, projectBuild);
                            }
                            reResolveReactorDependencies(nextPlanItem, projectBuild);
                        }
                        current = nextPlanItem;
                    }

                    final long wallClockTime = System.currentTimeMillis() - buildStartTime;
                    final BuildSuccess summary =
                        new BuildSuccess( projectBuild.getProject(), wallClockTime ); // - waitingTime 
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

    private void reResolveReactorDependencies(ExecutionPlanItem nextPlanItem, ProjectSegment projectBuild) {
        if ( requiresReResolutionOfUpstreamReactorArtifacts( nextPlanItem ) )
        {
            reresolveUpstreamProjectArtifacts(projectBuild);
        }
        else if (requiresReResolutionOfUpstreamTestScopedReactorArtifacts( nextPlanItem))
        {
            reresolveUpstreamTestScopedArtifacts( projectBuild);
        }
    }

    private void waitForAppropriateUpstreamExecutionsToFinish(BuildLogItem builtLogItem, ExecutionPlanItem nextPlanItem, ProjectSegment projectBuild) throws InterruptedException {
        for ( MavenProject upstreamProject : projectBuild.getImmediateUpstreamProjects() )
        {
            final MavenExecutionPlan upstreamPlan = executionPlans.get( upstreamProject );
            final String nextPhase = nextPlanItem.getLifecyclePhase();
            final ExecutionPlanItem inSchedule = upstreamPlan.findLastInPhase( nextPhase );

            if ( inSchedule != null )
            {
                long startWait = System.currentTimeMillis();
                inSchedule.waitUntilDone();
                builtLogItem.addWait( upstreamProject, inSchedule, startWait );
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

    private void reresolveUpstreamProjectArtifacts(ProjectSegment projectBuild) {
        for ( MavenProject upstreamProject : projectBuild.getTransitiveUpstreamProjects() ){
            Artifact upStreamArtifact = upstreamProject.getArtifact();
            Artifact dependencyArtifact =  findDependency(projectBuild.getProject(), upStreamArtifact);
            if (dependencyArtifact != null){
                dependencyArtifact.setFile( upStreamArtifact.getFile());
                dependencyArtifact.setResolved( true );
                dependencyArtifact.setRepository( upStreamArtifact.getRepository());
            }

        }
    }

    private void reresolveUpstreamTestScopedArtifacts(ProjectSegment projectBuild) {
        for ( MavenProject upstreamProject : projectBuild.getTransitiveUpstreamProjects() ){
            Artifact upStreamArtifact = findTestScopedArtifact(upstreamProject);
            Artifact dependencyArtifact =  findDependency(projectBuild.getProject(), upStreamArtifact);
            if (dependencyArtifact != null){
                dependencyArtifact.setFile( upStreamArtifact.getFile());
                dependencyArtifact.setResolved( upStreamArtifact.isResolved());
                dependencyArtifact.setRepository( upStreamArtifact.getRepository());
            }

        }
    }

    private Artifact findTestScopedArtifact(MavenProject upstreamProject) {
        if ( upstreamProject == null){
            return null;
        }
        
        List<Artifact> artifactList = upstreamProject.getAttachedArtifacts();
        for (Artifact artifact : artifactList) {
            if (Artifact.SCOPE_TEST.equals( artifact.getScope())){
                return artifact;
            }
        }
        return null;
    }

    private static Artifact findDependency(MavenProject project, Artifact upStreamArtifact) {
        if (upStreamArtifact == null){
            return null;
        }
        
        String key = ArtifactUtils.key( upStreamArtifact.getGroupId(),
                                        upStreamArtifact.getArtifactId(),
                                        upStreamArtifact.getVersion() );
        final Set<Artifact> deps = project.getDependencyArtifacts();
        for ( Artifact dep : deps )
        {
            String depKey = ArtifactUtils.key(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
            if ( key.equals( depKey ) )
            {
                return dep;
            }
        }
        return null;

    }

    private boolean requiresReResolutionOfUpstreamReactorArtifacts( ExecutionPlanItem nextExecutionPlanItem )
    {
        final String phase = nextExecutionPlanItem.getLifecyclePhase();
        return "package".equals(phase) ||  "install".equals( phase ) || "compile".equals( phase );
    }

    private boolean requiresReResolutionOfUpstreamTestScopedReactorArtifacts( ExecutionPlanItem nextExecutionPlanItem )
    {
        final String phase = nextExecutionPlanItem.getLifecyclePhase();
        return "package".equals(phase) || "install".equals( phase ) || "compile".equals( phase ) || "test-compile".equals( phase );
    }

    private void buildExecutionPlanItem(ExecutionPlanItem current, PhaseRecorder phaseRecorder, Schedule schedule, ReactorContext reactorContext, ProjectSegment projectBuild, DependencyContext dependencyContext) throws LifecycleExecutionException {
        if ( schedule != null && schedule.isMojoSynchronized() )
        {
            synchronized ( current.getPlugin() )
            {
                buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext,
                                        phaseRecorder );
            }
        }
        else
        {
            buildExecutionPlanItem( reactorContext, current, projectBuild, dependencyContext,
                                    phaseRecorder );
        }
    }


    private void buildExecutionPlanItem( ReactorContext reactorContext, ExecutionPlanItem node,
                                         ProjectSegment projectBuild, DependencyContext dependencyContext,
                                         PhaseRecorder phaseRecorder )
        throws LifecycleExecutionException
    {

        MavenProject currentProject = projectBuild.getProject();

        long buildStartTime = System.currentTimeMillis();

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
}