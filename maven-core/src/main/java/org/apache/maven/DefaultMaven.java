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
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResponse;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.model.Profile;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.profiles.AlwaysOnActivation;
import org.apache.maven.profiles.MavenProfilesBuilder;
import org.apache.maven.profiles.ProfilesConversionUtils;
import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.ReactorException;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.usability.ErrorDiagnoser;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    public static File userDir = new File( System.getProperty( "user.dir" ) );

    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    protected MavenProjectBuilder projectBuilder;

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    protected Map errorDiagnosers;

    protected MavenProfilesBuilder profilesBuilder;

    protected RuntimeInformation runtimeInformation;

    private static final long MB = 1024 * 1024;

    private static final int MS_PER_SEC = 1000;

    private static final int SEC_PER_MIN = 60;

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
        catch ( ComponentLifecycleException e )
        {
            throw new ReactorException( "Unable to configure Maven for execution", e );
        }

        EventDispatcher dispatcher = request.getEventDispatcher();

        String event = MavenEvents.REACTOR_EXECUTION;

        dispatcher.dispatchStart( event, request.getBaseDirectory() );

        ReactorManager rm;
        
        try
        {
            List files = getProjectFiles( request );

            List projects = collectProjects( files, request.getLocalRepository(), request.isRecursive(),
                                        request.getSettings() );
            
            // the reasoning here is that the list is still unsorted according to dependency, so the first project
            // SHOULD BE the top-level, or the one we want to start with if we're doing an aggregated build.

            if ( projects.isEmpty() )
            {
                List externalProfiles = getActiveExternalProfiles( null, request.getSettings() );

                MavenProject superProject = projectBuilder.buildStandaloneSuperProject( request.getLocalRepository(),
                                                                              externalProfiles );
                projects.add( superProject );
            }
            
            rm = new ReactorManager( projects );
            
            String requestFailureBehavior = request.getFailureBehavior();
            
            if ( requestFailureBehavior != null )
            {
                rm.setFailureBehavior( requestFailureBehavior );
            }
        }
        catch ( IOException e )
        {
            throw new ReactorException( "Error processing projects for the reactor: ", e );
        }
        catch ( ArtifactResolutionException e )
        {
            return dispatchErrorResponse( dispatcher, event, request.getBaseDirectory(), e );
        }
        catch ( ProjectBuildingException e )
        {
            return dispatchErrorResponse( dispatcher, event, request.getBaseDirectory(), e );
        }
        catch ( CycleDetectedException e )
        {
            return dispatchErrorResponse( dispatcher, event, request.getBaseDirectory(), e );
        }

        try
        {
            MavenSession session = createSession( request, rm );

            try
            {
                MavenExecutionResponse response = lifecycleExecutor.execute( session, rm, dispatcher );

                // TODO: is this perhaps more appropriate in the CLI?
                if ( response.isExecutionFailure() )
                {
                    dispatcher.dispatchError( event, request.getBaseDirectory(), response.getException() );

                    // TODO: yuck! Revisit when cleaning up the exception handling from the top down
                    Throwable exception = response.getException();

                    if ( ReactorManager.FAIL_AT_END.equals( rm.getFailureBehavior() ) && ( exception instanceof ReactorException ) )
                    {
                        logFailure( response, exception, null );
                        
                        if ( rm.hasMultipleProjects() )
                        {
                            writeReactorSummary( rm );
                        }
                    }
                    else if ( exception instanceof MojoExecutionException )
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

                    return response;
                }
                else
                {
                    logSuccess( response, rm );
                }
            }
            catch ( LifecycleExecutionException e )
            {
                throw new ReactorException( "Error executing project within the reactor", e );
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

    private void writeReactorSummary( ReactorManager rm )
    {
        // -------------------------
        // Reactor Summary:
        // -------------------------
        // o project-name...........FAILED
        // o project2-name..........SKIPPED (dependency build failed or was skipped)
        // o project-3-name.........SUCCESS
        
        line();
        getLogger().info( "Reactor Summary:" );
        line();
        
        for ( Iterator it = rm.getProjectsSortedByDependency().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();
            
            String id = project.getId();
            
            if ( rm.hasBuildFailure( id ) )
            {
                logReactorSummaryLine( project.getName(), "FAILED" );
            }
            else if ( rm.isBlackListed( id ) )
            {
                logReactorSummaryLine( project.getName(), "SKIPPED (dependency build failed or was skipped)" );
            }
            else
            {
                logReactorSummaryLine( project.getName(), "SUCCESS" );
            }
        }
        
        getLogger().info( "" );
        getLogger().info( "" );
    }

    private void logReactorSummaryLine( String name, String status )
    {
        StringBuffer messageBuffer = new StringBuffer();
        
        messageBuffer.append( name );
        
        int dotCount = 65;
        
        dotCount -= name.length();
        
        for ( int i = 0; i < dotCount; i++ )
        {
            messageBuffer.append( '.' );
        }
        
        messageBuffer.append( status );
        
        getLogger().info( messageBuffer.toString() );
    }

    private MavenExecutionResponse dispatchErrorResponse( EventDispatcher dispatcher, String event, String baseDirectory, Exception e )
    {
        dispatcher.dispatchError( event, baseDirectory, e );

        MavenExecutionResponse response = new MavenExecutionResponse();
        response.setStart( new Date() );
        response.setFinish( new Date() );
        response.setException( e );
        logFailure( response, e, null );

        return response;
    }

    private List collectProjects( List files, ArtifactRepository localRepository, boolean recursive, Settings settings )
        throws ProjectBuildingException, ReactorException, IOException, ArtifactResolutionException
    {
        List projects = new ArrayList( files.size() );

        for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
        {
            File file = (File) iterator.next();

            if ( RELEASE_POMv4.equals( file.getName() ) )
            {
                getLogger().info( "NOTE: Using release-pom: " + file + " in reactor build." );
            }

            MavenProject project = getProject( file, localRepository, settings );

            if ( project.getPrerequesites() != null && project.getPrerequesites().getMaven() != null )
            {
                DefaultArtifactVersion version = new DefaultArtifactVersion( project.getPrerequesites().getMaven() );
                if ( runtimeInformation.getApplicationVersion().compareTo( version ) < 0 )
                {
                    throw new ProjectBuildingException( "Unable to build project '" + project.getFile() +
                        "; it requires Maven version " + version.toString() );
                }
            }

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

                List collectedProjects = collectProjects( moduleFiles, localRepository, recursive, settings );
                projects.addAll( collectedProjects );
                project.setCollectedProjects( collectedProjects );
            }
            projects.add( project );
        }

        return projects;
    }

    public MavenProject getProject( File pom, ArtifactRepository localRepository, Settings settings )
        throws ProjectBuildingException, ArtifactResolutionException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException(
                    "The file " + pom.getAbsolutePath() + " you specified has zero length." );
            }
        }

        List externalProfiles = getActiveExternalProfiles( pom, settings );

        return projectBuilder.build( pom, localRepository, externalProfiles );
    }

    private List getActiveExternalProfiles( File pom, Settings settings )
        throws ProjectBuildingException
    {
        // TODO: apply profiles.xml and settings.xml Profiles here.
        List externalProfiles = new ArrayList();

        List settingsProfiles = settings.getProfiles();

        if ( settingsProfiles != null && !settingsProfiles.isEmpty() )
        {
            List settingsActiveProfileIds = settings.getActiveProfiles();

            for ( Iterator it = settings.getProfiles().iterator(); it.hasNext(); )
            {
                org.apache.maven.settings.Profile rawProfile = (org.apache.maven.settings.Profile) it.next();

                Profile profile = SettingsUtils.convertFromSettingsProfile( rawProfile );

                if ( settingsActiveProfileIds.contains( rawProfile.getId() ) )
                {
                    profile.setActivation( new AlwaysOnActivation() );
                }

                externalProfiles.add( profile );
            }
        }

        if ( pom != null )
        {
            try
            {
                ProfilesRoot root = profilesBuilder.buildProfiles( pom.getParentFile() );

                if ( root != null )
                {
                    for ( Iterator it = root.getProfiles().iterator(); it.hasNext(); )
                    {
                        org.apache.maven.profiles.Profile rawProfile = (org.apache.maven.profiles.Profile) it.next();

                        externalProfiles.add( ProfilesConversionUtils.convertFromProfileXmlProfile( rawProfile ) );
                    }
                }
            }
            catch ( IOException e )
            {
                throw new ProjectBuildingException( "Cannot read profiles.xml resource for pom: " + pom, e );
            }
            catch ( XmlPullParserException e )
            {
                throw new ProjectBuildingException( "Cannot parse profiles.xml resource for pom: " + pom, e );
            }
        }

        return externalProfiles;
    }

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the
    // session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request, ReactorManager rpm )
    {
        return new MavenSession( container, request.getSettings(), request.getLocalRepository(),
                                 request.getEventDispatcher(), rpm, request.getGoals(),
                                 request.getBaseDirectory() );
    }

    /**
     * @todo [BP] this might not be required if there is a better way to pass
     * them in. It doesn't feel quite right.
     * @todo [JC] we should at least provide a mapping of protocol-to-proxy for
     * the wagons, shouldn't we?
     */
    private void resolveParameters( Settings settings )
        throws ComponentLookupException, ComponentLifecycleException
    {
        WagonManager wagonManager = (WagonManager) container.lookup( WagonManager.ROLE );

        try
        {
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
        finally
        {
            container.release( wagonManager );
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

    protected void logSuccess( MavenExecutionResponse r, ReactorManager rm )
    {
        if ( rm.hasMultipleProjects() )
        {
            writeReactorSummary( rm );
        }
        
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

        //noinspection CallToSystemGC
        System.gc();

        Runtime r = Runtime.getRuntime();

        getLogger().info(
            "Final Memory: " + ( r.totalMemory() - r.freeMemory() ) / MB + "M/" + r.totalMemory() / MB + "M" );
    }

    protected void line()
    {
        getLogger().info( "----------------------------------------------------------------------------" );
    }
    
    protected static String formatTime( long ms )
    {
        long secs = ms / MS_PER_SEC;

        long min = secs / SEC_PER_MIN;

        secs = secs % SEC_PER_MIN;

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
        else if ( min == 0 )
        {
            msg += "< 1 second";
        }
        return msg;
    }

    private List getProjectFiles( MavenExecutionRequest request )
        throws IOException
    {
        List files = Collections.EMPTY_LIST;

        if ( request.isReactorActive() )
        {
            // TODO: should we now include the pom.xml in the current directory?
//            String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 );
//            String excludes = System.getProperty( "maven.reactor.excludes", POMv4 );

            String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 + ",**/" + RELEASE_POMv4 );
            String excludes = System.getProperty( "maven.reactor.excludes", POMv4 + "," + RELEASE_POMv4 );

            files = FileUtils.getFiles( userDir, includes, excludes );

            filterOneProjectFilePerDirectory( files );

            // make sure there is consistent ordering on all platforms, rather than using the filesystem ordering
            Collections.sort( files );
        }
        else if ( request.getPomFile() != null )
        {
            File projectFile = new File( request.getPomFile() ).getAbsoluteFile();

            if ( projectFile.exists() )
            {
                files = Collections.singletonList( projectFile );
            }
        }
        else
        {
            File projectFile = new File( userDir, RELEASE_POMv4 );

            if ( !projectFile.exists() )
            {
                projectFile = new File( userDir, POMv4 );
            }

            if ( projectFile.exists() )
            {
                files = Collections.singletonList( projectFile );
            }
        }

        return files;
    }

    private void filterOneProjectFilePerDirectory( List files )
    {
        List releaseDirs = new ArrayList();

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            File projectFile = (File) it.next();

            if ( RELEASE_POMv4.equals( projectFile.getName() ) )
            {
                releaseDirs.add( projectFile.getParentFile() );
            }
        }

        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            File projectFile = (File) it.next();

            // remove pom.xml files where there is a sibling release-pom.xml file...
            if ( !RELEASE_POMv4.equals( projectFile.getName() ) && releaseDirs.contains( projectFile.getParentFile() ) )
            {
                it.remove();
            }
        }
    }
}
