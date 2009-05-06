package org.apache.maven.execution;

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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.dag.CycleDetectedException;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class MavenSession
{
    private PlexusContainer container;
    
    private MavenExecutionRequest request;

    private MavenProject currentProject;
        
    private List<MavenProject> projects;
    
    private MavenProject topLevelProject;
    
    // Used by the embedder to verifyPlugin
    public MavenSession( MavenExecutionRequest request )
    {
        this.request = request;
    }

    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenProject project )
        throws CycleDetectedException, DuplicateProjectException
    {
        this( container, request, Arrays.asList( new MavenProject[]{ project } ) );        
    }    

    public MavenSession( PlexusContainer container, MavenExecutionRequest request, List<MavenProject> projects )
        throws CycleDetectedException, DuplicateProjectException
    {
        this.container = container;
        this.request = request;
        this.currentProject = projects.get( 0 );
        this.projects = projects;        
    }    
        
    public PlexusContainer getContainer()
    {
        return container;
    }

    public ArtifactRepository getLocalRepository()
    {
        return request.getLocalRepository();
    }

    public List<String> getGoals()
    {
        return request.getGoals();
    }

    public Properties getExecutionProperties()
    {
        return request.getProperties();
    }

    public Settings getSettings()
    {
        return request.getSettings();
    }

    public List<MavenProject> getSortedProjects()
    {
        return projects;
    }

    public String getExecutionRootDirectory()
    {
        return request.getBaseDirectory();
    }

    public boolean isUsingPOMsFromFilesystem()
    {
        return request.isProjectPresent();
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }

    public void setCurrentProject( MavenProject currentProject )
    {
        this.currentProject = currentProject;
    }

    public MavenProject getCurrentProject()
    {
        return currentProject;
    }

    public ProjectBuilderConfiguration getProjectBuilderConfiguration()
    {
        return request.getProjectBuildingConfiguration();
    }
    
    public List<String> getPluginGroups()
    {
        return request.getPluginGroups();
    }
    
    public boolean isOffline()
    {
        return request.isOffline();
    }        

    public MavenProject getTopLevelProject()
    {
        return topLevelProject;
    }
}