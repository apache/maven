/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.maven.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class MetadataResolutionRequest
{
    private Artifact artifact;

    // Needs to go away
    private Set<Artifact> artifactDependencies;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    // Not sure what to do with this?
    // Scope
    // Lock down lists
    private ArtifactFilter filter;

    // Needs to go away
    private List<ResolutionListener> listeners = new ArrayList<ResolutionListener>();

    // This is like a filter but overrides all transitive versions 
    private Map managedVersionMap;

    // This should not be in here, it's a component
    private ArtifactMetadataSource metadataSource;

    private boolean resolveRoot = true;

    /** result type - flat list; the default */
    private boolean asList = true;
    
    /** result type - dirty tree */
    private boolean asDirtyTree = false;
    
    /** result type - resolved tree */
    private boolean asResolvedTree = false;
    
    /** result type - graph */
    private boolean asGraph = false;
    
    public MetadataResolutionRequest()
    {  
    }
    
    public MetadataResolutionRequest( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )    
    {        
        this.artifact = artifact;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }
    
    public Artifact getArtifact()
    {
        return artifact;
    }

    public MetadataResolutionRequest setArtifact( Artifact artifact )
    {
        this.artifact = artifact;

        return this;
    }

    public MetadataResolutionRequest setArtifactDependencies( Set<Artifact> artifactDependencies )
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

    public MetadataResolutionRequest setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;

        return this;
    }

    public List<ArtifactRepository> getRemoteRepostories()
    {
        return remoteRepositories;
    }

    public MetadataResolutionRequest setRemoteRepostories( List<ArtifactRepository> remoteRepostories )
    {
        this.remoteRepositories = remoteRepostories;

        return this;
    }

    public ArtifactFilter getFilter()
    {
        return filter;
    }

    public MetadataResolutionRequest setFilter( ArtifactFilter filter )
    {
        this.filter = filter;

        return this;
    }

    public List<ResolutionListener> getListeners()
    {
        return listeners;
    }

    public MetadataResolutionRequest setListeners( List<ResolutionListener> listeners )
    {        
        this.listeners = listeners;
        
        return this;
    }
    
    public MetadataResolutionRequest addListener( ResolutionListener listener )
    {
        listeners.add( listener );

        return this;
    }

    // ------------------------------------------------------------------------
    //
    // ------------------------------------------------------------------------

    public ArtifactMetadataSource getMetadataSource()
    {
        return metadataSource;
    }

    public MetadataResolutionRequest setMetadataSource( ArtifactMetadataSource metadataSource )
    {
        this.metadataSource = metadataSource;

        return this;
    }

    public Map getManagedVersionMap()
    {
        return managedVersionMap;
    }

    public MetadataResolutionRequest setManagedVersionMap( Map managedVersionMap )
    {
        this.managedVersionMap = managedVersionMap;

        return this;
    }

    public MetadataResolutionRequest setResolveRoot( boolean resolveRoot )
    {
        this.resolveRoot = resolveRoot;
        
        return this;
    }
    
    public boolean isResolveRoot()
    {
        return resolveRoot;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer()
                .append( "REQUEST: " ).append(  "\n" )
                .append( "artifact: " ).append( artifact ).append(  "\n" )
                .append( artifactDependencies ).append(  "\n" )
                .append( "localRepository: " ).append(  localRepository ).append(  "\n" )
                .append( "remoteRepositories: " ).append(  remoteRepositories ).append(  "\n" )
                .append( "metadataSource: " ).append(  metadataSource ).append(  "\n" );
        
        return sb.toString();
    }

    public boolean isAsList()
    {
        return asList;
    }

    public MetadataResolutionRequest setAsList( boolean asList )
    {
        this.asList = asList;
        return this;
    }

    public boolean isAsDirtyTree()
    {
        return asDirtyTree;
    }

    public MetadataResolutionRequest setAsDirtyTree( boolean asDirtyTree )
    {
        this.asDirtyTree = asDirtyTree;
        return this;
    }

    public boolean isAsResolvedTree()
    {
        return asResolvedTree;
    }

    public MetadataResolutionRequest setAsResolvedTree( boolean asResolvedTree )
    {
        this.asResolvedTree = asResolvedTree;
        return this;
    }

    public boolean isAsGraph()
    {
        return asGraph;
    }

    public MetadataResolutionRequest setAsGraph( boolean asGraph )
    {
        this.asGraph = asGraph;
        return this;
    }
}
