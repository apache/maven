package org.apache.maven.lifecycle;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.extension.ExtensionManager;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.lifecycle.Execution;
import org.apache.maven.plugin.lifecycle.Lifecycle;
import org.apache.maven.plugin.lifecycle.Phase;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: DefaultLifecycleExecutor.java,v 1.16 2005/03/04 09:04:25
 *          jdcasey Exp $
 */
public class DefaultLifecycleExecutor
    extends AbstractLogEnabled
    implements LifecycleExecutor
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private ModelDefaultsInjector modelDefaultsInjector;

    private PluginManager pluginManager;

    private ExtensionManager extensionManager;

    private List phases;

    private ArtifactHandlerManager artifactHandlerManager;

    private List defaultReports;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a task. Each task may be a phase in the lifecycle or the
     * execution of a mojo.
     *
     * @param session
     * @param rm
     * @param dispatcher
     */
    public MavenExecutionResponse execute( MavenSession session, ReactorManager rm, EventDispatcher dispatcher )
        throws LifecycleExecutionException
    {
        MavenProject rootProject = rm.getTopLevelProject();

        List taskSegments = segmentTaskListByAggregationNeeds( session.getGoals(), session, rootProject );

        MavenExecutionResponse response = new MavenExecutionResponse();

        response.setStart( new Date() );

        try
        {
            // TODO: probably don't want to do all this up front
            for ( Iterator i = session.getSortedProjects().iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                for ( Iterator j = project.getBuildExtensions().iterator(); j.hasNext(); )
                {
                    Extension extension = (Extension) j.next();
                    extensionManager.addExtension( extension, project, session.getLocalRepository() );
                }

                Map handlers = findArtifactTypeHandlers( project, session.getSettings(), session.getLocalRepository() );
                artifactHandlerManager.addHandlers( handlers );
            }

            executeTaskSegments( taskSegments, rm, session, rootProject, dispatcher );

            if ( ReactorManager.FAIL_AT_END.equals( rm.getFailureBehavior() ) && rm.hasBuildFailures() )
            {
                response.setException( new ReactorException( "One or more projects failed to build." ) );
            }
        }
        catch ( MojoExecutionException e )
        {
            response.setException( e );
        }
        catch ( ArtifactResolutionException e )
        {
            response.setException( e );
        }
        catch ( PlexusContainerException e )
        {
            throw new LifecycleExecutionException( "Unable to initialise extensions", e );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Unable to initialise extensions", e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException( "Unable to initialise extensions", e );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new LifecycleExecutionException( "Unable to initialise extensions", e );
        }
        finally
        {
            response.setFinish( new Date() );
        }

        return response;
    }

    private void executeTaskSegments( List taskSegments, ReactorManager rm, MavenSession session,
                                      MavenProject rootProject, EventDispatcher dispatcher )
        throws PluginNotFoundException, MojoExecutionException, ArtifactResolutionException, LifecycleExecutionException
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

                    // !! This is ripe for refactoring to an aspect.
                    // Event monitoring.
                    String event = MavenEvents.PROJECT_EXECUTION;

                    dispatcher.dispatchStart( event, rootProject.getId() + " ( " + segment + " )" );

                    try
                    {
                        // only call once, with the top-level project (assumed to be provided as a parameter)...
                        for ( Iterator goalIterator = segment.getTasks().iterator(); goalIterator.hasNext(); )
                        {
                            String task = (String) goalIterator.next();

                            try
                            {
                                executeGoal( task, session, rootProject );
                            }
                            catch ( MojoExecutionException e )
                            {
                                handleExecutionFailure( rm, rootProject, e, task );
                            }
                            catch ( ArtifactResolutionException e )
                            {
                                handleExecutionFailure( rm, rootProject, e, task );
                            }
                        }

                        dispatcher.dispatchEnd( event, rootProject.getId() + " ( " + segment + " )" );
                    }
                    catch ( LifecycleExecutionException e )
                    {
                        dispatcher.dispatchError( event, rootProject.getId() + " ( " + segment + " )", e );

                        throw e;
                    }
                }
                else
                {
                    line();

                    getLogger().info( "SKIPPING " + rootProject.getName() );

                    getLogger().info( "  " + segment );

                    getLogger().info(
                        "This project has been banned from further executions due to previous failures." );

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

                        // !! This is ripe for refactoring to an aspect.
                        // Event monitoring.
                        String event = MavenEvents.PROJECT_EXECUTION;

                        dispatcher.dispatchStart( event, currentProject.getId() + " ( " + segment + " )" );

                        try
                        {
                            for ( Iterator goalIterator = segment.getTasks().iterator(); goalIterator.hasNext(); )
                            {
                                String task = (String) goalIterator.next();

                                try
                                {
                                    executeGoal( task, session, currentProject );
                                }
                                catch ( MojoExecutionException e )
                                {
                                    handleExecutionFailure( rm, currentProject, e, task );
                                }
                                catch ( ArtifactResolutionException e )
                                {
                                    handleExecutionFailure( rm, currentProject, e, task );
                                }
                            }

                            dispatcher.dispatchEnd( event, currentProject.getId() + " ( " + segment + " )" );
                        }
                        catch ( LifecycleExecutionException e )
                        {
                            dispatcher.dispatchError( event, currentProject.getId() + " ( " + segment + " )", e );

                            throw e;
                        }
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

    private void handleExecutionFailure( ReactorManager rm, MavenProject project, Exception e, String task )
        throws MojoExecutionException, ArtifactResolutionException
    {
        if ( ReactorManager.FAIL_FAST.equals( rm.getFailureBehavior() ) )
        {
            rm.registerBuildFailure( project, e, task );

            if ( e instanceof MojoExecutionException )
            {
                throw (MojoExecutionException) e;
            }
            else if ( e instanceof ArtifactResolutionException )
            {
                throw (ArtifactResolutionException) e;
            }
            else
            {
                getLogger().error( "Attempt to register inappropriate build-failure Exception.", e );

                throw new IllegalArgumentException( "Inappropriate build-failure Exception: " + e );
            }
        }
        else if ( ReactorManager.FAIL_AT_END.equals( rm.getFailureBehavior() ) )
        {
            rm.registerBuildFailure( project, e, task );

            rm.blackList( project );
        }
        // FIXME: how about the other cases?
    }

    private List segmentTaskListByAggregationNeeds( List tasks, MavenSession session, MavenProject project )
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
                if ( phases.contains( task ) )
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
                    MojoDescriptor mojo = null;
                    try
                    {
                        // definitely a CLI goal, can use prefix
                        mojo = getMojoDescriptor( task, session, project, task, true );
                    }
                    catch ( LifecycleExecutionException e )
                    {
                        getLogger().info(
                            "Cannot find mojo descriptor for: \'" + task + "\' - Treating as non-aggregator." );
                        getLogger().debug( "", e );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        getLogger().info(
                            "Cannot find mojo descriptor for: \'" + task + "\' - Treating as non-aggregator." );
                        getLogger().debug( "", e );
                    }

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
        throws LifecycleExecutionException, PluginNotFoundException, MojoExecutionException, ArtifactResolutionException
    {
        if ( phases.contains( task ) )
        {
            // we have a lifecycle phase, so lets bind all the necessary goals
            Map lifecycleMappings = constructLifecycleMappings( session, task, project );
            executeGoalWithLifecycle( task, session, lifecycleMappings, project );
        }
        else
        {
            executeStandaloneGoal( task, session, project );
        }
    }

    private void executeGoalWithLifecycle( String task, MavenSession session, Map lifecycleMappings,
                                           MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException, MojoExecutionException
    {
        List goals = processGoalChain( task, lifecycleMappings );

        executeGoals( goals, session, project );
    }

    private void executeStandaloneGoal( String task, MavenSession session, MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException, MojoExecutionException
    {
        // guaranteed to come from the CLI and not be part of a phase
        MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session, project, task, true );
        executeGoals( Collections.singletonList( new MojoExecution( mojoDescriptor ) ), session, project );
    }

    private void executeGoals( List goals, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, MojoExecutionException, ArtifactResolutionException
    {
        for ( Iterator i = goals.iterator(); i.hasNext(); )
        {
            MojoExecution mojoExecution = (MojoExecution) i.next();

            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            if ( mojoDescriptor.getExecutePhase() != null || mojoDescriptor.getExecuteGoal() != null )
            {
                forkLifecycle( mojoDescriptor, session, project );
            }

            if ( mojoDescriptor.isRequiresReports() )
            {
                List reports = getReports( project, mojoExecution, session );

                mojoExecution.setReports( reports );

                for ( Iterator j = mojoExecution.getForkedExecutions().iterator(); j.hasNext(); )
                {
                    MojoExecution forkedExecution = (MojoExecution) j.next();
                    MojoDescriptor descriptor = forkedExecution.getMojoDescriptor();

                    if ( descriptor.getExecutePhase() != null )
                    {
                        forkLifecycle( descriptor, session, project );
                    }
                }
            }

            try
            {
                pluginManager.executeMojo( project, mojoExecution, session );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }
        }
    }

    private List getReports( MavenProject project, MojoExecution mojoExecution, MavenSession session )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        List reportPlugins = project.getReportPlugins();

        if ( project.getModel().getReports() != null )
        {
            getLogger().error(
                "DEPRECATED: Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
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
                if ( tok.countTokens() != 2 )
                {
                    getLogger().warn( "Invalid default report ignored: '" + report + "' (must be groupId:artifactId)" );
                }
                else
                {
                    String groupId = tok.nextToken();
                    String artifactId = tok.nextToken();

                    boolean found = false;
                    for ( Iterator j = reportPlugins.iterator(); j.hasNext() && !found; )
                    {
                        ReportPlugin reportPlugin = (ReportPlugin) j.next();
                        if ( reportPlugin.getGroupId().equals( groupId ) &&
                            reportPlugin.getArtifactId().equals( artifactId ) )
                        {
                            found = true;
                        }
                    }

                    if ( !found )
                    {
                        ReportPlugin reportPlugin = new ReportPlugin();
                        reportPlugin.setGroupId( groupId );
                        reportPlugin.setArtifactId( artifactId );
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

                try
                {
                    if ( reportSets == null || reportSets.isEmpty() )
                    {
                        reports.addAll( getReports( reportPlugin, null, project, session, mojoExecution ) );
                    }
                    else
                    {
                        for ( Iterator j = reportSets.iterator(); j.hasNext(); )
                        {
                            ReportSet reportSet = (ReportSet) j.next();

                            reports.addAll( getReports( reportPlugin, reportSet, project, session, mojoExecution ) );
                        }
                    }
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException( "Error getting reports", e );
                }
                catch ( PluginVersionResolutionException e )
                {
                    throw new LifecycleExecutionException( "Error getting reports", e );
                }
            }
        }
        return reports;
    }

    private List getReports( ReportPlugin reportPlugin, ReportSet reportSet, MavenProject project, MavenSession session,
                             MojoExecution mojoExecution )
        throws PluginManagerException, PluginVersionResolutionException, ArtifactResolutionException
    {
        PluginDescriptor pluginDescriptor = pluginManager.verifyReportPlugin( reportPlugin, project, session );

        List reports = new ArrayList();
        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) i.next();

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

                MavenReport reportMojo = pluginManager.getReport( project, reportExecution, session );

                // Comes back null if it was a plugin, not a report - these are mojos in the reporting plugins that are not reports
                if ( reportMojo != null )
                {
                    reports.add( reportMojo );
                    mojoExecution.addMojoExecution( reportExecution );
                }
            }
        }
        return reports;
    }

    private void forkLifecycle( MojoDescriptor mojoDescriptor, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, MojoExecutionException, ArtifactResolutionException
    {
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();
        getLogger().info( "Preparing " + pluginDescriptor.getGoalPrefix() + ":" + mojoDescriptor.getGoal() );

        String targetPhase = mojoDescriptor.getExecutePhase();

        Map lifecycleMappings = null;
        if ( targetPhase != null )
        {
            // Create new lifecycle
            lifecycleMappings = constructLifecycleMappings( session, targetPhase, project );

            String executeLifecycle = mojoDescriptor.getExecuteLifecycle();
            if ( executeLifecycle != null )
            {
                Lifecycle lifecycleOverlay;
                try
                {
                    lifecycleOverlay = pluginDescriptor.getLifecycleMapping( executeLifecycle );
                }
                catch ( IOException e )
                {
                    throw new LifecycleExecutionException( "Unable to read lifecycle mapping file", e );
                }
                catch ( XmlPullParserException e )
                {
                    throw new LifecycleExecutionException( "Unable to parse lifecycle mapping file", e );
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
                        Execution e = (Execution) j.next();

                        for ( Iterator k = e.getGoals().iterator(); k.hasNext(); )
                        {
                            String goal = (String) k.next();
                            MojoDescriptor desc = getMojoDescriptor( pluginDescriptor, goal );

                            MojoExecution mojoExecution = new MojoExecution( desc, (Xpp3Dom) e.getConfiguration() );
                            addToLifecycleMappings( lifecycleMappings, phase.getId(), mojoExecution,
                                                    session.getSettings() );
                        }
                    }
                }
            }

            removeFromLifecycle( mojoDescriptor, lifecycleMappings );
        }

        MavenProject executionProject = new MavenProject( project );
        if ( targetPhase != null )
        {
            executeGoalWithLifecycle( targetPhase, session, lifecycleMappings, executionProject );
        }
        else
        {
            String goal = mojoDescriptor.getExecuteGoal();
            MojoDescriptor desc = getMojoDescriptor( pluginDescriptor, goal );
            executeGoals( Collections.singletonList( new MojoExecution( desc ) ), session, executionProject );
        }
        project.setExecutionProject( executionProject );
    }

    private MojoDescriptor getMojoDescriptor( PluginDescriptor pluginDescriptor, String goal )
        throws LifecycleExecutionException
    {
        MojoDescriptor desc = pluginDescriptor.getMojo( goal );

        if ( desc == null )
        {
            String message = "Required goal '" + goal + "' not found in plugin '" + pluginDescriptor.getGoalPrefix() +
                "'";
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

    private void removeFromLifecycle( MojoDescriptor mojoDescriptor, Map lifecycleMappings )
    {
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        String mojoIdWithVersion = pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId() + ":" +
            pluginDescriptor.getVersion() + ":" + mojoDescriptor.getGoal();

        String mojoIdWithoutVersion = pluginDescriptor.getGroupId() + ":" + pluginDescriptor.getArtifactId() + ":" +
            mojoDescriptor.getGoal();

        for ( Iterator it = lifecycleMappings.values().iterator(); it.hasNext(); )
        {
            List tasks = (List) it.next();

            if ( tasks.remove( mojoIdWithVersion ) || tasks.remove( mojoIdWithoutVersion ) )
            {
                getLogger().warn( "Removing: " + mojoDescriptor.getGoal() +
                    " from forked lifecycle, to prevent recursive invocation of this mojo." );
            }
        }
    }

    private Map constructLifecycleMappings( MavenSession session, String selectedPhase, MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        // first, bind those associated with the packaging
        Map lifecycleMappings = bindLifecycleForPackaging( session, selectedPhase, project );

        // next, loop over plugins and for any that have a phase, bind it
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            bindPluginToLifecycle( plugin, session, lifecycleMappings, project );
        }

        return lifecycleMappings;
    }

    private Map bindLifecycleForPackaging( MavenSession session, String selectedPhase, MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        Map mappings = findMappingsForLifecycle( session, project );

        Map lifecycleMappings = new HashMap();

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            String phase = (String) i.next();

            String phaseTasks = (String) mappings.get( phase );

            if ( phaseTasks != null )
            {
                for ( StringTokenizer tok = new StringTokenizer( phaseTasks, "," ); tok.hasMoreTokens(); )
                {
                    String goal = tok.nextToken().trim();

                    // Not from the CLI, don't use prefix
                    MojoDescriptor mojoDescriptor = getMojoDescriptor( goal, session, project, selectedPhase, false );

                    if ( mojoDescriptor.isDirectInvocationOnly() )
                    {
                        throw new LifecycleExecutionException( "Mojo: \'" + goal +
                            "\' requires direct invocation. It cannot be used as part of lifecycle: \'" +
                            project.getPackaging() + "\'." );
                    }

                    addToLifecycleMappings( lifecycleMappings, phase, new MojoExecution( mojoDescriptor ),
                                            session.getSettings() );
                }
            }

            if ( phase.equals( selectedPhase ) )
            {
                break;
            }
        }

        return lifecycleMappings;
    }

    private Map findMappingsForLifecycle( MavenSession session, MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        String packaging = project.getPackaging();
        LifecycleMapping m;

        try
        {
            m = (LifecycleMapping) findExtension( project, LifecycleMapping.ROLE, packaging, session.getSettings(),
                                                  session.getLocalRepository() );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException(
                "Cannot load extension plugin obtaining lifecycle mappings for: \'" + packaging + "\'.", e );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException(
                "Cannot load extension plugin obtaining lifecycle mappings for: \'" + packaging + "\'.", e );
        }

        if ( m == null )
        {
            try
            {
                m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
            }
            catch ( ComponentLookupException e )
            {
                throw new LifecycleExecutionException(
                    "Cannot find lifecycle mapping for packaging: \'" + packaging + "\'.", e );
            }
        }

        return m.getPhases();
    }

    private Object findExtension( MavenProject project, String role, String roleHint, Settings settings,
                                  ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginManagerException, PluginVersionResolutionException
    {
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            if ( plugin.isExtensions() )
            {
                pluginManager.verifyPlugin( plugin, project, settings, localRepository );

                // TODO: if moved to the plugin manager we already have the descriptor from above and so do can lookup the container directly
                try
                {
                    return pluginManager.getPluginComponent( plugin, role, roleHint );
                }
                catch ( ComponentLookupException e )
                {
                    getLogger().debug( "Unable to find the lifecycle component in the extension", e );
                }
            }
        }
        return null;
    }

    /**
     * @todo Not particularly happy about this. Would like WagonManager and ArtifactTypeHandlerManager to be able to
     * lookup directly, or have them passed in
     */
    private Map findArtifactTypeHandlers( MavenProject project, Settings settings, ArtifactRepository localRepository )
        throws ArtifactResolutionException, PluginManagerException, PluginVersionResolutionException
    {
        Map map = new HashMap();
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            if ( plugin.isExtensions() )
            {
                pluginManager.verifyPlugin( plugin, project, settings, localRepository );

                // TODO: if moved to the plugin manager we already have the descriptor from above and so do can lookup the container directly
                try
                {
                    Map components = pluginManager.getPluginComponents( plugin, ArtifactHandler.ROLE );
                    map.putAll( components );

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
                catch ( ComponentLookupException e )
                {
                    getLogger().debug( "Unable to find the lifecycle component in the extension", e );
                }
            }
        }
        return map;
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for that given phase.
     *
     * @param project
     * @param session
     */
    private void bindPluginToLifecycle( Plugin plugin, MavenSession session, Map phaseMap, MavenProject project )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        if ( plugin.getGoals() != null && !plugin.getGoals().isEmpty() )
        {
            getLogger().warn(
                "DEPRECATED: goal definitions for plugin '" + plugin.getKey() + "' must be in an executions element" );
        }

        PluginDescriptor pluginDescriptor;
        Settings settings = session.getSettings();

        pluginDescriptor = verifyPlugin( plugin, session, project );

        if ( pluginDescriptor.getMojos() != null && !pluginDescriptor.getMojos().isEmpty() )
        {
            // use the plugin if inherit was true in a base class, or it is in the current POM, otherwise use the default inheritence setting
            if ( plugin.isInheritanceApplied() || pluginDescriptor.isInheritedByDefault() )
            {
                bindGoalMapToLifecycle( pluginDescriptor, plugin.getGoalsAsMap(), phaseMap, settings );

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

    private PluginDescriptor verifyPlugin( Plugin plugin, MavenSession session, MavenProject project )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        PluginDescriptor pluginDescriptor;
        try
        {
            ArtifactRepository localRepository = session.getLocalRepository();
            pluginDescriptor = pluginManager.verifyPlugin( plugin, project, session.getSettings(), localRepository );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException( "Error resolving plugin version", e );
        }
        return pluginDescriptor;
    }

    /**
     * @deprecated
     */
    private void bindGoalMapToLifecycle( PluginDescriptor pluginDescriptor, Map goalMap, Map phaseMap,
                                         Settings settings )
    {
        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) i.next();

            Goal goal = (Goal) goalMap.get( mojoDescriptor.getGoal() );

            if ( goal != null )
            {
                // We have to check to see that the inheritance rules have been applied before binding this mojo.
                if ( mojoDescriptor.isInheritedByDefault() )
                {
                    if ( mojoDescriptor.getPhase() != null )
                    {
                        MojoExecution mojoExecution = new MojoExecution( mojoDescriptor );
                        addToLifecycleMappings( phaseMap, mojoDescriptor.getPhase(), mojoExecution, settings );
                    }
                }
            }
        }
    }

    private void bindExecutionToLifecycle( PluginDescriptor pluginDescriptor, Map phaseMap, PluginExecution execution,
                                           Settings settings )
        throws LifecycleExecutionException
    {
        for ( Iterator i = execution.getGoals().iterator(); i.hasNext(); )
        {
            String goal = (String) i.next();

            MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
            if ( mojoDescriptor == null )
            {
                throw new LifecycleExecutionException( "Goal from the POM '" + goal + "' was not found in the plugin" );
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
                        throw new LifecycleExecutionException( "Mojo: \'" + goal +
                            "\' requires direct invocation. It cannot be used as part of the lifecycle (it was included via the POM)." );
                    }

                    addToLifecycleMappings( phaseMap, phase, mojoExecution, settings );
                }
            }
        }
    }

    private void addToLifecycleMappings( Map lifecycleMappings, String phase, MojoExecution mojoExecution,
                                         Settings settings )
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
            getLogger().warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            goals.add( mojoExecution );
        }
    }

    private List processGoalChain( String task, Map phaseMap )
    {
        List goals = new ArrayList();

        // only execute up to the given phase
        int index = phases.indexOf( task );

        for ( int i = 0; i <= index; i++ )
        {
            String p = (String) phases.get( i );

            List phaseGoals = (List) phaseMap.get( p );

            if ( phaseGoals != null )
            {
                goals.addAll( phaseGoals );
            }
        }
        return goals;
    }

    private MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project,
                                              String invokedVia, boolean canUsePrefix )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        String goal;
        Plugin plugin = null;

        PluginDescriptor pluginDescriptor = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        int numTokens = tok.countTokens();

        // TODO: Add "&& canUsePrefix" to this boolean expression, and remove deprecation warning in next release.
        if ( numTokens == 2 )
        {
            if ( !canUsePrefix )
            {
                getLogger().warn(
                    "DEPRECATED: Mapped-prefix lookup of mojos are only supported from direct invocation. Please use specification of the form groupId:artifactId[:version]:goal instead. (Offending mojo: \'" +
                        task + "\', invoked via: \'" + invokedVia + "\')" );
            }

            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // Steps for retrieving the plugin model instance:
            // 1. request directly from the plugin collector by prefix
            try
            {
                pluginDescriptor = pluginManager.getPluginDescriptorForPrefix( prefix );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException(
                    "Cannot resolve plugin-prefix: \'" + prefix + "\' from plugin collector.", e );
            }

            if ( pluginDescriptor == null )
            {
                try
                {
                    plugin = pluginManager.getPluginDefinitionForPrefix( prefix, session, project );
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException(
                        "Cannot resolve plugin-prefix: \'" + prefix + "\' from plugin mappings metadata.", e );
                }
            }

            if ( pluginDescriptor != null )
            {
                plugin = new Plugin();

                plugin.setGroupId( pluginDescriptor.getGroupId() );
                plugin.setArtifactId( pluginDescriptor.getArtifactId() );
                plugin.setVersion( pluginDescriptor.getVersion() );
            }

            // 2. default to o.a.m.plugins and maven-<prefix>-plugin
            if ( plugin == null )
            {
                plugin = new Plugin();
                plugin.setGroupId( PluginDescriptor.getDefaultPluginGroupId() );
                plugin.setArtifactId( PluginDescriptor.getDefaultPluginArtifactId( prefix ) );
            }

            for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
            {
                Plugin buildPlugin = (Plugin) i.next();

                if ( buildPlugin.getKey().equals( plugin.getKey() ) )
                {
                    plugin = buildPlugin;
                    break;
                }
            }
        }
        else if ( numTokens == 3 || numTokens == 4 )
        {
            plugin = new Plugin();

            plugin.setGroupId( tok.nextToken() );
            plugin.setArtifactId( tok.nextToken() );

            if ( numTokens == 4 )
            {
                plugin.setVersion( tok.nextToken() );
            }

            goal = tok.nextToken();
        }
        else
        {
            String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or" +
                " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
            throw new LifecycleExecutionException( message );
        }

        if ( pluginDescriptor == null )
        {
            try
            {
                pluginDescriptor = pluginManager.verifyPlugin( plugin, project, session.getSettings(),
                                                               session.getLocalRepository() );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new LifecycleExecutionException( "Error resolving plugin version", e );
            }
        }

        injectHandlerPluginConfiguration( project, plugin );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
        if ( mojoDescriptor == null )
        {
            throw new LifecycleExecutionException( "Required goal not found: " + task );
        }

        return mojoDescriptor;
    }

    private void injectHandlerPluginConfiguration( MavenProject project, Plugin plugin )
    {
        String key = plugin.getKey();

        Plugin buildPlugin = (Plugin) project.getBuild().getPluginsAsMap().get( key );

        if ( buildPlugin == null )
        {
            PluginManagement pluginManagement = project.getPluginManagement();
            if ( pluginManagement != null )
            {
                Plugin managedPlugin = (Plugin) pluginManagement.getPluginsAsMap().get( key );

                if ( managedPlugin != null )
                {
                    modelDefaultsInjector.mergePluginWithDefaults( plugin, managedPlugin );
                }
            }

            project.addPlugin( plugin );
        }
    }

    protected void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
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
}
