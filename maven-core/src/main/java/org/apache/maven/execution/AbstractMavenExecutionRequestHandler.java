package org.apache.maven.execution;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.lifecycle.session.MavenSessionPhaseManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Date;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractMavenExecutionRequestHandler
    extends AbstractLogEnabled
    implements MavenExecutionRequestHandler, Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    protected MavenProjectBuilder projectBuilder;

    protected PluginManager pluginManager;

    protected PlexusContainer container;

    protected MavenSessionPhaseManager sessionPhaseManager;

    protected I18N i18n;

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request )
        throws Exception
    {
        MavenSession session = new MavenSession( container,
                                                 pluginManager,
                                                 request.getLocalRepository(),
                                                 request.getGoals() );

        return session;
    }

    /** @todo [BP] this might not be required if there is a better way to pass them in. It doesn't feel quite right. */
    private void resolveParameters( MavenExecutionRequest request )
        throws ComponentLookupException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );
        if ( request.getParameter( "maven.proxy.http.host" ) != null )
        {
            String p = request.getParameter( "maven.proxy.http.port" );
            int port = 8080;
            if ( p != null )
            {
                try
                {
                    port = Integer.valueOf( p ).intValue();
                }
                catch ( NumberFormatException e )
                {
                    getLogger().warn( "maven.proxy.http.port was not valid" );
                }
            }
            wagonManager.setProxy( "http", request.getParameter( "maven.proxy.http.host" ), port,
                                   request.getParameter( "maven.proxy.http.username" ),
                                   request.getParameter( "maven.proxy.http.password" ),
                                   request.getParameter( "maven.proxy.http.nonProxyHosts" ) );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void contextualize( Context context ) throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

    // ----------------------------------------------------------------------
    // Reporting / Logging
    // ----------------------------------------------------------------------

    protected void logError( MavenExecutionResponse r )
    {
        line();

        getLogger().error( "BUILD ERROR" );

        line();

        getLogger().error( "Cause: ", r.getException() );

        line();

        stats( r.getStart(), r.getFinish() );

        line();
    }

    protected void logFailure( MavenExecutionResponse r )
    {
        line();

        getLogger().info( "BUILD FAILURE" );

        line();

        getLogger().info( "Reason: " + r.getFailureResponse().shortMessage() );

        line();

        getLogger().info( r.getFailureResponse().longMessage() );

        line();

        stats( r.getStart(), r.getFinish() );

        line();
    }

    protected void logSuccess( MavenExecutionResponse r )
    {
        line();

        getLogger().info( "BUILD SUCCESSFUL" );

        line();

        stats( r.getStart(), r.getFinish() );

        line();
    }

    protected void stats( Date start, Date finish )
    {
        long time = finish.getTime() - start.getTime();

        getLogger().info( "Total time: " + formatTime( time ) );

        getLogger().info( "Finished at: " + finish );

        final long mb = 1024 * 1024;

        System.gc();

        Runtime r = Runtime.getRuntime();

        getLogger().info( "Final Memory: " + ( ( r.totalMemory() - r.freeMemory() ) / mb ) + "M/" + ( r.totalMemory() / mb ) + "M" );
    }

    protected void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
    }

    protected static String formatTime( long ms )
    {
        long secs = ms / 1000;

        long min = secs / 60;

        secs = secs % 60;

        if ( min > 0 )
        {
            return min + " minutes " + secs + " seconds";
        }
        else
        {
            return secs + " seconds";
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void handle( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
        try
        {
            request.setSession( createSession( request ) );

            response.setStart( new Date() );

            resolveParameters( request );

            sessionPhaseManager.execute( request, response );

            response.setFinish( new Date() );
        }
        catch ( Exception e )
        {
            response.setFinish( new Date() );

            response.setException( e );

            logError( response );

            return;
        }

        if ( response.isExecutionFailure() )
        {
            logFailure( response );
        }
        else
        {
            logSuccess( response );
        }
    }
}
