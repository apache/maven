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

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;

import static org.apache.maven.api.services.BaseRequest.nonNull;

@Experimental
public interface VersionResolverRequest {

    @Nonnull
    Session getSession();

    @Nonnull
    ArtifactCoordinate getArtifactCoordinate();

    @Nonnull
    static VersionResolverRequest build(@Nonnull Session session, @Nonnull ArtifactCoordinate artifactCoordinate) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .artifactCoordinate(nonNull(artifactCoordinate, "artifactCoordinate cannot be null"))
                .build();
    }

    @Nonnull
    static VersionResolverRequestBuilder builder() {
        return new VersionResolverRequestBuilder();
    }

    @NotThreadSafe
    class VersionResolverRequestBuilder {
        Session session;
        ArtifactCoordinate artifactCoordinate;

        public VersionResolverRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public VersionResolverRequestBuilder artifactCoordinate(ArtifactCoordinate artifactCoordinate) {
            this.artifactCoordinate = artifactCoordinate;
            return this;
        }

        public VersionResolverRequest build() {
            return new DefaultVersionResolverRequest(session, artifactCoordinate);
        }

        private static class DefaultVersionResolverRequest extends BaseRequest implements VersionResolverRequest {
            private final ArtifactCoordinate artifactCoordinate;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultVersionResolverRequest(@Nonnull Session session, @Nonnull ArtifactCoordinate artifactCoordinate) {
                super(session);
                this.artifactCoordinate = artifactCoordinate;
            }

            @Nonnull
            @Override
            public ArtifactCoordinate getArtifactCoordinate() {
                return artifactCoordinate;
            }
        }
    }
}
