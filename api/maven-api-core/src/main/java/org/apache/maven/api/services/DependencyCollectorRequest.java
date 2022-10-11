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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinate;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;

import static org.apache.maven.api.services.BaseRequest.nonNull;

/**
 * A request to collect the transitive dependencies and to build a dependency graph from them. There are three ways to
 * create a dependency graph. First, only the root dependency can be given. Second, a root dependency and direct
 * dependencies can be specified in which case the specified direct dependencies are merged with the direct dependencies
 * retrieved from the artifact descriptor of the root dependency. And last, only direct dependencies can be specified in
 * which case the root node of the resulting graph has no associated dependency.
 *
 * @since 4.0
 * @see DependencyCollector#collect(DependencyCollectorRequest)
 */
@Experimental
@Immutable
public interface DependencyCollectorRequest
{

    @Nonnull
    Session getSession();

    @Nonnull
    Optional<Artifact> getRootArtifact();

    @Nonnull
    Optional<DependencyCoordinate> getRoot();

    @Nonnull
    Collection<DependencyCoordinate> getDependencies();

    @Nonnull
    Collection<DependencyCoordinate> getManagedDependencies();

    boolean getVerbose();

    @Nonnull
    static DependencyCollectorRequest build( @Nonnull Session session, Artifact root )
    {
        return builder()
                .session( nonNull( session, "session cannot be null" ) )
                .rootArtifact( nonNull( root, "root cannot be null" ) )
                .build();
    }

    @Nonnull
    static DependencyCollectorRequest build( @Nonnull Session session, @Nonnull DependencyCoordinate root )
    {
        return builder()
                .session( nonNull( session, "session cannot be null" ) )
                .root( nonNull( root, "root cannot be null" ) )
                .build();
    }

    @Nonnull
    static DependencyCollectorRequest build( @Nonnull Session session, @Nonnull Project project )
    {
        return builder()
                .session( nonNull( session, "session cannot be null" ) )
                .rootArtifact( nonNull( project, "project cannot be null" ).getArtifact() )
                .dependencies( project.getDependencies() )
                .managedDependencies( project.getManagedDependencies() )
                .build();
    }

    @Nonnull
    static DependencyCollectorRequestBuilder builder()
    {
        return new DependencyCollectorRequestBuilder();
    }

    @NotThreadSafe
    class DependencyCollectorRequestBuilder
    {

        Session session;
        Artifact rootArtifact;
        DependencyCoordinate root;
        List<DependencyCoordinate> dependencies = Collections.emptyList();
        List<DependencyCoordinate> managedDependencies = Collections.emptyList();
        boolean verbose;

        DependencyCollectorRequestBuilder()
        {
        }

        @Nonnull
        public DependencyCollectorRequestBuilder session( @Nonnull Session session )
        {
            this.session = session;
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
        public DependencyCollectorRequestBuilder rootArtifact( @Nullable Artifact rootArtifact )
        {
            this.rootArtifact = rootArtifact;
            return this;
        }

        /**
         * @param root The root dependency
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyCollectorRequestBuilder root( @Nonnull DependencyCoordinate root )
        {
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
        public DependencyCollectorRequestBuilder dependencies( @Nullable List<DependencyCoordinate> dependencies )
        {
            this.dependencies = ( dependencies != null ) ? dependencies : Collections.emptyList();
            return this;
        }

        /**
         * Adds the specified direct dependency.
         *
         * @param dependency the dependency to add, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyCollectorRequestBuilder dependency( @Nullable DependencyCoordinate dependency )
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
         * @param managedDependencies the dependency management, may be {@code null}
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyCollectorRequestBuilder managedDependencies(
                        @Nullable List<DependencyCoordinate> managedDependencies )
        {
            this.managedDependencies = ( managedDependencies != null ) ? managedDependencies : Collections.emptyList();
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
        public DependencyCollectorRequestBuilder managedDependency( @Nullable DependencyCoordinate managedDependency )
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

        /**
         * Specifies that the collection should be verbose.
         *
         * @param verbose whether the collection should be verbose or not
         * @return this request for chaining, never {@code null}
         */
        @Nonnull
        public DependencyCollectorRequestBuilder verbose( boolean verbose )
        {
            this.verbose = verbose;
            return this;
        }

        @Nonnull
        public DependencyCollectorRequest build()
        {
            return new DefaultDependencyCollectorRequest(
                    session,
                    rootArtifact,
                    root,
                    dependencies,
                    managedDependencies,
                    verbose );
        }

        static class DefaultDependencyCollectorRequest extends BaseRequest
                implements DependencyCollectorRequest
        {
            private final Artifact rootArtifact;
            private final DependencyCoordinate root;
            private final Collection<DependencyCoordinate> dependencies;
            private final Collection<DependencyCoordinate> managedDependencies;
            private final boolean verbose;


            /**
             * Creates a request with the specified properties.
             *
             * @param session      {@link Session}
             * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code
             *                     null}.
             */
            DefaultDependencyCollectorRequest(
                    @Nonnull Session session,
                    @Nullable Artifact rootArtifact,
                    @Nullable DependencyCoordinate root,
                    @Nonnull Collection<DependencyCoordinate> dependencies,
                    @Nonnull Collection<DependencyCoordinate> managedDependencies,
                    boolean verbose )
            {
                super( session );
                this.rootArtifact = rootArtifact;
                this.root = root;
                this.dependencies = unmodifiable( nonNull( dependencies, "dependencies cannot be null" ) );
                this.managedDependencies = unmodifiable( nonNull( managedDependencies,
                                                         "managedDependencies cannot be null" ) );
                this.verbose = verbose;
            }

            @Nonnull
            @Override
            public Optional<Artifact> getRootArtifact()
            {
                return Optional.ofNullable( rootArtifact );
            }

            @Nonnull
            @Override
            public Optional<DependencyCoordinate> getRoot()
            {
                return Optional.ofNullable( root );
            }

            @Nonnull
            @Override
            public Collection<DependencyCoordinate> getDependencies()
            {
                return dependencies;
            }

            @Nonnull
            @Override
            public Collection<DependencyCoordinate> getManagedDependencies()
            {
                return managedDependencies;
            }

            @Override
            public boolean getVerbose()
            {
                return verbose;
            }

            @Nonnull
            @Override
            public String toString()
            {
                return getRoot() + " -> " + getDependencies();
            }

        }
    }

}
