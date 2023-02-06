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
package org.apache.maven.execution;

import java.util.List;

import org.apache.maven.project.MavenProject;

/**
 * Describes the inter-dependencies between projects in the reactor.
 *
 * @author Benjamin Bentmann
 * @since 3.0-alpha
 */
public interface ProjectDependencyGraph {

    /**
     * Gets all collected projects.
     *
     * @return All collected projects.
     *
     * @since 3.5.0
     */
    List<MavenProject> getAllProjects();

    /**
     * Gets all projects in their intended build order, i.e. after topologically sorting the projects according to their
     * inter-dependencies.
     *
     * @return The projects in the build order, never {@code null}.
     */
    List<MavenProject> getSortedProjects();

    /**
     * Gets the downstream projects of the specified project. A downstream project is a project that directly or
     * indirectly depends on the given project.
     *
     * @param project The project whose downstream projects should be retrieved, must not be {@code null}.
     * @param transitive A flag whether to retrieve all direct and indirect downstream projects or just the immediate
     *            downstream projects.
     * @return The downstream projects in the build order, never {@code null}.
     */
    List<MavenProject> getDownstreamProjects(MavenProject project, boolean transitive);

    /**
     * Gets the upstream projects of the specified project. An upstream project is a project that directly or indirectly
     * is a prerequisite of the given project.
     *
     * @param project The project whose upstream projects should be retrieved, must not be {@code null}.
     * @param transitive A flag whether to retrieve all direct and indirect upstream projects or just the immediate
     *            upstream projects.
     * @return The upstream projects in the build order, never {@code null}.
     */
    List<MavenProject> getUpstreamProjects(MavenProject project, boolean transitive);
}
