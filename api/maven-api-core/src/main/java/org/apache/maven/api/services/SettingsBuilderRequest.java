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
import java.util.Optional;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * Collects settings that control the building of effective settings.
 */
@Experimental
@Immutable
public interface SettingsBuilderRequest {

    @Nonnull
    Session getSession();

    /**
     * Gets the global settings path.
     *
     * @return the global settings path or {@code null} if none
     */
    @Nonnull
    Optional<Path> getGlobalSettingsPath();

    /**
     * Gets the global settings source.
     *
     * @return the global settings source or {@code null} if none
     */
    @Nonnull
    Optional<Source> getGlobalSettingsSource();

    /**
     * Gets the project settings source.
     *
     * @return the project settings source or {@code null} if none
     */
    @Nonnull
    Optional<Source> getProjectSettingsSource();

    /**
     * Gets the project settings path.
     *
     * @return the project settings path or {@code null} if none
     */
    @Nonnull
    Optional<Path> getProjectSettingsPath();

    /**
     * Gets the user settings path.
     *
     * @return the user settings path or {@code null} if none
     */
    @Nonnull
    Optional<Path> getUserSettingsPath();

    /**
     * Gets the user settings source.
     *
     * @return the user settings source or {@code null} if none
     */
    @Nonnull
    Optional<Source> getUserSettingsSource();

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session, @Nonnull Source globalSettingsSource, @Nonnull Source userSettingsSource) {
        return build(session, globalSettingsSource, null, userSettingsSource);
    }

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session, @Nonnull Path globalSettingsPath, @Nonnull Path userSettingsPath) {
        return build(session, globalSettingsPath, null, userSettingsPath);
    }

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session,
            @Nonnull Source globalSettingsSource,
            @Nonnull Source projectSettingsSource,
            @Nonnull Source userSettingsSource) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .globalSettingsSource(nonNull(globalSettingsSource, "globalSettingsSource cannot be null"))
                .projectSettingsSource(nonNull(projectSettingsSource, "projectSettingsSource cannot be null"))
                .userSettingsSource(nonNull(userSettingsSource, "userSettingsSource cannot be null"))
                .build();
    }

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session,
            @Nonnull Path globalSettingsPath,
            @Nonnull Path projectSettingsPath,
            @Nonnull Path userSettingsPath) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .globalSettingsPath(nonNull(globalSettingsPath, "globalSettingsPath cannot be null"))
                .projectSettingsPath(nonNull(projectSettingsPath, "projectSettingsPath cannot be null"))
                .userSettingsPath(nonNull(userSettingsPath, "userSettingsPath cannot be null"))
                .build();
    }

    @Nonnull
    static SettingsBuilderRequestBuilder builder() {
        return new SettingsBuilderRequestBuilder();
    }

    @NotThreadSafe
    class SettingsBuilderRequestBuilder {
        Session session;
        Path globalSettingsPath;
        Source globalSettingsSource;
        Path projectSettingsPath;
        Source projectSettingsSource;
        Path userSettingsPath;
        Source userSettingsSource;

        public SettingsBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public SettingsBuilderRequestBuilder globalSettingsPath(Path globalSettingsPath) {
            this.globalSettingsPath = globalSettingsPath;
            return this;
        }

        public SettingsBuilderRequestBuilder globalSettingsSource(Source globalSettingsSource) {
            this.globalSettingsSource = globalSettingsSource;
            return this;
        }

        public SettingsBuilderRequestBuilder projectSettingsPath(Path projectSettingsPath) {
            this.projectSettingsPath = projectSettingsPath;
            return this;
        }

        public SettingsBuilderRequestBuilder projectSettingsSource(Source projectSettingsSource) {
            this.projectSettingsSource = projectSettingsSource;
            return this;
        }

        public SettingsBuilderRequestBuilder userSettingsPath(Path userSettingsPath) {
            this.userSettingsPath = userSettingsPath;
            return this;
        }

        public SettingsBuilderRequestBuilder userSettingsSource(Source userSettingsSource) {
            this.userSettingsSource = userSettingsSource;
            return this;
        }

        public SettingsBuilderRequest build() {
            return new DefaultSettingsBuilderRequest(
                    session,
                    globalSettingsPath,
                    globalSettingsSource,
                    projectSettingsPath,
                    projectSettingsSource,
                    userSettingsPath,
                    userSettingsSource);
        }

        private static class DefaultSettingsBuilderRequest extends BaseRequest implements SettingsBuilderRequest {
            private final Path globalSettingsPath;
            private final Source globalSettingsSource;
            private final Path projectSettingsPath;
            private final Source projectSettingsSource;
            private final Path userSettingsPath;
            private final Source userSettingsSource;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultSettingsBuilderRequest(
                    @Nonnull Session session,
                    @Nullable Path globalSettingsPath,
                    @Nullable Source globalSettingsSource,
                    @Nullable Path projectSettingsPath,
                    @Nullable Source projectSettingsSource,
                    @Nullable Path userSettingsPath,
                    @Nullable Source userSettingsSource) {
                super(session);
                this.globalSettingsPath = globalSettingsPath;
                this.globalSettingsSource = globalSettingsSource;
                this.projectSettingsPath = projectSettingsPath;
                this.projectSettingsSource = projectSettingsSource;
                this.userSettingsPath = userSettingsPath;
                this.userSettingsSource = userSettingsSource;
            }

            @Nonnull
            @Override
            public Optional<Path> getGlobalSettingsPath() {
                return Optional.ofNullable(globalSettingsPath);
            }

            @Nonnull
            @Override
            public Optional<Source> getGlobalSettingsSource() {
                return Optional.ofNullable(globalSettingsSource);
            }

            @Nonnull
            @Override
            public Optional<Path> getProjectSettingsPath() {
                return Optional.ofNullable(projectSettingsPath);
            }

            @Nonnull
            @Override
            public Optional<Source> getProjectSettingsSource() {
                return Optional.ofNullable(projectSettingsSource);
            }

            @Nonnull
            @Override
            public Optional<Path> getUserSettingsPath() {
                return Optional.ofNullable(userSettingsPath);
            }

            @Nonnull
            @Override
            public Optional<Source> getUserSettingsSource() {
                return Optional.ofNullable(userSettingsSource);
            }
        }
    }
}
