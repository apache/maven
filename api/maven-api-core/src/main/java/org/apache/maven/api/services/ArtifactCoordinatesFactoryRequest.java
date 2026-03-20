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

import java.util.Objects;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A request for creating a {@link ArtifactCoordinates} object.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ArtifactCoordinatesFactoryRequest extends Request<Session> {

    @Nullable
    String getGroupId();

    @Nullable
    String getArtifactId();

    @Nullable
    String getVersion();

    @Nullable
    String getClassifier();

    @Nullable
    String getExtension();

    @Nullable
    String getType();

    @Nullable
    String getCoordinatesString();

    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(
            @Nonnull Session session,
            @Nullable String groupId,
            @Nullable String artifactId,
            @Nullable String version,
            @Nullable String extension) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .extension(extension)
                .build();
    }

    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(
            @Nonnull Session session,
            @Nullable String groupId,
            @Nullable String artifactId,
            @Nullable String version,
            @Nullable String classifier,
            @Nullable String extension,
            @Nullable String type) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .classifier(classifier)
                .extension(extension)
                .type(type)
                .build();
    }

    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(@Nonnull Session session, @Nonnull String coordinateString) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .coordinateString(requireNonNull(coordinateString, "coordinateString"))
                .build();
    }

    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(@Nonnull Session session, @Nonnull ArtifactCoordinates coordinates) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .groupId(requireNonNull(coordinates, "coordinates").getGroupId())
                .artifactId(coordinates.getArtifactId())
                .classifier(coordinates.getClassifier())
                .version(coordinates.getVersionConstraint().toString())
                .extension(coordinates.getExtension())
                .build();
    }

    static ArtifactFactoryRequestBuilder builder() {
        return new ArtifactFactoryRequestBuilder();
    }

    @NotThreadSafe
    class ArtifactFactoryRequestBuilder {
        @Nullable
        private Session session;

        @Nullable
        private RequestTrace trace;

        @Nullable
        private String groupId;

        @Nullable
        private String artifactId;

        @Nullable
        private String version;

        @Nullable
        private String classifier;

        @Nullable
        private String extension;

        @Nullable
        private String type;

        @Nullable
        private String coordinateString;

        ArtifactFactoryRequestBuilder() {}

        public ArtifactFactoryRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public ArtifactFactoryRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        public ArtifactFactoryRequestBuilder groupId(@Nullable String groupId) {
            this.groupId = groupId;
            return this;
        }

        public ArtifactFactoryRequestBuilder artifactId(@Nullable String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public ArtifactFactoryRequestBuilder version(@Nullable String version) {
            this.version = version;
            return this;
        }

        public ArtifactFactoryRequestBuilder classifier(@Nullable String classifier) {
            this.classifier = classifier;
            return this;
        }

        public ArtifactFactoryRequestBuilder extension(@Nullable String extension) {
            this.extension = extension;
            return this;
        }

        public ArtifactFactoryRequestBuilder type(@Nullable String type) {
            this.type = type;
            return this;
        }

        public ArtifactFactoryRequestBuilder coordinateString(@Nullable String coordinateString) {
            this.coordinateString = coordinateString;
            return this;
        }

        public ArtifactCoordinatesFactoryRequest build() {
            return new DefaultArtifactFactoryRequestArtifact(
                    requireNonNull(session, "session cannot be null"),
                    trace,
                    groupId,
                    artifactId,
                    version,
                    classifier,
                    extension,
                    type,
                    coordinateString);
        }

        private static class DefaultArtifactFactoryRequestArtifact extends BaseRequest<Session>
                implements ArtifactCoordinatesFactoryRequest {
            @Nullable
            private final String groupId;

            @Nullable
            private final String artifactId;

            @Nullable
            private final String version;

            @Nullable
            private final String classifier;

            @Nullable
            private final String extension;

            @Nullable
            private final String type;

            @Nullable
            private final String coordinatesString;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultArtifactFactoryRequestArtifact(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nullable String groupId,
                    @Nullable String artifactId,
                    @Nullable String version,
                    @Nullable String classifier,
                    @Nullable String extension,
                    @Nullable String type,
                    @Nullable String coordinatesString) {
                super(session, trace);
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.classifier = classifier;
                this.extension = extension;
                this.type = type;
                this.coordinatesString = coordinatesString;
            }

            @Nullable
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Nullable
            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Nullable
            @Override
            public String getVersion() {
                return version;
            }

            @Nullable
            @Override
            public String getClassifier() {
                return classifier;
            }

            @Nullable
            @Override
            public String getExtension() {
                return extension;
            }

            @Nullable
            @Override
            public String getType() {
                return type;
            }

            @Nullable
            @Override
            public String getCoordinatesString() {
                return coordinatesString;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultArtifactFactoryRequestArtifact that
                        && Objects.equals(groupId, that.groupId)
                        && Objects.equals(artifactId, that.artifactId)
                        && Objects.equals(version, that.version)
                        && Objects.equals(classifier, that.classifier)
                        && Objects.equals(extension, that.extension)
                        && Objects.equals(type, that.type)
                        && Objects.equals(coordinatesString, that.coordinatesString);
            }

            @Override
            public int hashCode() {
                return Objects.hash(groupId, artifactId, version, classifier, extension, type, coordinatesString);
            }

            @Override
            public String toString() {
                return "ArtifactFactoryRequestArtifact[" + "groupId='"
                        + groupId + '\'' + ", artifactId='"
                        + artifactId + '\'' + ", version='"
                        + version + '\'' + ", classifier='"
                        + classifier + '\'' + ", extension='"
                        + extension + '\'' + ", type='"
                        + type + '\'' + ", coordinatesString='"
                        + coordinatesString + '\'' + ']';
            }
        }
    }
}
