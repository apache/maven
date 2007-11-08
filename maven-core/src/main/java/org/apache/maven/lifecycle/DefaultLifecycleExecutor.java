package org.apache.maven.lifecycle;

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

import org.apache.maven.AggregatedBuildFailureException;
import org.apache.maven.BuildFailureException;
import org.apache.maven.NoGoalsSpecifiedException;
import org.apache.maven.ProjectBuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.lifecycle.plan.BuildPlanUtils;
import org.apache.maven.lifecycle.plan.BuildPlanner;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo because of aggregation, we ended up with cli-ish stuff in here (like line() and the project logging, without
 * much of the event handling)
 */
public class DefaultLifecycleExecutor
    extends AbstractLogEnabled
    implements LifecycleExecutor, Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private PluginManager pluginManager;

    private PluginLoader pluginLoader;

    private BuildPlanner buildPlanner;

    private MojoBindingFactory mojoBindingFactory;

    private BuildContextManager buildContextManager;

    // this is needed for setting the lookup realm before we start building a project.
    private PlexusContainer container;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a task. Each task may be a phase in the lifecycle or the execution of a mojo.
     *
     * @param session
     * @param reactorManager
     * @param dispatcher
     * @throws MojoFailureException
     */
    public void execute( final MavenSession session,
                         final ReactorManager reactorManager,
                         final EventDispatcher dispatcher )
        throws BuildFailureException, LifecycleExecutionException
    {
        // TODO: This is dangerous, particularly when it's just a collection of loose-leaf projects being built
        // within the same reactor (using an inclusion pattern to gather them up)...
        MavenProject rootProject = reactorManager.getTopLevelProject();

        List goals = session.getGoals();

        if ( goals.isEmpty() && ( rootProject != null ) )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }

        if ( goals.isEmpty() )
        {
            throw new NoGoalsSpecifiedException( "You must specify at least one goal. Try 'install'" );
        }

        List taskSegments = segmentTaskListByAggregationNeeds(
            goals,
            session,
            rootProject );

        // FIXME: This should be handled by the extension scanner.
