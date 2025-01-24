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
import java.util.Objects;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Profile;

import static java.util.Objects.requireNonNull;

/**
 * Request used to build a {@link org.apache.maven.api.Project} using
 * the {@link ProjectBuilder} service.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ModelBuilderRequest extends Request<Session> {

    /**
     * The possible request types for building a model.
     */
    enum RequestType {
        /**
         * The request is for building an initial model from a POM file in a project on the filesystem.
         */
        BUILD_PROJECT,
        /**
         * The request is for rebuilding the effective POM in a project on the filesystem.
         */
        BUILD_EFFECTIVE,
        /**
         * The request is used specifically to parse the POM used as a basis for creating the consumer POM.
         * This POM will not ungergo any profile activation.
         */
        BUILD_CONSUMER,
        /**
         * The request is for building a model from a parent POM file from a downloaded artifact.
         */
        CONSUMER_PARENT,
        /**
         * The request is for building a model from a dependency POM file from a downloaded artifact.
         */
        CONSUMER_DEPENDENCY
    }

    /**
     * The possible merge modes for combining remote repositories.
     */
    enum RepositoryMerging {

        /**
         * The repositories declared in the POM have precedence over the repositories specified in the request.
         */
        POM_DOMINANT,

        /**
         * The repositories specified in the request have precedence over the repositories declared in the POM.
         */
        REQUEST_DOMINANT,
    }

    @Nonnull
    ModelSource getSource();

    @Nonnull
    RequestType getRequestType();

    boolean isLocationTracking();

    boolean isRecursive();

    /**
     * Defines external profiles that may be activated for the given model.
     * Those are external profiles usually defined in {@link org.apache.maven.api.settings.Settings#getProfiles()}.
     */
    @Nonnull
    Collection<Profile> getProfiles();

    /**
     * List of profile ids that have been explicitly activated by the user.
     */
    @Nonnull
    List<String> getActiveProfileIds();

    /**
     * List of profile ids that have been explicitly deactivated by the user.
     */
    @Nonnull
    List<String> getInactiveProfileIds();

    /**
     * Provides a map of system properties.
     */
    @Nonnull
    Map<String, String> getSystemProperties();

    /**
     * Provides a map of user properties.
     * User properties
     */
    @Nonnull
    Map<String, String> getUserProperties();

    @Nonnull
    RepositoryMerging getRepositoryMerging();

    @Nullable
    List<RemoteRepository> getRepositories();

    @Nullable
    ModelTransformer getLifecycleBindingsInjector();

    @Nonnull
    static ModelBuilderRequest build(@Nonnull ModelBuilderRequest request, @Nonnull ModelSource source) {
        return builder(requireNonNull(request, "request cannot be null"))
                .source(requireNonNull(source, "source cannot be null"))
                .build();
    }

    @Nonnull
    static ModelBuilderRequest build(@Nonnull Session session, @Nonnull ModelSource source) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .source(requireNonNull(source, "source cannot be null"))
                .build();
    }

    @Nonnull
    static ModelBuilderRequest build(@Nonnull Session session, @Nonnull Path path) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .source(Sources.buildSource(path))
                .build();
    }

    @Nonnull
    static ModelBuilderRequestBuilder builder() {
        return new ModelBuilderRequestBuilder();
    }

    @Nonnull
    static ModelBuilderRequestBuilder builder(ModelBuilderRequest request) {
        return new ModelBuilderRequestBuilder(request);
    }

    @NotThreadSafe
    class ModelBuilderRequestBuilder {
        Session session;
        RequestTrace trace;
        RequestType requestType;
        boolean locationTracking;
        boolean recursive;
        ModelSource source;
        Collection<Profile> profiles;
        List<String> activeProfileIds;
        List<String> inactiveProfileIds;
        Map<String, String> systemProperties;
        Map<String, String> userProperties;
        RepositoryMerging repositoryMerging;
        List<RemoteRepository> repositories;
        ModelTransformer lifecycleBindingsInjector;

        ModelBuilderRequestBuilder() {}

        ModelBuilderRequestBuilder(ModelBuilderRequest request) {
            this.session = request.getSession();
            this.trace = request.getTrace();
            this.requestType = request.getRequestType();
            this.locationTracking = request.isLocationTracking();
            this.recursive = request.isRecursive();
            this.source = request.getSource();
            this.profiles = request.getProfiles();
            this.activeProfileIds = request.getActiveProfileIds();
            this.inactiveProfileIds = request.getInactiveProfileIds();
            this.systemProperties = request.getSystemProperties();
            this.userProperties = request.getUserProperties();
            this.repositoryMerging = request.getRepositoryMerging();
            this.repositories = request.getRepositories();
            this.lifecycleBindingsInjector = request.getLifecycleBindingsInjector();
        }

        public ModelBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public ModelBuilderRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        public ModelBuilderRequestBuilder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public ModelBuilderRequestBuilder locationTracking(boolean locationTracking) {
            this.locationTracking = locationTracking;
            return this;
        }

        public ModelBuilderRequestBuilder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public ModelBuilderRequestBuilder source(ModelSource source) {
            this.source = source;
            return this;
        }

        public ModelBuilderRequestBuilder profiles(List<Profile> profiles) {
            this.profiles = profiles;
            return this;
        }

        public ModelBuilderRequestBuilder activeProfileIds(List<String> activeProfileIds) {
            this.activeProfileIds = activeProfileIds;
            return this;
        }

        public ModelBuilderRequestBuilder inactiveProfileIds(List<String> inactiveProfileIds) {
            this.inactiveProfileIds = inactiveProfileIds;
            return this;
        }

        public ModelBuilderRequestBuilder systemProperties(Map<String, String> systemProperties) {
            this.systemProperties = systemProperties;
            return this;
        }

        public ModelBuilderRequestBuilder userProperties(Map<String, String> userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        public ModelBuilderRequestBuilder repositoryMerging(RepositoryMerging repositoryMerging) {
            this.repositoryMerging = repositoryMerging;
            return this;
        }

        public ModelBuilderRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        public ModelBuilderRequestBuilder lifecycleBindingsInjector(ModelTransformer lifecycleBindingsInjector) {
            this.lifecycleBindingsInjector = lifecycleBindingsInjector;
            return this;
        }

        public ModelBuilderRequest build() {
            return new DefaultModelBuilderRequest(
                    session,
                    trace,
                    requestType,
                    locationTracking,
                    recursive,
                    source,
                    profiles,
                    activeProfileIds,
                    inactiveProfileIds,
                    systemProperties,
                    userProperties,
                    repositoryMerging,
                    repositories,
                    lifecycleBindingsInjector);
        }

        private static class DefaultModelBuilderRequest extends BaseRequest<Session> implements ModelBuilderRequest {
            private final RequestType requestType;
            private final boolean locationTracking;
            private final boolean recursive;
            private final ModelSource source;
            private final Collection<Profile> profiles;
            private final List<String> activeProfileIds;
            private final List<String> inactiveProfileIds;
            private final Map<String, String> systemProperties;
            private final Map<String, String> userProperties;
            private final RepositoryMerging repositoryMerging;
            private final List<RemoteRepository> repositories;
            private final ModelTransformer lifecycleBindingsInjector;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultModelBuilderRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nonnull RequestType requestType,
                    boolean locationTracking,
                    boolean recursive,
                    @Nonnull ModelSource source,
                    Collection<Profile> profiles,
                    List<String> activeProfileIds,
                    List<String> inactiveProfileIds,
                    Map<String, String> systemProperties,
                    Map<String, String> userProperties,
                    RepositoryMerging repositoryMerging,
                    List<RemoteRepository> repositories,
                    ModelTransformer lifecycleBindingsInjector) {
                super(session, trace);
                this.requestType = requireNonNull(requestType, "requestType cannot be null");
                this.locationTracking = locationTracking;
                this.recursive = recursive;
                this.source = source;
                this.profiles = profiles != null ? List.copyOf(profiles) : List.of();
                this.activeProfileIds = activeProfileIds != null ? List.copyOf(activeProfileIds) : List.of();
                this.inactiveProfileIds = inactiveProfileIds != null ? List.copyOf(inactiveProfileIds) : List.of();
                this.systemProperties =
                        systemProperties != null ? Map.copyOf(systemProperties) : session.getSystemProperties();
                this.userProperties = userProperties != null ? Map.copyOf(userProperties) : session.getUserProperties();
                this.repositoryMerging = repositoryMerging;
                this.repositories = repositories != null ? List.copyOf(repositories) : null;
                this.lifecycleBindingsInjector = lifecycleBindingsInjector;
            }

            @Override
            public RequestType getRequestType() {
                return requestType;
            }

            @Override
            public boolean isLocationTracking() {
                return locationTracking;
            }

            @Override
            public boolean isRecursive() {
                return recursive;
            }

            @Nonnull
            @Override
            public ModelSource getSource() {
                return source;
            }

            @Override
            public Collection<Profile> getProfiles() {
                return profiles;
            }

            @Override
            public List<String> getActiveProfileIds() {
                return activeProfileIds;
            }

            @Override
            public List<String> getInactiveProfileIds() {
                return inactiveProfileIds;
            }

            @Override
            public Map<String, String> getSystemProperties() {
                return systemProperties;
            }

            @Override
            public Map<String, String> getUserProperties() {
                return userProperties;
            }

            @Override
            public RepositoryMerging getRepositoryMerging() {
                return repositoryMerging;
            }

            @Override
            public List<RemoteRepository> getRepositories() {
                return repositories;
            }

            @Override
            public ModelTransformer getLifecycleBindingsInjector() {
                return lifecycleBindingsInjector;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultModelBuilderRequest that
                        && locationTracking == that.locationTracking
                        && recursive == that.recursive
                        && requestType == that.requestType
                        && Objects.equals(source, that.source)
                        && Objects.equals(profiles, that.profiles)
                        && Objects.equals(activeProfileIds, that.activeProfileIds)
                        && Objects.equals(inactiveProfileIds, that.inactiveProfileIds)
                        && Objects.equals(systemProperties, that.systemProperties)
                        && Objects.equals(userProperties, that.userProperties)
                        && repositoryMerging == that.repositoryMerging
                        && Objects.equals(repositories, that.repositories)
                        && Objects.equals(lifecycleBindingsInjector, that.lifecycleBindingsInjector);
            }

            @Override
            public int hashCode() {
                return Objects.hash(
                        requestType,
                        locationTracking,
                        recursive,
                        source,
                        profiles,
                        activeProfileIds,
                        inactiveProfileIds,
                        systemProperties,
                        userProperties,
                        repositoryMerging,
                        repositories,
                        lifecycleBindingsInjector);
            }

            @Override
            public String toString() {
                return "ModelBuilderRequest[" + "requestType="
                        + requestType + ", locationTracking="
                        + locationTracking + ", recursive="
                        + recursive + ", source="
                        + source + ", profiles="
                        + profiles + ", activeProfileIds="
                        + activeProfileIds + ", inactiveProfileIds="
                        + inactiveProfileIds + ", systemProperties="
                        + systemProperties + ", userProperties="
                        + userProperties + ", repositoryMerging="
                        + repositoryMerging + ", repositories="
                        + repositories + ", lifecycleBindingsInjector="
                        + lifecycleBindingsInjector + ']';
            }
        }
    }
}
