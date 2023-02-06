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
package org.apache.maven.settings.building;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsBuilder {

    /**
     * Builds the effective settings of the specified settings files.
     *
     * @param request The settings building request that holds the parameters, must not be {@code null}.
     * @return The result of the settings building, never {@code null}.
     * @throws SettingsBuildingException If the effective settings could not be built.
     */
    SettingsBuildingResult build(SettingsBuildingRequest request) throws SettingsBuildingException;
}
