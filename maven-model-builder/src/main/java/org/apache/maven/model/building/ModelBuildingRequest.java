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
package org.apache.maven.model.building;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.WorkspaceModelResolver;

/**
 * Collects settings that control the building of effective models.
 *
 * @author Benjamin Bentmann
 */
public interface ModelBuildingRequest {

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
     * Denotes validation as performed by Maven 3.1. This validation level is meant for new projects.
     */
    int VALIDATION_LEVEL_MAVEN_3_1 = 31;

    /**
     * Denotes strict validation as recommended by the current Maven version.
     */
    int VALIDATION_LEVEL_STRICT = VALIDATION_LEVEL_MAVEN_3_0;

    /**
     * Gets the raw model to build. If not set, model source will be used to load raw model.
     *
     * @return The raw model to build or {@code null} if not set.
     */
    Model getRawModel();

    /**
     * Set raw model.
     *
     * @param rawModel
     */
    ModelBuildingRequest setRawModel(Model rawModel);

    /**
     * Gets the source of the POM to process.
     *
     * @return The source of the POM or {@code null} if not set.
     */
    ModelSource getModelSource();

    /**
     * Sets the source of the POM to process. Eventually, either {@link #setModelSource(ModelSource)} or
     * {@link #setPomFile(File)} must be set.
     *
     * @param modelSource The source of the POM to process, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelSource(ModelSource modelSource);

    /**
     * Gets the POM file of the project to build.
     *
     * @return The POM file of the project or {@code null} if not applicable (i.e. when processing a POM from the
     *         repository).
     */
    File getPomFile();

    /**
     * Sets the POM file of the project to build. Note that providing the path to a POM file via this method will make
     * the model builder operate in project mode. This mode is meant for effective models that are employed during the
     * build process of a local project. Hence the effective model will support the notion of a project directory. To
     * build the model for a POM from the repository, use {@link #setModelSource(ModelSource)} in combination with a
     * {@link FileModelSource} instead.
     *
     * @param pomFile The POM file of the project to build the effective model for, may be {@code null} to build the
     *            model of some POM from the repository.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setPomFile(File pomFile);

    /**
     * Gets the level of validation to perform on processed models.
     *
     * @return The level of validation to perform on processed models.
     */
    int getValidationLevel();

    /**
     * Sets the level of validation to perform on processed models. For building of projects,
     * {@link #VALIDATION_LEVEL_STRICT} should be used to ensure proper building. For the mere retrieval of dependencies
     * during artifact resolution, {@link #VALIDATION_LEVEL_MINIMAL} should be used to account for models of poor
     * quality. By default, models are validated in strict mode.
     *
     * @param validationLevel The level of validation to perform on processed models.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setValidationLevel(int validationLevel);

    /**
     * Indicates whether plugin executions and configurations should be processed. If enabled, lifecycle-induced plugin
     * executions will be injected into the model and common plugin configuration will be propagated to individual
     * executions.
     *
     * @return {@code true} if plugins should be processed, {@code false} otherwise.
     */
    boolean isProcessPlugins();

    /**
     * Controls the processing of plugin executions and configurations.
     *
     * @param processPlugins {@code true} to enable plugin processing, {@code false} otherwise.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setProcessPlugins(boolean processPlugins);

    /**
     * Indicates whether the model building should happen in two phases. If enabled, the initial invocation of the model
     * builder will only produce an interim result which may be used to analyze inter-model dependencies before the
     * final invocation of the model builder is performed.
     *
     * @return {@code true} if two-phase building is enabled, {@code false} if the model should be build in a single
     *         step.
     */
    boolean isTwoPhaseBuilding();

    /**
     * Enables/disables two-phase building. If enabled, the initial invocation of the model builder will only produce an
     * interim result which may be used to analyze inter-model dependencies before the final invocation of the model
     * builder is performed.
     *
     * @param twoPhaseBuilding {@code true} to enable two-phase building, {@code false} if the model should be build in
     *            a single step.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setTwoPhaseBuilding(boolean twoPhaseBuilding);

    /**
     * Indicates whether the model should track the line/column number of the model source from which it was parsed.
     *
     * @return {@code true} if location tracking is enabled, {@code false} otherwise.
     */
    boolean isLocationTracking();

    /**
     * Enables/disables the tracking of line/column numbers for the model source being parsed. By default, input
     * locations are not tracked.
     *
     * @param locationTracking {@code true} to enable location tracking, {@code false} to disable it.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setLocationTracking(boolean locationTracking);

    /**
     * Gets the external profiles that should be considered for model building.
     *
     * @return The external profiles that should be considered for model building, never {@code null}.
     */
    List<Profile> getProfiles();

    /**
     * Sets the external profiles that should be considered for model building.
     *
     * @param profiles The external profiles that should be considered for model building, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setProfiles(List<Profile> profiles);

    /**
     * Gets the identifiers of those profiles that should be activated by explicit demand.
     *
     * @return The identifiers of those profiles to activate, never {@code null}.
     */
    List<String> getActiveProfileIds();

    /**
     * Sets the identifiers of those profiles that should be activated by explicit demand.
     *
     * @param activeProfileIds The identifiers of those profiles to activate, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setActiveProfileIds(List<String> activeProfileIds);

    /**
     * Gets the identifiers of those profiles that should be deactivated by explicit demand.
     *
     * @return The identifiers of those profiles to deactivate, never {@code null}.
     */
    List<String> getInactiveProfileIds();

    /**
     * Sets the identifiers of those profiles that should be deactivated by explicit demand.
     *
     * @param inactiveProfileIds The identifiers of those profiles to deactivate, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setInactiveProfileIds(List<String> inactiveProfileIds);

    /**
     * Gets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @return The system properties, never {@code null}.
     */
    Properties getSystemProperties();

    /**
     * Sets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setSystemProperties(Properties systemProperties);

    /**
     * Gets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @return The user properties, never {@code null}.
     */
    Properties getUserProperties();

    /**
     * Sets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setUserProperties(Properties userProperties);

    /**
     * Gets the start time of the build.
     *
     * @return The start time of the build or {@code null} if unknown.
     */
    Date getBuildStartTime();

    /**
     * Sets the start time of the build.
     *
     * @param buildStartTime The start time of the build, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setBuildStartTime(Date buildStartTime);

    /**
     * Gets the model resolver to use for resolution of mixins or parents that are not locally reachable from the
     * project directory.
     *
     * @return The model resolver or {@code null} if not set.
     */
    ModelResolver getModelResolver();

    /**
     * Sets the model resolver to use for resolution of mixins or parents that are not locally reachable from the
     * project directory.
     *
     * @param modelResolver The model resolver to use, never {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelResolver(ModelResolver modelResolver);

    /**
     * Gets the model building listener to notify during the build process.
     *
     * @return The model building listener to notify or {@code null} if none.
     */
    ModelBuildingListener getModelBuildingListener();

    /**
     * Sets the model building listener to notify during the build process.
     *
     * @param modelBuildingListener The model building listener to notify, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelBuildingListener(ModelBuildingListener modelBuildingListener);

    /**
     * Gets the model cache to use for reuse of previously built models.
     *
     * @return The model cache or {@code null} if not set.
     */
    ModelCache getModelCache();

    /**
     * Sets the model cache to use for reuse of previously built models. This is an optional component that serves
     * performance optimizations.
     *
     * @param modelCache The model cache to use, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelCache(ModelCache modelCache);

    WorkspaceModelResolver getWorkspaceModelResolver();

    ModelBuildingRequest setWorkspaceModelResolver(WorkspaceModelResolver workspaceResolver);
}
