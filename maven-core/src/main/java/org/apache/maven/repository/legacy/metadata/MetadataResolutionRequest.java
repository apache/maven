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
package org.apache.maven.repository.legacy.metadata;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.RepositoryRequest;

/**
 * Forms a request to retrieve artifact metadata.
 *
 * @author Benjamin Bentmann
 */
public interface MetadataResolutionRequest extends RepositoryRequest {

    /**
     * Indicates whether network access to remote repositories has been disabled.
     *
     * @return {@code true} if remote access has been disabled, {@code false} otherwise.
     */
    boolean isOffline();

    /**
     * Enables/disables network access to remote repositories.
     *
     * @param offline {@code true} to disable remote access, {@code false} to allow network access.
     * @return This request, never {@code null}.
     */
    MetadataResolutionRequest setOffline(boolean offline);

    /**
     * Gets the artifact to resolve metadata for.
     *
     * @return The artifact to resolve metadata for or {@code null} if not set.
     */
    Artifact getArtifact();

    /**
     * Sets the artifact for which to resolve metadata.
     *
     * @param artifact The artifact for which to resolve metadata.
     * @return This request, never {@code null}.
     */
    MetadataResolutionRequest setArtifact(Artifact artifact);

    /**
     * Gets the local repository to use for the resolution.
     *
     * @return The local repository to use for the resolution or {@code null} if not set.
     */
    ArtifactRepository getLocalRepository();

    /**
     * Sets the local repository to use for the resolution.
     *
     * @param localRepository The local repository to use for the resolution.
     * @return This request, never {@code null}.
     */
    MetadataResolutionRequest setLocalRepository(ArtifactRepository localRepository);

    /**
     * Gets the remote repositories to use for the resolution.
     *
     * @return The remote repositories to use for the resolution, never {@code null}.
     */
    List<ArtifactRepository> getRemoteRepositories();

    /**
     * Sets the remote repositories to use for the resolution.
     *
     * @param remoteRepositories The remote repositories to use for the resolution.
     * @return This request, never {@code null}.
     */
    MetadataResolutionRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories);

    /**
     * Determines whether the managed version information should be retrieved.
     *
     * @return {@code true} if the dependency management information should be retrieved, {@code false} otherwise.
     */
    boolean isResolveManagedVersions();

    /**
     * Enables/disables resolution of the dependency management information.
     *
     * @param resolveManagedVersions {@code true} if the dependency management information should be retrieved, {@code
     *            false} otherwise.
     * @return This request, never {@code null}.
     */
    MetadataResolutionRequest setResolveManagedVersions(boolean resolveManagedVersions);
}
