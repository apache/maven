package org.apache.maven.project;

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

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DependencyFilter;

/**
 * A request to resolve the dependencies of a project.
 *
 * @author Benjamin Bentmann
 */
public interface DependencyResolutionRequest
{

    /**
     * Gets the project to resolve dependencies for.
     *
     * @return The project to resolve dependencies for or {@code null} if not set.
     */
    MavenProject getMavenProject();

    /**
     * Sets the project to resolve dependencies for.
     *
     * @param project The project to resolve dependencies for, may be {@code null}.
     * @return This request for chaining, never {@code null}.
     */
    DependencyResolutionRequest setMavenProject( MavenProject project );

    /**
     * Gets the filter used to exclude some dependencies from resolution.
     *
     * @return The filter to exclude dependencies from resolution or {@code null} to resolve all dependencies.
     */
    DependencyFilter getResolutionFilter();

    /**
     * Sets the filter used to exclude some dependencies from resolution. Note that this filter only controls the
     * resolution/download of dependency artifacts, not the inclusion of dependency nodes in the resolved dependency
     * graph.
     *
     * @param filter The filter to exclude dependencies from resolution, may be {@code null} to resolve all
     *            dependencies.
     * @return This request for chaining, never {@code null}.
     */
    DependencyResolutionRequest setResolutionFilter( DependencyFilter filter );

    /**
     * Gets the session to use for repository access.
     *
     * @return The repository session or {@code null} if not set.
     */
    RepositorySystemSession getRepositorySession();

    /**
     * Sets the session to use for repository access.
     *
     * @param repositorySession The repository session to use.
     * @return This request for chaining, never {@code null}.
     */
    DependencyResolutionRequest setRepositorySession( RepositorySystemSession repositorySession );

}
