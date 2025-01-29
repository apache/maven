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

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface ToolchainsBuilderRequest {
    @Nonnull
    ProtoSession getSession();

    /**
     * Gets the installation Toolchains source.
     *
     * @return the installation Toolchains source or {@code null} if none
     */
    @Nonnull
    Optional<Source> getInstallationToolchainsSource();

    /**
     * Gets the user Toolchains source.
     *
     * @return the user Toolchains source or {@code null} if none
     */
    @Nonnull
    Optional<Source> getUserToolchainsSource();

    @Nonnull
    static ToolchainsBuilderRequest build(
            @Nonnull ProtoSession session,
            @Nullable Source installationToolchainsFile,
            @Nullable Source userToolchainsSource) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .installationToolchainsSource(installationToolchainsFile)
                .userToolchainsSource(userToolchainsSource)
                .build();
    }

    @Nonnull
    static ToolchainsBuilderRequest build(
            @Nonnull ProtoSession session,
            @Nullable Path installationToolchainsFile,
            @Nullable Path userToolchainsPath) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .installationToolchainsSource(
                        installationToolchainsFile != null && Files.exists(installationToolchainsFile)
                                ? Sources.fromPath(installationToolchainsFile)
                                : null)
                .userToolchainsSource(
                        userToolchainsPath != null && Files.exists(userToolchainsPath)
                                ? Sources.fromPath(userToolchainsPath)
                                : null)
                .build();
    }

    @Nonnull
    static ToolchainsBuilderRequestBuilder builder() {
        return new ToolchainsBuilderRequestBuilder();
    }

    @NotThreadSafe
    class ToolchainsBuilderRequestBuilder {
        ProtoSession session;
        Source installationToolchainsSource;
        Source userToolchainsSource;

        public ToolchainsBuilderRequestBuilder session(ProtoSession session) {
            this.session = session;
            return this;
        }

        public ToolchainsBuilderRequestBuilder installationToolchainsSource(Source installationToolchainsSource) {
            this.installationToolchainsSource = installationToolchainsSource;
            return this;
        }

        public ToolchainsBuilderRequestBuilder userToolchainsSource(Source userToolchainsSource) {
            this.userToolchainsSource = userToolchainsSource;
            return this;
        }

        public ToolchainsBuilderRequest build() {
            return new ToolchainsBuilderRequestBuilder.DefaultToolchainsBuilderRequest(
                    session, installationToolchainsSource, userToolchainsSource);
        }

        private static class DefaultToolchainsBuilderRequest extends BaseRequest<ProtoSession>
                implements ToolchainsBuilderRequest {
            private final Source installationToolchainsSource;
            private final Source userToolchainsSource;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultToolchainsBuilderRequest(
                    @Nonnull ProtoSession session,
                    @Nullable Source installationToolchainsSource,
                    @Nullable Source userToolchainsSource) {
                super(session);
                this.installationToolchainsSource = installationToolchainsSource;
                this.userToolchainsSource = userToolchainsSource;
            }

            @Nonnull
            @Override
            public Optional<Source> getInstallationToolchainsSource() {
                return Optional.ofNullable(installationToolchainsSource);
            }

            @Nonnull
            @Override
            public Optional<Source> getUserToolchainsSource() {
                return Optional.ofNullable(userToolchainsSource);
            }
        }
    }
}
