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

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.RequestTrace;

/**
 * Describes an artifact or metadata operation performed against a repository.
 *
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface RepositoryEvent {

    /**
     * Returns the kind of repository operation represented by this event.
     */
    @Nonnull
    RepositoryEventType getType();

    /**
     * Returns the Maven session associated with the underlying repository system session.
     * Sessions derived from it share the same repository event and listener scope.
     */
    @Nonnull
    Session getSession();

    /**
     * Returns the artifact involved in the event, if any.
     */
    @Nonnull
    Optional<Artifact> getArtifact();

    /**
     * Returns the metadata involved in the event, if any.
     */
    @Nonnull
    Optional<RepositoryMetadata> getMetadata();

    /**
     * Returns the local path involved in the event, if any.
     */
    @Nonnull
    Optional<Path> getPath();

    /**
     * Returns the repository involved in the event, if any.
     */
    @Nonnull
    Optional<Repository> getRepository();

    /**
     * Returns the primary failure associated with the event, if any.
     */
    @Nonnull
    Optional<Exception> getException();

    /**
     * Returns all failures associated with the event.
     */
    @Nonnull
    List<Exception> getExceptions();

    /**
     * Returns the request trace associated with the event, if any.
     */
    @Nonnull
    Optional<RequestTrace> getTrace();
}
