package org.apache.maven;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectSorter;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author Jason van Zyl
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
        // Need a general way to inject standard properties
        if ( request.getStartTime() != null )
        {
            request.getProperties().put( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }        
        
        request.setStartTime( new Date() );
        
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenSession session = createMavenSession( request, result );        
        
        try
        {
            lifecycleExecutor.execute( session );
        }        
        catch ( Exception e )
        {            
            ExceptionHandler handler = new DefaultExceptionHandler();
            
            // This will only be more then one if we have fail at end on and we collect
            // them per project.
            ExceptionSummary es = handler.handleException( e );                        
         
            result.addException( e );

            result.setExceptionSummary( es );
            
            return result;
        }

        result.setTopologicallySortedProjects( session.getSortedProjects() );

        result.setProject( session.getTopLevelProject() );

        return result;
    }

    public MavenSession createMavenSession( MavenExecutionRequest request, MavenExecutionResult result )
    {
        MavenSession session;
        
        List<MavenProject> projects;

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

        try
        {                        
            ProjectSorter projectSorter = new ProjectSorter( projects );
            
            session = new MavenSession( container, request, projectSorter.getSortedProjects() );            
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

        return session;
    }

    protected List<MavenProject> getProjects( MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<File> files = Arrays.asList( new File[] { request.getPom() } );

        List<MavenProject> projects = collectProjects( files, request );

        return projects;
    }

    private List<MavenProject> collectProjects( List<File> files, MavenExecutionRequest request )
        throws MavenExecutionException
    {
        List<MavenProject> projects = new ArrayList<MavenProject>();

        for ( File file : files )
        {
            MavenProject project;

            try
            {
                project = projectBuilder.build( file, request.getProjectBuildingConfiguration() );
            }
            catch ( ProjectBuildingException e )
            {
                throw new MavenExecutionException( "Failed to build MavenProject instance for: " + file, file, e );
            }

            if ( ( project.getPrerequisites() != null ) && ( project.getPrerequisites().getMaven() != null ) )
            {
                DefaultArtifactVersion version = new DefaultArtifactVersion( project.getPrerequisites().getMaven() );

                if ( runtimeInformation.getApplicationInformation().getVersion().compareTo( version ) < 0 )
                {
                    throw new MavenExecutionException( "Unable to build project '" + file + "; it requires Maven version " + version.toString(), file );
                }
            }

            if ( ( project.getModules() != null ) && !project.getModules().isEmpty() && request.isRecursive() )
            {
                File basedir = file.getParentFile();

                List<File> moduleFiles = new ArrayList<File>();
                
                for ( String name : project.getModules() )
                {
                    if ( StringUtils.isEmpty( StringUtils.trim( name ) ) )
                    {
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

                List<MavenProject> collectedProjects = collectProjects( moduleFiles, request );

                projects.addAll( collectedProjects );
                
                project.setCollectedProjects( collectedProjects );
            }
            
            projects.add( project );
        }

        return projects;
    }
}
