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

import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 */
public interface SettingsBuilder extends Service {

    /**
     * Builds the effective settings of the specified settings files.
     *
     * @param request the settings building request that holds the parameters, must not be {@code null}
     * @return the result of the settings building, never {@code null}
     * @throws SettingsBuilderException if the effective settings could not be built
     */
    @Nonnull
    SettingsBuilderResult build(@Nonnull SettingsBuilderRequest request);

    /**
     * Builds the effective settings of the specified settings sources.
     *
     * @return the result of the settings building, never {@code null}
     * @throws SettingsBuilderException if the effective settings could not be built
     */
    @Nonnull
    default SettingsBuilderResult build(
            @Nonnull Session session, @Nonnull Source globalSettingsSource, @Nonnull Source userSettingsSource) {
        return build(SettingsBuilderRequest.build(session, globalSettingsSource, userSettingsSource));
    }

    /**
     * Builds the effective settings of the specified settings paths.
     *
     * @return the result of the settings building, never {@code null}
     * @throws SettingsBuilderException if the effective settings could not be built
     */
    @Nonnull
    default SettingsBuilderResult build(
            @Nonnull Session session, @Nonnull Path globalSettingsPath, @Nonnull Path userSettingsPath) {
        return build(SettingsBuilderRequest.build(session, globalSettingsPath, userSettingsPath));
    }
}
