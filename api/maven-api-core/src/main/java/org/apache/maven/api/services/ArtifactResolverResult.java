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
package org.apache.maven.api.services;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Repository;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Represents the result of resolving an artifact.
 * <p>
 * This interface provides access to resolved artifacts, their associated paths, and any related exceptions that
 * occurred during the resolution process.
 * </p>
 *
 * @since 4.0.0
 */
@Experimental
public interface ArtifactResolverResult extends Result<ArtifactResolverRequest> {

    /**
     * Returns a collection of resolved artifacts.
     *
     * @return A collection of {@link DownloadedArtifact} instances representing the resolved artifacts.
     */
    @Nonnull
    Collection<DownloadedArtifact> getArtifacts();

    /**
     * Retrieves the file system path associated with a specific artifact.
     *
     * @param artifact The {@link Artifact} whose path is to be retrieved.
     * @return The {@link Path} to the artifact, or {@code null} if unavailable.
     */
    @Nullable
    Path getPath(@Nonnull Artifact artifact);

    /**
     * Returns a mapping of artifact coordinates to their corresponding resolution results.
     *
     * @return A {@link Map} where keys are {@link ArtifactCoordinates} and values are {@link ResultItem} instances.
     */
    @Nonnull
    Map<? extends ArtifactCoordinates, ResultItem> getResults();

    /**
     * Retrieves the resolution result for a specific set of artifact coordinates.
     *
     * @param coordinates The {@link ArtifactCoordinates} identifying the artifact.
     * @return The corresponding {@link ResultItem}, or {@code null} if no result exists.
     */
    default ResultItem getResult(ArtifactCoordinates coordinates) {
        return getResults().get(coordinates);
    }

    /**
     * Represents an individual resolution result for an artifact.
     */
    interface ResultItem {

        /**
         * Returns the coordinates of the resolved artifact.
         *
         * @return The {@link ArtifactCoordinates} of the artifact.
         */
        @Nonnull
        ArtifactCoordinates getCoordinates();

        /**
         * Returns the resolved artifact.
         *
         * @return The {@link DownloadedArtifact} instance.
         */
        @Nonnull
        DownloadedArtifact getArtifact();

        /**
         * Returns a mapping of repositories to the exceptions encountered while resolving the artifact.
         *
         * @return A {@link Map} where keys are {@link Repository} instances and values are {@link Exception} instances.
         */
        @Nonnull
        Map<Repository, List<Exception>> getExceptions();

        /**
         * Returns the repository from which the artifact was resolved.
         *
         * @return The {@link Repository} instance.
         */
        @Nullable
        Repository getRepository();

        /**
         * Returns the file system path to the resolved artifact.
         *
         * @return The {@link Path} to the artifact.
         */
        @Nonnull
        Path getPath();

        /**
         * Indicates whether the requested artifact was resolved. Note that the artifact might have been successfully
         * resolved despite {@link #getExceptions()} indicating transfer errors while trying to fetch the artifact from some
         * of the specified remote repositories.
         *
         * @return {@code true} if the artifact was resolved, {@code false} otherwise.
         */
        boolean isResolved();

        /**
         * Indicates whether the requested artifact is not present in any of the specified repositories.
         *
         * @return {@code true} if the artifact is not present in any repository, {@code false} otherwise.
         */
        boolean isMissing();
    }
}
