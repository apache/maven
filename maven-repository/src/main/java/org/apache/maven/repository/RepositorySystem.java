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

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.events.TransferListener;

/**
 * @author Jason van Zyl
 */
public interface RepositorySystem
{
    static final String DEFAULT_LOCAL_REPO_ID = "local";
    
    static final String userHome = System.getProperty( "user.home" );
    
    static final File userMavenConfigurationHome = new File( userHome, ".m2" );
    
    static final File defaultUserLocalRepository = new File( userMavenConfigurationHome, "repository" );
    
    static final String DEFAULT_REMOTE_REPO_ID = "central";

    static final String DEFAULT_REMOTE_REPO_URL = "http://repo1.maven.org/maven2";

    Artifact createArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createProjectArtifact( String groupId, String artifactId, String version );

    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type, String classifier );
    
    Artifact createPluginArtifact( Plugin plugin );
    
    Artifact createDependencyArtifact( Dependency dependency );
        
    ArtifactRepository buildArtifactRepository( Repository repository )
        throws InvalidRepositoryException;
        
    ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException;    
    
    ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException;
    
    ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException;
    
    ArtifactResolutionResult resolve( ArtifactResolutionRequest request );

    MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request );
           
    //TODO: remove the request should already be processed to select the mirror for the request instead of the processing happen internally.
    // Mirrors    
    void addMirror( String id, String mirrorOf, String url );        
    List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories );  
    
    // Install
    
    // Deploy
    
    // Map types of artifacts
    
    // Raw file transfers
    void publish( ArtifactRepository repository, File source, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException;
    
    void retrieve( ArtifactRepository repository, File destination, String remotePath, TransferListener downloadMonitor )
        throws TransferFailedException, ResourceDoesNotExistException;        
}
