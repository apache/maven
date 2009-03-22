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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.maven.BuildFailureException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

/**
 * @author Jason van Zyl
 */
public class DefaultLifecycleExecutor
    extends AbstractLogEnabled
    implements LifecycleExecutor, Initializable
{
    //@Requirement
    //private getLogger() getLogger();

    @Requirement
    private PluginManager pluginManager;

    private List<Lifecycle> lifecycles;

    private List defaultReports;

    private Map<String, Lifecycle> phaseToLifecycleMap;

    @Requirement
    private Map<String, LifecycleMapping> lifecycleMappings;

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

        executeTaskSegments( goals, rm, session, rootProject, dispatcher );
    }

    public List<String> getLifecyclePhases()
    {
        for ( Lifecycle lifecycle : lifecycles )
        {
            if ( lifecycle.getId().equals( "default" ) )
            {
                return (List<String>) lifecycle.getPhases().values();
            }
        }

        return null;
    }

    public static boolean isValidPhaseName( final String phaseName )
    {
        return true;
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
                catch ( LifecycleExecutionException e )
                {
                    String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or"
                        + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";

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
        throws InvalidPluginException, PluginLoaderException, LifecycleExecutionException
    {
        MojoDescriptor descriptor;

        try
        {
            descriptor = getMojoDescriptor( task, session, project );
        }
        catch ( LifecycleExecutionException e )
        {
            throw new LifecycleExecutionException( "Cannot find the specified goal.", e );
        }

        return descriptor;
    }

    private void executeTaskSegments( List<String> goals, ReactorManager rm, MavenSession session, MavenProject rootProject, EventDispatcher dispatcher )
        throws LifecycleExecutionException, BuildFailureException
    {
        List<MavenProject> sortedProjects = session.getSortedProjects();

        for ( MavenProject currentProject : sortedProjects )
        {
            if ( !rm.isBlackListed( currentProject ) )
            {
                line();

                getLogger().info( "Building " + currentProject.getName() );

                line();

                // !! This is ripe for refactoring to an aspect.
                // Event monitoring.
                String event = MavenEvents.PROJECT_EXECUTION;

                long buildStartTime = System.currentTimeMillis();

                try
                {
                    session.setCurrentProject( currentProject );

                    for ( String goal : goals )
                    {
                        String target = currentProject.getId() + " ( " + goal + " )";
                        dispatcher.dispatchStart( event, target );
                        executeGoalAndHandleFailures( goal, session, currentProject, dispatcher, event, rm, buildStartTime, target );
                        dispatcher.dispatchEnd( event, target );
                    }
                }
                finally
                {
                    session.setCurrentProject( null );
                }

                rm.registerBuildSuccess( currentProject, System.currentTimeMillis() - buildStartTime );
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

    // 1. Find the lifecycle given the phase (default lifecycle when given install)
    // 2. Find the lifecycle mapping that corresponds to the project packaging (jar lifecycle mapping given the jar packaging)
    // 3. Find the mojos associated with the lifecycle given the project packaging (jar lifecycle mapping for the default lifecycle)
    private void executeGoal( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException
    {
        // 1. 
        Lifecycle lifecycle = phaseToLifecycleMap.get( task );
        
        // 2. 
        LifecycleMapping mapping = lifecycleMappings.get( project.getPackaging() );
        
        // 3.
        Map<String,String> lifecyclePhases = mapping.getLifecycles().get( "default" ).getPhases();
        
        for( String phase : lifecycle.getPhases().values() )
        {
            System.out.println( ">> " + phase );
        }
                
        /*
        try
        {            
            if ( lifecycle != null )
            {
                Map lifecycleMappings = constructLifecycleMappings( session, task, project, lifecycle );

                executeGoalWithLifecycle( task, session, lifecycleMappings, project, lifecycle );
            }
            else
            {
                executeStandaloneGoal( task, session, project );
            }
        }
        catch ( PluginNotFoundException e )
        {
            throw new BuildFailureException( "A required plugin was not found: " + e.getMessage(), e );
        }
        */
    }

    /*
    private void executeGoalWithLifecycle( String task, MavenSession session, Map lifecycleMappings, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        List goals = new ArrayList();

        // only execute up to the given phase
        int index = lifecycle.getPhases().indexOf( task );

        for ( int i = 0; i <= index; i++ )
        {
            String p = (String) lifecycle.getPhases().get( i );

            List phaseGoals = (List) lifecycleMappings.get( p );

            if ( phaseGoals != null )
            {
                goals.addAll( phaseGoals );
            }
        }

        if ( !goals.isEmpty() )
        {
            executeGoals( goals, session, project );
        }
        else
        {
            getLogger().info( "No goals needed for project - skipping" );
        }
    }
    */

    private void executeStandaloneGoal( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        // guaranteed to come from the CLI and not be part of a phase
        MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session, project );
        executeGoals( Collections.singletonList( new MojoExecution( mojoDescriptor ) ), session, project );
    }

    private void executeGoals( List<MojoExecution> goals, MavenSession session, MavenProject project )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        for ( MojoExecution mojoExecution : goals )
        {
            MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();

            if ( mojoDescriptor.isRequiresReports() )
            {
                List reports = getReports( project, mojoExecution, session );

                mojoExecution.setReports( reports );
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

    private List getReports( MavenProject project, MojoExecution mojoExecution, MavenSession session )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        List reportPlugins = project.getReportPlugins();

        if ( project.getModel().getReports() != null )
        {
            getLogger().error( "Plugin contains a <reports/> section: this is IGNORED - please use <reporting/> instead." );
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
                    getLogger().warn( "Invalid default report ignored: '" + report + "' (must be groupId:artifactId[:version])" );
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
        }
        return reports;
    }

    private List getReports( ReportPlugin reportPlugin, ReportSet reportSet, MavenProject project, MavenSession session, MojoExecution mojoExecution )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        PluginDescriptor pluginDescriptor = loadReport( reportPlugin, project, session );

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

    /*
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
    */

    /*
    private Map bindLifecycleForPackaging( MavenSession session, String selectedPhase, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, BuildFailureException, PluginNotFoundException
    {
        Map mappings = findMappingsForLifecycle( session, project, lifecycle );

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

                    addToLifecycleMappings( lifecycleMappings, phase, new MojoExecution( mojoDescriptor ), session );
                }
            }

            if ( phase.equals( selectedPhase ) )
            {
                break;
            }
        }

        return lifecycleMappings;
    }
    */

    private Map findMappingsForLifecycle( MavenSession session, MavenProject project, Lifecycle lifecycle )
        throws LifecycleExecutionException, PluginNotFoundException
    {
        String packaging = project.getPackaging();
        Map mappings = null;

        LifecycleMapping m;

        Map defaultMappings = lifecycle.getDefaultPhases();

        if ( mappings == null )
        {
            m = lifecycleMappings.get( packaging );

            mappings = null; //m.getLifecycles().get( lifecycle.getId() );                    
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
        PluginDescriptor pluginDescriptor = loadPlugin( plugin, project, session );

        if ( pluginDescriptor.getMojos() != null && !pluginDescriptor.getMojos().isEmpty() )
        {
            // use the plugin if inherit was true in a base class, or it is in the current POM, otherwise use the default inheritence setting
            if ( plugin.isInheritanceApplied() || pluginDescriptor.isInheritedByDefault() )
            {
                if ( plugin.getGoals() != null )
                {
                    getLogger().error( "Plugin contains a <goals/> section: this is IGNORED - please use <executions/> instead." );
                }

                List executions = plugin.getExecutions();

                if ( executions != null )
                {
                    for ( Iterator it = executions.iterator(); it.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) it.next();

                        bindExecutionToLifecycle( pluginDescriptor, phaseMap, execution, session );
                    }
                }
            }
        }
    }

    private void bindExecutionToLifecycle( PluginDescriptor pluginDescriptor, Map phaseMap, PluginExecution execution, MavenSession session )
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

                    addToLifecycleMappings( phaseMap, phase, mojoExecution, session );
                }
            }
        }
    }

    private void addToLifecycleMappings( Map lifecycleMappings, String phase, MojoExecution mojoExecution, MavenSession session )
    {
        List goals = (List) lifecycleMappings.get( phase );

        if ( goals == null )
        {
            goals = new ArrayList();
            lifecycleMappings.put( phase, goals );
        }

        MojoDescriptor mojoDescriptor = mojoExecution.getMojoDescriptor();
        if ( session.isOffline() && mojoDescriptor.isOnlineRequired() )
        {
            String goal = mojoDescriptor.getGoal();
            getLogger().warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            goals.add( mojoExecution );
        }
    }

    // all this logic should go to the plugin manager

    MojoDescriptor getMojoDescriptor( String task, MavenSession session, MavenProject project )
        throws LifecycleExecutionException
    {
        String goal;
        Plugin plugin;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        int numTokens = tok.countTokens();

        if ( numTokens == 2 )
        {
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            // This is the case where someone has executed a single goal from the command line
            // of the form:
            //
            // mvn remote-resources:process
            //
            // From the metadata stored on the server which has been created as part of a standard
            // Maven plugin deployment we will find the right PluginDescriptor from the remote
            // repository.

            plugin = pluginManager.findPluginForPrefix( prefix, project, session );

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

        PluginDescriptor pluginDescriptor = loadPlugin( plugin, project, session );

        // this has been simplified from the old code that injected the plugin management stuff, since
        // pluginManagement injection is now handled by the project method.
        project.addPlugin( plugin );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
        return mojoDescriptor;
    }

    protected void line()
    {
        getLogger().info( "------------------------------------------------------------------------" );
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

    public void initialize()
        throws InitializationException
    {
        // If people are going to make their own lifecycles then we need to tell people how to namespace them correctly so
        // that they don't interfere with internally defined lifecycles.

        phaseToLifecycleMap = new HashMap();

        for ( Iterator i = lifecycles.iterator(); i.hasNext(); )
        {
            Lifecycle lifecycle = (Lifecycle) i.next();

            for ( Iterator p = lifecycle.getPhases().values().iterator(); p.hasNext(); )
            {
                String phase = (String) p.next();

                // The first definition wins.
                if ( !phaseToLifecycleMap.containsKey( phase ) )
                {
                    phaseToLifecycleMap.put( phase, lifecycle );
                }
            }
        }
    }
}
