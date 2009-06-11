package org.apache.maven.model;

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

import org.apache.maven.model.resolution.ModelResolver;

/**
 * Collects settings that control the building of effective models.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelBuildingRequest
{

    /**
     * Gets the level of validation to perform on processed models.
     * 
     * @return {@code true} if lenient validation is enabled and only the dependency information is to be validated,
     *         {@code false} if strict validation is enabled and the entire model is validated.
     */
    boolean istLenientValidation();

    /**
     * Sets the level of validation to perform on processed models. For building of projects, strict validation should
     * be used to ensure proper building. For the mere retrievel of dependencies during artifact resolution, lenient
     * validation should be used to account for models of poor quality. By default, models are validated in strict mode.
     * 
     * @param lenientValidation A flag whether validation should be lenient instead of strict. For building of projects,
     *            strict validation should be used to ensure proper building. For the mere retrievel of dependencies
     *            during artifact resolution, lenient validation should be used to account for models of poor quality.
     * @return This request, never {@code null}.
     */
    ModelBuildingRequest setLenientValidation( boolean lenientValidation );

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

}
