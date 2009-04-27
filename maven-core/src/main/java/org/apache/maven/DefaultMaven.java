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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.reactor.MavenExecutionException;
import org.apache.maven.reactor.MissingModuleException;
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
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenSession session = createMavenSession( request, result );        

        if ( session.getReactorManager().hasMultipleProjects() )
        {
            //logger.info( "Reactor build order: " );

            for ( MavenProject project : session.getReactorManager().getSortedProjects() )
            {
                //logger.info( "  " + project.getName() );
            }
        }

        try
        {
            lifecycleExecutor.execute( session );
        }
        catch ( LifecycleExecutionException e )
        {
            result.addException( e );

            return result;
        }
        catch ( BuildFailureException e )
        {
            result.addException( e );

            return result;
        }

        result.setTopologicallySortedProjects( session.getReactorManager().getSortedProjects() );

        result.setProject( session.getReactorManager().getTopLevelProject() );

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
            session = new MavenSession( container, request, projects );
            
            result.setReactorManager( session.getReactorManager() );            
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
