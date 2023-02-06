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
package org.apache.maven.execution;

import org.apache.maven.settings.Settings;
import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 * Assists in populating an execution request for invocation of Maven.
 *
 * @author Benjamin Bentmann
 */
public interface MavenExecutionRequestPopulator {
    /**
     * Copies the values from the given toolchains into the specified execution request. This method will replace any
     * existing values in the execution request that are controlled by the toolchains. Hence, it is expected that this
     * method is called on a new/empty execution request before the caller mutates it to fit its needs.
     *
     * @param request The execution request to populate, must not be {@code null}.
     * @param toolchains The toolchains to copy into the execution request, may be {@code null}.
     * @return The populated execution request, never {@code null}.
     * @throws MavenExecutionRequestPopulationException If the execution request could not be populated.
     * @since 3.3.0
     */
    MavenExecutionRequest populateFromToolchains(MavenExecutionRequest request, PersistedToolchains toolchains)
            throws MavenExecutionRequestPopulationException;

    /**
     * Injects default values like plugin groups or repositories into the specified execution request.
     *
     * @param request The execution request to populate, must not be {@code null}.
     * @return The populated execution request, never {@code null}.
     * @throws MavenExecutionRequestPopulationException If the execution request could not be populated.
     */
    MavenExecutionRequest populateDefaults(MavenExecutionRequest request)
            throws MavenExecutionRequestPopulationException;

    /*if_not[MAVEN4]*/

    /**
     * Copies the values from the given settings into the specified execution request. This method will replace any
     * existing values in the execution request that are controlled by the settings. Hence, it is expected that this
     * method is called on a new/empty execution request before the caller mutates it to fit its needs.
     *
     * @param request The execution request to populate, must not be {@code null}.
     * @param settings The settings to copy into the execution request, may be {@code null}.
     * @return The populated execution request, never {@code null}.
     * @throws MavenExecutionRequestPopulationException If the execution request could not be populated.
     */
    @Deprecated
    MavenExecutionRequest populateFromSettings(MavenExecutionRequest request, Settings settings)
            throws MavenExecutionRequestPopulationException;

    /*end[MAVEN4]*/

}
