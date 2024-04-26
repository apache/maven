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
 * TODO: add validationLevel, activeProfileIds, inactiveProfileIds, resolveDependencies
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ModelBuilderRequest {

    /**
     * Denotes minimal validation of POMs. This validation level is meant for processing of POMs from repositories
     * during metadata retrieval.
     */
    int VALIDATION_LEVEL_MINIMAL = 0;

    /**
     * Denotes validation as performed by Maven 2.0. This validation level is meant as a compatibility mode to allow
     * users to migrate their projects.
     */
    int VALIDATION_LEVEL_MAVEN_2_0 = 20;

    /**
     * Denotes validation as performed by Maven 3.0. This validation level is meant for existing projects.
     */
    int VALIDATION_LEVEL_MAVEN_3_0 = 30;

    /**
     * Denotes validation as performed by Maven 3.1. This validation level is meant for existing projects.
     */
    int VALIDATION_LEVEL_MAVEN_3_1 = 31;

    /**
     * Denotes validation as performed by Maven 4.0. This validation level is meant for new projects.
     */
    int VALIDATION_LEVEL_MAVEN_4_0 = 40;

    /**
     * Denotes strict validation as recommended by the current Maven version.
     */
    int VALIDATION_LEVEL_STRICT = VALIDATION_LEVEL_MAVEN_4_0;

    @Nonnull
    Session getSession();

    @Nonnull
    ModelSource getSource();

    int getValidationLevel();

    boolean isTwoPhaseBuilding();

    boolean isLocationTracking();

    /**
     * Indicates if the model to be built is a local project or a dependency.
     * In case the project is loaded externally from a remote repository (as a dependency or
     * even as an external parent), the POM will be parsed in a lenient way.  Local POMs
     * are parsed more strictly.
     */
    boolean isProjectBuild();

    /**
     * Specifies whether plugin processing should take place for the built model.
     * This involves merging plugins specified by the {@link org.apache.maven.api.Packaging},
     * configuration expansion (merging configuration defined globally for a given plugin
     * using {@link org.apache.maven.api.model.Plugin#getConfiguration()}
     * into the configuration for each {@link org.apache.maven.api.model.PluginExecution}.
     */
    boolean isProcessPlugins();

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

    ModelResolver getModelResolver();

    @Nullable
    Object getListener();

    @Nullable
    ModelBuilderResult getInterimResult();

    @Nullable
    ModelTransformerContextBuilder getTransformerContextBuilder();

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
        int validationLevel;
        boolean locationTracking;
        boolean twoPhaseBuilding;
        ModelSource source;
        boolean projectBuild;
        boolean processPlugins = true;
        Collection<Profile> profiles;
        List<String> activeProfileIds;
        List<String> inactiveProfileIds;
        Map<String, String> systemProperties;
        Map<String, String> userProperties;
        ModelResolver modelResolver;
        Object listener;
        ModelBuilderResult interimResult;
        ModelTransformerContextBuilder transformerContextBuilder;

        ModelBuilderRequestBuilder() {}

        ModelBuilderRequestBuilder(ModelBuilderRequest request) {
            this.session = request.getSession();
            this.validationLevel = request.getValidationLevel();
            this.locationTracking = request.isLocationTracking();
            this.twoPhaseBuilding = request.isTwoPhaseBuilding();
            this.source = request.getSource();
            this.projectBuild = request.isProjectBuild();
            this.processPlugins = request.isProcessPlugins();
            this.profiles = request.getProfiles();
            this.activeProfileIds = request.getActiveProfileIds();
            this.inactiveProfileIds = request.getInactiveProfileIds();
            this.systemProperties = request.getSystemProperties();
            this.userProperties = request.getUserProperties();
            this.modelResolver = request.getModelResolver();
            this.listener = request.getListener();
            this.interimResult = request.getInterimResult();
            this.transformerContextBuilder = request.getTransformerContextBuilder();
        }

        public ModelBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public ModelBuilderRequestBuilder validationLevel(int validationLevel) {
            this.validationLevel = validationLevel;
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

        public ModelBuilderRequestBuilder source(ModelSource source) {
            this.source = source;
            return this;
        }

        public ModelBuilderRequestBuilder projectBuild(boolean projectBuild) {
            this.projectBuild = projectBuild;
            return this;
        }

        public ModelBuilderRequestBuilder processPlugins(boolean processPlugins) {
            this.processPlugins = processPlugins;
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

        public ModelBuilderRequestBuilder modelResolver(ModelResolver modelResolver) {
            this.modelResolver = modelResolver;
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

        public ModelBuilderRequestBuilder transformerContextBuilder(
                ModelTransformerContextBuilder transformerContextBuilder) {
            this.transformerContextBuilder = transformerContextBuilder;
            return this;
        }

        public ModelBuilderRequest build() {
            return new DefaultModelBuilderRequest(
                    session,
                    validationLevel,
                    locationTracking,
                    twoPhaseBuilding,
                    source,
                    projectBuild,
                    processPlugins,
                    profiles,
                    activeProfileIds,
                    inactiveProfileIds,
                    systemProperties,
                    userProperties,
                    modelResolver,
                    listener,
                    interimResult,
                    transformerContextBuilder);
        }

        private static class DefaultModelBuilderRequest extends BaseRequest implements ModelBuilderRequest {
            private final int validationLevel;
            private final boolean locationTracking;
            private final boolean twoPhaseBuilding;
            private final ModelSource source;
            private final boolean projectBuild;
            private final boolean processPlugins;
            private final Collection<Profile> profiles;
            private final List<String> activeProfileIds;
            private final List<String> inactiveProfileIds;
            private final Map<String, String> systemProperties;
            private final Map<String, String> userProperties;
            private final ModelResolver modelResolver;
            private final Object listener;
            private final ModelBuilderResult interimResult;
            private final ModelTransformerContextBuilder transformerContextBuilder;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultModelBuilderRequest(
                    @Nonnull Session session,
                    int validationLevel,
                    boolean locationTracking,
                    boolean twoPhaseBuilding,
                    @Nonnull ModelSource source,
                    boolean projectBuild,
                    boolean processPlugins,
                    Collection<Profile> profiles,
                    List<String> activeProfileIds,
                    List<String> inactiveProfileIds,
                    Map<String, String> systemProperties,
                    Map<String, String> userProperties,
                    ModelResolver modelResolver,
                    Object listener,
                    ModelBuilderResult interimResult,
                    ModelTransformerContextBuilder transformerContextBuilder) {
                super(session);
                this.validationLevel = validationLevel;
                this.locationTracking = locationTracking;
                this.twoPhaseBuilding = twoPhaseBuilding;
                this.source = source;
                this.projectBuild = projectBuild;
                this.processPlugins = processPlugins;
                this.profiles = profiles != null ? List.copyOf(profiles) : List.of();
                this.activeProfileIds = activeProfileIds != null ? List.copyOf(activeProfileIds) : List.of();
                this.inactiveProfileIds = inactiveProfileIds != null ? List.copyOf(inactiveProfileIds) : List.of();
                this.systemProperties =
                        systemProperties != null ? Map.copyOf(systemProperties) : session.getSystemProperties();
                this.userProperties = userProperties != null ? Map.copyOf(userProperties) : session.getUserProperties();
                this.modelResolver = modelResolver;
                this.listener = listener;
                this.interimResult = interimResult;
                this.transformerContextBuilder = transformerContextBuilder;
            }

            @Override
            public int getValidationLevel() {
                return validationLevel;
            }

            @Override
            public boolean isTwoPhaseBuilding() {
                return twoPhaseBuilding;
            }

            @Override
            public boolean isLocationTracking() {
                return locationTracking;
            }

            @Nonnull
            @Override
            public ModelSource getSource() {
                return source;
            }

            public boolean isProjectBuild() {
                return projectBuild;
            }

            @Override
            public boolean isProcessPlugins() {
                return processPlugins;
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
            public ModelResolver getModelResolver() {
                return modelResolver;
            }

            public Object getListener() {
                return listener;
            }

            @Override
            public ModelBuilderResult getInterimResult() {
                return interimResult;
            }

            public ModelTransformerContextBuilder getTransformerContextBuilder() {
                return transformerContextBuilder;
            }
        }
    }
}
