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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.model.Goal;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.MojoExecution;
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

    private List phases;

    private Map defaultPhases;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a task. Each task may be a phase in the lifecycle or the
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
            for ( Iterator i = tasks.iterator(); i.hasNext(); )
            {
                String task = (String) i.next();
                executeGoal( task, session );
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
        finally
        {
            response.setFinish( new Date() );
        }

        return response;
    }

    private void executeGoal( String task, MavenSession session )
        throws LifecycleExecutionException, PluginNotFoundException, MojoExecutionException, ArtifactResolutionException
    {
        Map phaseMap = new HashMap();

        String selectedPhase = null;

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            String p = (String) i.next();

            // Make a copy of the phase as we will modify it
            phaseMap.put( p, new ArrayList() );

            if ( task.equals( p ) )
            {
                selectedPhase = p;
            }
        }

        List goals;

        // Need to verify all the plugins up front, as standalone goals should use the version from the POM.
        for ( Iterator i = session.getProject().getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            verifyPlugin( plugin, session );
        }

        if ( selectedPhase != null )
        {
            // we have a lifecycle phase, so lets bind all the necessary goals
            constructLifecyclePhaseMap( session, phaseMap, selectedPhase );

            goals = processGoalChain( selectedPhase, phaseMap );
        }
        else
        {
            MojoDescriptor mojoDescriptor = getMojoDescriptor( task, session );
            goals = Collections.singletonList( new MojoExecution( mojoDescriptor ) );
        }

        for ( Iterator i = goals.iterator(); i.hasNext(); )
        {
            MojoExecution mojoExecution = (MojoExecution) i.next();

            String executePhase = mojoExecution.getMojoDescriptor().getExecutePhase();

            if ( executePhase != null )
            {
                // TODO: with introduction of cloned lifecyle, we want to avoid reconstructing some things - narrow
                executeGoal( executePhase, session );
            }

            try
            {
                pluginManager.executeMojo( mojoExecution, session );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }
        }
    }

    private void constructLifecyclePhaseMap( MavenSession session, Map phaseMap, String selectedPhase )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        // first, bind those associated with the packaging
        bindLifecycleForPackaging( session, phaseMap, selectedPhase );

        // next, loop over plugins and for any that have a phase, bind it
        for ( Iterator i = session.getProject().getBuildPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            bindPluginToLifecycle( plugin, session, phaseMap );
        }
    }

    private void bindLifecycleForPackaging( MavenSession session, Map phaseMap, String selectedPhase )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        Map mappings;
        String packaging = session.getProject().getPackaging();
        try
        {
            LifecycleMapping m = (LifecycleMapping) session.lookup( LifecycleMapping.ROLE, packaging );
            mappings = m.getPhases();
        }
        catch ( ComponentLookupException e )
        {
            getLogger().error( "No lifecycle mapping for type '" + packaging + "': using defaults" );
            mappings = defaultPhases;
        }

        boolean finished = false;
        for ( Iterator i = phases.iterator(); i.hasNext() && !finished; )
        {
            String phase = (String) i.next();

            String phaseTasks = (String) mappings.get( phase );

            if ( phaseTasks != null )
            {
                for ( StringTokenizer tok = new StringTokenizer( phaseTasks, "," ); tok.hasMoreTokens(); )
                {
                    String goal = tok.nextToken().trim();

                    MojoDescriptor mojoDescriptor = getMojoDescriptor( goal, session );
                    addToPhaseMap( phaseMap, phase, new MojoExecution( mojoDescriptor ), session.getSettings() );
                }
            }

            if ( phase.equals( selectedPhase ) )
            {
                finished = true;
            }
        }
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for that given phase.
     *
     * @param session
     */
    private void bindPluginToLifecycle( Plugin plugin, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        if ( plugin.getGoals() != null && !plugin.getGoals().isEmpty() )
        {
            getLogger().warn(
                "DEPRECATED: goal definitions for plugin '" + plugin.getKey() + "' must be in an executions element" );
        }

        PluginDescriptor pluginDescriptor;
        Settings settings = session.getSettings();

        pluginDescriptor = verifyPlugin( plugin, session );

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

    private PluginDescriptor verifyPlugin( Plugin plugin, MavenSession session )
        throws ArtifactResolutionException, LifecycleExecutionException
    {
        String groupId = plugin.getGroupId();

        String artifactId = plugin.getArtifactId();

        String version = plugin.getVersion();

        PluginDescriptor pluginDescriptor;
        try
        {
            MavenProject project = session.getProject();
            ArtifactRepository localRepository = session.getLocalRepository();
            pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, project, session.getSettings(),
                                                           localRepository );
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
        throws LifecycleExecutionException
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
                        addToPhaseMap( phaseMap, mojoDescriptor.getPhase(), mojoExecution, settings );
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
                if ( execution.getPhase() != null )
                {
                    addToPhaseMap( phaseMap, execution.getPhase(), mojoExecution, settings );
                }
                else if ( mojoDescriptor.getPhase() != null )
                {
                    // if the phase was not in the configuration, use the phase in the descriptor
                    addToPhaseMap( phaseMap, mojoDescriptor.getPhase(), mojoExecution, settings );
                }
            }
        }
    }

    private void addToPhaseMap( Map phaseMap, String phase, MojoExecution mojoExecution, Settings settings )
        throws LifecycleExecutionException
    {
        List goals = (List) phaseMap.get( phase );

        if ( goals == null )
        {
            String message = "Required phase '" + phase + "' not found";
            throw new LifecycleExecutionException( message );
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
            String message = "Invalid task '" + task + "': you must specify a valid lifecycle phase, or" +
                " a goal in the format plugin:goal or pluginGroupId:pluginArtifactId:pluginVersion:goal";
            throw new LifecycleExecutionException( message );
        }

        MavenProject project = session.getProject();
        if ( pluginDescriptor == null )
        {
            try
            {
                pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, project,
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

        injectHandlerPluginConfiguration( project, pluginDescriptor.getGroupId(), pluginDescriptor.getArtifactId(),
                                          pluginDescriptor.getVersion() );

        MojoDescriptor mojoDescriptor = pluginDescriptor.getMojo( goal );
        if ( mojoDescriptor == null )
        {
            throw new LifecycleExecutionException( "Required goal not found: " + task );
        }

        return mojoDescriptor;
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
}
