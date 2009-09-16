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
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelEventListener;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;

public class DefaultProjectBuildingRequest
    implements ProjectBuildingRequest
{

    private boolean offline;

    private boolean forceUpdate;

    private RepositoryCache repositoryCache;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private List<ArtifactRepository> pluginArtifactRepositories;

    private List<Server> servers;

    private List<Mirror> mirrors;

    private List<Proxy> proxies;

    private List<ModelEventListener> listeners;

    private MavenProject project;

    private int validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_STRICT;

    private boolean processPlugins;

    private List<Profile> profiles;

    private List<String> activeProfileIds;

    private List<String> inactiveProfileIds;

    private Properties systemProperties;

    private Properties userProperties;

    private Date buildStartTime;

    private boolean resolveDependencies;

    public DefaultProjectBuildingRequest()
    {
        processPlugins = true;
        profiles = new ArrayList<Profile>();
        activeProfileIds = new ArrayList<String>();
        inactiveProfileIds = new ArrayList<String>();
        systemProperties = new Properties();
        userProperties = new Properties();
        remoteRepositories = new ArrayList<ArtifactRepository>();
        pluginArtifactRepositories = new ArrayList<ArtifactRepository>();
        servers = new ArrayList<Server>();
        mirrors = new ArrayList<Mirror>();
        proxies = new ArrayList<Proxy>();
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject mavenProject )
    {
        this.project = mavenProject;
    }

    public DefaultProjectBuildingRequest setOffline( boolean offline )
    {
        this.offline = offline;

        return this;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public boolean isForceUpdate()
    {
        return forceUpdate;
    }

    public ProjectBuildingRequest setForceUpdate( boolean forceUpdate )
    {
        this.forceUpdate = forceUpdate;

        return this;
    }

    public ProjectBuildingRequest setRepositoryCache( RepositoryCache repositoryCache )
    {
        this.repositoryCache = repositoryCache;

        return this;
    }

    public RepositoryCache getRepositoryCache()
    {
        return repositoryCache;
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
        if ( remoteRepositories != null )
        {
            this.remoteRepositories = new ArrayList<ArtifactRepository>( remoteRepositories );
        }
        else
        {
            this.remoteRepositories.clear();
        }

        return this;
    }

    public List<ArtifactRepository> getPluginArtifactRepositories()
    {
        return pluginArtifactRepositories;
    }

    public ProjectBuildingRequest setPluginArtifactRepositories( List<ArtifactRepository> pluginArtifactRepositories )
    {
        if ( pluginArtifactRepositories != null )
        {
            this.pluginArtifactRepositories = new ArrayList<ArtifactRepository>( pluginArtifactRepositories );
        }
        else
        {
            this.pluginArtifactRepositories.clear();
        }

        return this;
    }

    public ProjectBuildingRequest setServers( List<Server> servers )
    {
        if ( servers != null )
        {
            this.servers = new ArrayList<Server>( servers );
        }
        else
        {
            this.servers.clear();
        }

        return this;
    }

    public List<Server> getServers()
    {
        return servers;
    }

    public ProjectBuildingRequest setMirrors( List<Mirror> mirrors )
    {
        if ( mirrors != null )
        {
            this.mirrors = new ArrayList<Mirror>( mirrors );
        }
        else
        {
            this.mirrors.clear();
        }

        return this;
    }

    public List<Mirror> getMirrors()
    {
        return mirrors;
    }

    public ProjectBuildingRequest setProxies( List<Proxy> proxies )
    {
        if ( proxies != null )
        {
            this.proxies = new ArrayList<Proxy>( proxies );
        }
        else
        {
            this.proxies.clear();
        }

        return this;
    }

    public List<Proxy> getProxies()
    {
        return proxies;
    }

    public Properties getSystemProperties()
    {
        return systemProperties;
    }

    public ProjectBuildingRequest setSystemProperties( Properties systemProperties )
    {
        if ( systemProperties != null )
        {
            this.systemProperties = new Properties();
            this.systemProperties.putAll( systemProperties );
        }
        else
        {
            this.systemProperties.clear();
        }

        return this;
    }

    public Properties getUserProperties()
    {
        return userProperties;
    }

    public ProjectBuildingRequest setUserProperties( Properties userProperties )
    {
        if ( userProperties != null )
        {
            this.userProperties = new Properties();
            this.userProperties.putAll( userProperties );
        }
        else
        {
            this.userProperties.clear();
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
    
    public ProjectBuildingRequest setResolveDependencies( boolean resolveDependencies )
    {
        this.resolveDependencies = resolveDependencies;
        return this;
    }

    public boolean isResolveDependencies()
    {
        return resolveDependencies;
    }

    public ProjectBuildingRequest setValidationLevel( int validationLevel )
    {
        this.validationLevel = validationLevel;
        return this;
    }

    public int getValidationLevel()
    {
        return validationLevel;
    }

    public List<String> getActiveProfileIds()
    {
        return activeProfileIds;
    }

    public void setActiveProfileIds( List<String> activeProfileIds )
    {
        if ( activeProfileIds != null )
        {
            this.activeProfileIds = new ArrayList<String>( activeProfileIds );
        }
        else
        {
            this.activeProfileIds.clear();
        }
    }

    public List<String> getInactiveProfileIds()
    {
        return inactiveProfileIds;
    }

    public void setInactiveProfileIds( List<String> inactiveProfileIds )
    {
        if ( inactiveProfileIds != null )
        {
            this.inactiveProfileIds = new ArrayList<String>( inactiveProfileIds );
        }
        else
        {
            this.inactiveProfileIds.clear();
        }
    }

    public void setProfiles( List<Profile> profiles )
    {
        if ( profiles != null )
        {
            this.profiles = new ArrayList<Profile>( profiles );
        }
        else
        {
            this.profiles.clear();
        }
    }

    public void addProfile( Profile profile )
    {
        profiles.add( profile );
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
