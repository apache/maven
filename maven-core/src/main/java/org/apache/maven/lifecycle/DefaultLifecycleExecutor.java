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
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultLifecycleExecutor.java,v 1.16 2005/03/04 09:04:25
 *          jdcasey Exp $
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private ArtifactResolver artifactResolver;

    private ArtifactHandlerManager artifactHandlerManager;

    private MavenProjectBuilder projectBuilder;

    private PluginManager pluginManager;

    private List phases;

    private Map phaseMap;

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
    {
        MavenExecutionResponse response = new MavenExecutionResponse();

        response.setStart( new Date() );

        try
        {
            // TODO: should enrich this with the type handler, but for now just
            // use "type" as is
            ArtifactHandler handler = artifactHandlerManager.getArtifactHandler( session.getProject().getPackaging() );

            if ( handler != null )
            {
                // TODO: perhaps each type should define their own lifecycle
                // completely, using the base as a default?
                // If so, remove both of these goals from type handler
                if ( handler.packageGoal() != null )
                {
                    verifyMojoPhase( handler.packageGoal(), session );
                }
                if ( handler.additionalPlugin() != null )
                {
                    processPluginPhases( "maven", handler.additionalPlugin(), session );
                }
            }

            processPluginConfiguration( session.getProject(), session );

            for ( Iterator i = tasks.iterator(); i.hasNext(); )
            {
                String task = (String) i.next();

                processGoalChain( task, session );

                if ( phaseMap.containsKey( task ) )
                {
                    executePhase( task, session, response );
                }
                else
                {
                    PluginExecutionResponse pluginResponse = executeMojo( task, session );

                    if ( pluginResponse.isExecutionFailure() )
                    {
                        response.setExecutionFailure( task, pluginResponse.getFailureResponse() );
                    }
                }
                if ( response.isExecutionFailure() )
                {
                    break;
                }
            }
        }
        catch ( Exception e )
        {
            response.setException( e );
        }
        finally
        {
            response.setFinish( new Date() );
        }

        return response;
    }

    // TODO: don't throw Exception
    private void processPluginConfiguration( MavenProject project, MavenSession mavenSession )
        throws Exception
    {
        for ( Iterator i = project.getPlugins().iterator(); i.hasNext(); )
        {
            Plugin plugin = (Plugin) i.next();

            // TODO: should this flag be used in verifyPlugin, completely disabling the plugin?
            if ( Boolean.TRUE != plugin.isDisabled() )
            {
                processPluginPhases( plugin.getGroupId(), plugin.getArtifactId(), mavenSession );
            }
        }
    }

    // TODO: don't throw Exception
    private void processPluginPhases( String groupId, String artifactId, MavenSession mavenSession )
        throws Exception
    {
        pluginManager.verifyPlugin( groupId, artifactId, mavenSession );
        PluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptor( groupId, artifactId );
        for ( Iterator j = pluginDescriptor.getMojos().iterator(); j.hasNext(); )
        {
            MojoDescriptor mojoDescriptor = (MojoDescriptor) j.next();

            // TODO: check if the goal exists in the configuration and is
            // disabled
            if ( mojoDescriptor.getPhase() != null )
            {
                Phase phase = (Phase) phaseMap.get( mojoDescriptor.getPhase() );
                phase.getGoals().add( mojoDescriptor.getId() );
            }
        }

    }

    private void processGoalChain( String task, MavenSession session )
        throws Exception
    {
        if ( phaseMap.containsKey( task ) )
        {
            // only execute up to the given phase
            int index = phases.indexOf( phaseMap.get( task ) );

            for ( int j = 0; j <= index; j++ )
            {
                Phase p = (Phase) phases.get( j );

                if ( p.getGoals() != null )
                {
                    for ( Iterator k = p.getGoals().iterator(); k.hasNext(); )
                    {
                        String goal = (String) k.next();

                        verifyMojoPhase( goal, session );
                    }
                }
            }
        }
        else
        {
            verifyMojoPhase( task, session );
        }
    }

    private void verifyMojoPhase( String task, MavenSession session )
        throws Exception
    {
        MojoDescriptor mojoDescriptor = pluginManager.getMojoDescriptor( task );
        if ( mojoDescriptor == null )
        {
            pluginManager.verifyPluginForGoal( task, session );
            mojoDescriptor = pluginManager.getMojoDescriptor( task );
            if ( mojoDescriptor == null )
            {
                throw new LifecycleExecutionException( "Required goal not found: " + task );
            }

            if ( mojoDescriptor.getPhase() != null )
            {
                Phase phase = (Phase) phaseMap.get( mojoDescriptor.getPhase() );
                phase.getGoals().add( task );
            }
        }
    }

    private void executePhase( String phase, MavenSession session, MavenExecutionResponse response )
        throws LifecycleExecutionException
    {
        // only execute up to the given phase
        int index = phases.indexOf( phaseMap.get( phase ) );

        EventDispatcher dispatcher = session.getEventDispatcher();

        for ( int j = 0; j <= index; j++ )
        {
            Phase p = (Phase) phases.get( j );

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

                        PluginExecutionResponse pluginResponse = executeMojo( goal, session );

                        if ( pluginResponse.isExecutionFailure() )
                        {
                            response.setExecutionFailure( goal, pluginResponse.getFailureResponse() );
                            return;
                        }
                    }
                }

                dispatcher.dispatchEnd( event, p.getId() );
            }
            catch ( LifecycleExecutionException e )
            {
                dispatcher.dispatchError( event, p.getId(), e );

                throw e;
            }
            // End event monitoring.
        }
    }

    protected PluginExecutionResponse executeMojo( String id, MavenSession session )
        throws LifecycleExecutionException
    {
        // ----------------------------------------------------------------------
        // We have something of the form <pluginId>:<mojoId>, so this might be
        // something like:
        //
        // clean:clean
        // idea:idea
        // archetype:create
        // ----------------------------------------------------------------------

        try
        {
            return pluginManager.executeMojo( session, id );
        }
        catch ( GoalExecutionException e )
        {
            throw new LifecycleExecutionException( "Problem executing " + id, e );
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public List getPhases()
    {
        return phases;
    }

    public Phase getPhase( String id )
    {
        return (Phase) phaseMap.get( id );
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void initialize()
        throws Exception
    {
        phaseMap = new HashMap();

        for ( Iterator i = phases.iterator(); i.hasNext(); )
        {
            Phase p = (Phase) i.next();

            phaseMap.put( p.getId(), p );
        }
    }
}