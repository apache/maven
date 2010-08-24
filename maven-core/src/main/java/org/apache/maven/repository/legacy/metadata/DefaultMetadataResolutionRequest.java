package org.apache.maven.repository.legacy.metadata;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;

/**
 * Forms a request to retrieve artifact metadata.
 * 
 * @author Benjamin Bentmann
 */
public class DefaultMetadataResolutionRequest
    implements MetadataResolutionRequest
{

    private Artifact artifact;

    private boolean resolveManagedVersions;

    private RepositoryRequest repositoryRequest;

    private List<Server> servers;

    private List<Mirror> mirrors;

    private List<Proxy> proxies;

    public DefaultMetadataResolutionRequest()
    {
        repositoryRequest = new DefaultRepositoryRequest();
    }

    public DefaultMetadataResolutionRequest( RepositoryRequest repositoryRequest )
    {
        this.repositoryRequest = new DefaultRepositoryRequest( repositoryRequest );
    }

    public DefaultMetadataResolutionRequest( ArtifactResolutionRequest resolutionRequest )
    {
        this.repositoryRequest = new DefaultRepositoryRequest( resolutionRequest );
        setServers( resolutionRequest.getServers() );
        setMirrors( resolutionRequest.getMirrors() );
        setProxies( resolutionRequest.getProxies() );
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public DefaultMetadataResolutionRequest setArtifact( Artifact artifact )
    {
        this.artifact = artifact;

        return this;
    }

    public ArtifactRepository getLocalRepository()
    {
        return repositoryRequest.getLocalRepository();
    }

    public DefaultMetadataResolutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        repositoryRequest.setLocalRepository( localRepository );

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return repositoryRequest.getRemoteRepositories();
    }

    public DefaultMetadataResolutionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        repositoryRequest.setRemoteRepositories( remoteRepositories );

        return this;
    }

    public boolean isResolveManagedVersions()
    {
        return resolveManagedVersions;
    }

    public DefaultMetadataResolutionRequest setResolveManagedVersions( boolean resolveManagedVersions )
    {
        this.resolveManagedVersions = resolveManagedVersions;

        return this;
    }

    public boolean isOffline()
    {
        return repositoryRequest.isOffline();
    }

    public DefaultMetadataResolutionRequest setOffline( boolean offline )
    {
        repositoryRequest.setOffline( offline );

        return this;
    }

    public boolean isForceUpdate()
    {
        return repositoryRequest.isForceUpdate();
    }

    public DefaultMetadataResolutionRequest setForceUpdate( boolean forceUpdate )
    {
        repositoryRequest.setForceUpdate( forceUpdate );

        return this;
    }

    public MetadataResolutionRequest setServers( List<Server> servers )
    {
        this.servers = servers;

        return this;
    }

    public List<Server> getServers()
    {
        if ( servers == null )
        {
            servers = new ArrayList<Server>();
        }

        return servers;
    }

    public MetadataResolutionRequest setMirrors( List<Mirror> mirrors )
    {
        this.mirrors = mirrors;

        return this;
    }

    public List<Mirror> getMirrors()
    {
        if ( mirrors == null )
        {
            mirrors = new ArrayList<Mirror>();
        }

        return mirrors;
    }

    public MetadataResolutionRequest setProxies( List<Proxy> proxies )
    {
        this.proxies = proxies;

        return this;
    }

    public List<Proxy> getProxies()
    {
        if ( proxies == null )
        {
            proxies = new ArrayList<Proxy>();
        }

        return proxies;
    }

}
