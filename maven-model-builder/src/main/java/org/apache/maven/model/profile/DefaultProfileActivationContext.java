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
package org.apache.maven.model.profile;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Describes the environmental context used to determine the activation status of profiles.
 *
 * @author Benjamin Bentmann
 */
public class DefaultProfileActivationContext implements ProfileActivationContext {

    private List<String> activeProfileIds = Collections.emptyList();

    private List<String> inactiveProfileIds = Collections.emptyList();

    private Map<String, String> systemProperties = Collections.emptyMap();

    private Map<String, String> userProperties = Collections.emptyMap();

    private Map<String, String> projectProperties = Collections.emptyMap();

    private File projectDirectory;

    @Override
    public List<String> getActiveProfileIds() {
        return activeProfileIds;
    }

    /**
     * Sets the identifiers of those profiles that should be activated by explicit demand.
     *
     * @param activeProfileIds The identifiers of those profiles to activate, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setActiveProfileIds(List<String> activeProfileIds) {
        this.activeProfileIds = unmodifiable(activeProfileIds);
        return this;
    }

    @Override
    public List<String> getInactiveProfileIds() {
        return inactiveProfileIds;
    }

    /**
     * Sets the identifiers of those profiles that should be deactivated by explicit demand.
     *
     * @param inactiveProfileIds The identifiers of those profiles to deactivate, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setInactiveProfileIds(List<String> inactiveProfileIds) {
        this.inactiveProfileIds = unmodifiable(inactiveProfileIds);
        return this;
    }

    @Override
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    /**
     * Sets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    @SuppressWarnings("unchecked")
    public DefaultProfileActivationContext setSystemProperties(Properties systemProperties) {
        return setSystemProperties(toMap(systemProperties));
    }

    /**
     * Sets the system properties to use for interpolation and profile activation. The system properties are collected
     * from the runtime environment like {@link System#getProperties()} and environment variables.
     *
     * @param systemProperties The system properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setSystemProperties(Map<String, String> systemProperties) {
        this.systemProperties = unmodifiable(systemProperties);
        return this;
    }

    @Override
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    /**
     * Sets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    @SuppressWarnings("unchecked")
    public DefaultProfileActivationContext setUserProperties(Properties userProperties) {
        return setUserProperties(toMap(userProperties));
    }

    /**
     * Sets the user properties to use for interpolation and profile activation. The user properties have been
     * configured directly by the user on his discretion, e.g. via the {@code -Dkey=value} parameter on the command
     * line.
     *
     * @param userProperties The user properties, may be {@code null}.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setUserProperties(Map<String, String> userProperties) {
        this.userProperties = unmodifiable(userProperties);
        return this;
    }

    @Override
    public File getProjectDirectory() {
        return projectDirectory;
    }

    /**
     * Sets the base directory of the current project.
     *
     * @param projectDirectory The base directory of the current project, may be {@code null} if profile activation
     *                         happens in the context of metadata retrieval rather than project building.
     * @return This context, never {@code null}.
     */
    public DefaultProfileActivationContext setProjectDirectory(File projectDirectory) {
        this.projectDirectory = projectDirectory;

        return this;
    }

    @Override
    public Map<String, String> getProjectProperties() {
        return projectProperties;
    }

    public DefaultProfileActivationContext setProjectProperties(Properties projectProperties) {
        return setProjectProperties(toMap(projectProperties));
    }

    public DefaultProfileActivationContext setProjectProperties(Map<String, String> projectProperties) {
        this.projectProperties = unmodifiable(projectProperties);

        return this;
    }

    private static List<String> unmodifiable(List<String> list) {
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    private static Map<String, String> unmodifiable(Map<String, String> map) {
        return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
    }

    private static Map<String, String> toMap(Properties properties) {
        if (properties != null && !properties.isEmpty()) {
            return properties.entrySet().stream()
                    .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));

        } else {
            return null;
        }
    }
}
