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

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.lifecycle.plan.BuildPlanUtils;
import org.apache.maven.lifecycle.plan.BuildPlanner;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.loader.PluginLoader;
import org.apache.maven.plugin.loader.PluginLoaderException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo because of aggregation, we ended up with cli-ish stuff in here (like line() and the project logging, without
 *       much of the event handling)
 */
public class DefaultLifecycleExecutor extends AbstractLogEnabled implements LifecycleExecutor
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private PluginManager pluginManager;

    private PluginLoader pluginLoader;

    private BuildPlanner buildPlanner;

    private ArtifactHandlerManager artifactHandlerManager;

    private MojoBindingFactory mojoBindingFactory;

    private BuildContextManager buildContextManager;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a task. Each task may be a phase in the lifecycle or the execution of a mojo.
     * 
     * @param session
     * @param rm
     * @param dispatcher
     */
    public void execute( final MavenSession session, final ReactorManager rm, final EventDispatcher dispatcher )
        throws BuildFailureException, LifecycleExecutionException
    {
        // TODO: This is dangerous, particularly when it's just a collection of loose-leaf projects being built
        // within the same reactor (using an inclusion pattern to gather them up)...
        MavenProject rootProject = rm.getTopLevelProject();

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
            throw new BuildFailureException( "You must specify at least one goal. Try 'install'" );
        }

        List taskSegments = segmentTaskListByAggregationNeeds( goals, session, rootProject );

        // TODO: probably don't want to do all this up front
        try
        {
            Map handlers = findArtifactTypeHandlers( session );

            artifactHandlerManager.addHandlers( handlers );
        }
        catch ( PluginNotFoundException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }

        executeTaskSegments( taskSegments, rm, session, rootProject, dispatcher );
    }

    private void executeTaskSegments( final List taskSegments, final ReactorManager rm, final MavenSession session,
                                      final MavenProject rootProject, final EventDispatcher dispatcher )
        throws LifecycleExecutionException, BuildFailureException
    {
        for ( Iterator it = taskSegments.iterator(); it.hasNext(); )
        {
            TaskSegment segment = (TaskSegment) it.next();

            if ( segment.aggregate() )
            {
                if ( !rm.isBlackListed( rootProject ) )
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

                    dispatcher.dispatchStart( event, target );

                    // NEW: To support forked execution under the new lifecycle architecture, the current project
                    // is stored in a build-context managed data type. This context type holds the current project
                    // for the fork being executed, plus a stack of projects used in the ancestor execution contexts.
                    LifecycleExecutionContext ctx = new LifecycleExecutionContext( rootProject );
                    ctx.store( buildContextManager );

                    // NEW: Build up the execution plan, including configuration.
                    List mojoBindings = getLifecycleBindings( segment.getTasks(), rootProject, target );

                    // NEW: Then, iterate over each binding in that plan, and execute the associated mojo.
                    // only call once, with the top-level project (assumed to be provided as a parameter)...
                    for ( Iterator mojoIterator = mojoBindings.iterator(); mojoIterator.hasNext(); )
                    {
                        MojoBinding binding = (MojoBinding) mojoIterator.next();

                        executeGoalAndHandleFailures( binding, session, dispatcher, event, rm, buildStartTime, target );
                    }

                    // clean up the execution context, so we don't pollute for future project-executions.
                    LifecycleExecutionContext.delete( buildContextManager );

                    rm.registerBuildSuccess( rootProject, System.currentTimeMillis() - buildStartTime );

                    dispatcher.dispatchEnd( event, target );
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

                    if ( !rm.isBlackListed( currentProject ) )
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

                        dispatcher.dispatchStart( event, target );

                        LifecycleExecutionContext ctx = new LifecycleExecutionContext( currentProject );
                        ctx.store( buildContextManager );

                        List mojoBindings = getLifecycleBindings( segment.getTasks(), currentProject, target );

                        for ( Iterator mojoIterator = mojoBindings.iterator(); mojoIterator.hasNext(); )
                        {
                            MojoBinding mojoBinding = (MojoBinding) mojoIterator.next();

                            getLogger().debug(
                                               "Mojo: " + mojoBinding.getGoal() + " has config:\n"
                                                               + mojoBinding.getConfiguration() );
                            executeGoalAndHandleFailures( mojoBinding, session, dispatcher, event, rm, buildStartTime,
                                                          target );
                        }

                        LifecycleExecutionContext.delete( buildContextManager );

                        rm.registerBuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );

                        dispatcher.dispatchEnd( event, target );
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

    /**
     * Retrieves the build plan for the current project, given the specified list of tasks. This build plan will consist
     * of MojoBindings, each fully configured to execute, which enables us to enumerate the full build plan to the debug
     * log-level, complete with the configuration each mojo will use.
     */
    private List getLifecycleBindings( final List tasks, final MavenProject project, final String targetDescription )
        throws LifecycleExecutionException
    {
        List mojoBindings;
        try
        {
            BuildPlan plan = buildPlanner.constructBuildPlan( tasks, project );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "\n\nOur build plan is:\n" + BuildPlanUtils.listBuildPlan( plan, false ) + "\n\n" );
            }

            mojoBindings = plan.renderExecutionPlan( new Stack() );
        }
        catch ( LifecycleException e )
        {
            throw new LifecycleExecutionException( "Failed to construct build plan for: " + targetDescription
                            + ". Reason: " + e.getMessage(), e );
        }

        return mojoBindings;
    }

    private void executeGoalAndHandleFailures( final MojoBinding mojoBinding, final MavenSession session,
                                               final EventDispatcher dispatcher, final String event,
                                               final ReactorManager rm, final long buildStartTime, final String target )
        throws BuildFailureException, LifecycleExecutionException
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
                pluginDescriptor = pluginLoader.loadPlugin( mojoBinding, project );
            }
            catch ( PluginLoaderException e )
            {
                if ( mojoBinding.isOptional() )
                {
                    getLogger().debug( "Skipping optional mojo execution: " + MojoBindingUtils.toString( mojoBinding ) );
                }
                else
                {
                    throw new LifecycleExecutionException( "Failed to load plugin for: "
                                    + MojoBindingUtils.toString( mojoBinding ) + ". Reason: " + e.getMessage(), e );
                }
            }

            if ( pluginDescriptor != null )
            {
                MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );

                mojoExecution.setConfiguration( (Xpp3Dom) mojoBinding.getConfiguration() );

                try
                {
                    pluginManager.executeMojo( project, mojoExecution, session );
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException( "Internal error in the plugin manager executing goal '"
                                    + mojoDescriptor.getId() + "': " + e.getMessage(), e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
                catch ( InvalidDependencyVersionException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
                catch ( MojoFailureException e )
                {
                    throw new BuildFailureException( e.getMessage(), e );
                }
                catch ( MojoExecutionException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
                catch ( PluginConfigurationException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
            }
            else
            {
                throw new LifecycleExecutionException( "Failed to load plugin for: "
                                + MojoBindingUtils.toString( mojoBinding ) + ". Reason: unknown" );
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
        catch ( BuildFailureException e )
        {
            dispatcher.dispatchError( event, target, e );

            if ( handleExecutionFailure( rm, project, e, mojoBinding, buildStartTime ) )
            {
                throw e;
            }
        }
    }

    private boolean handleExecutionFailure( final ReactorManager rm, final MavenProject project, final Exception e,
                                            final MojoBinding mojoBinding, final long buildStartTime )
    {
        rm.registerBuildFailure( project, e, MojoBindingUtils.toString( mojoBinding ), System.currentTimeMillis()
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

    private List segmentTaskListByAggregationNeeds( final List tasks, final MavenSession session,
                                                    final MavenProject rootProject )
        throws LifecycleExecutionException, BuildFailureException
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
                    // definitely a CLI goal, can use prefix
                    try
                    {
                        mojo = getMojoDescriptorForDirectInvocation( task, session, rootProject );
                    }
                    catch ( PluginLoaderException e )
                    {
                        // TODO: shouldn't hit this, investigate using the same resolution logic as
                        // others for plugins in the reactor
                        getLogger().info(
                                          "Cannot find mojo descriptor for: \'" + task
                                                          + "\' - Treating as non-aggregator." );

                        getLogger().debug( "", e );
                    }
                    catch ( LifecycleSpecificationException e )
                    {
                        String message =
                            "Invalid task '"
                                            + task
                                            + "': you must specify a valid lifecycle phase, or"
                                            + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

                        throw new BuildFailureException( message, e );
                    }
                    catch ( LifecycleLoaderException e )
                    {
                        String message = "Cannot find plugin to match task '" + task + "'.";

                        throw new BuildFailureException( message, e );
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

    /**
     * @todo Not particularly happy about this. Would like WagonManager and ArtifactTypeHandlerManager to be able to
     *       lookup directly, or have them passed in
     * 
     * @todo Move this sort of thing to the tail end of the project-building process
     */
    private Map findArtifactTypeHandlers( final MavenSession session )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        Map map = new HashMap();
        for ( Iterator projectIterator = session.getSortedProjects().iterator(); projectIterator.hasNext(); )
        {
            MavenProject project = (MavenProject) projectIterator.next();

            for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
            {
                Plugin plugin = (Plugin) i.next();

                if ( plugin.isExtensions() )
                {
                    verifyPlugin( plugin, project, session );

                    // TODO: if moved to the plugin manager we already have the descriptor from above and so do can
                    // lookup the container directly
                    try
                    {
                        Map components = pluginManager.getPluginComponents( plugin, ArtifactHandler.ROLE );
                        map.putAll( components );
                    }
                    catch ( ComponentLookupException e )
                    {
                        getLogger().debug( "Unable to find the lifecycle component in the extension", e );
                    }
                    catch ( PluginManagerException e )
                    {
                        throw new LifecycleExecutionException( "Error looking up available components from plugin '"
                                        + plugin.getKey() + "': " + e.getMessage(), e );
                    }

                    // shudder...
                    for ( Iterator j = map.values().iterator(); j.hasNext(); )
                    {
                        ArtifactHandler handler = (ArtifactHandler) j.next();
                        if ( project.getPackaging().equals( handler.getPackaging() ) )
                        {
                            project.getArtifact().setArtifactHandler( handler );
                        }
                    }
                }
            }
        }
        return map;
    }

    private PluginDescriptor verifyPlugin( final Plugin plugin, final MavenProject project, final MavenSession session )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        getLogger().debug( "Verifying plugin: " + plugin.getKey() );

        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginManager.verifyPlugin( plugin, project, session );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager getting plugin '"
                            + plugin.getKey() + "': " + e.getMessage(), e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( InvalidPluginException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactNotFoundException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        catch ( PluginVersionNotFoundException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
        return pluginDescriptor;
    }

    private MojoDescriptor getMojoDescriptorForDirectInvocation( final String task, final MavenSession session,
                                                                 final MavenProject project )
        throws LifecycleSpecificationException, PluginLoaderException, LifecycleLoaderException
    {
        MojoBinding binding = mojoBindingFactory.parseMojoBinding( task, project, true );

        PluginDescriptor descriptor = pluginLoader.loadPlugin( binding, project );
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
}
