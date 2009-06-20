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
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.ModelEventListener;
import org.apache.maven.model.Profile;

public class DefaultProjectBuildingRequest
    implements ProjectBuildingRequest
{
    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private List<ModelEventListener> listeners;
    
    private MavenProject topProject;

    private boolean lenientValidation;

    private boolean processPlugins;

    private List<Profile> profiles;

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties executionProperties;

    private Date buildStartTime;

    public DefaultProjectBuildingRequest()
    {
        processPlugins = true;
        profiles = new ArrayList<Profile>();
        activeProfileIds = new ArrayList<String>();
        inactiveProfileIds = new ArrayList<String>();
        executionProperties = new Properties();
        remoteRepositories = new ArrayList<ArtifactRepository>();
        pluginArtifactRepositories = new ArrayList<ArtifactRepository>();
    }

    public MavenProject getTopLevelProjectFromReactor()
    {
    	return topProject;
    }
    
    public void setTopLevelProjectForReactor(MavenProject mavenProject)
    {
    	this.topProject = mavenProject;
    }       

    public ProjectBuildingRequest setLocalRepository( ArtifactRepository localRepository )
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

    public ProjectBuildingRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories.clear();

        if ( remoteRepositories != null )
        {
            this.remoteRepositories.addAll( remoteRepositories );
        }

        return this;
    }

    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    public ProjectBuildingRequest setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifactRepositories )
    {
        this.pluginArtifactRepositories.clear();

        if ( pluginArtifactRepositories != null )
        {
            this.pluginArtifactRepositories.addAll( pluginArtifactRepositories );
        }

        return this;
    }
    
    public Properties getExecutionProperties()
    {
        return executionProperties;
    }

    public ProjectBuildingRequest setExecutionProperties( Properties executionProperties )
    {
        this.executionProperties.clear();

        if ( executionProperties != null )
        {
            this.executionProperties.putAll( executionProperties );
        }

        return this;
    }

    public List<ModelEventListener> getModelEventListeners()
    {
        return listeners;
    }

    public ProjectBuildingRequest setModelEventListeners( List<ModelEventListener> listeners )
    {
        this.listeners = listeners;
        return this;
    }

    public boolean isProcessPlugins()
    {
        return processPlugins;
    }

    public ProjectBuildingRequest setProcessPlugins( boolean processPlugins )
    {
        this.processPlugins = processPlugins;
        return this;
    }

    public ProjectBuildingRequest setLenientValidation( boolean lenientValidation )
    {
        this.lenientValidation = lenientValidation;
        return this;
    }

    public boolean istLenientValidation()
    {
        return lenientValidation;
    }

    public List<String> getActiveProfileIds()
    {
        return activeProfileIds;
    }

    public void setActiveProfileIds( List<String> activeProfileIds )
    {
        this.activeProfileIds.clear();

        if ( activeProfileIds != null )
        {
            this.activeProfileIds.addAll( activeProfileIds );
        }
    }

    public List<String> getInactiveProfileIds()
    {
        return inactiveProfileIds;
    }

    public void setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        this.inactiveProfileIds.clear();

        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds.addAll( inactiveProfileIds );
        }
    }

    public void setProfiles( List<Profile> profiles )
    {
        this.profiles.clear();

        if ( profiles != null )
        {
            this.profiles.addAll( profiles );
        }
    }
    
    public void addProfile( Profile profile )
    {
        profiles.add(profile);
    }

    public List<Profile> getProfiles()
    {
        return profiles;
    }

    public Date getBuildStartTime()
    {
        return buildStartTime;
    }

    public void setBuildStartTime( Date buildStartTime )
    {
        this.buildStartTime = buildStartTime;
    }

}
