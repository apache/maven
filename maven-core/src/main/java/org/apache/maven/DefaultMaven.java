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
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.PluginExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.settings.MavenSettings;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
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

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    protected ArtifactRepositoryFactory artifactRepositoryFactory;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    public MavenExecutionResponse execute( MavenExecutionRequest request ) throws ReactorException
    {
        EventDispatcher dispatcher = request.getEventDispatcher();
        String event = MavenEvents.REACTOR_EXECUTION;

        // TODO: goals are outer loop
        dispatcher.dispatchStart( event, request.getBaseDirectory() );
        try
        {
            List projects;

            try
            {
                projects = collectProjects( request.getFiles(), request.getLocalRepository(), request.isRecursive() );

                projects = MavenProject.getSortedProjects( projects );

                if ( projects.isEmpty() )
                {
                    projects.add( projectBuilder.buildStandaloneSuperProject( request.getLocalRepository() ) );
                }
            }
            catch ( IOException e )
            {
                throw new ReactorException( "Error processing projects for the reactor: ", e );
            }
            catch ( ProjectBuildingException e )
            {
                throw new ReactorException( "Error processing projects for the reactor: ", e );
            }
            catch ( CycleDetectedException e )
            {
                throw new ReactorException( "Error processing projects for the reactor: ", e );
            }

            for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
            {
                MavenProject project = (MavenProject) iterator.next();

                line();

                getLogger().info( "Building " + project.getName() );

                line();

                try
                {
                    MavenExecutionResponse response = processProject( request, project, dispatcher, request.getGoals() );
                    if ( response.isExecutionFailure() )
                    {
                        return response;
                    }
                }
                catch ( Exception e )
                {
                    throw new ReactorException( "Error executing project within the reactor", e );
                }
            }

            dispatcher.dispatchEnd( event, request.getBaseDirectory() );

            // TODO: not really satisfactory
            return null;
        }
        catch ( ReactorException e )
        {
            dispatcher.dispatchError( event, request.getBaseDirectory(), e );

            throw e;
        }
    }

    private List collectProjects( List files, ArtifactRepository localRepository, boolean recursive )
        throws ProjectBuildingException, ReactorException, IOException
    {
        List projects = new ArrayList( files.size() );

        for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
        {
            File file = (File) iterator.next();

            MavenProject project = getProject( file, localRepository );

            if ( project.getModules() != null && !project.getModules().isEmpty() && recursive )
            {
                // TODO: Really should fail if it was not? What if it is aggregating - eg "ear"?
                project.setPackaging( "pom" );

                String includes = StringUtils.join( project.getModules().iterator(), "/pom.xml," ) + "/pom.xml";

                if ( includes.indexOf( ".." ) >= 0 )
                {
                    throw new ReactorException( "Modules may not include '..'" );
                }

                List moduleFiles = FileUtils.getFiles( project.getFile().getParentFile(), includes, null );
                List collectedProjects = collectProjects( moduleFiles, localRepository, recursive );
                projects.addAll( collectedProjects );
                project.setCollectedProjects( collectedProjects );
            }
            projects.add( project );
        }

        return projects;
    }

    private MavenExecutionResponse processProject( MavenExecutionRequest request, MavenProject project,
                                                  EventDispatcher dispatcher, List goals ) throws Exception
    {
        MavenSession session = createSession( request, project );

        resolveParameters( request );

        // !! This is ripe for refactoring to an aspect.
        // Event monitoring.
        String event = MavenEvents.PROJECT_EXECUTION;

        dispatcher.dispatchStart( event, project.getId() );

        MavenExecutionResponse response = null;
        try
        {
            // Actual meat of the code.
            response = lifecycleExecutor.execute( goals, session );

            dispatcher.dispatchEnd( event, project.getId() );
        }
        catch ( Exception e )
        {
            dispatcher.dispatchError( event, project.getId(), e );
            throw e;
        }
        // End event monitoring.

        // TODO: is this perhaps more appropriate in the CLI?
        if ( response.isExecutionFailure() )
        {
            // TODO: yuck! Revisit when cleaning up the exception handling from the top down
            Throwable exception = response.getException();

            if ( exception instanceof PluginExecutionException )
            {
                if ( exception.getCause() == null )
                {
                    logFailure( response, (PluginExecutionException) exception );
                }
                else
                {
                    logError( response );
                }
            }
            else
            {
                // TODO: this should be a "FATAL" exception, reported to the
                // developers - however currently a LOT of
                // "user" errors fall through the cracks (like invalid POMs, as
                // one example)
                logError( response );
            }
        }
        else
        {
            logSuccess( response );
        }
        return response;
    }

    public MavenProject getProject( File pom, ArtifactRepository localRepository ) throws ProjectBuildingException
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

    //!! We should probably have the execution request handler create the
    // session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request, MavenProject project )
    {
        return new MavenSession( project, container, pluginManager, request.getSettings(),
                                 request.getLocalRepository(), request.getEventDispatcher(), request.getLog(),
                                 request.getGoals() );
    }

    /**
     * @todo [BP] this might not be required if there is a better way to pass
     * them in. It doesn't feel quite right.
     */
    private void resolveParameters( MavenExecutionRequest request ) throws ComponentLookupException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

        MavenSettings settings = request.getSettings();

        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            wagonManager.setProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                   proxy.getPassword(), proxy.getNonProxyHosts() );
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

    protected void logFailure( MavenExecutionResponse r, PluginExecutionException e )
    {
        line();

        getLogger().info( "BUILD FAILURE" );

        line();

        getLogger().info( "Reason: " + e.getMessage() );

        line();

        if ( e.getLongMessage() != null )
        {
            getLogger().info( e.getLongMessage() );

            line();
        }

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

        getLogger().info(
                          "Final Memory: " + ( ( r.totalMemory() - r.freeMemory() ) / mb ) + "M/"
                              + ( r.totalMemory() / mb ) + "M" );
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
}