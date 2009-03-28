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


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ReactorManager;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.monitor.event.DefaultEventDispatcher;
import org.apache.maven.monitor.event.EventDispatcher;
import org.apache.maven.monitor.event.MavenEvents;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.reactor.MissingModuleException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author Jason van Zyl
 * @version $Id$
 * @todo EventDispatcher should be a component as it is internal to maven.
 */
@Component(role = Maven.class)
public class DefaultMaven
    implements Maven
{
    @Requirement
    protected MavenProjectBuilder projectBuilder;
    
    @Requirement
    protected LifecycleExecutor lifecycleExecutor;

    @Requirement
    protected PlexusContainer container;

    @Requirement
    protected RuntimeInformation runtimeInformation;

    @Requirement
    private Logger logger;

    public List<String> getLifecyclePhases()
    {
        return lifecycleExecutor.getLifecyclePhases();
    }
    
    // ----------------------------------------------------------------------
    // Project execution
    // ----------------------------------------------------------------------

    // project build
    // artifact resolution
    // lifecycle execution

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        request.setStartTime( new Date() );

        MavenExecutionResult result = new DefaultMavenExecutionResult();

        ReactorManager reactorManager = createReactorManager( request, result );

        if ( result.hasExceptions() )
        {
            return result;
        }

        EventDispatcher dispatcher = new DefaultEventDispatcher( request.getEventMonitors() );

        String event = MavenEvents.MAVEN_EXECUTION;

        dispatcher.dispatchStart( event, request.getBaseDirectory() );

        MavenSession session = createSession( request, reactorManager, dispatcher );

        logger.info( "Scanning for projects..." );

        if ( reactorManager.hasMultipleProjects() )
        {
            logger.info( "Reactor build order: " );

            for( MavenProject project : reactorManager.getSortedProjects() )
            {
                logger.info( "  " + project.getName() );
            }
        }

        try
        {
            lifecycleExecutor.execute( session );
        }
        catch ( LifecycleExecutionException e )
        {
            result.addException( e );
            dispatcher.dispatchError( event, request.getBaseDirectory(), e );

            return result;
        }
        catch ( BuildFailureException e )
        {
            result.addException( e );
            dispatcher.dispatchError( event, request.getBaseDirectory(), e );

            return result;
        }

        result.setTopologicallySortedProjects( reactorManager.getSortedProjects() );

        result.setProject( reactorManager.getTopLevelProject() );

        dispatcher.dispatchEnd( event, request.getBaseDirectory() );

        return result;
    }    
    
    public ReactorManager createReactorManager( MavenExecutionRequest request, MavenExecutionResult result )
    {
        List projects;
        try
        {            
            projects = getProjects( request );

            if ( projects.isEmpty() )
            {
                projects.add( projectBuilder.buildStandaloneSuperProject( request.getProjectBuildingConfiguration() ) );

                request.setProjectPresent( false );
            }
        }
        catch ( ProjectBuildingException e )
        {
            result.addException( e );
            return null;
        }
        catch ( MavenExecutionException e )
        {
            result.addException( e );
            return null;
        }

        ReactorManager reactorManager;

        try
        {
            reactorManager = new ReactorManager( projects, request.getReactorFailureBehavior() );

            result.setReactorManager( reactorManager );
        }
        catch ( CycleDetectedException e )
        {
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            ProjectCycleException error = new ProjectCycleException( projects, message, e );

            result.addException( error );

            return null;
        }
        catch ( DuplicateProjectException e )
        {
            result.addException( e );

            return null;
        }

        return reactorManager;
    }

    protected List getProjects( MavenExecutionRequest request )
        throws MavenExecutionException
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

        projects = collectProjects( files, request, !request.useReactor() );

        return projects;
    }

    private List collectProjects( List files, MavenExecutionRequest request, boolean isRoot )
        throws MavenExecutionException
    {
        List projects = new ArrayList( files.size() );

        if ( !files.isEmpty() )
        {
            for ( Iterator iterator = files.iterator(); iterator.hasNext(); )
            {
                File file = (File) iterator.next();

                MavenProject project;
                
                try
                {
                    project = projectBuilder.build( file, request.getProjectBuildingConfiguration() );
                }
                catch ( ProjectBuildingException e )
                {
                    throw new MavenExecutionException( "Failed to build MavenProject instance for: " + file, file, e );
                }

                if ( isRoot )
                {
                    project.setExecutionRoot( true );
                }

                if ( ( project.getPrerequisites() != null ) && ( project.getPrerequisites().getMaven() != null ) )
                {
                    DefaultArtifactVersion version = new DefaultArtifactVersion( project.getPrerequisites().getMaven() );

                    if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( version ) < 0 )
                    {
                        throw new MavenExecutionException(
                            "Unable to build project '" + file +
                                "; it requires Maven version " + version.toString(), file );
                    }
                }

                if ( ( project.getModules() != null ) && !project.getModules().isEmpty() && request.isRecursive() )
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
                            logger.warn( "Empty module detected. Please check you don't have any empty module definitions in your POM." );

                            continue;
                        }

                        File moduleFile = new File( basedir, name );
                        if ( !moduleFile.exists() )
                        {
                            throw new MissingModuleException( name, moduleFile, file );
                        }
                        else if ( moduleFile.isDirectory() )
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
                        else
                        {
                            moduleFile = new File( moduleFile.toURI().normalize() );
                        }

                        moduleFiles.add( moduleFile );
                    }

                    List collectedProjects = collectProjects( moduleFiles, request, false );

                    projects.addAll( collectedProjects );
                    project.setCollectedProjects( collectedProjects );
                }
                projects.add( project );
            }
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

    protected MavenSession createSession( MavenExecutionRequest request, ReactorManager reactorManager, EventDispatcher dispatcher )
    {
        MavenSession session = new MavenSession( container, request );

        return session;
    }

    private List getProjectFiles( MavenExecutionRequest request )
        throws IOException
    {
        List files = Collections.EMPTY_LIST;

        File userDir = new File( request.getBaseDirectory() );

        if ( request.useReactor() )
        {
            String includes = System.getProperty( "maven.reactor.includes", "**/" + POMv4 );

            String excludes = System.getProperty( "maven.reactor.excludes", POMv4 );

            files = FileUtils.getFiles( userDir, includes, excludes );

            // make sure there is consistent ordering on all platforms, rather than using the filesystem ordering
            Collections.sort( files );
        }
        else if ( request.getPom() != null )
        {
            File projectFile = request.getPom().getAbsoluteFile();

            if ( projectFile.exists() )
            {
                files = Collections.singletonList( projectFile );
            }
        }
        else
        {
            File projectFile = new File( userDir, POMv4 );
            
            if ( projectFile.exists() )
            {
                files = Collections.singletonList( projectFile );
            }
        }

        return files;
    }
 
    // Lifecycle phases
    
    public List<Lifecycle> getBuildLifecyclePhases()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Lifecycle> getCleanLifecyclePhases()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<Lifecycle> getSiteLifecyclePhases()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
