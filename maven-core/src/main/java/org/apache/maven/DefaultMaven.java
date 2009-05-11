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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
    
    @Requirement
    List<LocalArtifactRepository> localArtifactRepositories; 
    
    public List<String> getLifecyclePhases()
    {
        return lifecycleExecutor.getLifecyclePhases();
    }

    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        // Need a general way to inject standard properties
        if ( request.getStartTime() != null )
        {
            request.getProperties().put( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }        
        
        request.setStartTime( new Date() );
        
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        
        DelegatingLocalArtifactRepository delegatingLocalArtifactRepository = new DelegatingLocalArtifactRepository();
        delegatingLocalArtifactRepository.addToEndOfSearchOrder( new UserLocalArtifactRepository( request.getLocalRepository() ) ); 
        
        if ( localArtifactRepositories != null && localArtifactRepositories.size() > 0 )
        {
            delegatingLocalArtifactRepository.addToBeginningOfSearchOrder( localArtifactRepositories.get( 0 ) );            
        }        
        
        request.setLocalRepository( delegatingLocalArtifactRepository );        
                
        MavenSession session;
        
        Map<String,MavenProject> projects;

        try
        {
            projects = getProjects( request );

            //TODO: We really need to get rid of this requirement in here. If we know there is no project present
            if ( projects.isEmpty() )
            {
                MavenProject project = projectBuilder.buildStandaloneSuperProject( request.getProjectBuildingConfiguration() ); 
                projects.put( ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );
                request.setProjectPresent( false );
            }
        }
        catch ( ProjectBuildingException e )
        {
            return processResult( result, e );
        }
        catch ( MavenExecutionException e )
        {
            return processResult( result, e );
        }
        
        try
        {                        
            ProjectSorter projectSorter = new ProjectSorter( projects.values() );
                        
            session = new MavenSession( container, request, projectSorter.getSortedProjects() );            
        }
        catch ( CycleDetectedException e )
        {            
            String message = "The projects in the reactor contain a cyclic reference: " + e.getMessage();

            ProjectCycleException error = new ProjectCycleException( message, e );

            return processResult( result, error );
        }
        catch ( DuplicateProjectException e )
        {
            return processResult( result, e );
        }
       
        // Desired order of precedence for local artifact repositories
        //
        // Reactor
        // Workspace
        // User Local Repository
                
        delegatingLocalArtifactRepository.addToBeginningOfSearchOrder( new ReactorArtifactRepository( projects ) );
        
        if ( result.hasExceptions() )
        {
            return result;
        }        

        try
        {
            lifecycleExecutor.execute( session );
        }        
        catch ( Exception e )
        {            
            return processResult( result, e );
        }

        result.setTopologicallySortedProjects( session.getProjects() );

        result.setProject( session.getTopLevelProject() );

        return result;
    }

    private MavenExecutionResult processResult( MavenExecutionResult result, Exception e )
    {
        ExceptionHandler handler = new DefaultExceptionHandler();
        
        ExceptionSummary es = handler.handleException( e );                        
     
        result.addException( e );
        
        result.setExceptionSummary( es );    
        
        return result;
    }
    
    protected Map<String,MavenProject> getProjects( MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        List<File> files = Arrays.asList( new File[] { request.getPom() } );

        Map<String,MavenProject> projects = collectProjects( files, request );

        return projects;
    }

    private Map<String,MavenProject> collectProjects( List<File> files, MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        Map<String,MavenProject> projects = new LinkedHashMap<String,MavenProject>();

        for ( File file : files )
        {
            MavenProject project = projectBuilder.build( file, request.getProjectBuildingConfiguration() );
            
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

                Map<String,MavenProject> collectedProjects = collectProjects( moduleFiles, request );

                projects.putAll( collectedProjects );                
            }
            
            projects.put( ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() ), project );
        }

        return projects;
    }
}
