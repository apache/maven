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
package org.apache.maven.artifact.metadata;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.repository.legacy.metadata.MetadataResolutionRequest;

/**
 * Provides some metadata operations, like querying the remote repository for a list of versions available for an
 * artifact - deprecated
 */
@Deprecated
public interface ArtifactMetadataSource extends org.apache.maven.repository.legacy.metadata.ArtifactMetadataSource {

    ResolutionGroup retrieve(MetadataResolutionRequest request) throws ArtifactMetadataRetrievalException;

    ResolutionGroup retrieve(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException;

    List<ArtifactVersion> retrieveAvailableVersions(MetadataResolutionRequest request)
            throws ArtifactMetadataRetrievalException;

    List<ArtifactVersion> retrieveAvailableVersions(
            Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories)
            throws ArtifactMetadataRetrievalException;

    List<ArtifactVersion> retrieveAvailableVersionsFromDeploymentRepository(
            Artifact artifact, ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws ArtifactMetadataRetrievalException;
}
