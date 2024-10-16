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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyCoordinatesFactoryRequest extends ArtifactCoordinatesFactoryRequest {

    String getScope();

    boolean isOptional();

    @Nonnull
    Collection<Exclusion> getExclusions();

    @Nonnull
    static DependencyCoordinatesFactoryRequest build(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type) {
        return DependencyCoordinatesFactoryRequest.builder()
                .session(nonNull(session, "session cannot be null"))
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .classifier(classifier)
                .extension(extension)
                .type(type)
                .build();
    }

    @Nonnull
    static DependencyCoordinatesFactoryRequest build(
            @Nonnull Session session, @Nonnull ArtifactCoordinates coordinates) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .groupId(nonNull(coordinates, "coordinates cannot be null").getGroupId())
                .artifactId(coordinates.getArtifactId())
                .version(coordinates.getVersionConstraint().asString())
                .classifier(coordinates.getClassifier())
                .extension(coordinates.getExtension())
                .build();
    }

    @Nonnull
    static DependencyCoordinatesFactoryRequest build(@Nonnull Session session, @Nonnull Dependency dependency) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .groupId(nonNull(dependency, "dependency").getGroupId())
                .artifactId(dependency.getArtifactId())
                .version(dependency.getVersion().asString())
                .classifier(dependency.getClassifier())
                .extension(dependency.getExtension())
                .type(dependency.getType().id())
                .scope(dependency.getScope().id())
                .optional(dependency.isOptional())
                .build();
    }

    @Nonnull
    static DependencyCoordinatesFactoryRequestBuilder builder() {
        return new DependencyCoordinatesFactoryRequestBuilder();
    }

    @NotThreadSafe
    class DependencyCoordinatesFactoryRequestBuilder {
        private Session session;
        private String groupId;
        private String artifactId;
        private String version;
        private String classifier;
        private String extension;
        private String type;
        private String coordinateString;
        private String scope;
        private boolean optional;
        private Collection<Exclusion> exclusions = Collections.emptyList();

        DependencyCoordinatesFactoryRequestBuilder() {}

        public DependencyCoordinatesFactoryRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder version(String version) {
            this.version = version;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder classifier(String classifier) {
            this.classifier = classifier;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder extension(String extension) {
            this.extension = extension;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder type(String type) {
            this.type = type;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder coordinateString(String coordinateString) {
            this.coordinateString = coordinateString;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder exclusions(Collection<Exclusion> exclusions) {
            if (exclusions != null) {
                if (this.exclusions.isEmpty()) {
                    this.exclusions = new ArrayList<>();
                }
                this.exclusions.addAll(exclusions);
            }
            return this;
        }

        public DependencyCoordinatesFactoryRequestBuilder exclusion(Exclusion exclusion) {
            if (exclusion != null) {
                if (this.exclusions.isEmpty()) {
                    this.exclusions = new ArrayList<>();
                }
                this.exclusions.add(exclusion);
            }
            return this;
        }

        public DependencyCoordinatesFactoryRequest build() {
            return new DefaultDependencyCoordinatesFactoryRequest(
                    session,
                    groupId,
                    artifactId,
                    version,
                    classifier,
                    extension,
                    type,
                    coordinateString,
                    scope,
                    optional,
                    exclusions);
        }

        private static class DefaultDependencyCoordinatesFactoryRequest extends BaseRequest
                implements DependencyCoordinatesFactoryRequest {
            private final String groupId;
            private final String artifactId;
            private final String version;
            private final String classifier;
            private final String extension;
            private final String type;
            private final String coordinateString;
            private final String scope;
            private final boolean optional;
            private final Collection<Exclusion> exclusions;

            @SuppressWarnings("checkstyle:ParameterNumber")
            private DefaultDependencyCoordinatesFactoryRequest(
                    @Nonnull Session session,
                    String groupId,
                    String artifactId,
                    String version,
                    String classifier,
                    String extension,
                    String type,
                    String coordinateString,
                    String scope,
                    boolean optional,
                    Collection<Exclusion> exclusions) {
                super(session);
                this.groupId = groupId;
                this.artifactId = artifactId;
                this.version = version;
                this.classifier = classifier;
                this.extension = extension;
                this.type = type;
                this.coordinateString = coordinateString;
                this.scope = scope;
                this.optional = optional;
                this.exclusions = exclusions;
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

            public String getCoordinatesString() {
                return coordinateString;
            }

            @Override
            public String getScope() {
                return scope;
            }

            @Override
            public boolean isOptional() {
                return optional;
            }

            @Nonnull
            @Override
            public Collection<Exclusion> getExclusions() {
                return exclusions;
            }
        }
    }
}
