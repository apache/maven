package org.apache.maven.model.building;

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

import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Profile;
import org.apache.maven.model.resolution.ModelResolver;

/**
 * Collects settings that control the building of effective models.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelBuildingRequest
{

    /**
     * Denotes minimal validation of POMs. This validation level is meant for processing of POMs from repositories
     * during metadata retrieval.
     */
    static final int VALIDATION_LEVEL_MINIMAL = 0;

    /**
     * Denotes validation as performed by Maven 2.0. This validation level is meant as a compatibility mode to allow
     * users to migrate their projects.
     */
    static final int VALIDATION_LEVEL_MAVEN_2_0 = 20;

    /**
     * Denotes validation as performed by Maven 3.0. This validation level is meant for existing projects.
     */
    static final int VALIDATION_LEVEL_MAVEN_3_0 = 30;

    /**
     * Denotes validation as performed by Maven 3.1. This validation level is meant for new projects.
     */
    static final int VALIDATION_LEVEL_MAVEN_3_1 = 31;

    /**
     * Denotes strict validation as recommended by the current Maven version.
     */
    static final int VALIDATION_LEVEL_STRICT = VALIDATION_LEVEL_MAVEN_3_0;

    /**
     * Gets the level of validation to perform on processed models.
     * 
     * @return The level of validation to perform on processed models.
     */
    int getValidationLevel();

    /**
     * Sets the level of validation to perform on processed models. For building of projects,
     * {@link #VALIDATION_LEVEL_STRICT} should be used to ensure proper building. For the mere retrievel of dependencies
     * during artifact resolution, {@link #VALIDATION_LEVEL_MINIMAL} should be used to account for models of poor
     * quality. By default, models are validated in strict mode.
     * 
     * @param validationLevel The level of validation to perform on processed models.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setValidationLevel( int validationLevel );

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
    ModelBuildingRequest setProcessPlugins( boolean processPlugins );

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
    ModelBuildingRequest setProfiles( List<Profile> profiles );

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
    ModelBuildingRequest setActiveProfileIds( List<String> activeProfileIds );

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
    ModelBuildingRequest setInactiveProfileIds( List<String> inactiveProfileIds );

    /**
     * Gets the execution properties.
     * 
     * @return The execution properties, never {@code null}.
     */
    Properties getExecutionProperties();

    /**
     * Sets the execution properties.
     * 
     * @param executionProperties The execution properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setExecutionProperties( Properties executionProperties );

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
    ModelBuildingRequest setBuildStartTime( Date buildStartTime );

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
     * @param modelResolver The model resolver to use, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelResolver( ModelResolver modelResolver );

    /**
     * Gets the model building listeners to notify during the build process.
     * 
     * @return The model building listeners to notify, never {@code null}.
     */
    List<ModelBuildingListener> getModelBuildingListeners();

    /**
     * Sets the model building listeners to notify during the build process.
     * 
     * @param modelBuildingListeners The model building listeners to notify, may be {@code null}.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setModelBuildingListeners( List<? extends ModelBuildingListener> modelBuildingListeners );

}
