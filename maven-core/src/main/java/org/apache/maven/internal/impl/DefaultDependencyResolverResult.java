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
package org.apache.maven.internal.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathType;
import org.apache.maven.api.services.DependencyResolverResult;

/**
 * The result of collecting dependencies with a dependency resolver.
 *
 * @see DefaultDependencyResolver#resolve(DependencyResolverRequest)
 */
final class DefaultDependencyResolverResult implements DependencyResolverResult {
    /**
     * The exceptions that occurred while building the dependency graph.
     */
    private final List<Exception> exceptions;

    /**
     * The root node of the dependency graph.
     */
    private final Node root;

    /**
     * The ordered list of the flattened dependency nodes.
     */
    private final List<Node> nodes;

    /**
     * The file paths of all dependencies, regardless on which Java tool option those paths should be placed.
     */
    private final List<Path> paths;

    /**
     * The dependencies together with the path to each dependency.
     */
    private final Map<Dependency, Path> dependencies;

    /**
     *
     * @param exceptions the exceptions that occurred while building the dependency graph
     * @param root the root node of the dependency graph
     */
    DefaultDependencyResolverResult(
            List<Exception> exceptions,
            Node root,
            List<Node> nodes,
            List<Path> paths,
            Map<Dependency, Path> dependencies) {
        this.exceptions = exceptions;
        this.root = root;
        this.nodes = nodes;
        this.paths = paths;
        this.dependencies = dependencies;
    }

    @Override
    public List<Exception> getExceptions() {
        return exceptions;
    }

    @Override
    public Node getRoot() {
        return root;
    }

    @Override
    public List<Node> getNodes() {
        return nodes;
    }

    @Override
    public List<Path> getPaths() {
        return paths;
    }

    @Override
    public Map<PathType, List<Path>> getDispatchedPaths() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Map<Dependency, Path> getDependencies() {
        return dependencies;
    }
}
