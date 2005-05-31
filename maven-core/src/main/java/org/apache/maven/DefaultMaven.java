package org.apache.maven;

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

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.settings.Mirror;
import org.apache.maven.model.settings.Proxy;
import org.apache.maven.model.settings.Server;
import org.apache.maven.model.settings.Settings;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectSorter;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.usability.ErrorDiagnoser;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.i18n.I18N;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 * @todo unify error reporting. We should return one response, always - and let the CLI decide how to render it. The reactor response should contain individual project responses
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

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    protected Map errorDiagnosers;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    public MavenExecutionResponse execute( MavenExecutionRequest request )
        throws ReactorException
    {
        if ( request.getSettings().isOffline() )
        {
            getLogger().info( "Maven is running in offline mode." );
        }

        try
        {
            resolveParameters( request.getSettings() );
        }
        catch ( ComponentLookupException e )
        {
            throw new ReactorException( "Unable to configure Maven for execution", e );
        }

        EventDispatcher dispatcher = request.getEventDispatcher();
        String event = MavenEvents.REACTOR_EXECUTION;

        dispatcher.dispatchStart( event, request.getBaseDirectory() );

        List projects;

        try
        {
            projects = collectProjects( request.getFiles(), request.getLocalRepository(), request.isRecursive() );

            projects = ProjectSorter.getSortedProjects( projects );

            if ( projects.isEmpty() )
            {
                projects.add( projectBuilder.buildStandaloneSuperProject( request.getLocalRepository() ) );
            }
        }
        catch ( IOException e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }
        catch ( CycleDetectedException e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }
        catch ( ArtifactResolutionException e )
        {
            dispatcher.dispatchError( event, request.getBaseDirectory(), e );

            MavenExecutionResponse response = new MavenExecutionResponse();
            response.setStart( new Date() );
            response.setFinish( new Date() );
            response.setException( e );
            logFailure( response, e, null );

            return response;
        }
        catch ( ProjectBuildingException e )
        {
            dispatcher.dispatchError( event, request.getBaseDirectory(), e );

            MavenExecutionResponse response = new MavenExecutionResponse();
            response.setStart( new Date() );
            response.setFinish( new Date() );
            response.setException( e );
            logFailure( response, e, null );

            return response;
        }

        try
        {
            for ( Iterator iterator = projects.iterator(); iterator.hasNext(); )
            {
                MavenProject project = (MavenProject) iterator.next();

                line();

                getLogger().info( "Building " + project.getName() );

                line();

                try
                {
                    MavenExecutionResponse response = processProject( request, project, dispatcher );
                    if ( response.isExecutionFailure() )
                    {
                        dispatcher.dispatchError( event, request.getBaseDirectory(), response.getException() );

                        return response;
                    }
                }
                catch ( LifecycleExecutionException e )
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
        throws ProjectBuildingException, ReactorException, IOException, ArtifactResolutionException
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

                File basedir = file.getParentFile();

                // Initial ordering is as declared in the modules section
                List moduleFiles = new ArrayList( project.getModules().size() );
                for ( Iterator i = project.getModules().iterator(); i.hasNext(); )
                {
                    String name = (String) i.next();
                    moduleFiles.add( new File( basedir, name + "/pom.xml" ) );
                }

                List collectedProjects = collectProjects( moduleFiles, localRepository, recursive );
                projects.addAll( collectedProjects );
                project.setCollectedProjects( collectedProjects );
            }
            projects.add( project );
        }

        return projects;
    }

    private MavenExecutionResponse processProject( MavenExecutionRequest request, MavenProject project,
                                                   EventDispatcher dispatcher )
        throws LifecycleExecutionException
    {
        List goals = request.getGoals();

        MavenSession session = createSession( request, project );

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
        catch ( LifecycleExecutionException e )
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

            if ( exception instanceof MojoExecutionException )
            {
                if ( exception.getCause() == null )
                {
                    MojoExecutionException e = (MojoExecutionException) exception;
                    logFailure( response, e, e.getLongMessage() );
                }
                else
                {
                    // TODO: throw exceptions like this, so "failures" are just that
                    logError( response );
                }
            }
            else if ( exception instanceof ArtifactResolutionException )
            {
                logFailure( response, exception, null );
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

    public MavenProject getProject( File pom, ArtifactRepository localRepository )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException( i18n.format( "empty.descriptor.error", pom ) );
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
        return new MavenSession( project, container, request.getSettings(), request.getLocalRepository(),
                                 request.getEventDispatcher(), request.getGoals() );
    }

    /**
     * @todo [BP] this might not be required if there is a better way to pass
     * them in. It doesn't feel quite right.
     * @todo [JC] we should at least provide a mapping of protocol-to-proxy for
     * the wagons, shouldn't we?
     */
    private void resolveParameters( Settings settings )
        throws ComponentLookupException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

        Proxy proxy = settings.getActiveProxy();

        if ( proxy != null )
        {
            wagonManager.addProxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(), proxy.getUsername(),
                                   proxy.getPassword(), proxy.getNonProxyHosts() );
        }

        for ( Iterator i = settings.getServers().iterator(); i.hasNext(); )
        {
            Server server = (Server) i.next();

            wagonManager.addAuthenticationInfo( server.getId(), server.getUsername(), server.getPassword(),
                                                server.getPrivateKey(), server.getPassphrase() );
        }

        for ( Iterator i = settings.getMirrors().iterator(); i.hasNext(); )
        {
            Mirror mirror = (Mirror) i.next();

            wagonManager.addMirror( mirror.getId(), mirror.getMirrorOf(), mirror.getUrl() );
        }
    }

    // ----------------------------------------------------------------------
    // Lifecylce Management
    // ----------------------------------------------------------------------

    public void contextualize( Context context )
        throws ContextException
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
        
        Throwable error = r.getException();

        String message = null;
        if ( errorDiagnosers != null )
        {
            for ( Iterator it = errorDiagnosers.values().iterator(); it.hasNext(); )
            {
                ErrorDiagnoser diagnoser = (ErrorDiagnoser) it.next();

                if ( diagnoser.canDiagnose( error ) )
                {
                    message = diagnoser.diagnose( error );
                }
            }
        }

        if ( message == null )
        {
            message = error.getMessage();
        }

        getLogger().info( "Diagnosis: " + message );
        
        line();
        
        getLogger().error( "Cause: ", r.getException() );

        line();

        stats( r.getStart(), r.getFinish() );

        line();
    }

    protected void logFailure( MavenExecutionResponse r, Throwable error, String longMessage )
    {
        line();

        getLogger().info( "BUILD FAILURE" );

        line();

        String message = null;
        if ( errorDiagnosers != null )
        {
            for ( Iterator it = errorDiagnosers.values().iterator(); it.hasNext(); )
            {
                ErrorDiagnoser diagnoser = (ErrorDiagnoser) it.next();

                if ( diagnoser.canDiagnose( error ) )
                {
                    message = diagnoser.diagnose( error );
                }
            }
        }

        if ( message == null )
        {
            message = "Reason: " + error.getMessage();
        }

        getLogger().info( message );

        line();

        if ( longMessage != null )
        {
            getLogger().info( longMessage );

            line();
        }

        // TODO: needs to honour -e
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Trace", error );

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

        getLogger().info( "Final Memory: " + ( ( r.totalMemory() - r.freeMemory() ) / mb ) + "M/" +
                          ( r.totalMemory() / mb ) + "M" );
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

        String msg = "";

        if ( min > 1 )
        {
            msg = min + " minutes ";
        }
        else if ( min == 1 )
        {
            msg = "1 minute ";
        }

        if ( secs > 1 )
        {
            msg += secs + " seconds";
        }
        else if ( secs == 1 )
        {
            msg += "1 second";
        }
        else
        {
            msg += "< 1 second";
        }
        return msg;
    }
}
