package org.apache.maven.lifecycle;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.goal.GoalExecutionException;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultLifecycleExecutor
    implements LifecycleExecutor, Initializable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private ArtifactResolver artifactResolver;

    private MavenProjectBuilder projectBuilder;

    private PluginManager pluginManager;

    private List phases;

    private Map phaseMap;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    /**
     * Execute a list of tasks. Each task may be a phase in the lifecycle
     * or the execution of a mojo.
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
            for ( Iterator i = tasks.iterator(); i.hasNext(); )
            {
                String task = (String) i.next();

                PluginExecutionResponse pluginResponse;

                if ( phaseMap.containsKey( task ) )
                {
                    executePhase( task, session, response );
                }
                else
                {
                    pluginResponse = executeMojo( task, session );

                    if ( pluginResponse.isExecutionFailure() )
                    {
                        response.setExecutionFailure( task, pluginResponse.getFailureResponse() );
                        break;
                    }
                }
            }
        }
        catch ( LifecycleExecutionException e )
        {
            response.setException( e );
        }
        finally
        {
            response.setFinish( new Date() );
        }

        return response;
    }

    protected void executePhase( String phase, MavenSession session, MavenExecutionResponse response )
        throws LifecycleExecutionException
    {
        int i = phases.indexOf( phaseMap.get( phase ) );

        for ( int j = 0; j <= i; j++ )
        {
            Phase p = (Phase) phases.get( j );

            if ( p.getGoal() != null )
            {
                PluginExecutionResponse pluginResponse = executeMojo( p.getGoal(), session );

                if ( pluginResponse.isExecutionFailure() )
                {
                    response.setExecutionFailure( p.getGoal(), pluginResponse.getFailureResponse() );
                    break;
                }
            }
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
