package org.apache.maven.artifact.repository;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

public interface ArtifactRepository
{
    String pathOf( Artifact artifact );

    String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata );

    String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository );

    String getUrl();

    void setUrl( String url );

    String getBasedir();

    String getProtocol();

    String getId();

    void setId( String id );

    ArtifactRepositoryPolicy getSnapshots();

    void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryPolicy getReleases();

    void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryLayout getLayout();

    void setLayout( ArtifactRepositoryLayout layout );

    String getKey();
    
    //
    // New interface methods for the repository system. 
    //
    Artifact find( Artifact artifact );
    
    void setAuthentication( Authentication authentication );
    
    Authentication getAuthentication();
}
