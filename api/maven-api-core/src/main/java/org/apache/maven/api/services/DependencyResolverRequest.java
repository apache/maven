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

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.JavaPathType;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

@Experimental
public interface DependencyResolverRequest extends DependencyCollectorRequest {

    @Nonnull
    PathScope getPathScope();

    /**
     * Returns a filter for the types of path (class-path, module-path, …) accepted by the tool.
     * For example, if a Java tools accepts only class-path elements, then the filter should return
     * {@code true} for {@link JavaPathType#CLASSES} and {@code false} for {@link JavaPathType#MODULES}.
     * If no filter is explicitly set, then the default is a filter accepting everything.
     *
     * @return a filter for the types of path (class-path, module-path, …) accepted by the tool
     */
    Predicate<PathType> getPathTypeFilter();

    @Nonnull
    static DependencyResolverRequestBuilder builder() {
        return new DependencyResolverRequestBuilder();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, Project project) {
        return build(session, project, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, Project project, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .project(project)
                .pathScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, DependencyCoordinate dependency) {
        return build(session, dependency, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, DependencyCoordinate dependency, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .dependency(dependency)
                .pathScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, List<DependencyCoordinate> dependencies) {
        return build(session, dependencies, PathScope.MAIN_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, List<DependencyCoordinate> dependencies, PathScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .dependencies(dependencies)
                .pathScope(scope)
                .build();
    }

    @NotThreadSafe
    class DependencyResolverRequestBuilder extends DependencyCollectorRequestBuilder {
        PathScope pathScope;

        Predicate<PathType> pathTypeFilter;

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder session(@Nonnull Session session) {
            super.session(session);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder project(@Nullable Project project) {
            super.project(project);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder rootArtifact(@Nullable Artifact rootArtifact) {
            super.rootArtifact(rootArtifact);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder root(@Nullable DependencyCoordinate root) {
            super.root(root);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder dependencies(@Nullable List<DependencyCoordinate> dependencies) {
            super.dependencies(dependencies);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder dependency(@Nullable DependencyCoordinate dependency) {
            super.dependency(dependency);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder managedDependencies(
                @Nullable List<DependencyCoordinate> managedDependencies) {
            super.managedDependencies(managedDependencies);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder managedDependency(@Nullable DependencyCoordinate managedDependency) {
            super.managedDependency(managedDependency);
            return this;
        }

        @Nonnull
        @Override
        public DependencyResolverRequestBuilder verbose(boolean verbose) {
            super.verbose(verbose);
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder pathScope(@Nonnull PathScope pathScope) {
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
        public DependencyResolverRequestBuilder pathTypeFilter(@Nonnull Collection<PathType> desiredTypes) {
            return pathTypeFilter(desiredTypes::contains);
        }

        @Override
        public DependencyResolverRequest build() {
            return new DefaultDependencyResolverRequest(
                    session,
                    project,
                    rootArtifact,
                    root,
                    dependencies,
                    managedDependencies,
                    verbose,
                    pathScope,
                    pathTypeFilter);
        }

        static class DefaultDependencyResolverRequest extends DefaultDependencyCollectorRequest
                implements DependencyResolverRequest {
            private final PathScope pathScope;

            private final Predicate<PathType> pathTypeFilter;

            DefaultDependencyResolverRequest(
                    Session session,
                    Project project,
                    Artifact rootArtifact,
                    DependencyCoordinate root,
                    Collection<DependencyCoordinate> dependencies,
                    Collection<DependencyCoordinate> managedDependencies,
                    boolean verbose,
                    PathScope pathScope,
                    Predicate<PathType> pathTypeFilter) {
                super(session, project, rootArtifact, root, dependencies, managedDependencies, verbose);
                this.pathScope = nonNull(pathScope, "pathScope cannot be null");
                this.pathTypeFilter = (pathTypeFilter != null) ? pathTypeFilter : (t) -> true;
                if (verbose) {
                    throw new IllegalArgumentException("verbose cannot be true for resolving dependencies");
                }
            }

            @Nonnull
            @Override
            public PathScope getPathScope() {
                return pathScope;
            }

            @Override
            public Predicate<PathType> getPathTypeFilter() {
                return pathTypeFilter;
            }
        }
    }
}
