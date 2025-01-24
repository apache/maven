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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A request for resolving an artifact.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ArtifactResolverRequest extends Request<Session> {

    @Nonnull
    Collection<? extends ArtifactCoordinates> getCoordinates();

    @Nonnull
    List<RemoteRepository> getRepositories();

    @Nonnull
    static ArtifactResolverRequestBuilder builder() {
        return new ArtifactResolverRequestBuilder();
    }

    @Nonnull
    static ArtifactResolverRequest build(
            @Nonnull Session session, @Nonnull Collection<? extends ArtifactCoordinates> coordinates) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .coordinates(requireNonNull(coordinates, "coordinates cannot be null"))
                .build();
    }

    @Nonnull
    static ArtifactResolverRequest build(
            @Nonnull Session session,
            @Nonnull Collection<? extends ArtifactCoordinates> coordinates,
            List<RemoteRepository> repositories) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .coordinates(requireNonNull(coordinates, "coordinates cannot be null"))
                .repositories(repositories)
                .build();
    }

    @NotThreadSafe
    class ArtifactResolverRequestBuilder {
        Session session;
        RequestTrace trace;
        Collection<? extends ArtifactCoordinates> coordinates;
        List<RemoteRepository> repositories;

        ArtifactResolverRequestBuilder() {}

        @Nonnull
        public ArtifactResolverRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequestBuilder coordinates(Collection<? extends ArtifactCoordinates> coordinates) {
            this.coordinates = coordinates;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        @Nonnull
        public ArtifactResolverRequest build() {
            return new DefaultArtifactResolverRequest(session, trace, coordinates, repositories);
        }

        private static class DefaultArtifactResolverRequest extends BaseRequest<Session>
                implements ArtifactResolverRequest {
            @Nonnull
            private final Collection<? extends ArtifactCoordinates> coordinates;

            @Nullable
            private final List<RemoteRepository> repositories;

            DefaultArtifactResolverRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nonnull Collection<? extends ArtifactCoordinates> coordinates,
                    @Nonnull List<RemoteRepository> repositories) {
                super(session, trace);
                this.coordinates = List.copyOf(requireNonNull(coordinates, "coordinates cannot be null"));
                this.repositories = repositories;
            }

            @Nonnull
            @Override
            public Collection<? extends ArtifactCoordinates> getCoordinates() {
                return coordinates;
            }

            @Nullable
            @Override
            public List<RemoteRepository> getRepositories() {
                return repositories;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultArtifactResolverRequest that
                        && Objects.equals(coordinates, that.coordinates)
                        && Objects.equals(repositories, that.repositories);
            }

            @Override
            public int hashCode() {
                return Objects.hash(coordinates, repositories);
            }

            @Override
            public String toString() {
                return "ArtifactResolverRequest[" + "coordinates="
                        + coordinates + ", repositories="
                        + repositories + ']';
            }
        }
    }
}
