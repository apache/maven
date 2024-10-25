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
package org.apache.maven.artifact.repository.metadata;

import java.util.List;

import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;

/**
 * RepositoryMetadataManager
 */
@Deprecated
public interface RepositoryMetadataManager {

    void resolve(RepositoryMetadata repositoryMetadata, RepositoryRequest repositoryRequest)
            throws RepositoryMetadataResolutionException;

    void resolve(
            RepositoryMetadata repositoryMetadata,
            List<ArtifactRepository> repositories,
            ArtifactRepository localRepository)
            throws RepositoryMetadataResolutionException;

    void resolveAlways(
            RepositoryMetadata metadata, ArtifactRepository localRepository, ArtifactRepository remoteRepository)
            throws RepositoryMetadataResolutionException;

    /**
     * Deploy metadata to the remote repository.
     *
     * @param metadata             the metadata to deploy
     * @param localRepository      the local repository to install to first
     * @param deploymentRepository the remote repository to deploy to
     * @throws RepositoryMetadataDeploymentException in case of metadata deployment issue
     */
    void deploy(ArtifactMetadata metadata, ArtifactRepository localRepository, ArtifactRepository deploymentRepository)
            throws RepositoryMetadataDeploymentException;

    /**
     * Install the metadata in the local repository.
     *
     * @param metadata        the metadata
     * @param localRepository the local repository
     * @throws RepositoryMetadataInstallationException in case of metadata installation issue
     */
    void install(ArtifactMetadata metadata, ArtifactRepository localRepository)
            throws RepositoryMetadataInstallationException;
}
