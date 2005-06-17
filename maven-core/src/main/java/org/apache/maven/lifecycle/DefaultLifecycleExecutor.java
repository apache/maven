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

import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.GoalInstance;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultLifecycleExecutor.java,v 1.16 2005/03/04 09:04:25
 *          jdcasey Exp $
 * @todo this is structured somewhat confusingly. Attempt to "flatten out" to reduce the number of paths through by
 * compiling the list of plugins/tasks first.
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

    private List phases;

    private Map defaultPhases;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a list of tasks. Each task may be a phase in the lifecycle or the
     * execution of a mojo.
     *
     * @param tasks
     * @param session
     */
    public MavenExecutionResponse execute( List tasks, MavenSession session )
        throws LifecycleExecutionException
    {
        MavenExecutionResponse response = new MavenExecutionResponse();

        response.setStart( new Date() );

        try
        {
            processGoals( session, tasks );
        }
        catch ( MojoExecutionException e )
        {
            response.setException( e );
        }
        catch ( ArtifactResolutionException e )
        {
            response.setException( e );
        }
        finally
        {
            response.setFinish( new Date() );
        }

        return response;
    }

    private void processGoals( MavenSession session, List tasks )
        throws LifecycleExecutionException, PluginNotFoundException, MojoExecutionException,
        ArtifactResolutionException
    {
        Map phaseMap = new HashMap();
        Map goalInstanceMap = new HashMap();

        String maxPhase = null;

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            String p = (String) i.next();

            // Make a copy of the phase as we will modify it
            phaseMap.put( p, new ArrayList() );

            if ( tasks.contains( p ) )
            {
                maxPhase = p;
            }
        }

        MavenProject project = session.getProject();

        if ( maxPhase != null )
        {
            Map mappings;
            try
            {
                LifecycleMapping m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, project.getPackaging() );
                mappings = m.getPhases();
            }
            catch ( ComponentLookupException e )
            {
                getLogger().error( "No lifecycle mapping for type '" + project.getPackaging() + "': using defaults" );
                mappings = defaultPhases;
            }

            for ( Iterator i = phases.iterator(); i.hasNext(); )
            {
                String phase = (String) i.next();

                String phaseTasks = (String) mappings.get( phase );

                if ( phaseTasks != null )
                {
                    for ( StringTokenizer tok = new StringTokenizer( phaseTasks, "," ); tok.hasMoreTokens(); )
                    {
                        String task = tok.nextToken().trim();

                        MojoDescriptor mojoDescriptor = configureMojo( task, session, phaseMap );

                        addToPhaseMap( phaseMap, phase, mojoDescriptor );

                        List matchingGoalInstances = findMatchingGoalInstances( mojoDescriptor, project );

                        for ( Iterator instanceIterator = matchingGoalInstances.iterator(); instanceIterator.hasNext(); )
                        {
                            GoalInstance goalInstance = (GoalInstance) instanceIterator.next();

                            addToGoalInstanceMap( goalInstanceMap, goalInstance );
                        }
                    }
                }

                if ( phase.equals( maxPhase ) )
                {
                    break;
                }
            }
        }

        processPluginConfiguration( project, session, phaseMap, goalInstanceMap );

        for ( Iterator i = tasks.iterator(); i.hasNext(); )
        {
            String task = (String) i.next();

            // verify that all loose-leaf goals have had GoalInstance(s) configured for them...
            // we only need to do this if the current task is not a phase name.
            if ( !phaseMap.containsKey( task ) )
            {
                MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session );

                if ( mojoDescriptor != null && !goalInstanceMap.containsKey( mojoDescriptor ) )
                {
                    List matchingGoalInstances = findMatchingGoalInstances( mojoDescriptor, project );

                    for ( Iterator instanceIterator = matchingGoalInstances.iterator(); instanceIterator.hasNext(); )
                    {
                        GoalInstance goalInstance = (GoalInstance) instanceIterator.next();

                        addToGoalInstanceMap( goalInstanceMap, goalInstance );
                    }
                }
            }

            // now we can proceed to actually load up the list of goals we're interested in.
            List goals = processGoalChain( task, session, phaseMap );

            for ( Iterator j = goals.iterator(); j.hasNext(); )
            {
                MojoDescriptor mojoDescriptor = (MojoDescriptor) j.next();

                List instances = (List) goalInstanceMap.get( mojoDescriptor );

                if ( instances != null )
                {
                    for ( Iterator instanceIterator = instances.iterator(); instanceIterator.hasNext(); )
                    {
                        GoalInstance instance = (GoalInstance) instanceIterator.next();

                        String executePhase = mojoDescriptor.getExecutePhase();

                        if ( executePhase != null )
                        {
                            // TODO: is this too broad to execute?
                            execute( Collections.singletonList( executePhase ), session );
                        }

                        try
                        {
                            pluginManager.executeMojo( session, instance );
                        }
                        catch ( PluginManagerException e )
                        {
                            throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
                        }
                    }
                }
                else
                {
                    throw new LifecycleExecutionException( "This goal has not been configured: "
                        + mojoDescriptor.getGoal() );
                }
            }
        }
    }

    private void addToGoalInstanceMap( Map goalInstanceMap, GoalInstance goalInstance )
    {
        MojoDescriptor mojoDescriptor = goalInstance.getMojoDescriptor();

        List instances = (List) goalInstanceMap.get( mojoDescriptor );

        if ( instances == null )
        {
            instances = new ArrayList();

            goalInstanceMap.put( mojoDescriptor, instances );
        }

        int idx = instances.indexOf( goalInstance );

        if ( idx > -1 )
        {
            GoalInstance cached = (GoalInstance) instances.get( idx );

            cached.incorporate( goalInstance );
        }
        else
        {
            instances.add( goalInstance );
        }
    }

    private void injectHandlerPluginConfiguration( MavenProject project, String groupId, String artifactId,
                                                  String version )
    {
        String key = Plugin.constructKey( groupId, artifactId );
        Plugin plugin = (Plugin) project.getBuild().getPluginsAsMap().get( key );

        if ( plugin == null )
        {
            plugin = new Plugin();
            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );
            plugin.setVersion( version );

            PluginManagement pluginManagement = project.getPluginManagement();
            if ( pluginManagement != null )
            {
                Plugin def = (Plugin) pluginManagement.getPluginsAsMap().get( key );
                if ( def != null )
                {
                    modelDefaultsInjector.mergePluginWithDefaults( plugin, def );
                }
            }

            project.addPlugin( plugin );
        }
    }

    private void processPluginConfiguration( MavenProject project, MavenSession mavenSession, Map phaseMap,
                                            Map goalInstanceMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            processPluginPhases( plugin, mavenSession, phaseMap, goalInstanceMap );
        }
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for that given phase.
     *
     * @param session
     * @param goalInstanceMap 
     */
    private void processPluginPhases( Plugin plugin, MavenSession session, Map phaseMap, Map goalInstanceMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        String groupId = plugin.getGroupId();

        String artifactId = plugin.getArtifactId();

        String version = plugin.getVersion();

        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, session.getProject(), session
                .getSettings(), session.getLocalRepository() );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
        }
        catch ( PluginVersionResolutionException e )
        {
            throw new LifecycleExecutionException( "Error resolving plugin version", e );
        }

        if ( plugin.isInheritanceApplied() || pluginDescriptor.isInheritedByDefault() )
        {
            processGoalContainerPhases( plugin, null, pluginDescriptor, session, plugin.getGoalsAsMap(), phaseMap,
                                        goalInstanceMap );

            List executions = plugin.getExecutions();

            if ( executions != null )
            {
                for ( Iterator it = executions.iterator(); it.hasNext(); )
                {
                    PluginExecution execution = (PluginExecution) it.next();

                    if ( execution.isInheritanceApplied() )
                    {
                        processGoalContainerPhases( plugin, execution, pluginDescriptor, session, execution
                            .getGoalsAsMap(), phaseMap, goalInstanceMap );
                    }
                }
            }
        }
    }

    private void processGoalContainerPhases( Plugin plugin, PluginExecution execution,
                                            PluginDescriptor pluginDescriptor, MavenSession session, Map goalMap,
                                            Map phaseMap, Map goalInstanceMap )
        throws LifecycleExecutionException
    {
        // ----------------------------------------------------------------------
        // Look to see if the plugin configuration specifies particular mojos
        // within the plugin. If this is the case then simply configure the
        // mojos the user has specified and ignore the rest.
        // ----------------------------------------------------------------------

        if ( pluginDescriptor.getMojos() != null )
        {
            for ( Iterator j = pluginDescriptor.getMojos().iterator(); j.hasNext(); )
            {
                MojoDescriptor mojoDescriptor = (MojoDescriptor) j.next();

                // TODO: remove later
                if ( mojoDescriptor.getGoal() == null )
                {
                    throw new LifecycleExecutionException( "The plugin " + pluginDescriptor.getId()
                        + " was built with an older version of Maven" );
                }

                Goal goal = (Goal) goalMap.get( mojoDescriptor.getGoal() );

                if ( goalMap.isEmpty() )
                {
                    configureMojoPhaseBinding( mojoDescriptor, phaseMap, session.getSettings() );

                    addToGoalInstanceMap( goalInstanceMap, new GoalInstance( plugin, execution, goal, mojoDescriptor ) );
                }
                else if ( goal != null )
                {
                    // We have to check to see that the inheritance rules have been applied before binding this mojo.
                    if ( goal.isInheritanceApplied() || mojoDescriptor.isInheritedByDefault() )
                    {
                        configureMojoPhaseBinding( mojoDescriptor, phaseMap, session.getSettings() );

                        addToGoalInstanceMap( goalInstanceMap, new GoalInstance( plugin, execution, goal,
                                                                                 mojoDescriptor ) );
                    }
                }
            }
        }
    }

    /**
     * Take a look at a mojo contained within a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for the stated phase.
     *
     * @param mojoDescriptor
     */
    private void configureMojoPhaseBinding( MojoDescriptor mojoDescriptor, Map phaseMap, Settings settings )
        throws LifecycleExecutionException
    {
        if ( settings.isOffline() && mojoDescriptor.isOnlineRequired() )
        {
            String goal = mojoDescriptor.getGoal();
            getLogger().warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            if ( mojoDescriptor.getPhase() != null )
            {
                addToPhaseMap( phaseMap, mojoDescriptor.getPhase(), mojoDescriptor );
            }
        }
    }

    private void addToPhaseMap( Map phaseMap, String phase, MojoDescriptor mojoDescriptor )
        throws LifecycleExecutionException
    {
        if ( phase != null )
        {
            List goals = (List) phaseMap.get( phase );

            if ( goals == null )
            {
                String message = "Required phase '" + phase + "' not found";
                throw new LifecycleExecutionException( message );
            }

            if ( !goals.contains( mojoDescriptor ) )
            {
                goals.add( mojoDescriptor );
            }
        }
    }

    private List processGoalChain( String task, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        List goals = new ArrayList();

        if ( phaseMap.containsKey( task ) )
        {
            // only execute up to the given phase
            int index = phases.indexOf( task );

            for ( int j = 0; j <= index; j++ )
            {
                String p = (String) phases.get( j );

                List phaseGoals = (List) phaseMap.get( p );

                if ( phaseGoals != null )
                {
                    goals.addAll( phaseGoals );
                }
            }
        }
        else
        {
            MojoDescriptor mojoDescriptor = configureMojo( task, session, phaseMap );

            goals.add( mojoDescriptor );
        }

        return goals;
    }

    private MojoDescriptor configureMojo( String task, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session );

        configureMojoPhaseBinding( mojoDescriptor, phaseMap, session.getSettings() );

        return mojoDescriptor;
    }

    private List findMatchingGoalInstances( MojoDescriptor mojoDescriptor, MavenProject project )
    {
        PluginDescriptor pluginDescriptor = mojoDescriptor.getPluginDescriptor();

        List plugins = project.getBuildPlugins();

        List matchingSteps = new ArrayList();

        Plugin plugin = null;

        for ( Iterator it = plugins.iterator(); it.hasNext(); )
        {
            plugin = (Plugin) it.next();

            if ( pluginDescriptor.getPluginLookupKey().equals( plugin.getKey() ) )
            {
                String mojoGoal = mojoDescriptor.getGoal();

                Goal unattached = (Goal) plugin.getGoalsAsMap().get( mojoDescriptor.getGoal() );

                if ( unattached != null )
                {
                    matchingSteps.add( new GoalInstance( plugin, unattached, mojoDescriptor ) );
                }

                List executions = plugin.getExecutions();

                if ( executions != null )
                {
                    for ( Iterator executionIterator = executions.iterator(); executionIterator.hasNext(); )
                    {
                        PluginExecution execution = (PluginExecution) executionIterator.next();

                        Goal attached = (Goal) execution.getGoalsAsMap().get( mojoDescriptor.getGoal() );

                        if ( attached != null )
                        {
                            matchingSteps.add( new GoalInstance( plugin, execution, attached, mojoDescriptor ) );
                        }
                    }
                }

                break;
            }
        }

        // if nothing is configured, then we need to add a "fully detached" step...
        if ( matchingSteps.isEmpty() )
        {
            matchingSteps.add( new GoalInstance( mojoDescriptor ) );
        }

        return matchingSteps;
    }

    private MojoDescriptor getMojoDescriptor( String task, MavenSession session )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        String groupId = null;
        String artifactId = null;
        String version = null;
        String goal = null;

        PluginDescriptor pluginDescriptor = null;

        StringTokenizer tok = new StringTokenizer( task, ":" );
        int numTokens = tok.countTokens();
        if ( numTokens == 2 )
        {
            String prefix = tok.nextToken();
            goal = tok.nextToken();

            pluginDescriptor = pluginManager.verifyPlugin( prefix );

            if ( pluginDescriptor == null )
            {
                groupId = PluginDescriptor.getDefaultPluginGroupId();
                artifactId = PluginDescriptor.getDefaultPluginArtifactId( prefix );
            }
        }
        else if ( numTokens == 4 )
        {
            groupId = tok.nextToken();
            artifactId = tok.nextToken();
            version = tok.nextToken();
            goal = tok.nextToken();
        }
        else
        {
            String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or"
                + " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
            throw new LifecycleExecutionException( message );
        }

        if ( pluginDescriptor == null )
        {
            try
            {
                injectHandlerPluginConfiguration( session.getProject(), groupId, artifactId, version );

                pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, session.getProject(),
                                                               session.getSettings(), session.getLocalRepository() );
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
        else
        {
            injectHandlerPluginConfiguration( session.getProject(), pluginDescriptor.getGroupId(), pluginDescriptor
                .getArtifactId(), pluginDescriptor.getVersion() );
        }

        MojoDescriptor mojoDescriptor = null;

        if ( pluginDescriptor.getMojos() != null )
        {
            // TODO: should be able to create a Map from this
            for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext() && mojoDescriptor == null; )
            {
                MojoDescriptor desc = (MojoDescriptor) i.next();
                if ( desc.getGoal().equals( goal ) )
                {
                    mojoDescriptor = desc;
                }
            }
        }
        else
        {
            throw new LifecycleExecutionException( "The plugin " + pluginDescriptor.getGroupId() + ":"
                + pluginDescriptor.getArtifactId() + ":" + pluginDescriptor.getVersion()
                + " doesn't contain any mojo. Check if it isn't corrupted." );
        }

        if ( mojoDescriptor == null )
        {
            throw new LifecycleExecutionException( "Required goal not found: " + task );
        }

        return mojoDescriptor;
    }

    public List getPhases()
    {
        return phases;
    }

}
