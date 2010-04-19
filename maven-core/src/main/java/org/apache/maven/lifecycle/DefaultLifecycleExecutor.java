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
package org.apache.maven.lifecycle;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.BuildListCalculator;
import org.apache.maven.lifecycle.internal.ConcurrencyDependencyGraph;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.LifecycleDebugLogger;
import org.apache.maven.lifecycle.internal.LifecycleExecutionPlanCalculator;
import org.apache.maven.lifecycle.internal.LifecycleModuleBuilder;
import org.apache.maven.lifecycle.internal.LifecycleTaskSegmentCalculator;
import org.apache.maven.lifecycle.internal.LifecycleThreadedBuilder;
import org.apache.maven.lifecycle.internal.LifecycleWeaveBuilder;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.internal.ProjectBuildList;
import org.apache.maven.lifecycle.internal.ProjectIndex;
import org.apache.maven.lifecycle.internal.ProjectSegment;
import org.apache.maven.lifecycle.internal.ReactorBuildStatus;
import org.apache.maven.lifecycle.internal.ReactorContext;
import org.apache.maven.lifecycle.internal.TaskSegment;
import org.apache.maven.lifecycle.internal.ThreadConfigurationService;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MojoNotFoundException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.prefix.NoPluginFoundForPrefixException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

/**
 * @author Jason van Zyl
 * @author Benjamin Bentmann
 * @author Kristian Rosenvold
 */
