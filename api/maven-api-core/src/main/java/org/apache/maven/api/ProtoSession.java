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
package org.apache.maven.api;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * The proto session, material used to create {@link Session}.
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
public interface ProtoSession {

    /**
     * Returns immutable user properties to use for interpolation. The user properties have been configured directly
     * by the user, e.g. via the {@code -Dkey=value} parameter on the command line.
     *
     * @return the user properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getUserProperties();

    /**
     * Returns immutable system properties to use for interpolation. The system properties are collected from the
     * runtime environment such as {@link System#getProperties()} and environment variables
     * (prefixed with {@code env.}).
     *
     * @return the system properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getSystemProperties();

    /**
     * Returns the properly overlaid map of properties: system + user.
     */
    @Nonnull
    Map<String, String> getEffectiveProperties();

    /**
     * Returns the start time of the session.
     *
     * @return the start time as an Instant object, never {@code null}
     */
    @Nonnull
    Instant getStartTime();

    /**
     * Gets the directory of the topmost project being built, usually the current directory or the
     * directory pointed at by the {@code -f/--file} command line argument.
     *
     * @return the directory of the topmost project, never {@code null}
     * @see Project#isTopProject()
     * @see #getRootDirectory()
     */
    @Nonnull
    Path getTopDirectory();

    /**
     * Gets the root directory of the session, which is the root directory for the top directory project.
     *
     * @return the root directory, never {@code null}
     * @throws IllegalStateException if the root directory could not be found
     * @see #getTopDirectory()
     * @see Project#getRootDirectory()
     * @see Project#isRootProject()
     */
    @Nonnull
    Path getRootDirectory();

    /**
     * Returns a proto session builder of this instance.
     */
    @Nonnull
    default Builder toBuilder() {
        try {
            return new Builder(
                    getUserProperties(), getSystemProperties(), getStartTime(), getTopDirectory(), getRootDirectory());
        } catch (IllegalStateException e) {
            return new Builder(getUserProperties(), getSystemProperties(), getStartTime(), getTopDirectory(), null);
        }
    }

    /**
     * Returns new builder from scratch.
     */
    static Builder newBuilder() {
        return new Builder().withStartTime(MonotonicClock.now());
    }

    class Builder {
        private Map<String, String> userProperties;
        private Map<String, String> systemProperties;
        private Instant startTime;
        private Path topDirectory;
        private Path rootDirectory;

        private Builder() {}

        private Builder(
                Map<String, String> userProperties,
                Map<String, String> systemProperties,
                Instant startTime,
                Path topDirectory,
                Path rootDirectory) {
            this.userProperties = userProperties;
            this.systemProperties = systemProperties;
            this.startTime = startTime;
            this.topDirectory = topDirectory;
            this.rootDirectory = rootDirectory;
        }

        public Builder withUserProperties(@Nonnull Map<String, String> userProperties) {
            this.userProperties = new HashMap<>(userProperties);
            return this;
        }

        public Builder withSystemProperties(@Nonnull Map<String, String> systemProperties) {
            this.systemProperties = new HashMap<>(systemProperties);
            return this;
        }

        public Builder withStartTime(@Nonnull Instant startTime) {
            this.startTime = requireNonNull(startTime, "startTime");
            return this;
        }

        public Builder withTopDirectory(@Nonnull Path topDirectory) {
            this.topDirectory = requireNonNull(topDirectory, "topDirectory");
            return this;
        }

        public Builder withRootDirectory(@Nullable Path rootDirectory) {
            this.rootDirectory = rootDirectory;
            return this;
        }

        public ProtoSession build() {
            return new Impl(userProperties, systemProperties, startTime, topDirectory, rootDirectory);
        }

        private static class Impl implements ProtoSession {
            private final Map<String, String> userProperties;
            private final Map<String, String> systemProperties;
            private final Map<String, String> effectiveProperties;
            private final Instant startTime;
            private final Path topDirectory;
            private final Path rootDirectory;

            private Impl(
                    Map<String, String> userProperties,
                    Map<String, String> systemProperties,
                    Instant startTime,
                    Path topDirectory,
                    Path rootDirectory) {
                this.userProperties = Map.copyOf(userProperties);
                this.systemProperties = Map.copyOf(systemProperties);
                Map<String, String> cp = new HashMap<>(systemProperties);
                cp.putAll(userProperties);
                this.effectiveProperties = Map.copyOf(cp);
                this.startTime = requireNonNull(startTime);
                this.topDirectory = requireNonNull(topDirectory);
                this.rootDirectory = rootDirectory;
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
            public Map<String, String> getEffectiveProperties() {
                return effectiveProperties;
            }

            @Override
            public Instant getStartTime() {
                return startTime;
            }

            @Override
            public Path getTopDirectory() {
                return topDirectory;
            }

            @Override
            public Path getRootDirectory() {
                if (rootDirectory == null) {
                    throw new IllegalStateException("root directory not set");
                }
                return rootDirectory;
            }
        }
    }
}
