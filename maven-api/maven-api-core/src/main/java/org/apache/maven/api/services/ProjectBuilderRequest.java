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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Request used to build a {@link org.apache.maven.api.Project} using
 * the {@link ProjectBuilder} service.
 *
 * TODO: add validationLevel, activeProfileIds, inactiveProfileIds, resolveDependencies
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ProjectBuilderRequest extends Request<Session> {

    /**
     * Gets the path to the project to build.
     * This is typically the path to a pom.xml file or a directory containing a pom.xml file.
     *
     * @return an optional containing the path to the project, or empty if not specified
     */
    @Nonnull
    Optional<Path> getPath();

    /**
     * Gets the source of the project to build.
     * This is an alternative to specifying a path, allowing the project to be built from
     * a model source such as a string or input stream.
     *
     * @return an optional containing the source of the project, or empty if not specified
     */
    @Nonnull
    Optional<Source> getSource();

    /**
     * Determines whether a stub model should be allowed when the POM is missing or unreadable.
     * A stub model contains only minimal information derived from the project's coordinates.
     *
     * @return true if a stub model should be allowed, false otherwise
     */
    boolean isAllowStubModel();

    /**
     * Determines whether the project builder should recursively build parent/child projects.
     * When true, the builder will process parent POMs and child modules as needed.
     *
     * @return true if the build should be recursive, false otherwise
     */
    boolean isRecursive();

    /**
     * Determines whether plugins should be processed during project building.
     * When true, the builder will process plugin information which may include
     * resolving plugin dependencies and executing plugin goals that participate in project building.
     *
     * @return true if plugins should be processed, false otherwise
     */
    boolean isProcessPlugins();

    /**
     * Gets the list of remote repositories to use for resolving dependencies during project building.
     * These repositories will be used in addition to any repositories defined in the project itself.
     *
     * @return the list of remote repositories, or null if not specified
     */
    @Nullable
    List<RemoteRepository> getRepositories();

    /**
     * Creates a new ProjectBuilderRequest with the specified session and source.
     *
     * @param session the Maven session
     * @param source the source of the project to build
     * @return a new ProjectBuilderRequest
     * @throws NullPointerException if session or source is null
     */
    @Nonnull
    static ProjectBuilderRequest build(@Nonnull Session session, @Nonnull Source source) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .source(requireNonNull(source, "source cannot be null"))
                .build();
    }

    /**
     * Creates a new ProjectBuilderRequest with the specified session and path.
     *
     * @param session the Maven session
     * @param path the path to the project to build
     * @return a new ProjectBuilderRequest
     * @throws NullPointerException if session or path is null
     */
    @Nonnull
    static ProjectBuilderRequest build(@Nonnull Session session, @Nonnull Path path) {
        return builder()
                .session(requireNonNull(session, "session cannot be null"))
                .path(requireNonNull(path, "path cannot be null"))
                .build();
    }

    /**
     * Creates a new builder for constructing a ProjectBuilderRequest.
     *
     * @return a new ProjectBuilderRequestBuilder
     */
    @Nonnull
    static ProjectBuilderRequestBuilder builder() {
        return new ProjectBuilderRequestBuilder();
    }

    /**
     * Builder for creating ProjectBuilderRequest instances.
     * This builder provides a fluent API for setting the various properties of a request.
     */
    @NotThreadSafe
    class ProjectBuilderRequestBuilder {
        Session session;
        RequestTrace trace;
        Path path;
        Source source;
        boolean allowStubModel;
        boolean recursive;
        boolean processPlugins = true;
        List<RemoteRepository> repositories;

        ProjectBuilderRequestBuilder() {}

        /**
         * Sets the Maven session for this request.
         *
         * @param session the Maven session
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder session(Session session) {
            this.session = session;
            return this;
        }

        /**
         * Sets the request trace for this request.
         * The trace is used for debugging and monitoring purposes.
         *
         * @param trace the request trace
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder trace(RequestTrace trace) {
            this.trace = trace;
            return this;
        }

        /**
         * Sets the path to the project to build.
         * This is typically the path to a pom.xml file or a directory containing a pom.xml file.
         *
         * @param path the path to the project
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder path(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the source of the project to build.
         * This is an alternative to specifying a path, allowing the project to be built from
         * a model source such as a string or input stream.
         *
         * @param source the source of the project
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder source(Source source) {
            this.source = source;
            return this;
        }

        /**
         * Sets whether plugins should be processed during project building.
         * When true, the builder will process plugin information which may include
         * resolving plugin dependencies and executing plugin goals that participate in project building.
         *
         * @param processPlugins true if plugins should be processed, false otherwise
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder processPlugins(boolean processPlugins) {
            this.processPlugins = processPlugins;
            return this;
        }

        /**
         * Sets the list of remote repositories to use for resolving dependencies during project building.
         * These repositories will be used in addition to any repositories defined in the project itself.
         *
         * @param repositories the list of remote repositories
         * @return this builder instance
         */
        public ProjectBuilderRequestBuilder repositories(List<RemoteRepository> repositories) {
            this.repositories = repositories;
            return this;
        }

        /**
         * Builds a new ProjectBuilderRequest with the current builder settings.
         *
         * @return a new ProjectBuilderRequest instance
         */
        public ProjectBuilderRequest build() {
            return new DefaultProjectBuilderRequest(
                    session, trace, path, source, allowStubModel, recursive, processPlugins, repositories);
        }

        private static class DefaultProjectBuilderRequest extends BaseRequest<Session>
                implements ProjectBuilderRequest {
            private final Path path;
            private final Source source;
            private final boolean allowStubModel;
            private final boolean recursive;
            private final boolean processPlugins;
            private final List<RemoteRepository> repositories;

            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultProjectBuilderRequest(
                    @Nonnull Session session,
                    @Nullable RequestTrace trace,
                    @Nullable Path path,
                    @Nullable Source source,
                    boolean allowStubModel,
                    boolean recursive,
                    boolean processPlugins,
                    @Nullable List<RemoteRepository> repositories) {
                super(session, trace);
                this.path = path;
                this.source = source;
                this.allowStubModel = allowStubModel;
                this.recursive = recursive;
                this.processPlugins = processPlugins;
                this.repositories = repositories;
            }

            @Nonnull
            @Override
            public Optional<Path> getPath() {
                return Optional.ofNullable(path);
            }

            @Nonnull
            @Override
            public Optional<Source> getSource() {
                return Optional.ofNullable(source);
            }

            @Override
            public boolean isAllowStubModel() {
                return allowStubModel;
            }

            @Override
            public boolean isRecursive() {
                return recursive;
            }

            @Override
            public boolean isProcessPlugins() {
                return processPlugins;
            }

            @Override
            public List<RemoteRepository> getRepositories() {
                return repositories;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof DefaultProjectBuilderRequest that
                        && allowStubModel == that.allowStubModel
                        && recursive == that.recursive
                        && processPlugins == that.processPlugins
                        && Objects.equals(path, that.path)
                        && Objects.equals(source, that.source)
                        && Objects.equals(repositories, that.repositories);
            }

            @Override
            public int hashCode() {
                return Objects.hash(path, source, allowStubModel, recursive, processPlugins, repositories);
            }

            @Override
            public String toString() {
                return "ProjectBuilderRequest[" + "path="
                        + path + ", source="
                        + source + ", allowStubModel="
                        + allowStubModel + ", recursive="
                        + recursive + ", processPlugins="
                        + processPlugins + ", repositories="
                        + repositories + ']';
            }
        }
    }
}