@Component( role = LifecycleExecutor.class )
public class DefaultLifecycleExecutor
    implements LifecycleExecutor
{

    @Requirement
    private ExecutionEventCatapult eventCatapult;

    @Requirement
    private LifeCyclePluginAnalyzer lifeCyclePluginAnalyzer;

    @Requirement
    private DefaultLifecycles defaultLifeCycles;

    @Requirement
    private Logger logger;

    @Requirement
    private LifecycleModuleBuilder lifecycleModuleBuilder;

    @Requirement
    private LifecycleWeaveBuilder lifeCycleWeaveBuilder;

    @Requirement
    private LifecycleThreadedBuilder lifecycleThreadedBuilder;

    @Requirement
    private BuildListCalculator buildListCalculator;

    @Requirement
    private LifecycleDebugLogger lifecycleDebugLogger;

    @Requirement
    private LifecycleTaskSegmentCalculator lifecycleTaskSegmentCalculator;

    @Requirement
    private LifecycleExecutionPlanCalculator lifecycleExecutionPlanCalculator;

    @Requirement
    private ThreadConfigurationService threadConfigService;

    public DefaultLifecycleExecutor()
    {
    }

    public void execute( MavenSession session )
    {
        eventCatapult.fire( ExecutionEvent.Type.SessionStarted, session, null );

        MavenExecutionResult result = session.getResult();

        try
        {
            if ( !session.isUsingPOMsFromFilesystem() && lifecycleTaskSegmentCalculator.requiresProject( session ) )
            {
                throw new MissingProjectException( "The goal you specified requires a project to execute" +
                    " but there is no POM in this directory (" + session.getExecutionRootDirectory() + ")." +
                    " Please verify you invoked Maven from the correct directory." );
            }

            final MavenExecutionRequest executionRequest = session.getRequest();
            boolean isThreaded = executionRequest.isThreadConfigurationPresent();
            session.setParallel( isThreaded );

            List<TaskSegment> taskSegments = lifecycleTaskSegmentCalculator.calculateTaskSegments( session );

            ProjectBuildList projectBuilds = buildListCalculator.calculateProjectBuilds( session, taskSegments );

            if ( projectBuilds.isEmpty() )
            {
                throw new NoGoalSpecifiedException( "No goals have been specified for this build." +
                    " You must specify a valid lifecycle phase or a goal in the format <plugin-prefix>:<goal> or" +
                    " <plugin-group-id>:<plugin-artifact-id>[:<plugin-version>]:<goal>." +
                    " Available lifecycle phases are: " + defaultLifeCycles.getLifecyclePhaseList() + "." );
            }

            ProjectIndex projectIndex = new ProjectIndex( session.getProjects() );

            if ( logger.isDebugEnabled() )
            {
                lifecycleDebugLogger.debugReactorPlan( projectBuilds );
            }

            ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();

            ReactorBuildStatus reactorBuildStatus = new ReactorBuildStatus( session.getProjectDependencyGraph() );
            ReactorContext callableContext =
                new ReactorContext( result, projectIndex, oldContextClassLoader, reactorBuildStatus );

            if ( isThreaded )
            {
                ExecutorService executor = threadConfigService.getExecutorService( executionRequest.getThreadCount(),
                                                                                   executionRequest.isPerCoreThreadCount(),
                                                                                   session.getProjects().size() );
                try
                {

                    final boolean isWeaveMode = LifecycleWeaveBuilder.isWeaveMode( executionRequest );
                    if ( isWeaveMode )
                    {
                        lifecycleDebugLogger.logWeavePlan( session );
                        CompletionService<ProjectSegment> service =
                            new ExecutorCompletionService<ProjectSegment>( executor );
                        lifeCycleWeaveBuilder.build( projectBuilds, callableContext, taskSegments, session, service,
                                                     reactorBuildStatus );
                    }
                    else
                    {
                        ConcurrencyDependencyGraph analyzer =
                            new ConcurrencyDependencyGraph( projectBuilds, session.getProjectDependencyGraph() );

                        CompletionService<ProjectSegment> service =
                            new ExecutorCompletionService<ProjectSegment>( executor );

                        lifecycleThreadedBuilder.build( session, callableContext, projectBuilds, taskSegments, analyzer,
                                                        service );
                    }
                }
                finally
                {
                    executor.shutdown();
                }
            }
            else
            {
                singleThreadedBuild( session, callableContext, projectBuilds, taskSegments, reactorBuildStatus );
            }

        }

        catch (

            Exception e

            )

        {
            result.addException( e );
        }

        eventCatapult.fire( ExecutionEvent.Type.SessionEnded, session, null );
    }

    private void singleThreadedBuild( MavenSession session, ReactorContext callableContext,
                                      ProjectBuildList projectBuilds, List<TaskSegment> taskSegments,
                                      ReactorBuildStatus reactorBuildStatus )
    {
        for ( TaskSegment taskSegment : taskSegments )
        {
            for ( ProjectSegment projectBuild : projectBuilds.getByTaskSegment( taskSegment ) )
            {
                try
                {
                    lifecycleModuleBuilder.buildProject( session, callableContext, projectBuild.getProject(),
                                                         taskSegment );
                    if ( reactorBuildStatus.isHalted() )
                    {
                        break;
                    }
                }
                catch ( Exception e )
                {
                    break;  // Why are we just ignoring this exception? Are exceptions are being used for flow control
                }

            }
        }
    }

    /**
     * * CRUFT GOES BELOW HERE ***
     */

    @Requirement
    private MojoDescriptorCreator mojoDescriptorCreator;

    // These methods deal with construction intact Plugin object that look like they come from a standard
    // <plugin/> block in a Maven POM. We have to do some wiggling to pull the sources of information
    // together and this really shows the problem of constructing a sensible default configuration but
    // it's all encapsulated here so it appears normalized to the POM builder.

    // We are going to take the project packaging and find all plugin in the default lifecycle and create
    // fully populated Plugin objects, including executions with goals and default configuration taken
    // from the plugin.xml inside a plugin.
    //
    // TODO: This whole method could probably removed by injecting lifeCyclePluginAnalyzer straight into client site.

    public Set<Plugin> getPluginsBoundByDefaultToAllLifecycles( String packaging )
    {
        return lifeCyclePluginAnalyzer.getPluginsBoundByDefaultToAllLifecycles( packaging );
    }

    // USED BY MAVEN HELP PLUGIN

    @SuppressWarnings( { "UnusedDeclaration" } )
    @Deprecated
    public Map<String, Lifecycle> getPhaseToLifecycleMap()
    {
        return defaultLifeCycles.getPhaseToLifecycleMap();
    }

    // NOTE: Backward-compat with maven-help-plugin:2.1

    @SuppressWarnings( { "UnusedDeclaration" } )
    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project, String invokedVia,
                                      boolean canUsePrefix, boolean isOptionalMojo )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginVersionResolutionException
    {
        return mojoDescriptorCreator.getMojoDescriptor( task, session, project );
    }

    // Used by m2eclipse

    @SuppressWarnings( { "UnusedDeclaration" } )
    public MavenExecutionPlan calculateExecutionPlan( MavenSession session, String... tasks )
        throws PluginNotFoundException, PluginResolutionException, PluginDescriptorParsingException,
        MojoNotFoundException, NoPluginFoundForPrefixException, InvalidPluginDescriptorException,
        PluginManagerException, LifecyclePhaseNotFoundException, LifecycleNotFoundException,
        PluginVersionResolutionException
    {

        List<TaskSegment> taskSegments = lifecycleTaskSegmentCalculator.calculateTaskSegments( session );

        TaskSegment mergedSegment = new TaskSegment( false );

        for ( TaskSegment taskSegment : taskSegments )
        {
            mergedSegment.getTasks().addAll( taskSegment.getTasks() );
        }

        return lifecycleExecutionPlanCalculator.calculateExecutionPlan( session, session.getCurrentProject(),
                                                                        mergedSegment.getTasks() );
    }

}