//        try
//        {
//            Map handlers = findArtifactTypeHandlers( session );
//
//            artifactHandlerManager.addHandlers( handlers );
//        }
//        catch ( PluginNotFoundException e )
//        {
//            throw new LifecycleExecutionException(
//                "Plugin could not be not found while searching for artifact-type handlers.",
//                rootProject,
//                e );
//        }

        executeTaskSegments(
            taskSegments,
            reactorManager,
            session,
            rootProject,
            dispatcher );
    }

    private void executeTaskSegments( final List taskSegments,
                                      final ReactorManager reactorManager,
                                      final MavenSession session,
                                      final MavenProject rootProject,
                                      final EventDispatcher dispatcher )
        throws LifecycleExecutionException, BuildFailureException
    {
        for ( Iterator it = taskSegments.iterator(); it.hasNext(); )
        {
            TaskSegment segment = (TaskSegment) it.next();

            if ( segment.aggregate() )
            {
                if ( !reactorManager.isBlackListed( rootProject ) )
                {
                    line();

                    getLogger().info( "Building " + rootProject.getName() );

                    getLogger().info( "  " + segment );

                    line();

                    String target = rootProject.getId() + " ( " + segment + " )";

                    getLogger().debug( "Constructing build plan for " + target );

                    // !! This is ripe for refactoring to an aspect.
                    // Event monitoring.
                    String event = MavenEvents.PROJECT_EXECUTION;

                    long buildStartTime = System.currentTimeMillis();

                    dispatcher.dispatchStart(
                        event,
                        target );

                    ClassRealm oldLookupRealm = setProjectLookupRealm( session, rootProject );

                    try
                    {
                        // NEW: To support forked execution under the new lifecycle architecture, the current project
                        // is stored in a build-context managed data type. This context type holds the current project
                        // for the fork being executed, plus a stack of projects used in the ancestor execution contexts.
                        LifecycleExecutionContext ctx = new LifecycleExecutionContext( rootProject );
                        ctx.store( buildContextManager );

                        // NEW: Build up the execution plan, including configuration.
                        List mojoBindings = getLifecycleBindings(
                            segment.getTasks(),
                            rootProject,
                            target );

                        // NEW: Then, iterate over each binding in that plan, and execute the associated mojo.
                        // only call once, with the top-level project (assumed to be provided as a parameter)...
                        for ( Iterator mojoIterator = mojoBindings.iterator(); mojoIterator.hasNext(); )
                        {
                            MojoBinding binding = (MojoBinding) mojoIterator.next();

                            try
                            {
                                executeGoalAndHandleFailures(
                                    binding,
                                    session,
                                    dispatcher,
                                    event,
                                    reactorManager,
                                    buildStartTime,
                                    target );
                            }
                            catch ( MojoFailureException e )
                            {
                                AggregatedBuildFailureException error = new AggregatedBuildFailureException(
                                                                                                             session.getExecutionRootDirectory(),
                                                                                                             binding,
                                                                                                             e );

                                dispatcher.dispatchError( event, target, error );

                                if ( handleExecutionFailure( reactorManager, rootProject, error, binding, buildStartTime ) )
                                {
                                    throw error;
                                }
                            }
                        }
                    }
                    finally
                    {
                        // clean up the execution context, so we don't pollute for future project-executions.
                        LifecycleExecutionContext.delete( buildContextManager );

                        restoreLookupRealm( oldLookupRealm );
                    }


                    reactorManager.registerBuildSuccess(
                        rootProject,
                        System.currentTimeMillis() - buildStartTime );

                    dispatcher.dispatchEnd(
                        event,
                        target );
                }
                else
                {
                    line();

                    getLogger().info( "SKIPPING " + rootProject.getName() );

                    getLogger().info( "  " + segment );

                    getLogger().info( "This project has been banned from further executions due to previous failures." );

                    line();
                }
            }
            else
            {
                List sortedProjects = session.getSortedProjects();

                // iterate over projects, and execute on each...
                for ( Iterator projectIterator = sortedProjects.iterator(); projectIterator.hasNext(); )
                {
                    MavenProject currentProject = (MavenProject) projectIterator.next();

                    if ( !reactorManager.isBlackListed( currentProject ) )
                    {
                        line();

                        getLogger().info( "Building " + currentProject.getName() );

                        getLogger().info( "  " + segment );

                        line();

                        String target = currentProject.getId() + " ( " + segment + " )";

                        // !! This is ripe for refactoring to an aspect.
                        // Event monitoring.
                        String event = MavenEvents.PROJECT_EXECUTION;

                        long buildStartTime = System.currentTimeMillis();

                        dispatcher.dispatchStart(
                            event,
                            target );

                        ClassRealm oldLookupRealm = setProjectLookupRealm( session, currentProject );

                        try
                        {
                            LifecycleExecutionContext ctx = new LifecycleExecutionContext( currentProject );
                            ctx.store( buildContextManager );

                            List mojoBindings = getLifecycleBindings(
                                segment.getTasks(),
                                currentProject,
                                target );

                            for ( Iterator mojoIterator = mojoBindings.iterator(); mojoIterator.hasNext(); )
                            {
                                MojoBinding binding = (MojoBinding) mojoIterator.next();

                                getLogger().debug(
                                    "Mojo: " + binding.getGoal() + " has config:\n"
                                        + binding.getConfiguration() );

                                try
                                {
                                    executeGoalAndHandleFailures( binding, session, dispatcher,
                                                                  event, reactorManager,
                                                                  buildStartTime, target );
                                }
                                catch ( MojoFailureException e )
                                {
                                    ProjectBuildFailureException error = new ProjectBuildFailureException(
                                                                                                           currentProject.getId(),
                                                                                                           binding,
                                                                                                           e );

                                    dispatcher.dispatchError( event, target, error );

                                    if ( handleExecutionFailure( reactorManager, currentProject, error, binding, buildStartTime ) )
                                    {
                                        throw error;
                                    }
                                }
                            }
                        }
                        finally
                        {
                            LifecycleExecutionContext.delete( buildContextManager );

                            restoreLookupRealm( oldLookupRealm );
                        }

                        reactorManager.registerBuildSuccess(
                            currentProject,
                            System.currentTimeMillis() - buildStartTime );

                        dispatcher.dispatchEnd(
                            event,
                            target );
                    }
                    else
                    {
                        line();

                        getLogger().info( "SKIPPING " + currentProject.getName() );

                        getLogger().info( "  " + segment );

                        getLogger().info(
                            "This project has been banned from further executions due to previous failures." );

                        line();
                    }
                }
            }
        }
    }

    private void restoreLookupRealm( ClassRealm oldLookupRealm )
    {
        if ( oldLookupRealm != null )
        {
            container.setLookupRealm( oldLookupRealm );
        }
    }

    private ClassRealm setProjectLookupRealm( MavenSession session,
                                              MavenProject rootProject )
        throws LifecycleExecutionException
    {
//        MavenProjectSession projectSession;
//        try
//        {
//            projectSession = session.getProjectSession( rootProject );
//        }
//        catch ( PlexusContainerException e )
//        {
//            throw new LifecycleExecutionException(
//                                                   "Failed to create project-specific session for: "
//                                                                   + rootProject.getId(),
//                                                   rootProject, e );
//        }
//        if ( projectSession != null )
//        {
//            return container.setLookupRealm( projectSession.getProjectRealm() );
//        }
//        else
//        {
//            return null;
//        }

        // TODO: Fix this to use project-level realm!
        return container.getLookupRealm();
    }

    /**
     * Retrieves the build plan for the current project, given the specified list of tasks. This build plan will consist
     * of MojoBindings, each fully configured to execute, which enables us to enumerate the full build plan to the debug
     * log-level, complete with the configuration each mojo will use.
     */
    private List getLifecycleBindings( final List tasks,
                                       final MavenProject project,
                                       final String targetDescription )
        throws LifecycleExecutionException
    {
        List mojoBindings;
        try
        {
            BuildPlan plan = buildPlanner.constructBuildPlan(
                tasks,
                project );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "\n\nOur build plan is:\n" + BuildPlanUtils.listBuildPlan(
                        plan,
                        false ) + "\n\n" );
            }

            mojoBindings = plan.renderExecutionPlan( new Stack() );
        }
        catch ( LifecycleException e )
        {
            throw new LifecycleExecutionException(
                "Failed to construct build plan for: " + targetDescription
                    + ". Reason: " + e.getMessage(), project,
                e );
        }

        return mojoBindings;
    }

    private void executeGoalAndHandleFailures( final MojoBinding mojoBinding,
                                               final MavenSession session,
                                               final EventDispatcher dispatcher,
                                               final String event,
                                               final ReactorManager rm,
                                               final long buildStartTime,
                                               final String target )
        throws LifecycleExecutionException, MojoFailureException
    {
        // NEW: Retrieve/use the current project stored in the execution context, for consistency.
        LifecycleExecutionContext ctx = LifecycleExecutionContext.read( buildContextManager );
        MavenProject project = ctx.getCurrentProject();

        // NEW: Since the MojoBinding instances are configured when the build plan is constructed,
        // all that remains to be done here is to load the PluginDescriptor, construct a MojoExecution
        // instance, and call PluginManager.executeMojo( execution ). The MojoExecutor is constructed
        // using both the PluginDescriptor and the MojoBinding.
        try
        {
            PluginDescriptor pluginDescriptor = null;
            try
            {
                pluginDescriptor = pluginLoader.loadPlugin(
                    mojoBinding,
                    project );
            }
            catch ( PluginLoaderException e )
            {
                if ( mojoBinding.isOptional() )
                {
                    getLogger().debug( "Skipping optional mojo execution: " + MojoBindingUtils.toString( mojoBinding ), e );
                    return;
                }
                else
                {
                    throw new LifecycleExecutionException(
                        "Failed to load plugin for: "
                            + MojoBindingUtils.toString( mojoBinding ) + ". Reason: " + e.getMessage(),
                            project,
                        e );
                }
            }

            if ( pluginDescriptor != null )
            {
                MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );

                mojoExecution.setConfiguration( (Xpp3Dom) mojoBinding.getConfiguration() );

                try
                {
                    pluginManager.executeMojo(
                        project,
                        mojoExecution,
                        session );
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException(
                        "Internal error in the plugin manager executing goal '"
                            + mojoDescriptor.getId() + "': " + e.getMessage(),
                            project,
                        e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    throw new LifecycleExecutionException(
                        e.getMessage(),
                        project,
                        e );
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new LifecycleExecutionException(
                        e.getMessage(),
                        project,
                        e );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new LifecycleExecutionException(
                        e.getMessage(),
                        project,
                        e );
                }
                catch ( PluginConfigurationException e )
                {
                    throw new LifecycleExecutionException(
                        e.getMessage(),
                        project,
                        e );
                }
            }
            else
            {
                throw new LifecycleExecutionException(
                    "Failed to load plugin for: "
                        + MojoBindingUtils.toString( mojoBinding ) + ". Reason: unknown", project );
            }
        }
        catch ( LifecycleExecutionException e )
        {
            dispatcher.dispatchError( event, target, e );

            if ( handleExecutionFailure( rm, project, e, mojoBinding, buildStartTime ) )
            {
                throw e;
            }
        }
    }

    private boolean handleExecutionFailure( final ReactorManager rm,
                                            final MavenProject project,
                                            final Exception e,
                                            final MojoBinding mojoBinding,
                                            final long buildStartTime )
    {
        rm.registerBuildFailure(
            project,
            e,
            MojoBindingUtils.toString( mojoBinding ),
            System.currentTimeMillis()
                - buildStartTime );

        if ( ReactorManager.FAIL_FAST.equals( rm.getFailureBehavior() ) )
        {
            return true;
        }
        else if ( ReactorManager.FAIL_AT_END.equals( rm.getFailureBehavior() ) )
        {
            rm.blackList( project );
        }
        // if NEVER, don't blacklist
        return false;
    }

    public TaskValidationResult isTaskValid( String task,
                                             MavenSession session,
                                             MavenProject rootProject )
    {
        //jvz: have to investigate plugins that are run without a root project or using Maven in reactor mode. Looks like we
        // were never validating these anyway if you look in the execution code.

        if ( rootProject != null )
        {

            if ( !LifecycleUtils.isValidPhaseName( task ) )
            {
                // definitely a CLI goal, can use prefix
                try
                {
                    getMojoDescriptorForDirectInvocation(
                        task,
                        session,
                        rootProject );

                    return new TaskValidationResult();
                }
                catch ( PluginLoaderException e )
                {
                    // TODO: shouldn't hit this, investigate using the same resolution logic as
                    // others for plugins in the reactor

                    return new TaskValidationResult(
                        task,
                        "Cannot find mojo descriptor for: \'" + task
                            + "\' - Treating as non-aggregator.", e );
                }
                catch ( LifecycleSpecificationException e )
                {
                    String message =
                        "Invalid task '"
                            + task
                            + "': you must specify a valid lifecycle phase, or"
                            + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

                    return new TaskValidationResult(
                        task,
                        message, e );

                }
                catch ( LifecycleLoaderException e )
                {
                    String message = "Failed to load one or more lifecycle definitions which may contain task: '" + task + "'.";

                    return new TaskValidationResult(
                        task,
                        message, e );
                }
            }
        }

        return new TaskValidationResult();
    }

    private List segmentTaskListByAggregationNeeds( final List tasks,
                                                    final MavenSession session,
                                                    final MavenProject rootProject )
    {
        List segments = new ArrayList();

        if ( rootProject != null )
        {
            TaskSegment currentSegment = null;

            for ( Iterator it = tasks.iterator(); it.hasNext(); )
            {
                String task = (String) it.next();

                // if it's a phase, then we don't need to check whether it's an aggregator.
                // simply add it to the current task partition.
                if ( LifecycleUtils.isValidPhaseName( task ) )
                {
                    if ( ( currentSegment != null ) && currentSegment.aggregate() )
                    {
                        segments.add( currentSegment );
                        currentSegment = null;
                    }

                    if ( currentSegment == null )
                    {
                        currentSegment = new TaskSegment();
                    }

                    currentSegment.add( task );
                }
                else
                {
                    MojoDescriptor mojo = null;

                    try
                    {
                        mojo = getMojoDescriptorForDirectInvocation(
                            task,
                            session,
                            rootProject );
                    }
                    catch ( Exception e )
                    {
                        // Won't happen as we've validated. So we need to change the code so that
                        // we don't have to do this.
                    }

                    // if the mojo descriptor was found, determine aggregator status according to:
                    // 1. whether the mojo declares itself an aggregator
                    // 2. whether the mojo DOES NOT require a project to function (implicitly avoid reactor)
                    if ( ( mojo != null ) && ( mojo.isAggregator() || !mojo.isProjectRequired() ) )
                    {
                        if ( ( currentSegment != null ) && !currentSegment.aggregate() )
                        {
                            segments.add( currentSegment );
                            currentSegment = null;
                        }

                        if ( currentSegment == null )
                        {
                            currentSegment = new TaskSegment( true );
                        }

                        currentSegment.add( task );
                    }
                    else
                    {
                        if ( ( currentSegment != null ) && currentSegment.aggregate() )
                        {
                            segments.add( currentSegment );
                            currentSegment = null;
                        }

                        if ( currentSegment == null )
                        {
                            currentSegment = new TaskSegment();
                        }

                        currentSegment.add( task );
                    }
                }
            }

            segments.add( currentSegment );
        }
        else
        {
            TaskSegment segment = new TaskSegment( false );
            for ( Iterator i = tasks.iterator(); i.hasNext(); )
            {
                segment.add( (String) i.next() );
            }
            segments.add( segment );
        }

        return segments;
    }

    private MojoDescriptor getMojoDescriptorForDirectInvocation( final String task,
                                                                 final MavenSession session,
                                                                 final MavenProject project )
        throws LifecycleSpecificationException, PluginLoaderException, LifecycleLoaderException
    {
        // we don't need to include report configuration here, since we're just looking for
        // an @aggregator flag...
        MojoBinding binding = mojoBindingFactory.parseMojoBinding(
            task,
            project,
            true );

        PluginDescriptor descriptor = pluginLoader.loadPlugin(
            binding,
            project );
        MojoDescriptor mojoDescriptor = descriptor.getMojo( binding.getGoal() );

        return mojoDescriptor;
    }

    protected void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
    }

    private static class TaskSegment
    {
        private boolean aggregate;

        private final List tasks = new ArrayList();

        TaskSegment()
        {

        }

        TaskSegment( final boolean aggregate )
        {
            this.aggregate = aggregate;
        }

        public String toString()
        {
            StringBuffer message = new StringBuffer();

            message.append( " task-segment: [" );

            for ( Iterator it = tasks.iterator(); it.hasNext(); )
            {
                String task = (String) it.next();

                message.append( task );

                if ( it.hasNext() )
                {
                    message.append( ", " );
                }
            }

            message.append( "]" );

            if ( aggregate )
            {
                message.append( " (aggregator-style)" );
            }

            return message.toString();
        }

        boolean aggregate()
        {
            return aggregate;
        }

        void add( final String task )
        {
            tasks.add( task );
        }

        List getTasks()
        {
            return tasks;
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
