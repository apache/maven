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

    default void artifactDescriptorInvalid(@Nonnull RepositoryEvent event) {}

    default void artifactDescriptorMissing(@Nonnull RepositoryEvent event) {}

    default void metadataInvalid(@Nonnull RepositoryEvent event) {}

    default void artifactResolving(@Nonnull RepositoryEvent event) {}

    default void artifactResolved(@Nonnull RepositoryEvent event) {}

    default void metadataResolving(@Nonnull RepositoryEvent event) {}

    default void metadataResolved(@Nonnull RepositoryEvent event) {}

    default void artifactDownloading(@Nonnull RepositoryEvent event) {}

    default void artifactDownloaded(@Nonnull RepositoryEvent event) {}

    default void metadataDownloading(@Nonnull RepositoryEvent event) {}

    default void metadataDownloaded(@Nonnull RepositoryEvent event) {}

    default void artifactInstalling(@Nonnull RepositoryEvent event) {}

    default void artifactInstalled(@Nonnull RepositoryEvent event) {}

    default void metadataInstalling(@Nonnull RepositoryEvent event) {}

    default void metadataInstalled(@Nonnull RepositoryEvent event) {}

    default void artifactDeploying(@Nonnull RepositoryEvent event) {}

    default void artifactDeployed(@Nonnull RepositoryEvent event) {}

    default void metadataDeploying(@Nonnull RepositoryEvent event) {}

    default void metadataDeployed(@Nonnull RepositoryEvent event) {}
}
