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
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.GoalNotFoundException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Repository;
import org.apache.maven.model.user.ProxyProfile;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.model.user.UserModelUtils;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
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
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

    public MavenExecutionResponse execute( MavenExecutionRequest request )
        throws GoalNotFoundException, Exception
    {
        EventDispatcher dispatcher = request.getEventDispatcher();
        String event = MavenEvents.REACTOR_EXECUTION;

        // TODO: goals are outer loop
        dispatcher.dispatchStart( event, request.getBaseDirectory() );
        try
        {
            List projects = new ArrayList();

            try
            {
                List files = request.getProjectFiles();

                for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
                {
                    File file = (File) iterator.next();

                    MavenProject project = getProject( file, request.getLocalRepository() );

                    projects.add( project );
                }

                projects = projectBuilder.getSortedProjects( projects );

                if ( projects.isEmpty() )
                {
                    projects.add( projectBuilder.buildSuperProject( request.getLocalRepository() ) );
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
                    boolean isPom = "pom".equals( project.getPackaging() );
                    if ( isPom )
                    {
                        // TODO: not required if discovered and cached
                        MavenExecutionResponse response = processProject( request, project, dispatcher,
                                                                          Collections.singletonList( "pom:install" ) );
                        if ( response.isExecutionFailure() )
                        {
                            return response;
                        }
                    }

                    if ( project.getModules() != null && !project.getModules().isEmpty() )
                    {
                        String includes = StringUtils.join( project.getModules().iterator(), "/pom.xml," ) + "/pom.xml";

                        File baseDir = project.getFile().getParentFile();

                        MavenExecutionRequest reactorRequest = new DefaultMavenExecutionRequest(
                            request.getLocalRepository(),
                            request.getUserModel(),
                            request.getEventDispatcher(),
                            request.getGoals(),
                            FileUtils.getFiles( baseDir, includes, null ),
                            baseDir.getPath() );

                        MavenExecutionResponse response = execute( reactorRequest );

                        if ( response != null && response.isExecutionFailure() )
                        {
                            return response;
                        }
                    }

                    if ( !isPom )
                    {
                        MavenExecutionResponse response = processProject( request, project, dispatcher,
                                                                          request.getGoals() );

                        if ( response.isExecutionFailure() )
                        {
                            return response;
                        }
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

    private MavenExecutionResponse processProject( MavenExecutionRequest request, MavenProject project,
                                                   EventDispatcher dispatcher, List goals )
        throws ComponentLookupException
    {
        MavenSession session = createSession( request );

        session.setProject( project );
        
        session.setRemoteRepositories( getArtifactRepositories( project, request.getUserModel() ) );

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
        }
        // End event monitoring.

        // TODO: is this perhaps more appropriate in the CLI?
        if ( response.isExecutionFailure() )
        {
            if ( response.getException() != null )
            {
                // TODO: this should be a "FATAL" exception, reported to the
                // developers - however currently a LOT of
                // "user" errors fall through the cracks (like invalid POMs, as
                // one example)
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
        return response;
    }

    private List getArtifactRepositories( MavenProject project, UserModel userModel )
    {
        List remoteRepos = new ArrayList();
        for ( Iterator it = project.getRepositories().iterator(); it.hasNext(); )
        {
            Repository modelRepo = (Repository) it.next();
            remoteRepos.add( artifactRepositoryFactory.createArtifactRepository( modelRepo, userModel ) );
        }

        return remoteRepos;
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

    //!! We should probably have the execution request handler create the
    // session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request )
    {
        return new MavenSession( container, pluginManager, request.getUserModel(), request.getLocalRepository(),
                                 request.getEventDispatcher(), request.getLog(), request.getGoals() );
    }

    /**
     * @todo [BP] this might not be required if there is a better way to pass
     * them in. It doesn't feel quite right.
     */
    private void resolveParameters( MavenExecutionRequest request )
        throws ComponentLookupException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

        UserModel userModel = request.getUserModel();

        ProxyProfile proxyProfile = UserModelUtils.getActiveProxyProfile( userModel );

        if ( proxyProfile != null )
        {
            wagonManager.setProxy( proxyProfile.getProtocol(), proxyProfile.getHost(), proxyProfile.getPort(),
                                   proxyProfile.getUsername(), proxyProfile.getPassword(),
                                   proxyProfile.getNonProxyHosts() );
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
        throws CycleDetectedException
    {
        return projectBuilder.getSortedProjects( projects );
    }

}