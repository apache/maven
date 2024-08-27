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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionRangeResolverRequest {

    @Nonnull
    Session getSession();

    @Nonnull
    ArtifactCoordinates getArtifactCoordinates();

    @Nonnull
    static VersionRangeResolverRequest build(
            @Nonnull Session session, @Nonnull ArtifactCoordinates artifactCoordinates) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .artifactCoordinates(nonNull(artifactCoordinates, "artifactCoordinates cannot be null"))
                .build();
    }

    @Nonnull
    static VersionResolverRequestBuilder builder() {
        return new VersionResolverRequestBuilder();
    }

    @NotThreadSafe
    class VersionResolverRequestBuilder {
        Session session;
        ArtifactCoordinates artifactCoordinates;

        public VersionResolverRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public VersionResolverRequestBuilder artifactCoordinates(ArtifactCoordinates artifactCoordinates) {
            this.artifactCoordinates = artifactCoordinates;
            return this;
        }

        public VersionRangeResolverRequest build() {
            return new DefaultVersionResolverRequest(session, artifactCoordinates);
        }

        private static class DefaultVersionResolverRequest extends BaseRequest implements VersionRangeResolverRequest {
            private final ArtifactCoordinates artifactCoordinates;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultVersionResolverRequest(@Nonnull Session session, @Nonnull ArtifactCoordinates artifactCoordinates) {
                super(session);
                this.artifactCoordinates = artifactCoordinates;
            }

            @Nonnull
            @Override
            public ArtifactCoordinates getArtifactCoordinates() {
                return artifactCoordinates;
            }
        }
    }
}
