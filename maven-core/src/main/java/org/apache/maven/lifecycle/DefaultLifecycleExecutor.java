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
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.binding.LifecycleBindingManager;
import org.apache.maven.lifecycle.binding.MojoBindingFactory;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.plan.BuildPlan;
import org.apache.maven.lifecycle.plan.BuildPlanUtils;
import org.apache.maven.lifecycle.plan.BuildPlanner;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.InvalidPluginException;
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
 * Responsible for orchestrating the process of building the ordered list of
 * steps required to achieve the specified set of tasks passed into Maven, then
 * executing these mojos in order. This class also manages the various error messages
 * that may occur during this process, and directing the behavior of the build
 * according to what's specified in {@link MavenExecutionRequest#getReactorFailureBehavior()}.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author jdcasey
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
    
    private LifecycleBindingManager lifecycleBindingManager;

    // this is needed for setting the lookup realm before we start building a project.
    private PlexusContainer container;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
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

        if ( ( ( goals == null ) || goals.isEmpty() ) && ( rootProject != null ) )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }

        if ( ( goals == null ) || goals.isEmpty() )
        {
            StringBuffer buffer = new StringBuffer( 1024 );

            buffer.append( "\n\n" );
            buffer.append( "You must specify at least one goal or lifecycle phase to perform build steps.\n" );
            buffer.append( "The following list illustrates some commonly used build commands:\n\n" );
            buffer.append( "  mvn clean\n" );
            buffer.append( "    Deletes any build output (e.g. class files or JARs).\n" );
            buffer.append( "  mvn test\n" );
            buffer.append( "    Runs the unit tests for the project.\n" );
            buffer.append( "  mvn install\n" );
            buffer.append( "    Copies the project artifacts into your local repository.\n" );
            buffer.append( "  mvn deploy\n" );
            buffer.append( "    Copies the project artifacts into the remote repository.\n" );
            buffer.append( "  mvn site\n" );
            buffer.append( "    Creates project documentation (e.g. reports or Javadoc).\n\n" );
            buffer.append( "Please see\n" );
            buffer.append( "http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html\n" );
            buffer.append( "for a complete description of available lifecycle phases.\n\n" );
            buffer.append( "Use \"mvn -?\" to show general usage information about Maven's command line.\n\n" );

            throw new NoGoalsSpecifiedException( buffer.toString() );
        }

        List taskSegments = segmentTaskListByAggregationNeeds(
            goals,
            session,
            rootProject );

        try
        {
            buildPlanner.constructInitialProjectBuildPlans( session );
        }
        catch ( LifecycleException e )
        {
            e.printStackTrace();
            throw new LifecycleExecutionException(
                                                   "Failed to construct one or more initial build plans."
                                                                   + " Reason: " + e.getMessage(),
                                                   e );
        }

        executeTaskSegments(
            taskSegments,
            reactorManager,
            session,
            rootProject,
            dispatcher );
    }

    /**
     * After the list of goals from {@link MavenSession#getGoals()} is segmented into
     * contiguous sets of aggregated and non-aggregated mojos and lifecycle phases,
     * this method is used to execute each task-segment. Its logic has a top-level fork
     * for each segment, which basically varies the project used to run the execution
     * according to aggregation needs. If the segment is aggregated, the root project
     * will be used to construct and execute the mojo bindings. Otherwise, this
     * method will iterate through each project, and execute all the goals implied
     * by the current task segment.
     */
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
                executeTaskSegmentForProject( segment, rootProject, reactorManager, dispatcher, session );
            }
            else
            {
                List sortedProjects = session.getSortedProjects();

                // iterate over projects, and execute on each...
                for ( Iterator projectIterator = sortedProjects.iterator(); projectIterator.hasNext(); )
                {
                    MavenProject currentProject = (MavenProject) projectIterator.next();

                    executeTaskSegmentForProject( segment, currentProject, reactorManager, dispatcher, session );
                }
            }
        }
    }

    private void executeTaskSegmentForProject( TaskSegment segment,
                                    MavenProject project,
                                    ReactorManager reactorManager,
                                    EventDispatcher dispatcher,
                                    MavenSession session )
        throws LifecycleExecutionException, BuildFailureException
    {
        if ( !reactorManager.isBlackListed( project ) )
        {
//            line();
//
//            getLogger().info( "Building " + project.getName() );
//
//            getLogger().info( "  " + segment );
//
//            line();

            String target = project.getName() + "\nId: " + project.getId() + "\n" + segment;

            getLogger().debug( "Constructing build plan for " + target );

            // !! This is ripe for refactoring to an aspect.
            // Event monitoring.
            String event = MavenEvents.PROJECT_EXECUTION;

            long buildStartTime = System.currentTimeMillis();

            dispatcher.dispatchStart(
                event,
                target );

            ClassRealm oldLookupRealm = setProjectLookupRealm( session, project );

            try
            {
                session.setCurrentProject( project );

                // NEW: Build up the execution plan, including configuration.
                List mojoBindings = getLifecycleBindings(
                    segment.getTasks(),
                    project,
                    session,
                    target );

                String currentPhase = null;

                // NEW: Then, iterate over each binding in that plan, and execute the associated mojo.
                // only call once, with the top-level project (assumed to be provided as a parameter)...
                for ( Iterator mojoIterator = mojoBindings.iterator(); mojoIterator.hasNext(); )
                {
                    MojoBinding binding = (MojoBinding) mojoIterator.next();

                    String phase = binding.getPhase() == null ? null : binding.getPhase().getName();

                    if ( ( currentPhase != null ) && !currentPhase.equals( phase ) )
                    {
                        dispatcher.dispatchEnd( MavenEvents.PHASE_EXECUTION, currentPhase );
                        currentPhase = null;
                    }

                    if ( ( currentPhase == null ) && ( phase != null ) )
                    {
                        currentPhase = phase;
                        dispatcher.dispatchStart( MavenEvents.PHASE_EXECUTION, currentPhase );
                    }

                    try
                    {
                        executeGoalAndHandleFailures(
                            binding,
                            session,
                            dispatcher,
                            event,
                            reactorManager,
                            buildStartTime,
                            target,
                            segment.aggregate() );
                    }
                    catch ( MojoFailureException e )
                    {
                        if ( segment.aggregate() )
                        {
                            AggregatedBuildFailureException error = new AggregatedBuildFailureException(
                                                                                                        session.getExecutionRootDirectory(),
                                                                                                        binding,
                                                                                                        e );

                           dispatcher.dispatchError( event, target, error );

                           if ( handleExecutionFailure( reactorManager, project, error, binding, buildStartTime ) )
                           {
                               throw error;
                           }
                        }
                        else
                        {
                            ProjectBuildFailureException error = new ProjectBuildFailureException(
                                                                                                  project.getId(),
                                                                                                  binding,
                                                                                                  e );

                           dispatcher.dispatchError( event, target, error );

                           if ( handleExecutionFailure( reactorManager, project, error, binding, buildStartTime ) )
                           {
                               throw error;
                           }
                        }
                    }
                }

                if ( currentPhase != null )
                {
                    dispatcher.dispatchEnd( MavenEvents.PHASE_EXECUTION, currentPhase );
                }
            }
            finally
            {
                session.setCurrentProject( null );
                restoreLookupRealm( oldLookupRealm );
            }


            reactorManager.registerBuildSuccess(
                project,
                System.currentTimeMillis() - buildStartTime );

            dispatcher.dispatchEnd(
                event,
                target );
        }
        else
        {
            line();

            getLogger().info( "SKIPPING " + project.getName() );

            getLogger().info( "  " + segment );

            getLogger().info( "This project has been banned from further executions due to previous failures." );

            line();
        }
    }

    /**
     * Since each project can have its own {@link ClassRealm} instance that inherits
     * from the core Maven realm, and contains the specific build-extension
     * components referenced in that project, the lookup realms must be managed for
     * each project that's used to fire off a mojo execution. This helps ensure
     * that unsafe {@link PlexusContainer#lookup(String)} and related calls will
     * have access to these build-extension components.
     * <br />
     * This method simply restores the original Maven-core lookup realm when a
     * project-specific realm is in use.
     */
    private void restoreLookupRealm( ClassRealm oldLookupRealm )
    {
        if ( oldLookupRealm != null )
        {
            container.setLookupRealm( oldLookupRealm );
        }
    }

    /**
     * Since each project can have its own {@link ClassRealm} instance that inherits
     * from the core Maven realm, and contains the specific build-extension
     * components referenced in that project, the lookup realms must be managed for
     * each project that's used to fire off a mojo execution. This helps ensure
     * that unsafe {@link PlexusContainer#lookup(String)} and related calls will
     * have access to these build-extension components.
     * <br />
     * This method is meant to find a project-specific realm, if one exists, for
     * use as the lookup realm for unsafe component lookups, using {@link PlexusContainer#setLookupRealm(ClassRealm)}.
     */
    private ClassRealm setProjectLookupRealm( MavenSession session,
                                              MavenProject rootProject )
        throws LifecycleExecutionException
    {
        ClassRealm projectRealm = session.getRealmManager().getProjectRealm( rootProject.getGroupId(), rootProject.getArtifactId(), rootProject.getVersion() );

        if ( projectRealm != null )
        {
            return container.setLookupRealm( projectRealm );
        }

        return container.getLookupRealm();
    }

    /**
     * Retrieves the build plan for the current project, given the specified list of tasks. This build plan will consist
     * of MojoBindings, each fully configured to execute, which enables us to enumerate the full build plan to the debug
     * log-level, complete with the configuration each mojo will use.
     */
    private List getLifecycleBindings( final List tasks,
                                       final MavenProject project,
                                       final MavenSession session,
                                       final String targetDescription )
        throws LifecycleExecutionException
    {
        List mojoBindings;
        try
        {
            BuildPlan plan = buildPlanner.constructBuildPlan( tasks, project, session, false );

            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "\n\nOur build plan is:\n" + BuildPlanUtils.listBuildPlan(
                        plan,
                        false ) + "\n\nfor task-segment: " + targetDescription );
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

    /**
     * Lookup the plugin containing the referenced mojo, validate that it is
     * allowed to execute in the current environment (according to whether
     * it's a direct-invocation-only or aggregator mojo, and the allowAggregators
     * flag), and execute the mojo. If any of these steps fails, this method will
     * consult with the {@link ReactorManager} to determine whether the build
     * should be stopped.
     * <br />
     * <b>NOTE:</b> If the binding is an aggregator mojo, and the specified project
     * is not the root project of the reactor (using {@link ReactorManager#getTopLevelProject()},
     * then print a DEBUG message and skip that execution.
     */
    private void executeGoalAndHandleFailures( final MojoBinding mojoBinding,
                                               final MavenSession session,
                                               final EventDispatcher dispatcher,
                                               final String event,
                                               final ReactorManager rm,
                                               final long buildStartTime,
                                               final String target,
                                               boolean allowAggregators )
        throws LifecycleExecutionException, MojoFailureException
    {
        MavenProject project = session.getCurrentProject();

        // NEW: Since the MojoBinding instances are configured when the build plan is constructed,
        // all that remains to be done here is to load the PluginDescriptor, construct a MojoExecution
        // instance, and call PluginManager.executeMojo( execution ). The MojoExecutor is constructed
        // using both the PluginDescriptor and the MojoBinding.
        try
        {
            PluginDescriptor pluginDescriptor;
            try
            {
                pluginDescriptor = pluginLoader.loadPlugin(
                    mojoBinding,
                    project,
                    session );
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

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( mojoBinding.getGoal() );

            // TODO: Figure out how to make this logic produce the same result when the binding is in a module.
            // At times, the module will build in isolation, in which case this logic would allow the aggregator to run.
            // In other cases, the module will be part of a reactor build, and the aggregator won't run, because it's not
            // bound to the root project.
//            if ( mojoDescriptor.isAggregator() && ( project != rm.getTopLevelProject() ) )
//            {
//                getLogger().debug( "Skipping mojo execution: " + MojoBindingUtils.toString( mojoBinding ) + "\nfor project: " + project.getId() + "\n\nIt is an aggregator mojo, and the current project is not the root project for the reactor." );
//                return;
//            }

            validateMojoExecution( mojoBinding, mojoDescriptor, project, allowAggregators );

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
        catch ( LifecycleExecutionException e )
        {
            dispatcher.dispatchError( event, target, e );

            if ( handleExecutionFailure( rm, project, e, mojoBinding, buildStartTime ) )
            {
                throw e;
            }
        }
    }

    /**
     * Verify that the specified {@link MojoBinding} is legal for execution under
     * the current circumstances. Currently, this mainly checks that aggregator
     * mojos and direct-invocation-only mojos are not bound to lifecycle phases.
     * <br/>
     * If an invalid mojo is detected, and it is brought in via the user's POM
     * (this will be checked using {@link MojoBinding#POM_ORIGIN} and {@link MojoBinding#getOrigin()}),
     * then a {@link LifecycleExecutionException} will be thrown. Otherwise, the mojo
     * was brought in via a lifecycle mapping or overlay, or as part of a forked execution.
     * In these cases, the error will be logged to the console, using the ERROR log-level (since the
     * user cannot fix this sort of problem easily).
     */
    private void validateMojoExecution( MojoBinding mojoBinding,
                                        MojoDescriptor mojoDescriptor,
                                        MavenProject project,
                                        boolean allowAggregators )
        throws LifecycleExecutionException
    {
        if ( mojoDescriptor.isAggregator() && !allowAggregators )
        {
            if ( MojoBinding.POM_ORIGIN.equals( mojoBinding.getOrigin() ) )
            {
                StringBuffer buffer = new StringBuffer();
                buffer.append( "\n\nDEPRECATED: Binding aggregator mojos to lifecycle phases in the POM is considered dangerous." );
                buffer.append( "\nThis feature has been deprecated. Please adjust your POM files accordingly." );
                buffer.append( "\n\nOffending mojo:\n\n" );
                buffer.append( MojoBindingUtils.toString( mojoBinding ) );
                buffer.append( "\n\nProject: " ).append( project.getId() );
                buffer.append( "\nPOM File: " ).append( String.valueOf( project.getFile() ) );
                buffer.append( "\n" );

                getLogger().warn( buffer.toString() );
            }
            else
            {
                StringBuffer buffer = new StringBuffer();
                buffer.append( "\n\nDEPRECATED: An aggregator mojo has been bound to your project's build lifecycle." );
                buffer.append( "\nThis feature is dangerous, and has been deprecated." );
                buffer.append( "\n\nOffending mojo:\n\n" );
                buffer.append( MojoBindingUtils.toString( mojoBinding ) );
                buffer.append( "\n\nDirect binding of aggregator mojos to the lifecycle is not allowed, but this binding was not configured from within in your POM." );
                buffer.append( "\n\nIts origin was: " ).append( mojoBinding.getOrigin() );
                if ( mojoBinding.getOriginDescription() != null )
                {
                    buffer.append( " (" ).append( mojoBinding.getOriginDescription() ).append( ")" );
                }

                buffer.append( "\n" );

                getLogger().warn( buffer.toString() );
            }
        }
        else if ( mojoDescriptor.isDirectInvocationOnly() && !MojoBinding.DIRECT_INVOCATION_ORIGIN.equals( mojoBinding.getOrigin() ) )
        {
            if ( MojoBinding.POM_ORIGIN.equals( mojoBinding.getOrigin() ) )
            {
                throw new LifecycleExecutionException( "Mojo:\n\n" + MojoBindingUtils.toString( mojoBinding ) + "\n\ncan only be invoked directly by the user. Binding it to lifecycle phases in the POM is not allowed.", project );
            }
            else
            {
                StringBuffer buffer = new StringBuffer();
                buffer.append( "\n\nSKIPPING execution of mojo:\n\n" ).append( MojoBindingUtils.toString( mojoBinding ) );
                buffer.append( "\n\nIt specifies direct-invocation only, but has been bound to the build lifecycle." );
                buffer.append( "\n\nDirect-invocation mojos can only be called by the user. This binding was not configured from within in your POM." );
                buffer.append( "\n\nIts origin was: " ).append( mojoBinding.getOrigin() );
                if ( mojoBinding.getOriginDescription() != null )
                {
                    buffer.append( " (" ).append( mojoBinding.getOriginDescription() ).append( ")" );
                }

                buffer.append( "\n" );

                getLogger().error( buffer.toString() );
            }
        }
    }

    /**
     * In the event that an error occurs during executeGoalAndHandleFailure(..),
     * this method is called to handle logging the error in the {@link ReactorManager},
     * then determining (again, from the reactor-manager) whether to stop the build.
     *
     * @return true if the build should stop, false otherwise.
     */
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

    /**
     * {@inheritDoc}
     */
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
                catch ( InvalidPluginException e )
                {
                    return new TaskValidationResult(
                        task,
                        e.getMessage(), e );
                }
            }
        }

        return new TaskValidationResult();
    }

    /**
     * Split up the list of goals from {@link MavenSession#getGoals()} according
     * to aggregation needs. Each adjacent goal in the list is included in a single
     * task segment. When the next goal references a different type of mojo or
     * lifecycle phase (eg. previous goal wasn't an aggregator, but next one is...or the reverse),
     * a new task segment is started and the new goal is added to that.
     *
     * @return the list of task-segments, each flagged according to aggregation needs.
     */
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

    /**
     * Retrieve the {@link MojoDescriptor} that corresponds to a given direct mojo
     * invocation. This is used during the fail-fast method isTaskValid(..), and also
     * during task-segmentation, to allow the lifecycle executor to determine whether
     * the mojo is an aggregator.
     */
    private MojoDescriptor getMojoDescriptorForDirectInvocation( String task,
                                                                 MavenSession session,
                                                                 MavenProject project )
        throws LifecycleSpecificationException, PluginLoaderException, LifecycleLoaderException,
        InvalidPluginException
    {
        // we don't need to include report configuration here, since we're just looking for
        // an @aggregator flag...
        MojoBinding binding = mojoBindingFactory.parseMojoBinding(
            task,
            project,
            session,
            true );

        PluginDescriptor descriptor = pluginLoader.loadPlugin(
            binding,
            project,
            session );

        MojoDescriptor mojoDescriptor = descriptor.getMojo( binding.getGoal() );

        if ( mojoDescriptor == null )
        {
            throw new InvalidPluginException( "Plugin: " + descriptor.getId() + " does not contain referenced mojo: " + binding.getGoal() );
        }

        return mojoDescriptor;
    }

    protected void line()
    {
        getLogger().info( "------------------------------------------------------------------------" );
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

        @Override
        public String toString()
        {
            StringBuffer message = new StringBuffer();

            message.append( "task-segment: [" );

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
    
    public List getLifecycles()
    {
        return lifecycleBindingManager.getLifecycles();
    }
}
