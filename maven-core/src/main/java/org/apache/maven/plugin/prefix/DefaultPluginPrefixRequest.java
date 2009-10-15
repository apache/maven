package org.apache.maven.plugin.prefix;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.ArtifactTransferListener;

/**
 * Collects settings required to resolve a plugin prefix.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultPluginPrefixRequest
    implements PluginPrefixRequest
{

    private String prefix;

    private List<String> pluginGroups;

    private Model pom;

    private RepositoryRequest repositoryRequest;

    /**
     * Creates an empty request.
     */
    public DefaultPluginPrefixRequest()
    {
        repositoryRequest = new DefaultRepositoryRequest();
    }

    /**
     * Creates a request by copying settings from the specified repository request.
     * 
     * @param repositoryRequest The repository request to copy from, must not be {@code null}.
     */
    public DefaultPluginPrefixRequest( RepositoryRequest repositoryRequest )
    {
        this.repositoryRequest = new DefaultRepositoryRequest( repositoryRequest );
    }

    /**
     * Creates a request for the specified plugin prefix and build session. The provided build session will be used to
     * configure repository settings. If the session has a current project, its plugin artifact repositories and model
     * will be used as well.
     * 
     * @param prefix The plugin prefix to resolve, must not be {@code null}.
     * @param session The build session from which to derive further settings, must not be {@code null}.
     */
    public DefaultPluginPrefixRequest( String prefix, MavenSession session )
    {
        setPrefix( prefix );

        this.repositoryRequest = new DefaultRepositoryRequest();

        setCache( session.getRepositoryCache() );
        setLocalRepository( session.getLocalRepository() );
        setOffline( session.isOffline() );
        setTransferListener( session.getRequest().getTransferListener() );

        MavenProject project = session.getCurrentProject();
        if ( project != null )
        {
            setRemoteRepositories( project.getPluginArtifactRepositories() );
            setPom( project.getModel() );
        }

        setPluginGroups( session.getPluginGroups() );
    }

    public String getPrefix()
    {
        return prefix;
    }

    public DefaultPluginPrefixRequest setPrefix( String prefix )
    {
        this.prefix = prefix;

        return this;
    }

    public List<String> getPluginGroups()
    {
        if ( pluginGroups == null )
        {
            pluginGroups = new ArrayList<String>();
        }

        return pluginGroups;
    }

    public DefaultPluginPrefixRequest setPluginGroups( List<String> pluginGroups )
    {
        this.pluginGroups = pluginGroups;

        return this;
    }

    public Model getPom()
    {
        return pom;
    }

    public DefaultPluginPrefixRequest setPom( Model pom )
    {
        this.pom = pom;

        return this;
    }

    public RepositoryCache getCache()
    {
        return repositoryRequest.getCache();
    }

    public DefaultPluginPrefixRequest setCache( RepositoryCache cache )
    {
        repositoryRequest.setCache( cache );

        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return repositoryRequest.getLocalRepository();
    }

    public DefaultPluginPrefixRequest setLocalRepository( ArtifactRepository localRepository )
    {
        repositoryRequest.setLocalRepository( localRepository );

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return repositoryRequest.getRemoteRepositories();
    }

    public DefaultPluginPrefixRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        repositoryRequest.setRemoteRepositories( remoteRepositories );

        return this;
    }

    public boolean isOffline()
    {
        return repositoryRequest.isOffline();
    }

    public DefaultPluginPrefixRequest setOffline( boolean offline )
    {
        repositoryRequest.setOffline( offline );

        return this;
    }

    public boolean isForceUpdate()
    {
        return repositoryRequest.isForceUpdate();
    }

    public DefaultPluginPrefixRequest setForceUpdate( boolean forceUpdate )
    {
        repositoryRequest.setForceUpdate( forceUpdate );

        return this;
    }

    public ArtifactTransferListener getTransferListener()
    {
        return repositoryRequest.getTransferListener();
    }

    public DefaultPluginPrefixRequest setTransferListener( ArtifactTransferListener transferListener )
    {
        repositoryRequest.setTransferListener( transferListener );

        return this;
    }

}
