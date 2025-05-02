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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * The result of a dependency resolution request.
 *
 * @since 4.0.0
 * @see DependencyResolver#resolve(DependencyResolverRequest)
 */
@Experimental
public interface DependencyResolverResult extends Result<DependencyResolverRequest> {

    /**
     * Gets the exceptions that occurred while building the dependency graph.
     *
     * @return the exceptions that occurred, never {@code null}
     */
    @Nonnull
    List<Exception> getExceptions();

    /**
     * Gets the root node of the dependency graph.
     *
     * @return the root node of the dependency graph or {@code null} if none
     */
    @Nullable
    Node getRoot();

    /**
     * The ordered list of the flattened dependency nodes.
     *
     * @return the ordered list of the flattened dependency nodes
     */
    @Nonnull
    List<Node> getNodes();

    /**
     * Returns the file paths of all dependencies, regardless of which tool option those paths should be placed on.
     * The returned list may contain a mix of Java class path, Java module path, and other types of path elements.
     * This collection has the same content as {@code getDependencies.values()} except that it does not contain
     * null elements.
     *
     * @return the paths of all dependencies
     */
    @Nonnull
    List<Path> getPaths();

    /**
     * Returns the file paths of all dependencies and their assignments to different paths.
     * The {@link PathType} keys identify, for example, {@code --class-path} or {@code --module-path} options.
     * In the case of Java tools, the map may also contain {@code --patch-module} options, which are
     * {@linkplain org.apache.maven.api.JavaPathType#patchModule(String) handled in a special way}.
     *
     * <h4>Design note</h4>
     * All types of path are determined together because they are sometime mutually exclusive.
     * For example, an artifact of type {@value org.apache.maven.api.Type#JAR} can be placed
     * either on the class-path or on the module-path. The project needs to make a choice
     * (possibly using heuristic rules), then add the dependency in only one of the paths
     * identified by {@link PathType}.
     *
     * @return file paths to place on the different tool options
     */
    @Nonnull
    Map<PathType, List<Path>> getDispatchedPaths();

    /**
     * {@return all dependencies associated with their paths}
     * Some dependencies may be associated with a null value if there is no path available.
     */
    @Nonnull
    Map<Dependency, Path> getDependencies();
}
