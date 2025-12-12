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

import java.util.List;
import java.util.Objects;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A request to resolve a version range to a list of matching versions.
 * This request is used by {@link VersionRangeResolver} to expand version ranges
 * (e.g., "[3.8,4.0)") into concrete versions available in the configured repositories.
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionRangeResolverRequest extends RepositoryAwareRequest {

    /**
     * Specifies which type of repositories to query when resolving version ranges.
     * This controls whether to search in release repositories, snapshot repositories, or both.
     *
     * @since 4.0.0
     */
    enum Nature {
        /**
         * Query only release repositories to discover versions.
         */
        RELEASE,
        /**
         * Query only snapshot repositories to discover versions.
         */
        SNAPSHOT,
        /**
         * Query both release and snapshot repositories to discover versions.
         * This is the default behavior.
         */
        RELEASE_OR_SNAPSHOT
    }

    /**
     * Gets the artifact coordinates whose version range should be resolved.
     * The coordinates may contain a version range (e.g., "[1.0,2.0)") or a single version.
     *
     * @return the artifact coordinates, never {@code null}
     */
    @Nonnull
    ArtifactCoordinates getArtifactCoordinates();

    /**
     * Gets the nature of repositories to query when resolving the version range.
     * This determines whether to search in release repositories, snapshot repositories, or both.
     *
     * @return the repository nature, never {@code null}
     */
    @Nonnull
    Nature getNature();

    /**
     * Creates a version range resolver request using the session's repositories.
     *
     * @param session the session to use, must not be {@code null}
     * @param artifactCoordinates the artifact coordinates whose version range should be resolved, must not be {@code null}
     * @return the version range resolver request, never {@code null}
     */
    @Nonnull
    static VersionRangeResolverRequest build(
            @Nonnull Session session, @Nonnull ArtifactCoordinates artifactCoordinates) {
        return build(session, artifactCoordinates, null, null);
    }

    /**
     * Creates a version range resolver request.
     *
     * @param session the session to use, must not be {@code null}
     * @param artifactCoordinates the artifact coordinates whose version range should be resolved, must not be {@code null}
     * @param repositories the repositories to use, or {@code null} to use the session's repositories
     * @return the version range resolver request, never {@code null}
     */
    @Nonnull
    static VersionRangeResolverRequest build(
            @Nonnull Session session,
            @Nonnull ArtifactCoordinates artifactCoordinates,
            @Nullable List<RemoteRepository> repositories) {
        return build(session, artifactCoordinates, repositories, null);
    }

    /**
     * Creates a version range resolver request.
     *
     * @param session the session to use, must not be {@code null}
     * @param artifactCoordinates the artifact coordinates whose version range should be resolved, must not be {@code null}
     * @param repositories the repositories to use, or {@code null} to use the session's repositories
     * @param nature the nature of repositories to query when resolving the version range, or {@code null} to use the default
     * @return the version range resolver request, never {@code null}
     */
    @Nonnull
    static VersionRangeResolverRequest build(
            @Nonnull Session session,
            @Nonnull ArtifactCoordinates artifactCoordinates,
            @Nullable List<RemoteRepository> repositories,
            @Nullable Nature nature) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .artifactCoordinates(requireNonNull(artifactCoordinates, "artifactCoordinates cannot be null"))
                .repositories(repositories)
                .nature(nature)
                .build();
    }

    /**
     * Creates a new builder for version range resolver requests.
     *
     * @return a new builder, never {@code null}
     */
    @Nonnull
    static VersionResolverRequestBuilder builder() {
        return new VersionResolverRequestBuilder();
    }

    /**
     * Builder for {@link VersionRangeResolverRequest}.
     */
    @NotThreadSafe
    class VersionResolverRequestBuilder {
        Session session;
        RequestTrace trace;
        ArtifactCoordinates artifactCoordinates;
        List<RemoteRepository> repositories;
        Nature nature = Nature.RELEASE_OR_SNAPSHOT;

        /**
         * Sets the session to use for the request.
         *
         * @param session the session, must not be {@code null}
         * @return this builder, never {@code null}
         */
        public VersionResolverRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        /**
         * Sets the request trace for debugging and diagnostics.
         *
         * @param trace the request trace, may be {@code null}
         * @return this builder, never {@code null}
         */
        public VersionResolverRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        /**
         * Sets the artifact coordinates whose version range should be resolved.
         *
         * @param artifactCoordinates the artifact coordinates, must not be {@code null}
         * @return this builder, never {@code null}
         */
        public VersionResolverRequestBuilder artifactCoordinates(ArtifactCoordinates artifactCoordinates) {
            this.artifactCoordinates = artifactCoordinates;
            return this;
        }

        /**
         * Sets the nature of repositories to query when resolving the version range.
         * If {@code null} is provided, defaults to {@link Nature#RELEASE_OR_SNAPSHOT}.
         *
         * @param nature the repository nature, or {@code null} to use the default
         * @return this builder, never {@code null}
         */
        public VersionResolverRequestBuilder nature(Nature nature) {
            this.nature = Objects.requireNonNullElse(nature, Nature.RELEASE_OR_SNAPSHOT);
            return this;
        }

        /**
         * Sets the repositories to use for resolving the version range.
         *
         * @param repositories the repositories, or {@code null} to use the session's repositories
         * @return this builder, never {@code null}
         */
        public VersionResolverRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        /**
         * Builds the version range resolver request.
         *
         * @return the version range resolver request, never {@code null}
         */
        public VersionRangeResolverRequest build() {
            return new DefaultVersionResolverRequest(session, trace, artifactCoordinates, repositories, nature);
        }

        private static class DefaultVersionResolverRequest extends BaseRequest<Session>
                implements VersionRangeResolverRequest {
            private final ArtifactCoordinates artifactCoordinates;
            private final List<RemoteRepository> repositories;
            private final Nature nature;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultVersionResolverRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nonnull ArtifactCoordinates artifactCoordinates,
                    @Nullable List<RemoteRepository> repositories,
                    @Nonnull Nature nature) {
                super(session, trace);
                this.artifactCoordinates = requireNonNull(artifactCoordinates);
                this.repositories = validate(repositories);
                this.nature = requireNonNull(nature);
            }

            @Nonnull
            @Override
            public ArtifactCoordinates getArtifactCoordinates() {
                return artifactCoordinates;
            }

            @Nullable
            @Override
            public List<RemoteRepository> getRepositories() {
                return repositories;
            }

            @Nonnull
            @Override
            public Nature getNature() {
                return nature;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultVersionResolverRequest that
                        && Objects.equals(artifactCoordinates, that.artifactCoordinates)
                        && Objects.equals(repositories, that.repositories)
                        && nature == that.nature;
            }

            @Override
            public int hashCode() {
                return Objects.hash(artifactCoordinates, repositories, nature);
            }

            @Override
            public String toString() {
                return "VersionResolverRequest[" + "artifactCoordinates="
                        + artifactCoordinates + ", repositories="
                        + repositories + ", nature="
                        + nature + ']';
            }
        }
    }
}
