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
package org.apache.maven.project;

import java.util.List;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;

/**
 * The result of a project dependency resolution.
 *
 * @author Benjamin Bentmann
 */
public interface DependencyResolutionResult {

    /**
     * Gets the dependency graph of the project.
     *
     * @return The dependency graph or {@code null} if not available.
     */
    DependencyNode getDependencyGraph();

    /**
     * Gets the transitive dependencies of the project that were not excluded by
     * {@link DependencyResolutionRequest#getResolutionFilter()}. This list is a union of the results from
     * {@link #getResolvedDependencies()} and {@link #getUnresolvedDependencies()}.
     *
     * @return The transitive dependencies, never {@code null}.
     */
    List<Dependency> getDependencies();

    /**
     * Gets the dependencies that were successfully resolved.
     *
     * @return The resolved dependencies, never {@code null}.
     */
    List<Dependency> getResolvedDependencies();

    /**
     * Gets the dependencies that could not be resolved.
     *
     * @return The unresolved dependencies, never {@code null}.
     */
    List<Dependency> getUnresolvedDependencies();

    /**
     * Gets the errors that occurred while building the dependency graph.
     *
     * @return The errors that occurred while building the dependency graph, never {@code null}.
     */
    List<Exception> getCollectionErrors();

    /**
     * Gets the errors that occurred while resolving the specified dependency.
     *
     * @param dependency The dependency for which to retrieve the errors, must not be {@code null}.
     * @return The resolution errors for the specified dependency, never {@code null}.
     */
    List<Exception> getResolutionErrors(Dependency dependency);
}
