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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.ModelEventListener;
import org.apache.maven.model.Profile;

public class DefaultProjectBuilderConfiguration
    implements ProjectBuilderConfiguration
{
    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;
    
    private Properties executionProperties;

    private List<ModelEventListener> listeners;
    
    private MavenProject topProject;
    
    private boolean processPlugins = true;
    
    private List<String> activeProfileIds;
    
    private List<String> inactiveProfileIds;
    
    private List<Profile> profiles;
        
    public MavenProject getTopLevelProjectFromReactor()
    {
    	return topProject;
    }
    
    public void setTopLevelProjectForReactor(MavenProject mavenProject)
    {
    	this.topProject = mavenProject;
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
        if ( remoteRepositories == null )
        {
            remoteRepositories = new ArrayList<ArtifactRepository>();
        }
        return remoteRepositories;
    }

    public ProjectBuilderConfiguration setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
        return this;
    }
    
    public Properties getExecutionProperties()
    {
        if ( executionProperties == null )
        {
            executionProperties = new Properties();
        }
        return executionProperties;
    }

    public ProjectBuilderConfiguration setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties = executionProperties;
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

    public boolean isProcessPlugins()
    {
        return processPlugins;
    }

    public ProjectBuilderConfiguration setProcessPlugins( boolean processPlugins )
    {
        this.processPlugins = processPlugins;
        return this;
    }

    public List<String> getActiveProfileIds()
    {
        if ( activeProfileIds == null )
        {
            activeProfileIds = new ArrayList<String>();
        }
        return activeProfileIds;        
    }

    public void setActiveProfileIds( List<String> activeProfileIds )
    {
        this.activeProfileIds = activeProfileIds;      
    }

    public List<String> getInactiveProfileIds()
    {
        if ( inactiveProfileIds == null )
        {
            inactiveProfileIds = new ArrayList<String>();
        }
        return inactiveProfileIds;
    }

    public void setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        this.inactiveProfileIds = inactiveProfileIds;
    }

    public void setProfiles( List<Profile> profiles )
    {
        this.profiles = profiles;
    }
    
    public void addProfile( Profile profile )
    {
        if ( profiles == null )
        {
            profiles = new ArrayList<Profile>();
        }
        
        profiles.add( profile );
    }

    public List<Profile> getProfiles()
    {
        if ( profiles == null )
        {
            profiles = new ArrayList<Profile>();
        }
        return profiles;
    }

}
