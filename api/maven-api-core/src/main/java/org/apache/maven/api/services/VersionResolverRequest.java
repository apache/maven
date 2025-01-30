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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionResolverRequest extends Request<Session> {

    @Nonnull
    ArtifactCoordinates getArtifactCoordinates();

    @Nullable
    List<RemoteRepository> getRepositories();

    @Nonnull
    static VersionResolverRequest build(@Nonnull Session session, @Nonnull ArtifactCoordinates artifactCoordinates) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .artifactCoordinates(requireNonNull(artifactCoordinates, "artifactCoordinates cannot be null"))
                .build();
    }

    @Nonnull
    static VersionResolverRequest build(
            @Nonnull Session session,
            @Nonnull ArtifactCoordinates artifactCoordinates,
            @Nullable List<RemoteRepository> repositories) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .artifactCoordinates(requireNonNull(artifactCoordinates, "artifactCoordinates cannot be null"))
                .repositories(repositories)
                .build();
    }

    @Nonnull
    static VersionResolverRequestBuilder builder() {
        return new VersionResolverRequestBuilder();
    }

    @NotThreadSafe
    class VersionResolverRequestBuilder {
        Session session;
        RequestTrace trace;
        ArtifactCoordinates artifactCoordinates;
        List<RemoteRepository> repositories;

        public VersionResolverRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public VersionResolverRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        public VersionResolverRequestBuilder artifactCoordinates(ArtifactCoordinates artifactCoordinates) {
            this.artifactCoordinates = artifactCoordinates;
            return this;
        }

        public VersionResolverRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        public VersionResolverRequest build() {
            return new DefaultVersionResolverRequest(session, trace, artifactCoordinates, repositories);
        }

        private static class DefaultVersionResolverRequest extends BaseRequest<Session>
                implements VersionResolverRequest {
            private final ArtifactCoordinates artifactCoordinates;
            private final List<RemoteRepository> repositories;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultVersionResolverRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nonnull ArtifactCoordinates artifactCoordinates,
                    @Nullable List<RemoteRepository> repositories) {
                super(session, trace);
                this.artifactCoordinates = artifactCoordinates;
                this.repositories = repositories;
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

            @Override
            public String toString() {
                return "VersionResolverRequest[" + "artifactCoordinates="
                        + artifactCoordinates + ", repositories="
                        + repositories + ']';
            }
        }
    }
}
