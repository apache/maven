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
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.Version;

import static java.util.Objects.requireNonNull;

public class DefaultProtoSession implements ProtoSession {
    private final Version mavenVersion;
    private final Instant startTime;
    private final Map<String, String> systemProperties;
    private final Map<String, String> userProperties;
    private final Path topDirectory;
    private final Path rootDirectory;

    private DefaultProtoSession(
            Version mavenVersion,
            Instant startTime,
            Map<String, String> systemProperties,
            Map<String, String> userProperties,
            Path topDirectory,
            Path rootDirectory) {
        this.mavenVersion = requireNonNull(mavenVersion);
        this.startTime = requireNonNull(startTime);
        this.systemProperties = Map.copyOf(requireNonNull(systemProperties));
        this.userProperties = Map.copyOf(requireNonNull(userProperties));
        this.topDirectory = requireNonNull(topDirectory);
        this.rootDirectory = requireNonNull(rootDirectory);
    }

    @Override
    public Version getMavenVersion() {
        return mavenVersion;
    }

    @Override
    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    @Override
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    @Override
    public Path getTopDirectory() {
        return topDirectory;
    }

    @Override
    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Builder toBuilder() {
        return new Builder(mavenVersion, startTime, systemProperties, userProperties, topDirectory, rootDirectory);
    }

    public static class Builder {
        private Version mavenVersion;
        private Instant startTime;
        private Map<String, String> systemProperties;
        private Map<String, String> userProperties;
        private Path topDirectory;
        private Path rootDirectory;

        public Builder() {
            this(null, Instant.now(), new HashMap<>(), new HashMap<>(), null, null);
        }

        public Builder(
                Version mavenVersion,
                Instant startTime,
                Map<String, String> systemProperties,
                Map<String, String> userProperties,
                Path topDirectory,
                Path rootDirectory) {
            this.mavenVersion = mavenVersion;
            this.startTime = startTime;
            this.systemProperties = systemProperties;
            this.userProperties = userProperties;
            this.topDirectory = topDirectory;
            this.rootDirectory = rootDirectory;
        }

        public Builder withMavenVersion(Version mavenVersion) {
            this.mavenVersion = mavenVersion;
            return this;
        }

        public Builder withStartTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withSystemProperties(Map<String, String> systemProperties) {
            this.systemProperties = systemProperties;
            return this;
        }

        public Builder withUserProperties(Map<String, String> userProperties) {
            this.userProperties = userProperties;
            return this;
        }

        public Builder putAllUserProperties(Map<String, String> userProperties) {
            requireNonNull(userProperties);
            this.userProperties = new HashMap<>(this.userProperties);
            this.userProperties.putAll(userProperties);
            return this;
        }

        public Builder withTopDirectory(Path topDirectory) {
            this.topDirectory = topDirectory;
            return this;
        }

        public Builder withRootDirectory(Path rootDirectory) {
            this.rootDirectory = rootDirectory;
            return this;
        }

        public DefaultProtoSession build() {
            return new DefaultProtoSession(
                    mavenVersion, startTime, systemProperties, userProperties, topDirectory, rootDirectory);
        }
    }
}
