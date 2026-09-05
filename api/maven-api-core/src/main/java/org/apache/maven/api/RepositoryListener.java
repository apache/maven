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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Receives repository events emitted while resolving, installing, and deploying artifacts and metadata.
 * Implementations must be thread-safe because callbacks may occur concurrently.
 * Runtime exceptions thrown by a listener do not stop repository processing or notification of other listeners.
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
public interface RepositoryListener {

    /**
     * Called when an artifact descriptor could not be parsed.
     *
     * @param event the event containing the artifact and parse failure
     */
    default void artifactDescriptorInvalid(@Nonnull RepositoryEvent event) {}

    /**
     * Called when an artifact descriptor could not be found.
     *
     * @param event the event containing the artifact whose descriptor is missing
     */
    default void artifactDescriptorMissing(@Nonnull RepositoryEvent event) {}

    /**
     * Called when repository metadata could not be parsed.
     *
     * @param event the event containing the metadata and parse failure
     */
    default void metadataInvalid(@Nonnull RepositoryEvent event) {}

    /**
     * Called before an artifact is resolved.
     *
     * @param event the event containing the artifact and, when applicable, its repository
     */
    default void artifactResolving(@Nonnull RepositoryEvent event) {}

    /**
     * Called after an artifact resolution attempt finishes.
     *
     * @param event the event containing the artifact and the resulting path or failure
     */
    default void artifactResolved(@Nonnull RepositoryEvent event) {}

    /**
     * Called before repository metadata is resolved.
     *
     * @param event the event containing the metadata and, when applicable, its repository
     */
    default void metadataResolving(@Nonnull RepositoryEvent event) {}

    /**
     * Called after a repository metadata resolution attempt finishes.
     *
     * @param event the event containing the metadata and the resulting path or failure
     */
    default void metadataResolved(@Nonnull RepositoryEvent event) {}

    /**
     * Called before an artifact is downloaded from a remote repository.
     *
     * @param event the event containing the artifact and remote repository
     */
    default void artifactDownloading(@Nonnull RepositoryEvent event) {}

    /**
     * Called after an artifact download attempt finishes.
     *
     * @param event the event containing the artifact, remote repository, and resulting path or failure
     */
    default void artifactDownloaded(@Nonnull RepositoryEvent event) {}

    /**
     * Called before repository metadata is downloaded from a remote repository.
     *
     * @param event the event containing the metadata and remote repository
     */
    default void metadataDownloading(@Nonnull RepositoryEvent event) {}

    /**
     * Called after a repository metadata download attempt finishes.
     *
     * @param event the event containing the metadata, remote repository, and resulting path or failure
     */
    default void metadataDownloaded(@Nonnull RepositoryEvent event) {}

    /**
     * Called before an artifact is installed into the local repository.
     *
     * @param event the event containing the artifact and source path
     */
    default void artifactInstalling(@Nonnull RepositoryEvent event) {}

    /**
     * Called after an artifact installation attempt finishes.
     *
     * @param event the event containing the artifact, local repository path, and any failure
     */
    default void artifactInstalled(@Nonnull RepositoryEvent event) {}

    /**
     * Called before repository metadata is installed into the local repository.
     *
     * @param event the event containing the metadata and source path
     */
    default void metadataInstalling(@Nonnull RepositoryEvent event) {}

    /**
     * Called after a repository metadata installation attempt finishes.
     *
     * @param event the event containing the metadata, local repository path, and any failure
     */
    default void metadataInstalled(@Nonnull RepositoryEvent event) {}

    /**
     * Called before an artifact is deployed to a remote repository.
     *
     * @param event the event containing the artifact, remote repository, and source path
     */
    default void artifactDeploying(@Nonnull RepositoryEvent event) {}

    /**
     * Called after an artifact deployment attempt finishes.
     *
     * @param event the event containing the artifact, remote repository, and any failure
     */
    default void artifactDeployed(@Nonnull RepositoryEvent event) {}

    /**
     * Called before repository metadata is deployed to a remote repository.
     *
     * @param event the event containing the metadata, remote repository, and source path
     */
    default void metadataDeploying(@Nonnull RepositoryEvent event) {}

    /**
     * Called after a repository metadata deployment attempt finishes.
     *
     * @param event the event containing the metadata, remote repository, and any failure
     */
    default void metadataDeployed(@Nonnull RepositoryEvent event) {}
}
