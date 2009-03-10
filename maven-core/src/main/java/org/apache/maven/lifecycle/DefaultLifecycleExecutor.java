package org.apache.maven.lifecycle;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.model.LifecycleBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.PluginConfigurationException;
import org.apache.maven.plugin.PluginLoaderException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.PluginVersionNotFoundException;
import org.apache.maven.plugin.PluginVersionResolutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Phase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Jason van Zyl
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Component(role = LifecycleExecutor.class)
public class DefaultLifecycleExecutor
    implements LifecycleExecutor
{
    @Requirement
    private Logger logger;
    
    @Requirement
    private PluginManager pluginManager;

    private List<Lifecycle> lifecycles;

    private List defaultReports;

    private Map phaseToLifecycleMap;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public static boolean isValidPhaseName( final String phaseName )
    {
        LifecycleBindings test = new LifecycleBindings();
        for ( Iterator it = test.getBindingList().iterator(); it.hasNext(); )
        {
            LifecycleBinding binding = (LifecycleBinding) it.next();

            if ( binding.getPhaseNamesInOrder().contains( phaseName ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public TaskValidationResult isTaskValid( String task, MavenSession session, MavenProject rootProject )
    {
        //jvz: have to investigate plugins that are run without a root project or using Maven in reactor mode. Looks like we
        // were never validating these anyway if you look in the execution code.

        if ( rootProject != null )
        {
            if ( !isValidPhaseName( task ) )
            {
                // definitely a CLI goal, can use prefix
                try
                {
                    getMojoDescriptorForDirectInvocation( task, session, rootProject );

                    return new TaskValidationResult();
                }
                catch ( PluginLoaderException e )
                {
                    // TODO: shouldn't hit this, investigate using the same resolution logic as
                    // others for plugins in the reactor

                    return new TaskValidationResult( task, "Cannot find mojo descriptor for: \'" + task + "\' - Treating as non-aggregator.", e );
                }
                catch ( LifecycleSpecificationException e )
                {
                    String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or"
                        + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

                    return new TaskValidationResult( task, message, e );
                }
                catch ( LifecycleLoaderException e )
                {
                    String message = "Failed to load one or more lifecycle definitions which may contain task: '" + task + "'.";

                    return new TaskValidationResult( task, message, e );
                }
                catch ( InvalidPluginException e )
                {
                    return new TaskValidationResult( task, e.getMessage(), e );
                }
            }
        }

        return new TaskValidationResult();
    }

    /**
     * Retrieve the {@link MojoDescriptor} that corresponds to a given direct mojo invocation. This
     * is used during the fail-fast method isTaskValid(..), and also during task-segmentation, to
     * allow the lifecycle executor to determine whether the mojo is an aggregator.
     * 
     * @throws PluginLoaderException
     */
    private MojoDescriptor getMojoDescriptorForDirectInvocation( String task, MavenSession session, MavenProject project )
        throws InvalidPluginException, LifecycleSpecificationException, LifecycleLoaderException, PluginLoaderException
    {        
        MojoDescriptor descriptor;
        
        try
        {
            descriptor = getMojoDescriptor( task, session, project );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new LifecycleSpecificationException( "Cannot find the specified goal.", e );
        }
                
        if ( descriptor == null )
        {
            throw new InvalidPluginException( "Plugin: " + descriptor.getId() + " does not contain referenced mojo: " + descriptor.getGoal() );
        }

        return descriptor;
    }

    /**
     * Execute a task. Each task may be a phase in the lifecycle or the execution of a mojo.
     * 
     * @param session
     * @param rm
     * @param dispatcher
     */
    public void execute( MavenSession session, ReactorManager rm, EventDispatcher dispatcher )
        throws BuildFailureException, LifecycleExecutionException
    {
        // TODO: This is dangerous, particularly when it's just a collection of loose-leaf projects being built
        // within the same reactor (using an inclusion pattern to gather them up)...
        MavenProject rootProject = rm.getTopLevelProject();

        List goals = session.getGoals();

        if ( goals.isEmpty() && rootProject != null )
        {
            String goal = rootProject.getDefaultGoal();

            if ( goal != null )
            {
                goals = Collections.singletonList( goal );
            }
        }

        if ( goals.isEmpty() )
        {
            throw new BuildFailureException( "\n\nYou must specify at least one goal. Try 'mvn install' to build or 'mvn --help' for options \nSee http://maven.apache.org for more information.\n\n" );
        }

        List taskSegments = segmentTaskListByAggregationNeeds( goals, session, rootProject );

        executeTaskSegments( taskSegments, rm, session, rootProject, dispatcher );
    }

    private void executeTaskSegments( List taskSegments, ReactorManager rm, MavenSession session, MavenProject rootProject, EventDispatcher dispatcher )
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

                    logger.info( "Building " + rootProject.getName() );

                    logger.info( "  " + segment );

                    line();

                    // !! This is ripe for refactoring to an aspect.
                    // Event monitoring.
                    String event = MavenEvents.PROJECT_EXECUTION;

                    long buildStartTime = System.currentTimeMillis();

                    String target = rootProject.getId() + " ( " + segment + " )";

                    dispatcher.dispatchStart( event, target );

                    try
                    {
                        session.setCurrentProject( rootProject );

                        // only call once, with the top-level project (assumed to be provided as a parameter)...
                        for ( Iterator goalIterator = segment.getTasks().iterator(); goalIterator.hasNext(); )
                        {
                            String task = (String) goalIterator.next();

                            executeGoalAndHandleFailures( task, session, rootProject, dispatcher, event, rm, buildStartTime, target );
                        }

                        rm.registerBuildSuccess( rootProject, System.currentTimeMillis() - buildStartTime );

                    }
                    finally
                    {
                        session.setCurrentProject( null );
                    }

                    dispatcher.dispatchEnd( event, target );
                }
                else
                {
                    line();

                    logger.info( "SKIPPING " + rootProject.getName() );

                    logger.info( "  " + segment );

                    logger.info( "This project has been banned from further executions due to previous failures." );

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

                        logger.info( "Building " + currentProject.getName() );

                        logger.info( "  " + segment );

                        line();

                        // !! This is ripe for refactoring to an aspect.
                        // Event monitoring.
                        String event = MavenEvents.PROJECT_EXECUTION;

                        long buildStartTime = System.currentTimeMillis();

                        String target = currentProject.getId() + " ( " + segment + " )";
                        dispatcher.dispatchStart( event, target );

                        try
                        {
                            session.setCurrentProject( currentProject );

                            for ( Iterator goalIterator = segment.getTasks().iterator(); goalIterator.hasNext(); )
                            {
                                String task = (String) goalIterator.next();

                                executeGoalAndHandleFailures( task, session, currentProject, dispatcher, event, rm, buildStartTime, target );
                            }

                        }
                        finally
                        {
                            session.setCurrentProject( null );
                        }

                        rm.registerBuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );

                        dispatcher.dispatchEnd( event, target );
                    }
                    else
                    {
                        line();

                        logger.info( "SKIPPING " + currentProject.getName() );

                        logger.info( "  " + segment );

                        logger.info( "This project has been banned from further executions due to previous failures." );

                        line();
                    }
                }
            }
        }
    }

    private void executeGoalAndHandleFailures( String task, MavenSession session, MavenProject project, EventDispatcher dispatcher, String event, ReactorManager rm, long buildStartTime, String target )
        throws BuildFailureException, LifecycleExecutionException
    {
        try
        {
            executeGoal( task, session, project );
        }
        catch ( LifecycleExecutionException e )
        {
            dispatcher.dispatchError( event, target, e );

            if ( handleExecutionFailure( rm, project, e, task, buildStartTime ) )
            {
                throw e;
            }
        }
        catch ( BuildFailureException e )
        {
            dispatcher.dispatchError( event, target, e );

            if ( handleExecutionFailure( rm, project, e, task, buildStartTime ) )
            {
                throw e;
            }
        }
    }

    private boolean handleExecutionFailure( ReactorManager rm, MavenProject project, Exception e, String task, long buildStartTime )
    {
        rm.registerBuildFailure( project, e, task, System.currentTimeMillis() - buildStartTime );

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

    private List segmentTaskListByAggregationNeeds( List tasks, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException
    {
        List segments = new ArrayList();

        if ( project != null )
        {

            TaskSegment currentSegment = null;
            for ( Iterator it = tasks.iterator(); it.hasNext(); )
            {
                String task = (String) it.next();

                // if it's a phase, then we don't need to check whether it's an aggregator.
                // simply add it to the current task partition.
                if ( getPhaseToLifecycleMap().containsKey( task ) )
                {
                    if ( currentSegment != null && currentSegment.aggregate() )
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
                    MojoDescriptor mojo = getMojoDescriptor( task, session, project );

                    // if the mojo descriptor was found, determine aggregator status according to:
                    // 1. whether the mojo declares itself an aggregator
                    // 2. whether the mojo DOES NOT require a project to function (implicitly avoid reactor)
                    if ( mojo != null && ( mojo.isAggregator() || !mojo.isProjectRequired() ) )
                    {
                        if ( currentSegment != null && !currentSegment.aggregate() )
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
                        if ( currentSegment != null && currentSegment.aggregate() )
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

    private void executeGoal( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException
    {
        try
        {
            Stack forkEntryPoints = new Stack();
            if ( getPhaseToLifecycleMap().containsKey( task ) )
            {
                Lifecycle lifecycle = getLifecycleForPhase( task );

                // we have a lifecycle phase, so lets bind all the necessary goals

                Map lifecycleMappings = constructLifecycleMappings( session, task, project, lifecycle );

                executeGoalWithLifecycle( task, forkEntryPoints, session, lifecycleMappings, project, lifecycle );
            }
            else
            {
                executeStandaloneGoal( task, forkEntryPoints, session, project );
            }
        }
        catch ( PluginNotFoundException e )
        {
            throw new BuildFailureException( "A required plugin was not found: " + e.getMessage(), e );
        }
    }

    private void executeGoalWithLifecycle( String task, Stack forkEntryPoints, MavenSession session, Map lifecycleMappings, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        List goals = processGoalChain( task, lifecycleMappings, lifecycle );

        if ( !goals.isEmpty() )
        {
            executeGoals( goals, forkEntryPoints, session, project );
        }
        else
        {
            logger.info( "No goals needed for project - skipping" );
        }
    }

    private void executeStandaloneGoal( String task, Stack forkEntryPoints, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        // guaranteed to come from the CLI and not be part of a phase
        MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session, project );
        executeGoals( Collections.singletonList( new MojoExecution( mojoDescriptor ) ), forkEntryPoints, session, project );
    }

    private void executeGoals( List goals, Stack forkEntryPoints, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        for ( Iterator i = goals.iterator(); i.hasNext(); )
        {
            MojoExecution mojoExecution = (MojoExecution) i.next();
            
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            if ( mojoDescriptor.getExecutePhase() != null || mojoDescriptor.getExecuteGoal() != null )
            {
                forkEntryPoints.push( mojoDescriptor );

                forkLifecycle( mojoDescriptor, forkEntryPoints, session, project );

                forkEntryPoints.pop();
            }

            if ( mojoDescriptor.isRequiresReports() )
            {
                List reports = getReports( project, forkEntryPoints, mojoExecution, session );

                mojoExecution.setReports( reports );

                for ( Iterator j = mojoExecution.getForkedExecutions().iterator(); j.hasNext(); )
                {
                    MojoExecution forkedExecution = (MojoExecution) j.next();
                    MojoDescriptor descriptor = forkedExecution.getMojoDescriptor();

                    if ( descriptor.getExecutePhase() != null )
                    {
                        forkEntryPoints.push( descriptor );

                        forkLifecycle( descriptor, forkEntryPoints, session, project );

                        forkEntryPoints.pop();
                    }
                }
            }

            try
            {
                pluginManager.executeMojo( project, mojoExecution, session );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager executing goal '" + mojoDescriptor.getId() + "': " + e.getMessage(), e );
            }
            catch ( MojoFailureException e )
            {
                throw new BuildFailureException( e.getMessage(), e );
            }
            catch ( PluginConfigurationException e )
            {
                throw new LifecycleExecutionException( e.getMessage(), e );
            }
        }
    }

    private List getReports( MavenProject project, Stack forkEntryPoints, MojoExecution mojoExecution, MavenSession session )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        List reportPlugins = project.getReportPlugins();

        if ( project.getModel().getReports() != null )
        {
            logger.error( "Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
        }

        if ( project.getReporting() == null || !project.getReporting().isExcludeDefaults() )
        {
            if ( reportPlugins == null )
            {
                reportPlugins = new ArrayList();
            }
            else
            {
                reportPlugins = new ArrayList( reportPlugins );
            }

            for ( Iterator i = defaultReports.iterator(); i.hasNext(); )
            {
                String report = (String) i.next();

                StringTokenizer tok = new StringTokenizer( report, ":" );
                int count = tok.countTokens();
                if ( count != 2 && count != 3 )
                {
                    logger.warn( "Invalid default report ignored: '" + report + "' (must be groupId:artifactId[:version])" );
                }
                else
                {
                    String groupId = tok.nextToken();
                    String artifactId = tok.nextToken();
                    String version = tok.hasMoreTokens() ? tok.nextToken() : null;

                    boolean found = false;
                    for ( Iterator j = reportPlugins.iterator(); j.hasNext() && !found; )
                    {
                        ReportPlugin reportPlugin = (ReportPlugin) j.next();
                        if ( reportPlugin.getGroupId().equals( groupId ) && reportPlugin.getArtifactId().equals( artifactId ) )
                        {
                            found = true;
                        }
                    }

                    if ( !found )
                    {
                        ReportPlugin reportPlugin = new ReportPlugin();
                        reportPlugin.setGroupId( groupId );
                        reportPlugin.setArtifactId( artifactId );
                        reportPlugin.setVersion( version );
                        reportPlugins.add( reportPlugin );
                    }
                }
            }
        }

        List reports = new ArrayList();
        if ( reportPlugins != null )
        {
            for ( Iterator it = reportPlugins.iterator(); it.hasNext(); )
            {
                ReportPlugin reportPlugin = (ReportPlugin) it.next();

                List reportSets = reportPlugin.getReportSets();

                if ( reportSets == null || reportSets.isEmpty() )
                {
                    reports.addAll( getReports( reportPlugin, forkEntryPoints, null, project, session, mojoExecution ) );
                }
                else
                {
                    for ( Iterator j = reportSets.iterator(); j.hasNext(); )
                    {
                        ReportSet reportSet = (ReportSet) j.next();

                        reports.addAll( getReports( reportPlugin, forkEntryPoints, reportSet, project, session, mojoExecution ) );
                    }
                }
            }
        }
        return reports;
    }

    private List getReports( ReportPlugin reportPlugin, Stack forkEntryPoints, ReportSet reportSet, MavenProject project, MavenSession session, MojoExecution mojoExecution )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        PluginDescriptor pluginDescriptor = loadReport( reportPlugin, project, session );

        List reports = new ArrayList();

        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) i.next();

            if ( forkEntryPoints.contains( mojoDescriptor ) )
            {
                logger.debug( "Omitting report: " + mojoDescriptor.getFullGoalName() + " from reports list. It initiated part of the fork currently executing." );
                continue;
            }

            // TODO: check ID is correct for reports
            // if the POM configured no reports, give all from plugin
            if ( reportSet == null || reportSet.getReports().contains( mojoDescriptor.getGoal() ) )
            {
                String id = null;
                if ( reportSet != null )
                {
                    id = reportSet.getId();
                }

                MojoExecution reportExecution = new MojoExecution( mojoDescriptor, id );

                try
                {
                    MavenReport reportMojo = pluginManager.getReport( project, reportExecution, session );

                    // Comes back null if it was a plugin, not a report - these are mojos in the reporting plugins that are not reports
                    if ( reportMojo != null )
                    {
                        reports.add( reportMojo );

                        mojoExecution.addMojoExecution( reportExecution );
                    }
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException( "Error getting reports from the plugin '" + reportPlugin.getKey() + "': " + e.getMessage(), e );
                }
                catch ( PluginConfigurationException e )
                {
                    throw new LifecycleExecutionException( "Error getting reports from the plugin '" + reportPlugin.getKey() + "'", e );
                }
                catch ( ArtifactNotFoundException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
                catch ( ArtifactResolutionException e )
                {
                    throw new LifecycleExecutionException( e.getMessage(), e );
                }
            }
        }
        return reports;
    }

    private void forkLifecycle( MojoDescriptor mojoDescriptor, Stack ancestorLifecycleForkers, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        logger.info( "Preparing " + pluginDescriptor.getGoalPrefix() + ":" + mojoDescriptor.getGoal() );

        if ( mojoDescriptor.isAggregator() )
        {
            for ( Iterator i = session.getSortedProjects().iterator(); i.hasNext(); )
            {
                MavenProject reactorProject = (MavenProject) i.next();

                line();

                logger.info( "Building " + reactorProject.getName() );

                line();

                forkProjectLifecycle( mojoDescriptor, ancestorLifecycleForkers, session, reactorProject );
            }
        }
        else
        {
            forkProjectLifecycle( mojoDescriptor, ancestorLifecycleForkers, session, project );
        }
    }

    private void forkProjectLifecycle( MojoDescriptor mojoDescriptor, Stack forkEntryPoints, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        forkEntryPoints.push( mojoDescriptor );

        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String targetPhase = mojoDescriptor.getExecutePhase();

        Map lifecycleMappings = null;
        if ( targetPhase != null )
        {
            Lifecycle lifecycle = getLifecycleForPhase( targetPhase );

            // Create new lifecycle
            lifecycleMappings = constructLifecycleMappings( session, targetPhase, project, lifecycle );

            String executeLifecycle = mojoDescriptor.getExecuteLifecycle();
            if ( executeLifecycle != null )
            {
                org.apache.maven.plugin.lifecycle.Lifecycle lifecycleOverlay;
                try
                {
                    lifecycleOverlay = pluginDescriptor.getLifecycleMapping( executeLifecycle );
                }
                catch ( IOException e )
                {
                    throw new LifecycleExecutionException( "Unable to read lifecycle mapping file: " + e.getMessage(), e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new LifecycleExecutionException( "Unable to parse lifecycle mapping file: " + e.getMessage(), e );
                }

                if ( lifecycleOverlay == null )
                {
                    throw new LifecycleExecutionException( "Lifecycle '" + executeLifecycle + "' not found in plugin" );
                }

                for ( Iterator i = lifecycleOverlay.getPhases().iterator(); i.hasNext(); )
                {
                    Phase phase = (Phase) i.next();
                    for ( Iterator j = phase.getExecutions().iterator(); j.hasNext(); )
                    {
                        Execution exec = (Execution) j.next();

                        for ( Iterator k = exec.getGoals().iterator(); k.hasNext(); )
                        {
                            String goal = (String) k.next();

                            PluginDescriptor lifecyclePluginDescriptor;
                            String lifecycleGoal;

                            // Here we are looking to see if we have a mojo from an external plugin.
                            // If we do then we need to lookup the plugin descriptor for the externally
                            // referenced plugin so that we can overly the execution into the lifecycle.
                            // An example of this is the corbertura plugin that needs to call the surefire
                            // plugin in forking mode.
                            //
                            //<phase>
                            //  <id>test</id>
                            //  <executions>
                            //    <execution>
                            //      <goals>
                            //        <goal>org.apache.maven.plugins:maven-surefire-plugin:test</goal>
                            //      </goals>
                            //      <configuration>
                            //        <classesDirectory>${project.build.directory}/generated-classes/cobertura</classesDirectory>
                            //        <ignoreFailures>true</ignoreFailures>
                            //        <forkMode>once</forkMode>
                            //      </configuration>
                            //    </execution>
                            //  </executions>
                            //</phase>

                            // ----------------------------------------------------------------------
                            //
                            // ----------------------------------------------------------------------

                            if ( goal.indexOf( ":" ) > 0 )
                            {
                                String[] s = StringUtils.split( goal, ":" );

                                String groupId = s[0];
                                String artifactId = s[1];
                                lifecycleGoal = s[2];

                                Plugin plugin = new Plugin();
                                plugin.setGroupId( groupId );
                                plugin.setArtifactId( artifactId );

                                lifecyclePluginDescriptor = loadPlugin( plugin, project, session );
                            }
                            else
                            {
                                lifecyclePluginDescriptor = pluginDescriptor;
                                lifecycleGoal = goal;
                            }

                            Xpp3Dom configuration = (Xpp3Dom) exec.getConfiguration();
                            if ( phase.getConfiguration() != null )
                            {
                                configuration = Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ), configuration );
                            }

                            MojoDescriptor desc = getMojoDescriptor( lifecyclePluginDescriptor, lifecycleGoal );
                            MojoExecution mojoExecution = new MojoExecution( desc, configuration );
                            addToLifecycleMappings( lifecycleMappings, phase.getId(), mojoExecution, session.getSettings() );
                        }
                    }

                    if ( phase.getConfiguration() != null )
                    {
                        // Merge in general configuration for a phase.
                        // TODO: this is all kind of backwards from the POMM. Let's align it all under 2.1.
                        //   We should create a new lifecycle executor for modelVersion >5.0.0
                        for ( Iterator j = lifecycleMappings.values().iterator(); j.hasNext(); )
                        {
                            List tasks = (List) j.next();

                            for ( Iterator k = tasks.iterator(); k.hasNext(); )
                            {
                                MojoExecution exec = (MojoExecution) k.next();

                                Xpp3Dom configuration = Xpp3Dom.mergeXpp3Dom( new Xpp3Dom( (Xpp3Dom) phase.getConfiguration() ), exec.getConfiguration() );

                                exec.setConfiguration( configuration );
                            }
                        }
                    }

                }
            }

            removeFromLifecycle( forkEntryPoints, lifecycleMappings );
        }

        MavenProject executionProject = new MavenProject( project );
        if ( targetPhase != null )
        {
            Lifecycle lifecycle = getLifecycleForPhase( targetPhase );

            executeGoalWithLifecycle( targetPhase, forkEntryPoints, session, lifecycleMappings, executionProject, lifecycle );
        }
        else
        {
            String goal = mojoDescriptor.getExecuteGoal();
            MojoDescriptor desc = getMojoDescriptor( pluginDescriptor, goal );
            executeGoals( Collections.singletonList( new MojoExecution( desc ) ), forkEntryPoints, session, executionProject );
        }
        project.setExecutionProject( executionProject );
    }

    private Lifecycle getLifecycleForPhase( String phase )
        throws BuildFailureException, LifecycleExecutionException
    {
        Lifecycle lifecycle = (Lifecycle) getPhaseToLifecycleMap().get( phase );

        if ( lifecycle == null )
        {
            throw new BuildFailureException( "Unable to find lifecycle for phase '" + phase + "'" );
        }
        return lifecycle;
    }

    MojoDescriptor getMojoDescriptor( PluginDescriptor pluginDescriptor, String goal )
        throws LifecycleExecutionException
    {
        MojoDescriptor desc = pluginDescriptor.getMojo( goal );

        if ( desc == null )
        {
            String message = "Required goal '" + goal + "' not found in plugin '" + pluginDescriptor.getGoalPrefix() + "'";
            int index = goal.indexOf( ':' );
            if ( index >= 0 )
            {
                String prefix = goal.substring( index + 1 );
                if ( prefix.equals( pluginDescriptor.getGoalPrefix() ) )
                {
                    message = message + " (goals should not be prefixed - try '" + prefix + "')";
                }
            }
            throw new LifecycleExecutionException( message );
        }
        return desc;
    }

    private void removeFromLifecycle( Stack lifecycleForkers, Map lifecycleMappings )
    {
        for ( Iterator lifecycleIterator = lifecycleMappings.values().iterator(); lifecycleIterator.hasNext(); )
        {
            List tasks = (List) lifecycleIterator.next();

            for ( Iterator taskIterator = tasks.iterator(); taskIterator.hasNext(); )
            {
                MojoExecution execution = (MojoExecution) taskIterator.next();

                if ( lifecycleForkers.contains( execution.getMojoDescriptor() ) )
                {
                    taskIterator.remove();
                    logger.warn( "Removing: " + execution.getMojoDescriptor().getGoal() + " from forked lifecycle, to prevent recursive invocation." );
                }
            }
        }
    }

    private Map constructLifecycleMappings( MavenSession session, String selectedPhase, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        // first, bind those associated with the packaging
        Map lifecycleMappings = bindLifecycleForPackaging( session, selectedPhase, project, lifecycle );

        // next, loop over plugins and for any that have a phase, bind it
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            bindPluginToLifecycle( plugin, session, lifecycleMappings, project );
        }

        return lifecycleMappings;
    }

    private Map bindLifecycleForPackaging( MavenSession session, String selectedPhase, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        Map mappings = findMappingsForLifecycle( session, project, lifecycle );

        List optionalMojos = findOptionalMojosForLifecycle( session, project, lifecycle );

        Map lifecycleMappings = new HashMap();

        for ( Iterator i = lifecycle.getPhases().iterator(); i.hasNext(); )
        {
            String phase = (String) i.next();

            String phaseTasks = (String) mappings.get( phase );

            if ( phaseTasks != null )
            {
                for ( StringTokenizer tok = new StringTokenizer( phaseTasks, "," ); tok.hasMoreTokens(); )
                {
                    String goal = tok.nextToken().trim();

                    // Not from the CLI, don't use prefix
                    MojoDescriptor mojoDescriptor = getMojoDescriptor( goal, session, project );

                    if ( mojoDescriptor == null )
                    {
                        continue;
                    }

                    if ( mojoDescriptor.isDirectInvocationOnly() )
                    {
                        throw new LifecycleExecutionException( "Mojo: \'" + goal + "\' requires direct invocation. It cannot be used as part of lifecycle: \'" + project.getPackaging() + "\'." );
                    }

                    addToLifecycleMappings( lifecycleMappings, phase, new MojoExecution( mojoDescriptor ), session.getSettings() );
                }
            }

            if ( phase.equals( selectedPhase ) )
            {
                break;
            }
        }

        return lifecycleMappings;
    }

    private Map findMappingsForLifecycle( MavenSession session, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        Map mappings = null;

        LifecycleMapping m;

        Map defaultMappings = lifecycle.getDefaultPhases();

        if ( mappings == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                mappings = m.getPhases( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                if ( defaultMappings == null )
                {
                    throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging + "\'.", e );
                }
            }
        }

        if ( mappings == null )
        {
            if ( defaultMappings == null )
            {
                throw new LifecycleExecutionException( "Cannot find lifecycle mapping for packaging: \'" + packaging + "\', and there is no default" );
            }
            else
            {
                mappings = defaultMappings;
            }
        }

        return mappings;
    }

    private List findOptionalMojosForLifecycle( MavenSession session, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        List optionalMojos = null;

        LifecycleMapping m;

        if ( optionalMojos == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
                optionalMojos = m.getOptionalMojos( lifecycle.getId() );
            }
            catch ( ComponentLookupException e )
            {
                logger.debug( "Error looking up lifecycle mapping to retrieve optional mojos. Lifecycle ID: " + lifecycle.getId() + ". Error: " + e.getMessage(), e );
            }
        }

        if ( optionalMojos == null )
        {
            optionalMojos = Collections.EMPTY_LIST;
        }

        return optionalMojos;
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a phase in the
     * lifecycle and if it does place it at the end of the list of goals to execute for that given
     * phase.
     * 
     * @param project
     * @param session
     * @throws PluginVersionNotFoundException
     * @throws PluginManagerException
     * @throws InvalidPluginException
     * @throws PluginVersionResolutionException
     * @throws ArtifactNotFoundException
     * @throws ArtifactResolutionException
     */
    private void bindPluginToLifecycle( Plugin plugin, MavenSession session, Map phaseMap, MavenProject project )
        throws LifecycleExecutionException
    {
        Settings settings = session.getSettings();

        PluginDescriptor pluginDescriptor = loadPlugin( plugin, project, session );

        if ( pluginDescriptor.getMojos() != null && !pluginDescriptor.getMojos().isEmpty() )
        {
            // use the plugin if inherit was true in a base class, or it is in the current POM, otherwise use the default inheritence setting
            if ( plugin.isInheritanceApplied() || pluginDescriptor.isInheritedByDefault() )
            {
                if ( plugin.getGoals() != null )
                {
                    logger.error( "Plugin contains a <goals/> section: this is IGNORED - please use <executions/> instead." );
                }

                List executions = plugin.getExecutions();

                if ( executions != null )
                {
                    for ( Iterator it = executions.iterator(); it.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) it.next();

                        bindExecutionToLifecycle( pluginDescriptor, phaseMap, execution, settings );
                    }
                }
            }
        }
    }

    private void bindExecutionToLifecycle( PluginDescriptor pluginDescriptor, Map phaseMap, PluginExecution execution, Settings settings )
        throws LifecycleExecutionException
    {
        for ( Iterator i = execution.getGoals().iterator(); i.hasNext(); )
        {
            String goal = (String) i.next();

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
            if ( mojoDescriptor == null )
            {
                throw new LifecycleExecutionException( "'" + goal + "' was specified in an execution, but not found in the plugin" );
            }

            // We have to check to see that the inheritance rules have been applied before binding this mojo.
            if ( execution.isInheritanceApplied() || mojoDescriptor.isInheritedByDefault() )
            {
                MojoExecution mojoExecution = new MojoExecution( mojoDescriptor, execution.getId() );

                String phase = execution.getPhase();

                if ( phase == null )
                {
                    // if the phase was not in the configuration, use the phase in the descriptor
                    phase = mojoDescriptor.getPhase();
                }

                if ( phase != null )
                {
                    if ( mojoDescriptor.isDirectInvocationOnly() )
                    {
                        throw new LifecycleExecutionException( "Mojo: \'" + goal + "\' requires direct invocation. It cannot be used as part of the lifecycle (it was included via the POM)." );
                    }

                    addToLifecycleMappings( phaseMap, phase, mojoExecution, settings );
                }
            }
        }
    }

    private void addToLifecycleMappings( Map lifecycleMappings, String phase, MojoExecution mojoExecution, Settings settings )
    {
        List goals = (List) lifecycleMappings.get( phase );

        if ( goals == null )
        {
            goals = new ArrayList();
            lifecycleMappings.put( phase, goals );
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
        if ( settings.isOffline() && mojoDescriptor.isOnlineRequired() )
        {
            String goal = mojoDescriptor.getGoal();
            logger.warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            goals.add( mojoExecution );
        }
    }

    private List processGoalChain( String task, Map phaseMap, Lifecycle lifecycle )
    {
        List goals = new ArrayList();

        // only execute up to the given phase
        int index = lifecycle.getPhases().indexOf( task );

        for ( int i = 0; i <= index; i++ )
        {
            String p = (String) lifecycle.getPhases().get( i );

            List phaseGoals = (List) phaseMap.get( p );

            if ( phaseGoals != null )
            {
                goals.addAll( phaseGoals );
            }
        }
        return goals;
    }

    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        String goal;
        Plugin plugin;

        PluginDescriptor pluginDescriptor = null;        
        String[] taskSegments = StringUtils.split( task, ":" );
        
        if ( taskSegments.length == 2 )
        {
            String prefix = taskSegments[0];
            goal = taskSegments[1];
            
            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.
            
            plugin = pluginManager.findPluginForPrefix( prefix, project, session );
                        
            if ( plugin == null )
            {
                plugin = new Plugin();
                plugin.setGroupId( pluginDescriptor.getGroupId() );
                plugin.setArtifactId( pluginDescriptor.getArtifactId() );
                plugin.setVersion( pluginDescriptor.getVersion() );
            }

            // Search plugin in the current POM
            if ( plugin == null )
            {
                for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
                {
                    Plugin buildPlugin = (Plugin) i.next();

                    PluginDescriptor desc = loadPlugin( buildPlugin, project, session );

                    if ( prefix.equals( desc.getGoalPrefix() ) )
                    {
                        plugin = buildPlugin;
                    }
                }
            }

            // Default to o.a.m.plugins and maven-<prefix>-plugin
            if ( plugin == null )
            {
                plugin = new Plugin();
                plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
                plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );
            }
        }
        else if ( taskSegments.length == 3 || taskSegments.length == 4 )
        {
            plugin = new Plugin();
            plugin.setGroupId( taskSegments[0] );
            plugin.setArtifactId( taskSegments[1] );

            if ( taskSegments.length == 4 )
            {
                plugin.setVersion( taskSegments[3] );
            }

            goal = taskSegments[4];
        }
        else
        {
            String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or" + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
            throw new LifecycleExecutionException( message );
        }

        if ( plugin.getVersion() == null )
        {
            for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
            {
                Plugin buildPlugin = (Plugin) i.next();

                if ( buildPlugin.getKey().equals( plugin.getKey() ) )
                {
                    plugin = buildPlugin;
                    break;
                }
            }

            project.injectPluginManagementInfo( plugin );
        }

        if ( pluginDescriptor == null )
        {
            pluginDescriptor = loadPlugin( plugin, project, session );
        }

        // this has been simplified from the old code that injected the plugin management stuff, since
        // pluginManagement injection is now handled by the project method.
        project.addPlugin( plugin );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
        
        return mojoDescriptor;
    }

    protected void line()
    {
        logger.info( "------------------------------------------------------------------------" );
    }

    public Map getPhaseToLifecycleMap()
        throws LifecycleExecutionException
    {
        if ( phaseToLifecycleMap == null )
        {
            phaseToLifecycleMap = new HashMap();

            for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
            {
                Lifecycle lifecycle = (Lifecycle) i.next();

                for ( Iterator p = lifecycle.getPhases().iterator(); p.hasNext(); )
                {
                    String phase = (String) p.next();

                    if ( phaseToLifecycleMap.containsKey( phase ) )
                    {
                        Lifecycle prevLifecycle = (Lifecycle) phaseToLifecycleMap.get( phase );
                        throw new LifecycleExecutionException( "Phase '" + phase + "' is defined in more than one lifecycle: '" + lifecycle.getId() + "' and '" + prevLifecycle.getId() + "'" );
                    }
                    else
                    {
                        phaseToLifecycleMap.put( phase, lifecycle );
                    }
                }
            }
        }
        return phaseToLifecycleMap;
    }

    private static class TaskSegment
    {
        private boolean aggregate;

        private List tasks = new ArrayList();

        TaskSegment()
        {

        }

        TaskSegment( boolean aggregate )
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

        void add( String task )
        {
            tasks.add( task );
        }

        List getTasks()
        {
            return tasks;
        }
    }

    private PluginDescriptor loadPlugin( Plugin plugin, MavenProject project, MavenSession session )
        throws LifecycleExecutionException
    {
        try
        {
            return pluginManager.loadPlugin( plugin, project, session );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
    }

    private PluginDescriptor loadReport( ReportPlugin plugin, MavenProject project, MavenSession session )
        throws LifecycleExecutionException
    {
        try
        {
            return pluginManager.loadReportPlugin( plugin, project, session );
        }
        catch ( PluginLoaderException e )
        {
            throw new LifecycleExecutionException( e.getMessage(), e );
        }
    }
}
