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

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Profile;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * Request used to build a {@link org.apache.maven.api.Project} using
 * the {@link ProjectBuilder} service.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ModelBuilderRequest {

    /**
     * The possible request types for building a model.
     */
    enum RequestType {
        /**
         * The request is for building a model from a POM file in a project on the filesystem.
         */
        BUILD_POM,
        /**
         * The request is for building a model from a parent POM file from a downloaded artifact.
         */
        PARENT_POM,
        /**
         * The request is for building a model from a dependency POM file from a downloaded artifact.
         */
        DEPENDENCY
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
    Session getSession();

    @Nonnull
    ModelSource getSource();

    @Nonnull
    RequestType getRequestType();

    boolean isTwoPhaseBuilding();

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
    Object getListener();

    @Nullable
    ModelBuilderResult getInterimResult();

    @Nullable
    List<RemoteRepository> getRepositories();

    @Nonnull
    static ModelBuilderRequest build(@Nonnull ModelBuilderRequest request, @Nonnull ModelSource source) {
        return builder(nonNull(request, "request cannot be null"))
                .source(nonNull(source, "source cannot be null"))
                .build();
    }

    @Nonnull
    static ModelBuilderRequest build(@Nonnull Session session, @Nonnull ModelSource source) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .source(nonNull(source, "source cannot be null"))
                .build();
    }

    @Nonnull
    static ModelBuilderRequest build(@Nonnull Session session, @Nonnull Path path) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .source(ModelSource.fromPath(path))
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
        RequestType requestType;
        boolean locationTracking;
        boolean twoPhaseBuilding;
        boolean recursive;
        ModelSource source;
        Collection<Profile> profiles;
        List<String> activeProfileIds;
        List<String> inactiveProfileIds;
        Map<String, String> systemProperties;
        Map<String, String> userProperties;
        RepositoryMerging repositoryMerging;
        Object listener;
        ModelBuilderResult interimResult;
        List<RemoteRepository> repositories;

        ModelBuilderRequestBuilder() {}

        ModelBuilderRequestBuilder(ModelBuilderRequest request) {
            this.session = request.getSession();
            this.requestType = request.getRequestType();
            this.locationTracking = request.isLocationTracking();
            this.twoPhaseBuilding = request.isTwoPhaseBuilding();
            this.recursive = request.isRecursive();
            this.source = request.getSource();
            this.profiles = request.getProfiles();
            this.activeProfileIds = request.getActiveProfileIds();
            this.inactiveProfileIds = request.getInactiveProfileIds();
            this.systemProperties = request.getSystemProperties();
            this.userProperties = request.getUserProperties();
            this.repositoryMerging = request.getRepositoryMerging();
            this.listener = request.getListener();
            this.interimResult = request.getInterimResult();
            this.repositories = request.getRepositories();
        }

        public ModelBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public ModelBuilderRequestBuilder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public ModelBuilderRequestBuilder twoPhaseBuilding(boolean twoPhaseBuilding) {
            this.twoPhaseBuilding = twoPhaseBuilding;
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

        public ModelBuilderRequestBuilder listener(Object listener) {
            this.listener = listener;
            return this;
        }

        public ModelBuilderRequestBuilder interimResult(ModelBuilderResult interimResult) {
            this.interimResult = interimResult;
            return this;
        }

        public ModelBuilderRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        public ModelBuilderRequest build() {
            return new DefaultModelBuilderRequest(
                    session,
                    requestType,
                    locationTracking,
                    twoPhaseBuilding,
                    recursive,
                    source,
                    profiles,
                    activeProfileIds,
                    inactiveProfileIds,
                    systemProperties,
                    userProperties,
                    repositoryMerging,
                    listener,
                    interimResult,
                    repositories);
        }

        private static class DefaultModelBuilderRequest extends BaseRequest implements ModelBuilderRequest {
            private final RequestType requestType;
            private final boolean locationTracking;
            private final boolean twoPhaseBuilding;
            private final boolean recursive;
            private final ModelSource source;
            private final Collection<Profile> profiles;
            private final List<String> activeProfileIds;
            private final List<String> inactiveProfileIds;
            private final Map<String, String> systemProperties;
            private final Map<String, String> userProperties;
            private final RepositoryMerging repositoryMerging;
            private final Object listener;
            private final ModelBuilderResult interimResult;
            private final List<RemoteRepository> repositories;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultModelBuilderRequest(
                    @Nonnull Session session,
                    @Nonnull RequestType requestType,
                    boolean locationTracking,
                    boolean twoPhaseBuilding,
                    boolean recursive,
                    @Nonnull ModelSource source,
                    Collection<Profile> profiles,
                    List<String> activeProfileIds,
                    List<String> inactiveProfileIds,
                    Map<String, String> systemProperties,
                    Map<String, String> userProperties,
                    RepositoryMerging repositoryMerging,
                    Object listener,
                    ModelBuilderResult interimResult,
                    List<RemoteRepository> repositories) {
                super(session);
                this.requestType = nonNull(requestType, "requestType cannot be null");
                this.locationTracking = locationTracking;
                this.twoPhaseBuilding = twoPhaseBuilding;
                this.recursive = recursive;
                this.source = source;
                this.profiles = profiles != null ? List.copyOf(profiles) : List.of();
                this.activeProfileIds = activeProfileIds != null ? List.copyOf(activeProfileIds) : List.of();
                this.inactiveProfileIds = inactiveProfileIds != null ? List.copyOf(inactiveProfileIds) : List.of();
                this.systemProperties =
                        systemProperties != null ? Map.copyOf(systemProperties) : session.getSystemProperties();
                this.userProperties = userProperties != null ? Map.copyOf(userProperties) : session.getUserProperties();
                this.repositoryMerging = repositoryMerging;
                this.listener = listener;
                this.interimResult = interimResult;
                this.repositories = repositories != null ? List.copyOf(repositories) : null;
            }

            @Override
            public RequestType getRequestType() {
                return requestType;
            }

            @Override
            public boolean isTwoPhaseBuilding() {
                return twoPhaseBuilding;
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
            public Object getListener() {
                return listener;
            }

            @Override
            public ModelBuilderResult getInterimResult() {
                return interimResult;
            }

            @Override
            public List<RemoteRepository> getRepositories() {
                return repositories;
            }
        }
    }
}
