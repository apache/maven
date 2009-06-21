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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.exception.DefaultExceptionHandler;
import org.apache.maven.exception.ExceptionHandler;
import org.apache.maven.exception.ExceptionSummary;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.DuplicateProjectException;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectSorter;
import org.apache.maven.lifecycle.LifecycleExecutor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.repository.DelegatingLocalArtifactRepository;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
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
    private Logger logger;

    @Requirement
    protected ProjectBuilder projectBuilder;

    @Requirement
    protected LifecycleExecutor lifecycleExecutor;

    @Requirement
    protected PlexusContainer container;
    
    public MavenExecutionResult execute( MavenExecutionRequest request )
    {
        //TODO: Need a general way to inject standard properties
        if ( request.getStartTime() != null )
        {
            request.getProperties().put( "${build.timestamp}", new SimpleDateFormat( "yyyyMMdd-hhmm" ).format( request.getStartTime() ) );
        }        
        
        request.setStartTime( new Date() );
        
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        
        DelegatingLocalArtifactRepository delegatingLocalArtifactRepository = new DelegatingLocalArtifactRepository( request.getLocalRepository() );
        
        request.setLocalRepository( delegatingLocalArtifactRepository );        
                
        MavenSession session;
        
        Map<String,MavenProject> projects;

        //TODO: optimize for the single project or no project
        
        try
        {
            projects = getProjectsForMavenReactor( request );
                                                
            //TODO: We really need to get rid of this requirement in here. If we know there is no project present
            if ( projects.isEmpty() )
            {
                MavenProject project = projectBuilder.buildStandaloneSuperProject( request.getProjectBuildingRequest() ); 
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
                                    
            session = new MavenSession( container, request, result, projectSorter.getSortedProjects() );            
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
                
        delegatingLocalArtifactRepository.setBuildReactor( new ReactorArtifactRepository( projects ) );
        
        if ( result.hasExceptions() )
        {
            return result;
        }        

        lifecycleExecutor.execute( session );

        validateActivatedProfiles( session.getProjects(), request.getActiveProfiles() );

        if ( session.getResult().hasExceptions() )
        {        
            return processResult( result, session.getResult().getExceptions().get( 0 ) );
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
    
    protected Map<String,MavenProject> getProjectsForMavenReactor( MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        // We have no POM file.
        //
        if ( request.getPom() == null || !request.getPom().exists() )
        {
            return new HashMap<String,MavenProject>();
        }
        
        List<File> files = Arrays.asList( request.getPom().getAbsoluteFile() );

        List<MavenProject> projects = new ArrayList<MavenProject>();

        collectProjects( projects, files, request );

        Map<String, MavenProject> index = new LinkedHashMap<String, MavenProject>();
        Map<String, List<File>> collisions = new LinkedHashMap<String, List<File>>();

        for ( MavenProject project : projects )
        {
            String projectId = ArtifactUtils.key( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            MavenProject collision = index.get( projectId );

            if ( collision == null )
            {
                index.put( projectId, project );
            }
            else
            {
                List<File> pomFiles = collisions.get( projectId );

                if ( pomFiles == null )
                {
                    pomFiles = new ArrayList<File>( Arrays.asList( collision.getFile(), project.getFile() ) );
                    collisions.put( projectId, pomFiles );
                }
                else
                {
                    pomFiles.add( project.getFile() );
                }
            }
        }

        if ( !collisions.isEmpty() )
        {
            throw new org.apache.maven.DuplicateProjectException( "Two or more projects in the reactor"
                + " have the same identifier, please make sure that <groupId>:<artifactId>:<version>"
                + " is unique for each project: " + collisions, collisions );
        }

        return index;
    }

    private void collectProjects( List<MavenProject> projects, List<File> files, MavenExecutionRequest request )
        throws MavenExecutionException, ProjectBuildingException
    {
        for ( File file : files )
        {
            MavenProject project = projectBuilder.build( file, request.getProjectBuildingRequest() );
            
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

                collectProjects( projects, moduleFiles, request );
            }

            projects.add( project );
        }
    }

    private void validateActivatedProfiles( List<MavenProject> projects, List<String> activeProfileIds )
    {
        Collection<String> notActivatedProfileIds = new LinkedHashSet<String>( activeProfileIds );

        for ( MavenProject project : projects )
        {
            for ( List<String> profileIds : project.getInjectedProfileIds().values() )
            {
                notActivatedProfileIds.removeAll( profileIds );
            }
        }

        for ( String notActivatedProfileId : notActivatedProfileIds )
        {
            logger.warn( "Profile with id \"" + notActivatedProfileId + "\" has not been activated." );
        }
    }

}
