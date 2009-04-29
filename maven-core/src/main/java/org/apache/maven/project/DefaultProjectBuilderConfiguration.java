package org.apache.maven.project;

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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.ModelEventListener;
import org.apache.maven.model.Plugin;
import org.apache.maven.profiles.ProfileManager;

public class DefaultProjectBuilderConfiguration
    implements ProjectBuilderConfiguration
{
    private ProfileManager globalProfileManager;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;
    
    private Properties userProperties;

    //!!jvz Find out who added this. It's wrong, the execution properties are what come from the embedder setup not system properties. 
    private Properties executionProperties = System.getProperties();

    private Date buildStartTime;

    private List<ModelEventListener> listeners;
    
    private MavenProject topProject;
    
    private Set<Plugin> plugins;
    
    public void setPlugins(Set<Plugin> plugins)
    {
    	this.plugins = plugins;
    }
    
    public Set<Plugin> getPlugins()
    {
    	if(plugins == null)
    	{
    		plugins = new HashSet<Plugin>();
    	}
    	return plugins;
    }
    
    public MavenProject getTopLevelProjectFromReactor()
    {
    	return topProject;
    }
    
    public void setTopLevelProjectForReactor(MavenProject mavenProject)
    {
    	this.topProject = mavenProject;
    }
        
    public ProjectBuilderConfiguration setGlobalProfileManager( ProfileManager globalProfileManager )
    {
        this.globalProfileManager = globalProfileManager;
        return this;
    }

    public ProfileManager getGlobalProfileManager()
    {
        return globalProfileManager;
    }

    public ProjectBuilderConfiguration setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }
        
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
        return this;
    }

    public ProjectBuilderConfiguration setUserProperties( Properties userProperties )
    {
        this.userProperties = userProperties;
        return this;
    }

    public Properties getUserProperties()
    {
        if ( userProperties == null )
        {
            userProperties = new Properties();
        }

        return userProperties;
    }

    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties = executionProperties;
        return this;
    }

    public Date getBuildStartTime()
    {
        return buildStartTime;
    }

    public ProjectBuilderConfiguration setBuildStartTime( Date buildStartTime )
    {
        this.buildStartTime = buildStartTime;
        return this;
    }

    public List<ModelEventListener> getModelEventListeners()
    {
        return listeners;
    }

    public ProjectBuilderConfiguration setModelEventListeners( List<ModelEventListener> listeners )
    {
        this.listeners = listeners;
        return this;
    }
}
