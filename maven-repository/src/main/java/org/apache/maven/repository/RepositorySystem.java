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
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;

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

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createProjectArtifact( String groupId, String artifactId, String metaVersionId );

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

    /**
     * this is the new metadata-based entry point into repository system. By default - it will transitively resolve metadata
     * for the supplied root GAV and return a flat set of dependency metadatas. Tweaking the request allows user to ask for 
     * various formats of the response - resolved tree, resolved graph or dirty tree. Only the resolved tree is implemented now
     * in MercuryRepositorySystem, LegacyRepositorySystem ignores this call for now.  
     * 
     * @param request - supplies all necessary details for the resolution configuration
     * @return
     */
    MetadataResolutionResult resolveMetadata( MetadataResolutionRequest request );
           
    //TODO: remove the request should already be processed to select the mirror for the request instead of the processing happen internally.
    // Mirrors    
    void addMirror( String id, String mirrorOf, String url );        
    List<ArtifactRepository> getMirrors( List<ArtifactRepository> repositories );    
}
