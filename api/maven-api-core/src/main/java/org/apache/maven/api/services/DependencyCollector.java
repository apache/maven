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

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;

import org.apache.maven.api.Session;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Project;

/**
 * The DependencyCollector service can be used to collect dependencies
 * for a given artifact and builds a graph of them.
 * The dependencies collection mechanism will not download any artifacts,
 * and only the pom files will be downloaded.
 *
 * @author Robert Scholte
 * @author Guillaume Nodet
 */
public interface DependencyCollector extends Service
{

    /**
     * Collects the transitive dependencies and builds a dependency graph.
     * Note that this operation is only concerned about determining the coordinates of the
     * transitive dependencies and does not actually resolve the artifact files.
     *
     * @param request The dependency collection request, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @throws IllegalArgumentException if an argument is null or invalid
     *
     * @see DependencyCollector#collect(Session, Project)
     * @see DependencyCollector#collect(Session, Dependency)
     * @see DependencyCollector#collect(Session, Artifact)
     */
    @Nonnull
    DependencyCollectorResult collect( @Nonnull DependencyCollectorRequest request );

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param root The Maven Dependency, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyCollectorRequest)
     */
    @Nonnull
    default DependencyCollectorResult collect( @Nonnull Session session,
                                               @Nonnull Dependency root )
    {
        return collect( DependencyCollectorRequest.build( session, root ) );
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param project The {@link Project}, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyCollectorRequest)
     */
    @Nonnull
    default DependencyCollectorResult collect( @Nonnull Session session,
                                               @Nonnull Project project )
    {
        return collect( DependencyCollectorRequest.build( session, project ) );
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     *
     * @param session The {@link Session}, must not be {@code null}.
     * @param artifact The {@link Artifact}, must not be {@code null}.
     * @return The collection result, never {@code null}.
     * @throws DependencyCollectorException If the dependency tree could not be built.
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyCollectorRequest)
     */
    @Nonnull
    default DependencyCollectorResult collect( @Nonnull Session session,
                                               @Nonnull Artifact artifact )
    {
        return collect( DependencyCollectorRequest.build( session, artifact ) );
    }

}
