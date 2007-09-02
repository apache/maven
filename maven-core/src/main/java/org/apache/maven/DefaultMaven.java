package org.apache.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.SystemBuildContext;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.ExecutionBuildContext;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.execution.SessionContext;
import org.apache.maven.extension.BuildExtensionScanner;
import org.apache.maven.extension.ExtensionScanningException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Settings;
import org.apache.maven.usability.diagnostics.ErrorDiagnostics;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * @author jason van zyl
 * @version $Id$
 * @todo EventDispatcher should be a component as it is internal to maven.
 */
public class DefaultMaven
    extends AbstractLogEnabled
    implements Maven, Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    protected BuildContextManager buildContextManager;

    protected MavenProjectBuilder projectBuilder;

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    protected ErrorDiagnostics errorDiagnostics;

    protected RuntimeInformation runtimeInformation;

    private BuildExtensionScanner buildExtensionScanner;

    private static final long MB = 1024 * 1024;

    private static final int MS_PER_SEC = 1000;

    private static final int SEC_PER_MIN = 60;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    // project build
    // artifact resolution
    // lifecycle execution

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        request.setStartTime( new Date() );

        initializeBuildContext( request );

        EventDispatcher dispatcher = new DefaultEventDispatcher( request.getEventMonitors() );

        String event = MavenEvents.REACTOR_EXECUTION;

        dispatcher.dispatchStart( event, request.getBaseDirectory() );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        getLogger().info( "Scanning for projects..." );

        boolean foundProjects = true;

        List projects;

        try
        {
            projects = getProjects( request );

            if ( projects.isEmpty() )
            {
                projects.add( getSuperProject( request ) );

                foundProjects = false;
            }
        }
        catch ( Exception e )
        {
            result.addException( e );

            return result;
        }

        ReactorManager reactorManager;

        try
        {
            reactorManager = new ReactorManager( projects, request.getReactorFailureBehavior() );
        }
        catch ( CycleDetectedException e )
        {
            result.addException( new BuildFailureException(
                "The projects in the reactor contain a cyclic reference: " + e.getMessage(), e ) );

            return result;
        }
        catch ( DuplicateProjectException e )
        {
            result.addException( new BuildFailureException( e.getMessage(), e ) );

            return result;
        }

        // Display the order of the projects
        //CLI:move

        if ( reactorManager.hasMultipleProjects() )
        {
            getLogger().info( "Reactor build order: " );

            for ( Iterator i = reactorManager.getSortedProjects().iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                getLogger().info( "  " + project.getName() );
            }
        }

        MavenSession session = createSession( request, reactorManager, dispatcher );

        session.setUsingPOMsFromFilesystem( foundProjects );

        try
        {
            lifecycleExecutor.execute( session, reactorManager, dispatcher );
        }
        catch ( Exception e )
        {
            result.addException( new BuildFailureException( e.getMessage(), e ) );
        }

        // old doExecute

        if ( result.hasExceptions() )
        {
            for ( Iterator i = result.getExceptions().iterator(); i.hasNext(); )
            {
                Exception e = (Exception) i.next();

                dispatcher.dispatchError( event, request.getBaseDirectory(), e );

                logError( e, request.isShowErrors() );

                stats( request.getStartTime() );

                line();
            }
        }

        // Either the build was successful, or it was a fail_at_end/fail_never reactor build

        // TODO: should all the logging be left to the CLI?
        logReactorSummary( reactorManager );

        if ( reactorManager != null && reactorManager.hasBuildFailures() )
        {
            logErrors( reactorManager, request.isShowErrors() );

            if ( !ReactorManager.FAIL_NEVER.equals( reactorManager.getFailureBehavior() ) )
            {
                dispatcher.dispatchError( event, request.getBaseDirectory(), null );

                getLogger().info( "BUILD ERRORS" );

                line();

                stats( request.getStartTime() );

                line();

                result.addException( new MavenExecutionException( "Some builds failed" ) );

                return result;
            }
            else
            {
                getLogger().info( " + Ignoring failures" );
            }
        }

        logSuccess( reactorManager );

        stats( request.getStartTime() );

        line();

        dispatcher.dispatchEnd( event, request.getBaseDirectory() );

        result.setTopologicallySortedProjects( reactorManager.getSortedProjects() );

        result.setProject( reactorManager.getTopLevelProject() );

        return result;
    }

    /**
     * Initialize some context objects to be stored in the container's context map for reference by
     * other Maven components (including custom components that need more information about the
     * build than is supplied to them by the APIs).
     */
    private void initializeBuildContext( MavenExecutionRequest request )
    {
        new ExecutionBuildContext( request ).store( buildContextManager );

        SystemBuildContext systemContext = SystemBuildContext.getSystemBuildContext( buildContextManager, true );

        systemContext.setSystemProperties( request.getProperties() );
        systemContext.store( buildContextManager );
    }

    private void logErrors( ReactorManager rm, boolean showErrors )
    {
        for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( rm.hasBuildFailure( project ) )
            {
                BuildFailure buildFailure = rm.getBuildFailure( project );

                getLogger().info(
                    "Error for project: " + project.getName() + " (during " + buildFailure.getTask() + ")" );

                line();

                logDiagnostics( buildFailure.getCause() );

                logTrace( buildFailure.getCause(), showErrors );
            }
        }

        if ( !showErrors )
        {
            getLogger().info( "For more information, run Maven with the -e switch" );

            line();
        }
    }

    private MavenProject getSuperProject( MavenExecutionRequest request )
        throws MavenExecutionException
    {
        MavenProject superProject;
        try
        {
            superProject = projectBuilder.buildStandaloneSuperProject();

        }
        catch ( ProjectBuildingException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
        return superProject;
    }

    private List getProjects( MavenExecutionRequest request )
        throws MavenExecutionException, BuildFailureException
    {
        List projects;

        List files;
        try
        {
            files = getProjectFiles( request );
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException( "Error selecting project files for the reactor: " + e.getMessage(), e );
        }

        // TODO: We should probably do this discovery just-in-time, if we can move to building project
        // instances just-in-time.
        try
        {
            buildExtensionScanner.scanForBuildExtensions( files, request.getLocalRepository(), request.getProfileManager() );
        }
        catch ( ExtensionScanningException e )
        {
            throw new MavenExecutionException( "Error scanning for extensions: " + e.getMessage(), e );
        }

        try
        {
            projects = collectProjects( files, request.getLocalRepository(), request.isRecursive(),
                                        request.getSettings(), request.getProfileManager(), !request.useReactor() );

        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
        catch ( ProfileActivationException e )
        {
            throw new MavenExecutionException( e.getMessage(), e );
        }
        return projects;
    }

    private void logReactorSummaryLine( String name, String status )
    {
        logReactorSummaryLine( name, status, -1 );
    }

    private void logReactorSummaryLine( String name, String status, long time )
    {
        StringBuffer messageBuffer = new StringBuffer();

        messageBuffer.append( name );

        int dotCount = 54;

        dotCount -= name.length();

        messageBuffer.append( " " );

        for ( int i = 0; i < dotCount; i++ )
        {
            messageBuffer.append( '.' );
        }

        messageBuffer.append( " " );

        messageBuffer.append( status );

        if ( time >= 0 )
        {
            messageBuffer.append( " [" );

            messageBuffer.append( getFormattedTime( time ) );

            messageBuffer.append( "]" );
        }

        getLogger().info( messageBuffer.toString() );
    }

    private static String getFormattedTime( long time )
    {
        String pattern = "s.SSS's'";
        if ( time / 60000L > 0 )
        {
            pattern = "m:s" + pattern;
            if ( time / 3600000L > 0 )
            {
                pattern = "H:m" + pattern;
            }
        }
        DateFormat fmt = new SimpleDateFormat( pattern );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return fmt.format( new Date( time ) );
    }

    private List collectProjects( List files, ArtifactRepository localRepository, boolean recursive, Settings settings,
                                  ProfileManager globalProfileManager, boolean isRoot )
        throws ArtifactResolutionException, ProjectBuildingException, ProfileActivationException,
        MavenExecutionException, BuildFailureException
    {
        List projects = new ArrayList( files.size() );

        for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
        {
            File file = (File) iterator.next();

            boolean usingReleasePom = false;

            if ( RELEASE_POMv4.equals( file.getName() ) )
            {
                getLogger().info( "NOTE: Using release-pom: " + file + " in reactor build." );
                usingReleasePom = true;
            }

            MavenProject project = getProject( file, localRepository, settings, globalProfileManager );

            if ( isRoot )
            {
                project.setExecutionRoot( true );
            }

            if ( project.getPrerequisites() != null && project.getPrerequisites().getMaven() != null )
            {
                DefaultArtifactVersion version = new DefaultArtifactVersion( project.getPrerequisites().getMaven() );
                if ( runtimeInformation.getApplicationVersion().compareTo( version ) < 0 )
                {
                    throw new BuildFailureException( "Unable to build project '" + project.getFile() +
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

                    if ( StringUtils.isEmpty( StringUtils.trim( name ) ) )
                    {
                        getLogger().warn(
                            "Empty module detected. Please check you don't have any empty module definitions in your POM." );

                        continue;
                    }

                    File moduleFile;

                    if ( usingReleasePom )
                    {
                        moduleFile = new File( basedir, name + "/" + Maven.RELEASE_POMv4 );
                    }
                    else
                    {
                        moduleFile = new File( basedir, name + "/" + Maven.POMv4 );
                    }

                    if ( Os.isFamily( Os.FAMILY_WINDOWS ) )
                    {
                        // we don't canonicalize on unix to avoid interfering with symlinks

                        try
                        {
                            moduleFile = moduleFile.getCanonicalFile();
                        }
                        catch ( IOException e )
                        {
                            throw new MavenExecutionException( "Unable to canonicalize file name " + moduleFile, e );
                        }
                    }

                    moduleFiles.add( moduleFile );
                }

                List collectedProjects =
                    collectProjects( moduleFiles, localRepository, recursive, settings, globalProfileManager, false );
                projects.addAll( collectedProjects );
                project.setCollectedProjects( collectedProjects );
            }
            projects.add( project );
        }

        return projects;
    }

    public MavenProject getProject( File pom, ArtifactRepository localRepository, Settings settings,
                                    ProfileManager globalProfileManager )
        throws ProjectBuildingException, ArtifactResolutionException, ProfileActivationException
    {
        if ( pom.exists() )
        {
            if ( pom.length() == 0 )
            {
                throw new ProjectBuildingException( "unknown", "The file " + pom.getAbsolutePath() +
                    " you specified has zero length." );
            }
        }

        return projectBuilder.build( pom, localRepository, globalProfileManager );
    }

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the
    // session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request, ReactorManager rpm,
                                          EventDispatcher dispatcher )
    {
        MavenSession session = new MavenSession( container, request, dispatcher, rpm );

        SessionContext ctx = new SessionContext( session );
        ctx.store( buildContextManager );

        return session;
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

    protected void logFatal( Throwable error )
    {
        line();

        getLogger().error( "FATAL ERROR" );

        line();

        logDiagnostics( error );

        logTrace( error, true );
    }

    protected void logError( Exception e, boolean showErrors )
    {
        line();

        getLogger().error( "BUILD ERROR" );

        line();

        logDiagnostics( e );

        logTrace( e, showErrors );

        if ( !showErrors )
        {
            getLogger().info( "For more information, run Maven with the -e switch" );

            line();
        }
    }

    protected void logFailure( BuildFailureException e, boolean showErrors )
    {
        line();

        getLogger().error( "BUILD FAILURE" );

        line();

        logDiagnostics( e );

        logTrace( e, showErrors );
    }

    private void logTrace( Throwable t, boolean showErrors )
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Trace", t );

            line();
        }
        else if ( showErrors )
        {
            getLogger().info( "Trace", t );

            line();
        }
    }

    private void logDiagnostics( Throwable t )
    {
        String message = null;
        if ( errorDiagnostics != null )
        {
            message = errorDiagnostics.diagnose( t );
        }

        if ( message == null )
        {
            message = t.getMessage();
        }

        getLogger().info( message );

        line();
    }

    protected void logSuccess( ReactorManager rm )
    {
        line();

        getLogger().info( "BUILD SUCCESSFUL" );

        line();
    }

    private void logReactorSummary( ReactorManager rm )
    {
        if ( rm != null && rm.hasMultipleProjects() && rm.executedMultipleProjects() )
        {
            getLogger().info( "" );
            getLogger().info( "" );

            // -------------------------
            // Reactor Summary:
            // -------------------------
            // o project-name...........FAILED
            // o project2-name..........SKIPPED (dependency build failed or was skipped)
            // o project-3-name.........SUCCESS

            line();
            getLogger().info( "Reactor Summary:" );
            line();

            for ( Iterator it = rm.getSortedProjects().iterator(); it.hasNext(); )
            {
                MavenProject project = (MavenProject) it.next();

                if ( rm.hasBuildFailure( project ) )
                {
                    logReactorSummaryLine( project.getName(), "FAILED", rm.getBuildFailure( project ).getTime() );
                }
                else if ( rm.isBlackListed( project ) )
                {
                    logReactorSummaryLine( project.getName(), "SKIPPED (dependency build failed or was skipped)" );
                }
                else if ( rm.hasBuildSuccess( project ) )
                {
                    logReactorSummaryLine( project.getName(), "SUCCESS", rm.getBuildSuccess( project ).getTime() );
                }
                else
                {
                    logReactorSummaryLine( project.getName(), "NOT BUILT" );
                }
            }
            line();
        }
    }

    protected void stats( Date start )
    {
        Date finish = new Date();

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
        getLogger().info( "------------------------------------------------------------------------" );
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

        File userDir = new File( request.getBaseDirectory() );

        if ( request.useReactor() )
        {
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
