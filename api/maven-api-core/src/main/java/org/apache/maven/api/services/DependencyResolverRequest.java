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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

/**
 * A request to collect the transitive dependencies and to build a dependency graph from them. There are three ways to
 * create a dependency graph. First, only the root dependency can be given. Second, a root dependency and direct
 * dependencies can be specified in which case the specified direct dependencies are merged with the direct dependencies
 * retrieved from the artifact descriptor of the root dependency. And last, only direct dependencies can be specified in
 * which case the root node of the resulting graph has no associated dependency.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyResolverRequest {

    enum RequestType {
        COLLECT,
        FLATTEN,
        RESOLVE
    }

    @Nonnull
    Session getSession();

    @Nonnull
    RequestType getRequestType();

    @Nonnull
    Optional<Project> getProject();

    @Nonnull
    Optional<Artifact> getRootArtifact();

    @Nonnull
    Optional<DependencyCoordinate> getRoot();

    @Nonnull
    Collection<DependencyCoordinate> getDependencies();

    @Nonnull
    Collection<DependencyCoordinate> getManagedDependencies();

    boolean getVerbose();

    @Nullable
    PathScope getPathScope();

    /**
     * Returns a filter for the types of path (class-path, module-path, …) accepted by the tool.
     * For example, if a Java tools accepts only class-path elements, then the filter should return
     * {@code true} for {@link JavaPathType#CLASSES} and {@code false} for {@link JavaPathType#MODULES}.
     * If no filter is explicitly set, then the default is a filter accepting everything.
     *
     * @return a filter for the types of path (class-path, module-path, …) accepted by the tool
     */
    @Nullable
    Predicate<PathType> getPathTypeFilter();

    @Nonnull
    static DependencyResolverRequestBuilder builder() {
        return new DependencyResolverRequestBuilder();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, RequestType requestType, Artifact rootArtifact) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .requestType(requestType)
                .rootArtifact(rootArtifact)
                .pathScope(PathScope.MAIN_RUNTIME)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, RequestType requestType, Project project) {
        return build(session, requestType, project, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, RequestType requestType, Project project, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .requestType(requestType)
                .project(project)
                .pathScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, RequestType requestType, DependencyCoordinate dependency) {
        return build(session, requestType, dependency, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(
            Session session, RequestType requestType, DependencyCoordinate dependency, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .requestType(requestType)
                .dependency(dependency)
                .pathScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(
            Session session, RequestType requestType, List<DependencyCoordinate> dependencies) {
        return build(session, requestType, dependencies, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(
            Session session, RequestType requestType, List<DependencyCoordinate> dependencies, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .requestType(requestType)
                .dependencies(dependencies)
                .pathScope(scope)
                .build();
    }

    @NotThreadSafe
    class DependencyResolverRequestBuilder {

        Session session;
        RequestType requestType;
        Project project;
        Artifact rootArtifact;
        DependencyCoordinate root;
        List<DependencyCoordinate> dependencies = Collections.emptyList();
        List<DependencyCoordinate> managedDependencies = Collections.emptyList();
        boolean verbose;
        PathScope pathScope;
        Predicate<PathType> pathTypeFilter;

        DependencyResolverRequestBuilder() {}

        @Nonnull
        public DependencyResolverRequestBuilder session(@Nonnull Session session) {
            this.session = session;
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder requestType(@Nonnull RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder project(@Nullable Project project) {
            this.project = project;
            return this;
        }

        /**
         * Sets the root artifact for the dependency graph.
         * This must not be confused with {@link #root(DependencyCoordinate)}: The root <em>dependency</em>, like any
         * other specified dependency, will be subject to dependency collection/resolution, i.e. should have an artifact
         * descriptor and a corresponding artifact file. The root <em>artifact</em> on the other hand is only used
         * as a label for the root node of the graph in case no root dependency was specified. As such, the configured
         * root artifact is ignored if {@link #root(DependencyCoordinate)} has been set.
         *
         * @param rootArtifact the root artifact for the dependency graph, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder rootArtifact(@Nullable Artifact rootArtifact) {
            this.rootArtifact = rootArtifact;
            return this;
        }

        /**
         * @param root The root dependency
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder root(@Nonnull DependencyCoordinate root) {
            this.root = root;
            return this;
        }

        /**
         * Sets the direct dependencies. If both a root dependency and direct dependencies are given in the request, the
         * direct dependencies from the request will be merged with the direct dependencies from the root dependency's
         * artifact descriptor, giving higher priority to the dependencies from the request.
         *
         * @param dependencies the direct dependencies, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder dependencies(@Nullable List<DependencyCoordinate> dependencies) {
            this.dependencies = (dependencies != null) ? dependencies : Collections.emptyList();
            return this;
        }

        /**
         * Adds the specified direct dependency.
         *
         * @param dependency the dependency to add, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder dependency(@Nullable DependencyCoordinate dependency) {
            if (dependency != null) {
                if (this.dependencies.isEmpty()) {
                    this.dependencies = new ArrayList<>();
                }
                this.dependencies.add(dependency);
            }
            return this;
        }

        /**
         * Sets the dependency management to apply to transitive dependencies. To clarify, this management does not
         * apply to
         * the direct dependencies of the root node.
         *
         * @param managedDependencies the dependency management, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder managedDependencies(
                @Nullable List<DependencyCoordinate> managedDependencies) {
            this.managedDependencies = (managedDependencies != null) ? managedDependencies : Collections.emptyList();
            return this;
        }

        /**
         * Adds the specified managed dependency.
         *
         * @param managedDependency The managed dependency to add, may be {@code null} in which case the call
         *                          will have no effect.
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder managedDependency(@Nullable DependencyCoordinate managedDependency) {
            if (managedDependency != null) {
                if (this.managedDependencies.isEmpty()) {
                    this.managedDependencies = new ArrayList<>();
                }
                this.managedDependencies.add(managedDependency);
            }
            return this;
        }

        /**
         * Specifies that the collection should be verbose.
         *
         * @param verbose whether the collection should be verbose or not
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyResolverRequestBuilder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder pathScope(@Nullable PathScope pathScope) {
            this.pathScope = pathScope;
            return this;
        }

        /**
         * Filters the types of paths to include in the result.
         * The result will contain only the paths of types for which the predicate returned {@code true}.
         * It is recommended to apply a filter for retaining only the types of paths of interest,
         * because it can resolve ambiguities when a path could be of many types.
         *
         * @param pathTypeFilter predicate evaluating whether a path type should be included in the result
         * @return {@code this} for method call chaining
         */
        @Nonnull
        public DependencyResolverRequestBuilder pathTypeFilter(@Nonnull Predicate<PathType> pathTypeFilter) {
            this.pathTypeFilter = pathTypeFilter;
            return this;
        }

        /**
         * Specifies the type of paths to include in the result. This is a convenience method for
         * {@link #pathTypeFilter(Predicate)} using {@link Collection#contains(Object)} as the filter.
         *
         * @param desiredTypes the type of paths to include in the result
         * @return {@code this} for method call chaining
         */
        @Nonnull
        public DependencyResolverRequestBuilder pathTypeFilter(@Nonnull Collection<? extends PathType> desiredTypes) {
            return pathTypeFilter(desiredTypes::contains);
        }

        @Nonnull
        public DependencyResolverRequest build() {
            return new DefaultDependencyResolverRequest(
                    session,
                    requestType,
                    project,
                    rootArtifact,
                    root,
                    dependencies,
                    managedDependencies,
                    verbose,
                    pathScope,
                    pathTypeFilter);
        }

        static class DefaultDependencyResolverRequest extends BaseRequest implements DependencyResolverRequest {
            private final RequestType requestType;
            private final Project project;
            private final Artifact rootArtifact;
            private final DependencyCoordinate root;
            private final Collection<DependencyCoordinate> dependencies;
            private final Collection<DependencyCoordinate> managedDependencies;
            private final boolean verbose;
            private final PathScope pathScope;
            private final Predicate<PathType> pathTypeFilter;

            /**
             * Creates a request with the specified properties.
             *
             * @param session      {@link Session}
             * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code
             *                     null}.
             */
            @SuppressWarnings("checkstyle:ParameterNumber")
            DefaultDependencyResolverRequest(
                    @Nonnull Session session,
                    @Nonnull RequestType requestType,
                    @Nullable Project project,
                    @Nullable Artifact rootArtifact,
                    @Nullable DependencyCoordinate root,
                    @Nonnull Collection<DependencyCoordinate> dependencies,
                    @Nonnull Collection<DependencyCoordinate> managedDependencies,
                    boolean verbose,
                    @Nullable PathScope pathScope,
                    @Nullable Predicate<PathType> pathTypeFilter) {
                super(session);
                this.requestType = nonNull(requestType, "requestType cannot be null");
                this.project = project;
                this.rootArtifact = rootArtifact;
                this.root = root;
                this.dependencies = unmodifiable(nonNull(dependencies, "dependencies cannot be null"));
                this.managedDependencies =
                        unmodifiable(nonNull(managedDependencies, "managedDependencies cannot be null"));
                this.verbose = verbose;
                this.pathScope = pathScope;
                this.pathTypeFilter = (pathTypeFilter != null) ? pathTypeFilter : (t) -> true;
                if (verbose && requestType != RequestType.COLLECT) {
                    throw new IllegalArgumentException("verbose cannot only be true when collecting dependencies");
                }
            }

            @Nonnull
            @Override
            public RequestType getRequestType() {
                return requestType;
            }

            @Nonnull
            @Override
            public Optional<Project> getProject() {
                return Optional.ofNullable(project);
            }

            @Nonnull
            @Override
            public Optional<Artifact> getRootArtifact() {
                return Optional.ofNullable(rootArtifact);
            }

            @Nonnull
            @Override
            public Optional<DependencyCoordinate> getRoot() {
                return Optional.ofNullable(root);
            }

            @Nonnull
            @Override
            public Collection<DependencyCoordinate> getDependencies() {
                return dependencies;
            }

            @Nonnull
            @Override
            public Collection<DependencyCoordinate> getManagedDependencies() {
                return managedDependencies;
            }

            @Override
            public boolean getVerbose() {
                return verbose;
            }

            @Override
            public PathScope getPathScope() {
                return pathScope;
            }

            @Override
            public Predicate<PathType> getPathTypeFilter() {
                return pathTypeFilter;
            }

            @Nonnull
            @Override
            public String toString() {
                return getRoot() + " -> " + getDependencies();
            }
        }
    }
}
