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

import org.apache.maven.api.annotations.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;

/**
 * A request to collect the transitive dependencies and to build a dependency graph from them. There are three ways to
 * create a dependency graph. First, only the root dependency can be given. Second, a root dependency and direct
 * dependencies can be specified in which case the specified direct dependencies are merged with the direct dependencies
 * retrieved from the artifact descriptor of the root dependency. And last, only direct dependencies can be specified in
 * which case the root node of the resulting graph has no associated dependency.
 *
 * @see DependencyCollector#collect(DependencyCollectorRequest)
 */
public interface DependencyCollectorRequest
{

    @Nonnull
    Session getSession();

    @Nonnull
    Optional<Artifact> getRootArtifact();

    @Nonnull
    Optional<Dependency> getRoot();

    @Nonnull
    Collection<Dependency> getDependencies();

    @Nonnull
    Collection<Dependency> getManagedDependencies();

    static DependencyCollectorRequest build( Session session, Artifact root )
    {
        BaseRequest.requireNonNull( session, "session" );
        return builder()
                .session( session )
                .root( session.createDependency( root ) )
                .build();
    }

    static DependencyCollectorRequest build( Session session, Dependency root )
    {
        return builder()
                .session( session )
                .root( root )
                .build();
    }

    static DependencyCollectorRequest build( Session session, Project project )
    {
        BaseRequest.requireNonNull( session, "session" );
        return builder()
                .session( session )
                .root( session.createDependency( project.getArtifact() ) )
                .dependencies( project.getDependencies() )
                .managedDependencies( project.getManagedDependencies() )
                .build();
    }

    static DependencyCollectorRequestBuilder builder()
    {
        return new DependencyCollectorRequestBuilder();
    }

    class DependencyCollectorRequestBuilder
    {

        Session session;
        Artifact rootArtifact;
        Dependency root;
        List<Dependency> dependencies = Collections.emptyList();
        List<Dependency> managedDependencies = Collections.emptyList();

        @Nonnull
        public DependencyCollectorRequestBuilder session( @Nonnull Session session )
        {
            this.session = session;
            return this;
        }

        /**
         * Sets the root artifact for the dependency graph.
         * This must not be confused with {@link #root(Dependency)}: The root <em>dependency</em>, like any
         * other specified dependency, will be subject to dependency collection/resolution, i.e. should have an artifact
         * descriptor and a corresponding artifact file. The root <em>artifact</em> on the other hand is only used
         * as a label for the root node of the graph in case no root dependency was specified. As such, the configured
         * root artifact is ignored if {@link #root(Dependency)} has been set.
         *
         * @param rootArtifact The root artifact for the dependency graph, may be {@code null}.
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyCollectorRequestBuilder rootArtifact( Artifact rootArtifact )
        {
            this.rootArtifact = rootArtifact;
            return this;
        }

        /**
         * @param root The root dependency
         * @return This request for chaining, never {@code null}.
         */
        @Nonnull
        public DependencyCollectorRequestBuilder root( @Nonnull Dependency root )
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
        public DependencyCollectorRequestBuilder dependencies( List<Dependency> dependencies )
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
        public DependencyCollectorRequestBuilder dependency( Dependency dependency )
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
        public DependencyCollectorRequestBuilder managedDependencies( List<Dependency> managedDependencies )
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
        public DependencyCollectorRequestBuilder managedDependency( Dependency managedDependency )
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

        public DependencyCollectorRequest build()
        {
            return new DefaultDependencyCollectorRequest(
                    session,
                    rootArtifact,
                    root,
                    dependencies,
                    managedDependencies );
        }

        static class DefaultDependencyCollectorRequest extends BaseRequest
                implements DependencyCollectorRequest
        {
            private final Artifact rootArtifact;
            private final Dependency root;
            private final Collection<Dependency> dependencies;
            private final Collection<Dependency> managedDependencies;


            /**
             * Creates a request with the specified properties.
             *
             * @param session      {@link Session}
             * @param rootArtifact The root dependency whose transitive dependencies should be collected, may be {@code
             *                     null}.
             */
            DefaultDependencyCollectorRequest(
                    Session session,
                    Artifact rootArtifact,
                    Dependency root,
                    Collection<Dependency> dependencies,
                    Collection<Dependency> managedDependencies )
            {
                super( session );
                this.rootArtifact = rootArtifact;
                this.root = root;
                this.dependencies = dependencies != null && !dependencies.isEmpty()
                        ? unmodifiable( dependencies, "dependencies" ) : Collections.emptyList();
                this.managedDependencies = managedDependencies != null && !managedDependencies.isEmpty()
                        ? unmodifiable( managedDependencies, "managedDependencies" ) : Collections.emptyList();

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

            @Override
            public String toString()
            {
                return getRoot() + " -> " + getDependencies();
            }

        }
    }

}
