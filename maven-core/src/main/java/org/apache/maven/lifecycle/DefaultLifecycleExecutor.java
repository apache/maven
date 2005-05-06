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
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.settings.Settings;
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

    private ArtifactHandlerManager artifactHandlerManager;

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

                injectHandlerPluginConfiguration( project, additionalPluginGroupId, additionalPluginArtifactId, null );
            }
        }

        processPluginConfiguration( project, session, phaseMap );

        for ( Iterator i = tasks.iterator(); i.hasNext(); )
        {
            String task = (String) i.next();

            List goals = processGoalChain( task, session, phaseMap );

            for ( Iterator j = goals.iterator(); j.hasNext(); )
            {
                MojoDescriptor mojo = (MojoDescriptor) j.next();

                if ( mojo.getExecutePhase() != null )
                {
                    // TODO: is this too broad to execute?
                    execute( Collections.singletonList( mojo.getExecutePhase() ), session );
                }

                try
                {
                    pluginManager.executeMojo( session, mojo );
                }
                catch ( PluginManagerException e )
                {
                    throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
                }
            }
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

            Plugin def = (Plugin) project.getPluginManagement().getPluginsAsMap().get( key );
            if ( def != null )
            {
                modelDefaultsInjector.mergePluginWithDefaults( plugin, def );
            }

            project.addPlugin( plugin );
        }
    }

    private void processPluginConfiguration( MavenProject project, MavenSession mavenSession, Map phaseMap )
        throws LifecycleExecutionException, ArtifactResolutionException
    {
        for ( Iterator i = project.getBuildPlugins().iterator(); i.hasNext(); )
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

        String version = plugin.getVersion();

        PluginDescriptor pluginDescriptor;
        try
        {
            pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, session );
        }
        catch ( PluginManagerException e )
        {
            throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
        }

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
                throw new LifecycleExecutionException(
                    "The plugin " + artifactId + " was built with an older version of Maven" );
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
                    String message = "Required phase '" + mojoDescriptor.getPhase() + "' not found";
                    throw new LifecycleExecutionException( message );
                }
                phase.getGoals().add( mojoDescriptor.getId() );
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

                        goals.add( configureMojo( goal, session, phaseMap ) );

                    }
                }
            }
        }
        else
        {
            goals.add( configureMojo( task, session, phaseMap ) );
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

        if ( pluginDescriptor == null )
        {
            injectHandlerPluginConfiguration( session.getProject(), groupId, artifactId, version );
            try
            {
                pluginDescriptor = pluginManager.verifyPlugin( groupId, artifactId, version, session );
            }
            catch ( PluginManagerException e )
            {
                throw new LifecycleExecutionException( "Internal error in the plugin manager", e );
            }
        }

        MojoDescriptor mojoDescriptor = null;

        // TODO: should be able to create a Map from this
        for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext() && mojoDescriptor == null; )
        {
            MojoDescriptor desc = (MojoDescriptor) i.next();
            if ( desc.getGoal().equals( goal ) )
            {
                mojoDescriptor = desc;
            }
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