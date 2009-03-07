package org.apache.maven.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @author Jason van Zyl
 */
public interface RepositorySystem
{
    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId );

    Artifact createPluginArtifact( Plugin plugin );
    
    Artifact createDependencyArtifact( Dependency dependency );
    
    //REMOVE
    // This will disappear when we actually deal with resolving a root dependency and its dependencies. This is used everywhere because of that
    // deficiency
    Set<Artifact> createArtifacts( List<Dependency> dependencies, String inheritedScope, ArtifactFilter dependencyFilter, MavenRepositoryWrapper reactor )
        throws VersionNotFoundException;

    // Repository creation

    // maven model
    ArtifactRepository buildArtifactRepository( Repository repository )
        throws InvalidRepositoryException;
        
    ArtifactRepository createLocalRepository( String url, String repositoryId )
        throws IOException;

    ArtifactResolutionResult resolve( ArtifactResolutionRequest request );

    //MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request );
       
    //REMOVE
    // Network enablement: this needs to go as we will know at a higher level from the embedder if the system is offline or not, we should not have to
    // deal with this here.
    void setOnline( boolean online );
    boolean isOnline();
    
    //REMOVE
    // These should be associated with repositories and the repositories should be examine as part of metadatda and
    // artifact resolution. So these methods should also not be here.
    void addProxy( String protocol, String host, int port, String username, String password, String nonProxyHosts );
    void addAuthenticationInfo( String repositoryId, String username, String password, String privateKey, String passphrase );
    void addPermissionInfo( String repositoryId, String filePermissions, String directoryPermissions );
    
    // Mirrors
    
    void addMirror( String id, String mirrorOf, String url );        
    List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories );    
}
