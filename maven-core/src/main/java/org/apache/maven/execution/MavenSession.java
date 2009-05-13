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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
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

    private MavenExecutionResult result;
    
    private MavenProject currentProject;
        
    /**
     * These projects have already been topologically sorted in the {@link org.apache.maven.Maven} component before
     * being passed into the session.
     */
    private List<MavenProject> projects;
    
    private MavenProject topLevelProject;
    
    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenExecutionResult result, MavenProject project )
        throws CycleDetectedException, DuplicateProjectException
    {
        this( container, request, result, Arrays.asList( new MavenProject[]{ project } ) );        
    }    

    public MavenSession( PlexusContainer container, MavenExecutionRequest request, MavenExecutionResult result, List<MavenProject> projects )
        throws CycleDetectedException, DuplicateProjectException
    {
        this.container = container;
        this.request = request;
        this.result = result;
        this.currentProject = projects.get( 0 );
        this.projects = projects;        
    }    
        
    @Deprecated
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
    
    public List<MavenProject> getProjects()
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

    public MavenExecutionResult getResult()
    {
        return result;
    }        
    
    // Backward compat
    public Map<String,Map<String,Object>> getPluginContext( PluginDescriptor pluginDescriptor, MavenProject project )
    {
        return new HashMap<String,Map<String,Object>>();
    }    

    /*
    private Map pluginContextsByProjectAndPluginKey = new HashMap();
    
    public Map getPluginContext( PluginDescriptor plugin, MavenProject project )
    {
        Map pluginContextsByKey = (Map) pluginContextsByProjectAndPluginKey.get( project.getId() );

        if ( pluginContextsByKey == null )
        {
            pluginContextsByKey = new HashMap();

            pluginContextsByProjectAndPluginKey.put( project.getId(), pluginContextsByKey );
        }

        Map pluginContext = (Map) pluginContextsByKey.get( plugin.getPluginLookupKey() );

        if ( pluginContext == null )
        {
            pluginContext = new HashMap();
            pluginContextsByKey.put( plugin.getPluginLookupKey(), pluginContext );
        }

        return pluginContext;
    }
    */
    
}