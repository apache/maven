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
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultLifecycleExecutor.java,v 1.16 2005/03/04 09:04:25
 *          jdcasey Exp $
 * @todo there is some duplication between this and the plugin manager
 */
public class DefaultLifecycleExecutor
    extends AbstractLogEnabled
    implements LifecycleExecutor
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private ArtifactResolver artifactResolver;

    private ArtifactHandlerManager artifactHandlerManager;

    private MavenProjectBuilder projectBuilder;

    private PluginManager pluginManager;

    private List phases;

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
        catch ( ArtifactHandlerNotFoundException e )
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
        throws ArtifactHandlerNotFoundException, LifecycleExecutionException, PluginNotFoundException,
        MojoExecutionException, ArtifactResolutionException
    {
        Map phaseMap = new HashMap();

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            Phase p = (Phase) i.next();

            // Make a copy of the phase as we will modify it
            phaseMap.put( p.getId(), new Phase( p ) );
        }

        MavenProject project = session.getProject();

        ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler( project.getPackaging() );

        if ( artifactHandler != null )
        {
            if ( artifactHandler.packageGoal() != null )
            {
                configureMojo( artifactHandler.packageGoal(), session, phaseMap );
            }

            if ( artifactHandler.additionalPlugin() != null )
            {
                String additionalPluginGroupId = PluginDescriptor.getDefaultPluginGroupId();

                String additionalPluginArtifactId = PluginDescriptor.getDefaultPluginArtifactId(
                    artifactHandler.additionalPlugin() );

                injectHandlerPluginConfiguration( project, additionalPluginGroupId, additionalPluginArtifactId );
            }
        }

        processPluginConfiguration( project, session, phaseMap );

        for ( Iterator i = tasks.iterator(); i.hasNext(); )
        {
            String task = (String) i.next();

            processGoalChain( task, session, phaseMap );

            try
            {
                if ( phaseMap.containsKey( task ) )
                {
                    executePhase( task, session, phaseMap );
                }
                else
                {
                    executeMojo( task, session );
                }
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }
        }
    }

    private void injectHandlerPluginConfiguration( MavenProject project, String groupId, String artifactId )
    {
        // TODO: use the model injector, or just lookup the versions from the project?
        // They need to be injected, but we should track the required plugins first, then just sweep through.

        // TODO: this is a bit of a hack to get the version from plugin management - please fix

        Plugin plugin = findPlugin( project.getPlugins(), groupId, artifactId );

        if ( plugin == null )
        {
            plugin = new Plugin();
            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );
            project.addPlugin( plugin );
        }

        if ( plugin.getVersion() == null )
        {
            while ( project != null )
            {
                PluginManagement pluginManagement = project.getPluginManagement();

                if ( pluginManagement != null )
                {
                    Plugin management = findPlugin( pluginManagement.getPlugins(), groupId, artifactId );
                    if ( management != null && management.getVersion() != null )
                    {
                        plugin.setVersion( management.getVersion() );
                        break;
                    }
                }
                project = project.getParent();
            }

            if ( plugin.getVersion() == null )
            {
                // TODO: this has probably supplanted the default in the plugin manager
                plugin.setVersion( PluginDescriptor.getDefaultPluginVersion() );
            }
        }

    }

    private static Plugin findPlugin( List plugins, String groupId, String artifactId )
    {
        Plugin plugin = null;

        for ( Iterator i = plugins.iterator(); i.hasNext() && plugin == null; )
        {
            Plugin p = (Plugin) i.next();
            if ( groupId.equals( p.getGroupId() ) && artifactId.equals( p.getArtifactId() ) )
            {
                plugin = p;
            }
        }
        return plugin;
    }

    private void processPluginConfiguration( MavenProject project, MavenSession mavenSession, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        for ( Iterator i = project.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            processPluginPhases( plugin, mavenSession, phaseMap );
        }
    }

    /**
     * Take each mojo contained with a plugin, look to see whether it contributes to a
     * phase in the lifecycle and if it does place it at the end of the list of goals
     * to execute for that given phase.
     *
     * @param session
     */
    private void processPluginPhases( Plugin plugin, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        String groupId = plugin.getGroupId();

        String artifactId = plugin.getArtifactId();

        try
        {
            pluginManager.verifyPlugin( groupId, artifactId, session );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
        }

        PluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptor( groupId, artifactId );

        // ----------------------------------------------------------------------
        // Look to see if the plugin configuration specifies particular mojos
        // within the plugin. If this is the case then simply configure the
        // mojos the user has specified and ignore the rest.
        // ----------------------------------------------------------------------

        for ( Iterator j = pluginDescriptor.getMojos().iterator(); j.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) j.next();

            // TODO: remove later
            if ( mojoDescriptor.getGoal() == null )
            {
                throw new LifecycleExecutionException( "The plugin " + artifactId + " was built with an older version of Maven" );
            }

            if ( plugin.getGoals().isEmpty() || plugin.getGoalsAsMap().containsKey( mojoDescriptor.getGoal() ) )
            {
                configureMojoPhaseBinding( mojoDescriptor, phaseMap, session.getSettings() );
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
        if ( settings.getActiveProfile().isOffline() && mojoDescriptor.requiresOnline() )
        {
            String goal = mojoDescriptor.getGoal();
            getLogger().warn( goal + " requires online mode, but maven is currently offline. Disabling " + goal + "." );
        }
        else
        {
            if ( mojoDescriptor.getPhase() != null )
            {
                Phase phase = (Phase) phaseMap.get( mojoDescriptor.getPhase() );

                if ( phase == null )
                {
                    throw new LifecycleExecutionException(
                        "Required phase '" + mojoDescriptor.getPhase() + "' not found" );
                }
                phase.getGoals().add( mojoDescriptor.getId() );
            }
        }
    }

    private void processGoalChain( String task, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        if ( phaseMap.containsKey( task ) )
        {
            // only execute up to the given phase
            int index = phases.indexOf( phaseMap.get( task ) );

            for ( int j = 0; j <= index; j++ )
            {
                // TODO: phases should just be strings...
                Phase p = (Phase) phases.get( j );

                p = (Phase) phaseMap.get( p.getId() );

                if ( p.getGoals() != null )
                {
                    for ( Iterator k = p.getGoals().iterator(); k.hasNext(); )
                    {
                        String goal = (String) k.next();

                        configureMojo( goal, session, phaseMap );
                    }
                }
            }
        }
        else
        {
            configureMojo( task, session, phaseMap );
        }
    }

    private void configureMojo( String task, MavenSession session, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( task );

        if ( mojoDescriptor == null )
        {
            String groupId = PluginDescriptor.getDefaultPluginGroupId();

            String artifactId = PluginDescriptor.getPluginArtifactIdFromGoal( task );

            injectHandlerPluginConfiguration( session.getProject(), groupId, artifactId );

            try
            {
                pluginManager.verifyPlugin( groupId, artifactId, session );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }

            mojoDescriptor = pluginManager.getMojoDescriptor( task );

            if ( mojoDescriptor == null )
            {
                throw new LifecycleExecutionException( "Required goal not found: " + task );
            }
        }

        configureMojoPhaseBinding( mojoDescriptor, phaseMap, session.getSettings() );
    }

    private void executePhase( String phase, MavenSession session, Map phaseMap )
        throws MojoExecutionException, PluginNotFoundException, PluginManagerException, ArtifactResolutionException,
        LifecycleExecutionException
    {
        // only execute up to the given phase
        int index = phases.indexOf( phaseMap.get( phase ) );

        EventDispatcher dispatcher = session.getEventDispatcher();

        for ( int j = 0; j <= index; j++ )
        {
            Phase p = (Phase) phases.get( j );

            p = (Phase) phaseMap.get( p.getId() );

            String event = MavenEvents.PHASE_EXECUTION;

            // !! This is ripe for refactoring to an aspect.
            // Event monitoring.
            dispatcher.dispatchStart( event, p.getId() );
            try
            {
                if ( p.getGoals() != null )
                {
                    for ( Iterator i = p.getGoals().iterator(); i.hasNext(); )
                    {
                        String goal = (String) i.next();

                        executeMojo( goal, session );
                    }
                }
            }
            catch ( MojoExecutionException e )
            {
                dispatcher.dispatchError( event, p.getId(), e );
                throw e;
            }
            catch ( PluginManagerException e )
            {
                dispatcher.dispatchError( event, p.getId(), e );
                throw e;
            }
            catch ( ArtifactResolutionException e )
            {
                dispatcher.dispatchError( event, p.getId(), e );
                throw e;
            }

            dispatcher.dispatchEnd( event, p.getId() );
        }
    }

    protected void executeMojo( String id, MavenSession session )
        throws MojoExecutionException, PluginNotFoundException, PluginManagerException, ArtifactResolutionException,
        LifecycleExecutionException
    {
        Logger logger = getLogger();
        logger.debug( "Resolving artifacts from:" );
        logger.debug( "\t{localRepository: " + session.getLocalRepository() + "}" );
        logger.debug( "\t{remoteRepositories: " + session.getRemoteRepositories() + "}" );

        pluginManager.verifyPluginForGoal( id, session );

        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( id );

        if ( mojoDescriptor == null )
        {
            throw new MojoExecutionException( "Unable to find goal: " + id );
        }

        if ( mojoDescriptor.getExecutePhase() != null )
        {
            // TODO: is this too broad to execute?
            execute( Collections.singletonList( mojoDescriptor.getExecutePhase() ), session );
        }

        pluginManager.executeMojo( session, mojoDescriptor );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getPhases()
    {
        return phases;
    }

}