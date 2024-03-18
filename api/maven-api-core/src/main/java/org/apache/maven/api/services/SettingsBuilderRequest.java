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

import java.nio.file.Files;
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
        return build(session, Source.fromPath(globalSettingsPath), null, Source.fromPath(userSettingsPath));
    }

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session,
            @Nullable Source globalSettingsSource,
            @Nullable Source projectSettingsSource,
            @Nullable Source userSettingsSource) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .globalSettingsSource(globalSettingsSource)
                .projectSettingsSource(projectSettingsSource)
                .userSettingsSource(userSettingsSource)
                .build();
    }

    @Nonnull
    static SettingsBuilderRequest build(
            @Nonnull Session session,
            @Nullable Path globalSettingsPath,
            @Nullable Path projectSettingsPath,
            @Nullable Path userSettingsPath) {
        return builder()
                .session(nonNull(session, "session cannot be null"))
                .globalSettingsSource(
                        globalSettingsPath != null && Files.exists(globalSettingsPath)
                                ? Source.fromPath(globalSettingsPath)
                                : null)
                .projectSettingsSource(
                        projectSettingsPath != null && Files.exists(projectSettingsPath)
                                ? Source.fromPath(projectSettingsPath)
                                : null)
                .userSettingsSource(
                        userSettingsPath != null && Files.exists(userSettingsPath)
                                ? Source.fromPath(userSettingsPath)
                                : null)
                .build();
    }

    @Nonnull
    static SettingsBuilderRequestBuilder builder() {
        return new SettingsBuilderRequestBuilder();
    }

    @NotThreadSafe
    class SettingsBuilderRequestBuilder {
        Session session;
        Source globalSettingsSource;
        Source projectSettingsSource;
        Source userSettingsSource;

        public SettingsBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        public SettingsBuilderRequestBuilder globalSettingsSource(Source globalSettingsSource) {
            this.globalSettingsSource = globalSettingsSource;
            return this;
        }

        public SettingsBuilderRequestBuilder projectSettingsSource(Source projectSettingsSource) {
            this.projectSettingsSource = projectSettingsSource;
            return this;
        }

        public SettingsBuilderRequestBuilder userSettingsSource(Source userSettingsSource) {
            this.userSettingsSource = userSettingsSource;
            return this;
        }

        public SettingsBuilderRequest build() {
            return new DefaultSettingsBuilderRequest(
                    session, globalSettingsSource, projectSettingsSource, userSettingsSource);
        }

        private static class DefaultSettingsBuilderRequest extends BaseRequest implements SettingsBuilderRequest {
            private final Source globalSettingsSource;
            private final Source projectSettingsSource;
            private final Source userSettingsSource;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultSettingsBuilderRequest(
                    @Nonnull Session session,
                    @Nullable Source globalSettingsSource,
                    @Nullable Source projectSettingsSource,
                    @Nullable Source userSettingsSource) {
                super(session);
                this.globalSettingsSource = globalSettingsSource;
                this.projectSettingsSource = projectSettingsSource;
                this.userSettingsSource = userSettingsSource;
            }

            @Nonnull
            @Override
            public Optional<Source> getGlobalSettingsSource() {
                return Optional.ofNullable(globalSettingsSource);
            }

            @Nonnull
            @Override
            public Optional<Source> getProjectSettingsSource() {
                return Optional.ofNullable(projectSettingsSource);
            }

            @Nonnull
            @Override
            public Optional<Source> getUserSettingsSource() {
                return Optional.ofNullable(userSettingsSource);
            }
        }
    }
}
