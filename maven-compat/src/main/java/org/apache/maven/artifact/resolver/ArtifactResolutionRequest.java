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
import org.apache.maven.wagon.events.TransferListener;

/**
 * A resolution request allows you to either use an existing MavenProject, or a coordinate (gid:aid:version)
 * to process a POMs dependencies.
 *
 * @author Jason van Zyl
 */
public class ArtifactResolutionRequest
    implements RepositoryRequest
{

    private Artifact artifact;

    // Needs to go away
    // These are really overrides now, projects defining dependencies for a plugin that override what is
    // specified in the plugin itself.
    private Set<Artifact> artifactDependencies;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    private RepositoryCache cache;

    private ArtifactFilter filter;

    // Needs to go away
    private List<ResolutionListener> listeners = new ArrayList<ResolutionListener>();

    // This is like a filter but overrides all transitive versions 
    private Map managedVersionMap;

    private TransferListener transferListener;
    
    private boolean resolveRoot = true;

    private boolean resolveTransitively = false;

    public ArtifactResolutionRequest()
    {
        // nothing here
    }

    public ArtifactResolutionRequest( RepositoryRequest request )
    {
        setLocalRepository( request.getLocalRepository() );
        setRemoteRepositories( request.getRemoteRepositories() );
        setCache( request.getCache() );
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

    public ArtifactFilter getFilter()
    {
        return filter;
    }

    public ArtifactResolutionRequest setFilter( ArtifactFilter filter )
    {
        this.filter = filter;

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

    public Map getManagedVersionMap()
    {
        return managedVersionMap;
    }

    public ArtifactResolutionRequest setManagedVersionMap( Map managedVersionMap )
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
    
    public TransferListener getTransferListener()
    {
        return transferListener;
    }

    public ArtifactResolutionRequest setTransferListener( TransferListener transferListener )
    {
        this.transferListener = transferListener;
        return this;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder()
                .append( "REQUEST: " ).append(  "\n" )
                .append( "artifact: " ).append( artifact ).append(  "\n" )
                .append( artifactDependencies ).append(  "\n" )
                .append( "localRepository: " ).append(  localRepository ).append(  "\n" )
                .append( "remoteRepositories: " ).append(  remoteRepositories ).append(  "\n" );
        
        return sb.toString();
    }

    public RepositoryCache getCache()
    {
        return cache;
    }

    public ArtifactResolutionRequest setCache( RepositoryCache cache )
    {
        this.cache = cache;

        return this;
    }

}
