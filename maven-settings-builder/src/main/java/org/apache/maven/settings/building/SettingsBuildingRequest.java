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

import java.io.File;
import java.util.Properties;

/**
 * Collects settings that control the building of effective settings.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsBuildingRequest {

    /**
     * Gets the global settings file.
     *
     * @return The global settings file or {@code null} if none.
     */
    File getGlobalSettingsFile();

    /**
     * Sets the global settings file. A non-existent settings file is equivalent to empty settings. If both user
     * settings and global settings are given, the user settings take precedence.
     *
     * @param globalSettingsFile The global settings file, may be {@code null} to disable global settings.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setGlobalSettingsFile(File globalSettingsFile);

    /**
     * Gets the global settings source.
     *
     * @return The global settings source or {@code null} if none.
     */
    SettingsSource getGlobalSettingsSource();

    /**
     * Sets the global settings source. If both user settings and a global settings are given, the user settings take
     * precedence.
     *
     * @param globalSettingsSource The global settings source, may be {@code null} to disable global settings.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setGlobalSettingsSource(SettingsSource globalSettingsSource);

    /**
     * Gets the user settings file.
     *
     * @return The user settings file or {@code null} if none.
     */
    File getUserSettingsFile();

    /**
     * Sets the user settings file. A non-existent settings file is equivalent to empty settings. If both a user
     * settings file and a global settings file are given, the user settings take precedence.
     *
     * @param userSettingsFile The user settings file, may be {@code null} to disable user settings.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setUserSettingsFile(File userSettingsFile);

    /**
     * Gets the user settings source.
     *
     * @return The user settings source or {@code null} if none.
     */
    SettingsSource getUserSettingsSource();

    /**
     * Sets the user settings source. If both user settings and a global settings are given, the user settings take
     * precedence.
     *
     * @param userSettingsSource The user settings source, may be {@code null} to disable user settings.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setUserSettingsSource(SettingsSource userSettingsSource);

    /**
     * Gets the system properties to use for interpolation. The system properties are collected from the runtime
     * environment like {@link System#getProperties()} and environment variables.
     *
     * @return The system properties, never {@code null}.
     */
    Properties getSystemProperties();

    /**
     * Sets the system properties to use for interpolation. The system properties are collected from the runtime
     * environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setSystemProperties(Properties systemProperties);

    /**
     * Gets the user properties to use for interpolation. The user properties have been configured directly by the user
     * on his discretion, e.g. via the {@code -Dkey=value} parameter on the command line.
     *
     * @return The user properties, never {@code null}.
     */
    Properties getUserProperties();

    /**
     * Sets the user properties to use for interpolation. The user properties have been configured directly by the user
     * on his discretion, e.g. via the {@code -Dkey=value} parameter on the command line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This request, never {@code null}.
     */
    SettingsBuildingRequest setUserProperties(Properties userProperties);
}
