package org.apache.maven.plugin.version;

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

import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.ArtifactTransferListener;

/**
 * Collects settings required to resolve the version for a plugin.
 * 
 * @since 3.0-alpha-3
 * @author Benjamin Bentmann
 */
public class DefaultPluginVersionRequest
    implements PluginVersionRequest
{

    private String groupId;

    private String artifactId;

    private Model pom;

    private RepositoryRequest repositoryRequest;

    /**
     * Creates an empty request.
     */
    public DefaultPluginVersionRequest()
    {
        repositoryRequest = new DefaultRepositoryRequest();
    }

    /**
     * Creates a request by copying settings from the specified repository request.
     * 
     * @param repositoryRequest The repository request to copy from, must not be {@code null}.
     */
    public DefaultPluginVersionRequest( RepositoryRequest repositoryRequest )
    {
        this.repositoryRequest = new DefaultRepositoryRequest( repositoryRequest );
    }

    /**
     * Creates a request for the specified plugin by copying settings from the specified repository request.
     * 
     * @param The plugin for which to resolve a version, must not be {@code null}.
     * @param repositoryRequest The repository request to copy from, must not be {@code null}.
     */
    public DefaultPluginVersionRequest( Plugin plugin, RepositoryRequest repositoryRequest )
    {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.repositoryRequest = new DefaultRepositoryRequest( repositoryRequest );
    }

    /**
     * Creates a request for the specified plugin by copying settings from the specified build session. If the session
     * has a current project, its plugin artifact repositories will be used as well.
     * 
     * @param The plugin for which to resolve a version, must not be {@code null}.
     * @param repositoryRequest The repository request to copy from, must not be {@code null}.
     */
    public DefaultPluginVersionRequest( Plugin plugin, MavenSession session )
    {
        this.groupId = plugin.getGroupId();
        this.artifactId = plugin.getArtifactId();
        this.repositoryRequest = new DefaultRepositoryRequest();

        setCache( session.getRepositoryCache() );
        setLocalRepository( session.getLocalRepository() );
        setOffline( session.isOffline() );
        setForceUpdate( session.getRequest().isUpdateSnapshots() );
        setTransferListener( session.getRequest().getTransferListener() );

        MavenProject project = session.getCurrentProject();
        if ( project != null )
        {
            setRemoteRepositories( project.getPluginArtifactRepositories() );
        }
    }

    public String getGroupId()
    {
        return groupId;
    }

    public DefaultPluginVersionRequest setGroupId( String groupId )
    {
        this.groupId = groupId;

        return this;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public DefaultPluginVersionRequest setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;

        return this;
    }

    public Model getPom()
    {
        return pom;
    }

    public DefaultPluginVersionRequest setPom( Model pom )
    {
        this.pom = pom;

        return this;
    }

    public RepositoryCache getCache()
    {
        return repositoryRequest.getCache();
    }

    public DefaultPluginVersionRequest setCache( RepositoryCache cache )
    {
        repositoryRequest.setCache( cache );

        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return repositoryRequest.getLocalRepository();
    }

    public DefaultPluginVersionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        repositoryRequest.setLocalRepository( localRepository );

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return repositoryRequest.getRemoteRepositories();
    }

    public DefaultPluginVersionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        repositoryRequest.setRemoteRepositories( remoteRepositories );

        return this;
    }

    public boolean isOffline()
    {
        return repositoryRequest.isOffline();
    }

    public DefaultPluginVersionRequest setOffline( boolean offline )
    {
        repositoryRequest.setOffline( offline );

        return this;
    }

    public boolean isForceUpdate()
    {
        return repositoryRequest.isForceUpdate();
    }

    public DefaultPluginVersionRequest setForceUpdate( boolean forceUpdate )
    {
        repositoryRequest.setForceUpdate( forceUpdate );

        return this;
    }

    public ArtifactTransferListener getTransferListener()
    {
        return repositoryRequest.getTransferListener();
    }

    public DefaultPluginVersionRequest setTransferListener( ArtifactTransferListener transferListener )
    {
        repositoryRequest.setTransferListener( transferListener );

        return this;
    }

}
