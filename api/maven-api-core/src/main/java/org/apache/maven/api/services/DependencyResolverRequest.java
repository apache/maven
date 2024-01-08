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
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

@Experimental
public interface DependencyResolverRequest extends DependencyCollectorRequest {

    @Nonnull
    ResolutionScope getResolutionScope();

    /**
     * {@return a filter for the types of path (class-path, module-path, …) accepted by the tool}.
     * For example, if a Java tools accepts only class-path elements, then the filter should return
     * {@code true} for {@link JavaPathType#CLASSES} and {@code false} for {@link JavaPathType#MODULES}.
     * If no filter is explicitly set, then the default is a filter accepting everything.
     */
    Predicate<PathType> getPathTypeFilter();

    @Nonnull
    static DependencyResolverRequestBuilder builder() {
        return new DependencyResolverRequestBuilder();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, Project project) {
        return build(session, project, ResolutionScope.PROJECT_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, Project project, ResolutionScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .project(project)
                .resolutionScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, DependencyCoordinate dependency) {
        return build(session, dependency, ResolutionScope.PROJECT_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, DependencyCoordinate dependency, ResolutionScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .dependency(dependency)
                .resolutionScope(scope)
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build(Session session, List<DependencyCoordinate> dependencies) {
        return build(session, dependencies, ResolutionScope.PROJECT_RUNTIME);
    }

    @Nonnull
    static DependencyResolverRequest build(
            Session session, List<DependencyCoordinate> dependencies, ResolutionScope scope) {
        return new DependencyResolverRequestBuilder()
                .session(session)
                .dependencies(dependencies)
                .resolutionScope(scope)
                .build();
    }

    @NotThreadSafe
    class DependencyResolverRequestBuilder extends DependencyCollectorRequestBuilder {
        private ResolutionScope resolutionScope;

        private Predicate<PathType> pathTypeFilter;

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
        public DependencyResolverRequestBuilder resolutionScope(@Nonnull ResolutionScope resolutionScope) {
            this.resolutionScope = resolutionScope;
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder pathTypeFilter(@Nonnull Predicate<PathType> pathTypeFilter) {
            this.pathTypeFilter = pathTypeFilter;
            return this;
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
                    resolutionScope,
                    pathTypeFilter);
        }

        static class DefaultDependencyResolverRequest extends DefaultDependencyCollectorRequest
                implements DependencyResolverRequest {
            private final ResolutionScope resolutionScope;

            private final Predicate<PathType> pathTypeFilter;

            DefaultDependencyResolverRequest(
                    Session session,
                    Project project,
                    Artifact rootArtifact,
                    DependencyCoordinate root,
                    Collection<DependencyCoordinate> dependencies,
                    Collection<DependencyCoordinate> managedDependencies,
                    boolean verbose,
                    ResolutionScope resolutionScope,
                    Predicate<PathType> pathTypeFilter) {
                super(session, project, rootArtifact, root, dependencies, managedDependencies, verbose);
                this.resolutionScope = nonNull(resolutionScope, "resolutionScope cannot be null");
                this.pathTypeFilter = (pathTypeFilter != null) ? pathTypeFilter : (t) -> true;
                if (verbose) {
                    throw new IllegalArgumentException("verbose cannot be true for resolving dependencies");
                }
            }

            @Nonnull
            @Override
            public ResolutionScope getResolutionScope() {
                return resolutionScope;
            }

            @Override
            public Predicate<PathType> getPathTypeFilter() {
                return pathTypeFilter;
            }
        }
    }
}
