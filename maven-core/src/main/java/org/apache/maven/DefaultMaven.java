package org.apache.maven;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenReactorExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.lifecycle.goal.GoalNotFoundException;
import org.apache.maven.lifecycle.session.MavenSessionPhaseManager;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.ReactorException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class DefaultMaven
    extends AbstractLogEnabled
    implements Maven, Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    private I18N i18n;

    protected MavenProjectBuilder projectBuilder;

    protected PluginManager pluginManager;

    protected MavenSessionPhaseManager sessionPhaseManager;

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    public MavenExecutionResponse execute( MavenExecutionRequest request )
        throws GoalNotFoundException, Exception
    {
        MavenExecutionResponse response = new MavenExecutionResponse();

        handleProject( request );

        return response;
    }

    public void handleProject( MavenExecutionRequest request )
        throws Exception
    {
        MavenSession session = createSession( request );

        MavenProject project = getProject( (File) request.getProjectFiles().get( 0 ), request.getLocalRepository() );

        session.setProject( project );

        resolveParameters( request );

        MavenExecutionResponse response = lifecycleExecutor.execute( request.getGoals(), session );

        if ( response.isExecutionFailure() )
        {
            if ( response.getException() != null )
            {
                logError( response );
            }
            else
            {
                logFailure( response );
            }
        }
        else
        {
            logSuccess( response );
        }
    }

    // ----------------------------------------------------------------------
    // Reactor
    // ----------------------------------------------------------------------

    public void handleReactor( MavenExecutionRequest request, MavenExecutionResponse response )
        throws Exception
    {
        List projects = new ArrayList();

        getLogger().info( "Starting the reactor..." );

        try
        {
            List files = FileUtils.getFiles( new File( System.getProperty( "user.dir" ) ),
                                             ( (MavenReactorExecutionRequest) request ).getIncludes(),
                                             ( (MavenReactorExecutionRequest) request ).getExcludes() );

            for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
            {
                File file = (File) iterator.next();

                MavenProject project = getProject( file, request.getLocalRepository() );

                projects.add( project );
            }

            projects = projectBuilder.getSortedProjects( projects );
        }
        catch ( Exception e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }

        getLogger().info( "Our processing order:" );

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            getLogger().info( project.getName() );
        }

        for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();

            System.out.println( "\n\n\n" );

            line();

            getLogger().info( "Building " + project.getName() );

            line();

            //MavenProjectExecutionRequest projectExecutionRequest = request.createProjectExecutionRequest( project );

            //handleProject( projectExecutionRequest, response );

            if ( response.isExecutionFailure() )
            {
                break;
            }
        }
    }

    public MavenProject getProject( File pom, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException( i18n.format( "empty.descriptor.error", pom.getName() ) );
            }
        }

        return projectBuilder.build( pom, localRepository );
    }

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request )
        throws Exception
    {
        return new MavenSession( container, pluginManager, request.getLocalRepository(), request.getGoals() );
    }

    /**
     * @todo [BP] this might not be required if there is a better way to pass them in. It doesn't feel quite right.
     */
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

    // ----------------------------------------------------------------------
    // Reactor
    // ----------------------------------------------------------------------

    public List getSortedProjects( List projects )
        throws Exception
    {
        return projectBuilder.getSortedProjects( projects );
    }

}
