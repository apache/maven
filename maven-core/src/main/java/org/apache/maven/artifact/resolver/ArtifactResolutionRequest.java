package org.apache.maven.artifact.resolver;

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
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryCache;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;

/**
 * A resolution request allows you to either use an existing MavenProject, or a coordinate (gid:aid:version)
 * to process a POMs dependencies.
 *
 * @author Jason van Zyl
 */
public class ArtifactResolutionRequest
    implements RepositoryRequest
{
    private static final String LS = System.lineSeparator();

    private Artifact artifact;

    // Needs to go away
    // These are really overrides now, projects defining dependencies for a plugin that override what is
    // specified in the plugin itself.
    private Set<Artifact> artifactDependencies;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private ArtifactFilter collectionFilter;

    private ArtifactFilter resolutionFilter;

    // Needs to go away
    private List<ResolutionListener> listeners = new ArrayList<>();

    // This is like a filter but overrides all transitive versions
    private Map<String, Artifact> managedVersionMap;

    private boolean resolveRoot = true;

    private boolean resolveTransitively = false;

    private boolean offline;

    private boolean forceUpdate;

    private List<Server> servers;

    private List<Mirror> mirrors;

    private List<Proxy> proxies;

    public ArtifactResolutionRequest()
    {
        // nothing here
    }

    public ArtifactResolutionRequest( RepositoryRequest request )
    {
        setLocalRepository( request.getLocalRepository() );
        setRemoteRepositories( request.getRemoteRepositories() );
        setOffline( request.isOffline() );
        setForceUpdate( request.isForceUpdate() );
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public ArtifactResolutionRequest setArtifact( Artifact artifact )
    {
        this.artifact = artifact;

        return this;
    }

    public ArtifactResolutionRequest setArtifactDependencies( Set<Artifact> artifactDependencies )
    {
        this.artifactDependencies = artifactDependencies;

        return this;
    }

    public Set<Artifact> getArtifactDependencies()
    {
        return artifactDependencies;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public ArtifactResolutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public ArtifactResolutionRequest setRemoteRepositories( List<ArtifactRepository> remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;

        return this;
    }

    /**
     * Gets the artifact filter that controls traversal of the dependency graph.
     *
     * @return The filter used to determine which of the artifacts in the dependency graph should be traversed or
     *         {@code null} to collect all transitive dependencies.
     */
    public ArtifactFilter getCollectionFilter()
    {
        return collectionFilter;
    }

    public ArtifactResolutionRequest setCollectionFilter( ArtifactFilter filter )
    {
        this.collectionFilter = filter;

        return this;
    }

    /**
     * Gets the artifact filter that controls downloading of artifact files. This filter operates on those artifacts
     * that have been included by the {@link #getCollectionFilter()}.
     *
     * @return The filter used to determine which of the artifacts should have their files resolved or {@code null} to
     *         resolve the files for all collected artifacts.
     */
    public ArtifactFilter getResolutionFilter()
    {
        return resolutionFilter;
    }

    public ArtifactResolutionRequest setResolutionFilter( ArtifactFilter filter )
    {
        this.resolutionFilter = filter;

        return this;
    }

    public List<ResolutionListener> getListeners()
    {
        return listeners;
    }

    public ArtifactResolutionRequest setListeners( List<ResolutionListener> listeners )
    {
        this.listeners = listeners;

        return this;
    }

    public ArtifactResolutionRequest addListener( ResolutionListener listener )
    {
        listeners.add( listener );

        return this;
    }

    public Map<String, Artifact> getManagedVersionMap()
    {
        return managedVersionMap;
    }

    public ArtifactResolutionRequest setManagedVersionMap( Map<String, Artifact> managedVersionMap )
    {
        this.managedVersionMap = managedVersionMap;

        return this;
    }

    public ArtifactResolutionRequest setResolveRoot( boolean resolveRoot )
    {
        this.resolveRoot = resolveRoot;

        return this;
    }

    public boolean isResolveRoot()
    {
        return resolveRoot;
    }

    public ArtifactResolutionRequest setResolveTransitively( boolean resolveDependencies )
    {
        this.resolveTransitively = resolveDependencies;

        return this;
    }

    public boolean isResolveTransitively()
    {
        return resolveTransitively;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder()
                .append( "REQUEST: " ).append( LS )
                .append( "artifact: " ).append( artifact ).append( LS )
                .append( artifactDependencies ).append( LS )
                .append( "localRepository: " ).append( localRepository ).append( LS )
                .append( "remoteRepositories: " ).append( remoteRepositories );

        return sb.toString();
    }

    public boolean isOffline()
    {
        return offline;
    }

    public ArtifactResolutionRequest setOffline( boolean offline )
    {
        this.offline = offline;

        return this;
    }

    public boolean isForceUpdate()
    {
        return forceUpdate;
    }

    public ArtifactResolutionRequest setForceUpdate( boolean forceUpdate )
    {
        this.forceUpdate = forceUpdate;

        return this;
    }

    public ArtifactResolutionRequest setServers( List<Server> servers )
    {
        this.servers = servers;

        return this;
    }

    public List<Server> getServers()
    {
        if ( servers == null )
        {
            servers = new ArrayList<>();
        }

        return servers;
    }

    public ArtifactResolutionRequest setMirrors( List<Mirror> mirrors )
    {
        this.mirrors = mirrors;

        return this;
    }

    public List<Mirror> getMirrors()
    {
        if ( mirrors == null )
        {
            mirrors = new ArrayList<>();
        }

        return mirrors;
    }

    public ArtifactResolutionRequest setProxies( List<Proxy> proxies )
    {
        this.proxies = proxies;

        return this;
    }

    public List<Proxy> getProxies()
    {
        if ( proxies == null )
        {
            proxies = new ArrayList<>();
        }

        return proxies;
    }

    //
    // Used by Tycho and will break users and force them to upgrade to Maven 3.1 so we should really leave
    // this here, possibly indefinitely.
    //
    public ArtifactResolutionRequest setCache( RepositoryCache cache )
    {
        return this;
    }
}
