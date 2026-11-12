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
 * @deprecated Use {@link ArtifactCoordinatesFactory#create(Session, String, String, String, String, String, String)}
 *             directly.
 */
@Deprecated
@Experimental
@Immutable
public interface ArtifactCoordinatesFactoryRequest extends Request<Session> {

    /** @return the group identifier */
    String getGroupId();

    /** @return the artifact identifier */
    String getArtifactId();

    /** @return the version */
    String getVersion();

    /** @return the classifier, may be {@code null} */
    String getClassifier();

    /** @return the file extension, may be {@code null} */
    String getExtension();

    /** @return the artifact type, may be {@code null} */
    String getType();

    /** @return the coordinate string in the format {@code groupId:artifactId[:extension[:classifier]]:version}, may be {@code null} */
    String getCoordinatesString();

    /**
     * Builds a request from individual coordinate components.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param extension the file extension
     * @return a new request, never {@code null}
     * @deprecated Use {@link ArtifactCoordinatesFactory#create(Session, String, String, String, String)} directly.
     */
    @Deprecated
    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(
            @Nonnull Session session, String groupId, String artifactId, String version, String extension) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .extension(extension)
                .build();
    }

    /**
     * Builds a request from individual coordinate components including classifier and type.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param classifier the classifier
     * @param extension the file extension
     * @param type the artifact type
     * @return a new request, never {@code null}
     * @deprecated Use {@link ArtifactCoordinatesFactory#create(Session, String, String, String, String, String, String)}
     *             directly.
     */
    @Deprecated
    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type) {
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

    /**
     * Builds a request from a coordinates string.
     *
     * @param session the session
     * @param coordinateString the coordinate string
     * @return a new request, never {@code null}
     * @deprecated Use {@link ArtifactCoordinatesFactory#create(Session, String)} directly.
     */
    @Deprecated
    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(@Nonnull Session session, @Nonnull String coordinateString) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .coordinatesString(requireNonNull(coordinateString, "coordinateString"))
                .build();
    }

    /**
     * Builds a request from existing artifact coordinates.
     *
     * @param session the session
     * @param coordinates the existing artifact coordinates
     * @return a new request, never {@code null}
     * @deprecated Use {@link ArtifactCoordinatesFactory#create(Session, org.apache.maven.api.Artifact)} directly.
     */
    @Deprecated
    @Nonnull
    static ArtifactCoordinatesFactoryRequest build(@Nonnull Session session, @Nonnull ArtifactCoordinates coordinates) {
        return ArtifactCoordinatesFactoryRequest.builder()
                .session(requireNonNull(session, "session"))
                .groupId(requireNonNull(coordinates, "coordinates").groupId())
                .artifactId(coordinates.artifactId())
                .version(coordinates.versionConstraint().toString())
                .classifier(coordinates.classifier())
                .extension(coordinates.extension())
                .build();
    }

    /** @return a new builder instance */
    @Deprecated
    static ArtifactCoordinatesFactoryRequestBuilder builder() {
        return new ArtifactCoordinatesFactoryRequestBuilder();
    }

    @Deprecated
    @NotThreadSafe
    class ArtifactCoordinatesFactoryRequestBuilder {
        private Session session;
        private RequestTrace trace;
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;
        private String extension;
        private String type;
        private String coordinatesString;

        ArtifactCoordinatesFactoryRequestBuilder() {}

        public ArtifactCoordinatesFactoryRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder version(String version) {
            this.version = version;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder type(String type) {
            this.type = type;
            return this;
        }

        public ArtifactCoordinatesFactoryRequestBuilder coordinatesString(String coordinatesString) {
            this.coordinatesString = coordinatesString;
            return this;
        }

        public ArtifactCoordinatesFactoryRequest build() {
            return new DefaultArtifactCoordinatesFactoryRequest(
                    session, trace, groupId, artifactId, version, classifier, extension, type, coordinatesString);
        }

        private static class DefaultArtifactCoordinatesFactoryRequest extends BaseRequest<Session>
                implements ArtifactCoordinatesFactoryRequest {
            private final String groupId;
            private final String artifactId;
            private final String version;
            private final String classifier;
            private final String extension;
            private final String type;
            private final String coordinatesString;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultArtifactCoordinatesFactoryRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    String groupId,
                    String artifactId,
                    String version,
                    String classifier,
                    String extension,
                    String type,
                    String coordinatesString) {
                super(session, trace);
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.classifier = classifier;
                this.extension = extension;
                this.type = type;
                this.coordinatesString = coordinatesString;
            }

            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public String getArtifactId() {
                return artifactId;
            }

            @Override
            public String getVersion() {
                return version;
            }

            @Override
            public String getClassifier() {
                return classifier;
            }

            @Override
            public String getExtension() {
                return extension;
            }

            @Override
            public String getType() {
                return type;
            }

            @Override
            public String getCoordinatesString() {
                return coordinatesString;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultArtifactCoordinatesFactoryRequest that
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
                return "ArtifactCoordinatesFactoryRequest[" + "groupId='"
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
