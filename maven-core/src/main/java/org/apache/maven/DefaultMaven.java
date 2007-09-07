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
import org.apache.maven.lifecycle.TaskValidationResult;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.DuplicateProjectException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.settings.Settings;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author jason van zyl
 * @version $Id$
 * @todo EventDispatcher should be a component as it is internal to maven.
 */
public class DefaultMaven
    extends AbstractLogEnabled
    implements Maven,
    Contextualizable
{
    // ----------------------------------------------------------------------
    // Components
    // ----------------------------------------------------------------------

    protected BuildContextManager buildContextManager;

    protected MavenProjectBuilder projectBuilder;

    protected LifecycleExecutor lifecycleExecutor;

    protected PlexusContainer container;

    protected RuntimeInformation runtimeInformation;

    private BuildExtensionScanner buildExtensionScanner;

    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    // project build
    // artifact resolution
    // lifecycle execution

    public ReactorManager createReactorManager( MavenExecutionRequest request,
                                                MavenExecutionResult result )
    {
        List projects;

        try
        {
            projects = getProjects( request );

            if ( projects.isEmpty() )
            {
                projects.add( projectBuilder.buildStandaloneSuperProject() );

                request.setProjectPresent( false );
            }
        }
        catch ( Exception e )
        {
            result.addException( e );

            return null;
        }

        ReactorManager reactorManager;

        try
        {
            reactorManager = new ReactorManager(
                projects,
                request.getReactorFailureBehavior() );

            result.setReactorManager( reactorManager );
        }
        catch ( CycleDetectedException e )
        {
            result.addException(
                new BuildFailureException(
                    "The projects in the reactor contain a cyclic reference: " + e.getMessage(),
                    e ) );

            return null;
        }
        catch ( DuplicateProjectException e )
        {
            result.addException(
                new BuildFailureException(
                    e.getMessage(),
                    e ) );

            return null;
        }

        return reactorManager;
    }

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        request.setStartTime( new Date() );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        ReactorManager reactorManager = createReactorManager(
            request,
            result );

        if ( result.hasExceptions() )
        {
            return result;
        }

        EventDispatcher dispatcher = new DefaultEventDispatcher( request.getEventMonitors() );

        String event = MavenEvents.REACTOR_EXECUTION;

        dispatcher.dispatchStart(
            event,
            request.getBaseDirectory() );

        MavenSession session = createSession(
            request,
            reactorManager,
            dispatcher );

        for ( Iterator i = request.getGoals().iterator(); i.hasNext(); )
        {
            String goal = (String) i.next();

            TaskValidationResult tvr = lifecycleExecutor.isTaskValid( goal, session, reactorManager.getTopLevelProject() );

            if ( !tvr.isTaskValid() )
            {
                result.addException( new BuildFailureException( tvr.getMessage() ) );

                return result;
            }
        }

        getLogger().info( "Scanning for projects..." );

        if ( reactorManager.hasMultipleProjects() )
        {
            getLogger().info( "Reactor build order: " );

            for ( Iterator i = reactorManager.getSortedProjects().iterator(); i.hasNext(); )
            {
                MavenProject project = (MavenProject) i.next();

                getLogger().info( "  " + project.getName() );
            }
        }

        initializeBuildContext( request );

        try
        {
            lifecycleExecutor.execute(
                session,
                reactorManager,
                dispatcher );
        }
        catch ( Exception e )
        {
            result.addException(
                new BuildFailureException(
                    e.getMessage(),
                    e ) );

            return result;
        }

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

        SystemBuildContext systemContext = SystemBuildContext.getSystemBuildContext(
            buildContextManager,
            true );

        systemContext.setSystemProperties( request.getProperties() );
        systemContext.store( buildContextManager );
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
            throw new MavenExecutionException(
                "Error selecting project files for the reactor: " + e.getMessage(),
                e );
        }

        // TODO: We should probably do this discovery just-in-time, if we can move to building project
        // instances just-in-time.
        try
        {
            buildExtensionScanner.scanForBuildExtensions(
                files,
                request.getLocalRepository(),
                request.getProfileManager() );
        }
        catch ( ExtensionScanningException e )
        {
            throw new MavenExecutionException(
                "Error scanning for extensions: " + e.getMessage(),
                e );
        }

        try
        {
            projects = collectProjects(
                files,
                request.getLocalRepository(),
                request.isRecursive(),
                request.getProfileManager(),
                !request.useReactor() );

        }
        catch ( ArtifactResolutionException e )
        {
            throw new MavenExecutionException(
                e.getMessage(),
                e );
        }
        catch ( ProjectBuildingException e )
        {
            throw new MavenExecutionException(
                e.getMessage(),
                e );
        }
        catch ( ProfileActivationException e )
        {
            throw new MavenExecutionException(
                e.getMessage(),
                e );
        }
        return projects;
    }

    private List collectProjects( List files,
                                  ArtifactRepository localRepository,
                                  boolean recursive,
                                  ProfileManager globalProfileManager,
                                  boolean isRoot )
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

            MavenProject project = projectBuilder.build(
                file,
                localRepository,
                globalProfileManager );

            if ( isRoot )
            {
                project.setExecutionRoot( true );
            }

            if ( project.getPrerequisites() != null && project.getPrerequisites().getMaven() != null )
            {
                DefaultArtifactVersion version = new DefaultArtifactVersion( project.getPrerequisites().getMaven() );
                if ( runtimeInformation.getApplicationVersion().compareTo( version ) < 0 )
                {
                    throw new BuildFailureException(
                        "Unable to build project '" + project.getFile() +
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
                        moduleFile = new File(
                            basedir,
                            name + "/" + Maven.RELEASE_POMv4 );
                    }
                    else
                    {
                        moduleFile = new File(
                            basedir,
                            name + "/" + Maven.POMv4 );
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
                            throw new MavenExecutionException(
                                "Unable to canonicalize file name " + moduleFile,
                                e );
                        }
                    }

                    moduleFiles.add( moduleFile );
                }

                List collectedProjects =
                    collectProjects(
                        moduleFiles,
                        localRepository,
                        recursive,
                        globalProfileManager,
                        false );
                projects.addAll( collectedProjects );
                project.setCollectedProjects( collectedProjects );
            }
            projects.add( project );
        }

        return projects;
    }

    // ----------------------------------------------------------------------
    // Methods used by all execution request handlers
    // ----------------------------------------------------------------------

    //!! We should probably have the execution request handler create the
    // session as
    // the session type would be specific to the request i.e. having a project
    // or not.

    protected MavenSession createSession( MavenExecutionRequest request,
                                          ReactorManager rpm,
                                          EventDispatcher dispatcher )
    {
        MavenSession session = new MavenSession(
            container,
            request,
            dispatcher,
            rpm );

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

    private List getProjectFiles( MavenExecutionRequest request )
        throws IOException
    {
        List files = Collections.EMPTY_LIST;

        File userDir = new File( request.getBaseDirectory() );

        if ( request.useReactor() )
        {
            String includes = System.getProperty(
                "maven.reactor.includes",
                "**/" + POMv4 + ",**/" + RELEASE_POMv4 );
            String excludes = System.getProperty(
                "maven.reactor.excludes",
                POMv4 + "," + RELEASE_POMv4 );

            files = FileUtils.getFiles(
                userDir,
                includes,
                excludes );

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
            File projectFile = new File(
                userDir,
                RELEASE_POMv4 );

            if ( !projectFile.exists() )
            {
                projectFile = new File(
                    userDir,
                    POMv4 );
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
