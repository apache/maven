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

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.Scope;
import org.apache.maven.api.Session;
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
    public List<Node> flatten(Session s, Node node, ResolutionScope scope) throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = session.getRepositorySystem()
                .flattenDependencyNodes(session.getSession(), root, getScopeDependencyFilter(scope));
        dependencies.remove(root);
        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getScopeDependencyFilter(ResolutionScope scope) {
        Set<String> scopes = scope.scopes().stream().map(Scope::id).collect(Collectors.toSet());
        return (n, p) -> {
            org.eclipse.aether.graph.Dependency d = n.getDependency();
            return d == null || scopes.contains(d.getScope());
        };
    }

    /**
     * Collects, flattens and resolves the dependencies.
     *
     * @param request the request to resolve
     * @return the result of the resolution
     */
    @Override
    public DependencyResolverResult resolve(final DependencyResolverRequest request)
            throws DependencyCollectorException, DependencyResolverException, ArtifactResolverException {
        nonNull(request, "request");
        final InternalSession session = InternalSession.from(request.getSession());

        if (request.getProject().isPresent()) {
            final DependencyResolutionResult resolved = resolveDependencies(
                    request.getSession(), request.getProject().get(), request.getResolutionScope());

            final Map<org.eclipse.aether.graph.Dependency, org.eclipse.aether.graph.DependencyNode> nodes = stream(
                            resolved.getDependencyGraph())
                    .filter(n -> n.getDependency() != null)
                    .collect(Collectors.toMap(DependencyNode::getDependency, n -> n));

            final Node root = session.getNode(resolved.getDependencyGraph());
            List<Node> dependencies = new ArrayList<>();
            Map<Dependency, Path> artifacts = new LinkedHashMap<>();
            List<Path> paths = new ArrayList<>();
            for (org.eclipse.aether.graph.Dependency dep : resolved.getResolvedDependencies()) {
                dependencies.add(session.getNode(nodes.get(dep)));
                Path path = dep.getArtifact().getFile().toPath();
                artifacts.put(session.getDependency(dep), path);
                paths.add(path);
            }
            return new DefaultDependencyResolverResult(
                    resolved.getCollectionErrors(), root, dependencies, paths, artifacts);
        }

        final DependencyCollectorResult collectorResult =
                session.getService(DependencyCollector.class).collect(request);
        final List<Node> nodes = flatten(session, collectorResult.getRoot(), request.getResolutionScope());
        List<Dependency> deps =
                nodes.stream().map(Node::getDependency).filter(Objects::nonNull).collect(Collectors.toList());
        List<ArtifactCoordinate> coordinates =
                deps.stream().map(Artifact::toCoordinate).collect(Collectors.toList());
        final Map<Artifact, Path> artifacts = session.resolveArtifacts(coordinates);
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

    private static Stream<DependencyNode> stream(final DependencyNode node) {
        return Stream.concat(Stream.of(node), node.getChildren().stream().flatMap(DefaultDependencyResolver::stream));
    }

    private static DependencyResolutionResult resolveDependencies(
            final Session session, final Project project, final ResolutionScope scope) {
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

    private static MavenProject getMavenProject(final Project project) {
        return ((DefaultProject) project).getProject();
    }

    private static Collection<String> toScopes(final ResolutionScope scope) {
        return map(scope.scopes(), Scope::id);
    }
}
