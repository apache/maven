package org.apache.maven.api.services;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * A request to collect and resolve a dependency graph.
 */
@Immutable
public interface DependencyResolverRequest extends DependencyCollectorRequest
{

    @Nonnull
    Optional<Predicate<Node>> getFilter();

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Artifact root )
    {
        return build( session, root , null );
    }

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Artifact root,
                                            @Nullable Predicate<Node> filter )
    {
        return builder()
                .session( nonNull( session, "session can not be null" ) )
                .rootArtifact( nonNull( root, "root can not be null" ) )
                .filter( filter )
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Dependency root )
    {
        return build( session, root, null );
    }

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Dependency root,
                                            @Nullable Predicate<Node> filter )
    {
        return builder()
                .session( nonNull( session, "session can not be null" ) )
                .root( nonNull( root, "root can not be null" ) )
                .filter( filter )
                .build();
    }

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Project project )
    {
        return build( session, project, null );
    }

    @Nonnull
    static DependencyResolverRequest build( @Nonnull Session session,
                                            @Nonnull Project project,
                                            @Nullable Predicate<Node> filter )
    {
        nonNull( session, "session can not be null" );
        nonNull( project, "project can not be null" );
        return builder()
                .session( session )
                .root( session.createDependency( project.getArtifact() ) )
                .dependencies( project.getDependencies() )
                .managedDependencies( project.getManagedDependencies() )
                .filter( filter )
                .build();
    }

    @Nonnull
    static DependencyResolverRequestBuilder builder()
    {
        return new DependencyResolverRequestBuilder();
    }

    @NotThreadSafe
    class DependencyResolverRequestBuilder 
    {
        Session session;
        Artifact rootArtifact;
        Dependency root;
        List<Dependency> dependencies = Collections.emptyList();
        List<Dependency> managedDependencies = Collections.emptyList();
        Predicate<Node> filter;

        @Nonnull
        public DependencyResolverRequestBuilder session( @Nonnull Session session )
        {
            this.session = session;
            return this;
        }

        /**
         * Sets the root artifact for the dependency graph.
         * This must not be confused with {@link #root(Dependency)}: The root <em>dependency</em>, like
         * any other  specified dependency, will be subject to dependency collection/resolution, i.e. should have an
         * artifact descriptor and a corresponding artifact file. The root <em>artifact</em> on the other hand is only
         * used as a label for the root node of the graph in case no root dependency was specified. As such, the
         * configured root artifact is ignored if {@link #root(Dependency)} has not been called.
         *
         * @param rootArtifact The root artifact for the dependency graph, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder rootArtifact( Artifact rootArtifact )
        {
            this.rootArtifact = rootArtifact;
            return this;
        }

        /**
         *
         * @param root The root dependency
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder root( @Nonnull Dependency root )
        {
            this.root = root;
            return this;
        }

        /**
         * Sets the direct dependencies. If both a root dependency and direct dependencies are given in the request, the
         * direct dependencies from the request will be merged with the direct dependencies from the root dependency's
         * artifact descriptor, giving higher priority to the dependencies from the request.
         *
         * @param dependencies The direct dependencies, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder dependencies( List<Dependency> dependencies )
        {
            this.dependencies = ( dependencies != null ) ? dependencies : Collections.emptyList();
            return this;
        }

        /**
         * Adds the specified direct dependency.
         *
         * @param dependency The dependency to add, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder dependency( @Nullable Dependency dependency )
        {
            if ( dependency != null )
            {
                if ( this.dependencies.isEmpty() )
                {
                    this.dependencies = new ArrayList<>();
                }
                this.dependencies.add( dependency );
            }
            return this;
        }


        /**
         * Sets the dependency management to apply to transitive dependencies. To clarify, this management does not
         * apply to
         * the direct dependencies of the root node.
         *
         * @param managedDependencies The dependency management, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder managedDependencies( List<Dependency> managedDependencies )
        {
            this.managedDependencies = ( managedDependencies != null ) ? managedDependencies : Collections.emptyList();
            return this;
        }

        /**
         * Adds the specified managed dependency.
         *
         * @param managedDependency The managed dependency to add, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyResolverRequestBuilder managedDependency( @Nullable Dependency managedDependency )
        {
            if ( managedDependency != null )
            {
                if ( this.managedDependencies.isEmpty() )
                {
                    this.managedDependencies = new ArrayList<>();
                }
                this.managedDependencies.add( managedDependency );
            }
            return this;
        }

        @Nonnull
        public DependencyResolverRequestBuilder filter( @Nullable Predicate<Node> filter )
        {
            this.filter = filter;
            return this;
        }

        @Nonnull
        public DependencyResolverRequest build()
        {
            return new DefaultDependencyResolverRequest(
                    session,
                    rootArtifact,
                    root,
                    dependencies,
                    managedDependencies,
                    filter );
        }

        private static class DefaultDependencyResolverRequest extends BaseRequest
                implements DependencyResolverRequest
        {
            private final Artifact rootArtifact;
            private final Dependency root;
            private final Collection<Dependency> dependencies;
            private final Collection<Dependency> managedDependencies;
            private final Predicate<Node> filter;


            /**
             * Creates a request with the specified properties.
             *
             * @param session      {@link Session}
             * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code
             *                     null}.
             */
            DefaultDependencyResolverRequest(
                    @Nonnull Session session,
                    @Nullable Artifact rootArtifact,
                    @Nullable Dependency root,
                    @Nullable Collection<Dependency> dependencies,
                    @Nullable Collection<Dependency> managedDependencies,
                    @Nullable Predicate<Node> filter )
            {
                super( session );
                this.rootArtifact = rootArtifact;
                this.root = root;
                this.dependencies = unmodifiable( dependencies );
                this.managedDependencies = unmodifiable( managedDependencies );
                this.filter = filter;
            }

            @Nonnull
            @Override
            public Optional<Artifact> getRootArtifact()
            {
                return Optional.ofNullable( rootArtifact );
            }

            @Nonnull
            @Override
            public Optional<Dependency> getRoot()
            {
                return Optional.ofNullable( root );
            }

            @Nonnull
            @Override
            public Collection<Dependency> getDependencies()
            {
                return dependencies;
            }

            @Nonnull
            @Override
            public Collection<Dependency> getManagedDependencies()
            {
                return managedDependencies;
            }

            @Nonnull
            @Override
            public Optional<Predicate<Node>> getFilter()
            {
                return Optional.ofNullable( filter );
            }

            @Override
            public String toString()
            {
                return getRoot() + " -> " + getDependencies();
            }

        }
    }
}
