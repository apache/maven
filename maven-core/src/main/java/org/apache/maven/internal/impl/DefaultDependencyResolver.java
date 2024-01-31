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

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.*;
import org.apache.maven.api.services.*;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultDependencyResolver implements DependencyResolver {

    @Override
    public List<Node> flatten(Session s, Node node, PathScope scope) throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = session.getRepositorySystem()
                .flattenDependencyNodes(session.getSession(), root, getScopeDependencyFilter(scope));
        dependencies.remove(root);
        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getScopeDependencyFilter(PathScope scope) {
        Set<String> scopes =
                scope.dependencyScopes().stream().map(DependencyScope::id).collect(Collectors.toSet());
        return (n, p) -> {
            org.eclipse.aether.graph.Dependency d = n.getDependency();
            return d == null || scopes.contains(d.getScope());
        };
    }

    @Override
    public DependencyResolverResult resolve(DependencyResolverRequest request)
            throws DependencyCollectorException, DependencyResolverException, ArtifactResolverException {
        nonNull(request, "request can not be null");
        InternalSession session = InternalSession.from(request.getSession());

        if (request.getProject().isPresent()) {
            DependencyResolutionResult result = resolveDependencies(
                    request.getSession(), request.getProject().get(), request.getPathScope());

            Map<org.eclipse.aether.graph.Dependency, org.eclipse.aether.graph.DependencyNode> nodes = stream(
                            result.getDependencyGraph())
                    .filter(n -> n.getDependency() != null)
                    .collect(Collectors.toMap(DependencyNode::getDependency, n -> n));

            Node root = session.getNode(result.getDependencyGraph());
            List<Node> dependencies = new ArrayList<>();
            Map<Dependency, Path> artifacts = new LinkedHashMap<>();
            List<Path> paths = new ArrayList<>();
            for (org.eclipse.aether.graph.Dependency dep : result.getResolvedDependencies()) {
                dependencies.add(session.getNode(nodes.get(dep)));
                Path path = dep.getArtifact().getFile().toPath();
                artifacts.put(session.getDependency(dep), path);
                paths.add(path);
            }
            return new DefaultDependencyResolverResult(
                    result.getCollectionErrors(), root, dependencies, paths, artifacts);
        }

        DependencyCollectorResult collectorResult =
                session.getService(DependencyCollector.class).collect(request);
        List<Node> nodes = flatten(session, collectorResult.getRoot(), request.getPathScope());
        List<Dependency> deps =
                nodes.stream().map(Node::getDependency).filter(Objects::nonNull).collect(Collectors.toList());
        List<ArtifactCoordinate> coordinates =
                deps.stream().map(Artifact::toCoordinate).collect(Collectors.toList());
        Map<Artifact, Path> artifacts = session.resolveArtifacts(coordinates);
        Map<Dependency, Path> dependencies = new LinkedHashMap<>();
        List<Path> paths = new ArrayList<>();
        for (Dependency d : deps) {
            Path path = artifacts.get(d);
            if (dependencies.put(d, path) != null) {
                throw new IllegalStateException("Duplicate key");
            }
            paths.add(path);
        }

        return new DefaultDependencyResolverResult(
                collectorResult.getExceptions(), collectorResult.getRoot(), nodes, paths, dependencies);
    }

    private Stream<DependencyNode> stream(DependencyNode node) {
        return Stream.concat(Stream.of(node), node.getChildren().stream().flatMap(this::stream));
    }

    private DependencyResolutionResult resolveDependencies(Session session, Project project, PathScope scope) {
        Collection<String> toResolve = toScopes(scope);
        try {
            LifecycleDependencyResolver lifecycleDependencyResolver =
                    session.getService(Lookup.class).lookup(LifecycleDependencyResolver.class);
            return lifecycleDependencyResolver.getProjectDependencyResolutionResult(
                    getMavenProject(project),
                    toResolve,
                    toResolve,
                    InternalSession.from(session).getMavenSession(),
                    false,
                    Collections.emptySet());
        } catch (LifecycleExecutionException e) {
            throw new DependencyResolverException("Unable to resolve project dependencies", e);
        }
    }

    private MavenProject getMavenProject(Project project) {
        return ((DefaultProject) project).getProject();
    }

    private Collection<String> toScopes(PathScope scope) {
        return map(scope.dependencyScopes(), DependencyScope::id);
    }

    static class DefaultDependencyResolverResult implements DependencyResolverResult {
        private final List<Exception> exceptions;
        private final Node root;
        private final List<Node> nodes;
        private final List<Path> paths;
        private final Map<Dependency, Path> dependencies;

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
        public Map<Dependency, Path> getDependencies() {
            return dependencies;
        }
    }
}
