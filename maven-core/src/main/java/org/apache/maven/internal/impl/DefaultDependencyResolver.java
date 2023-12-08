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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.ResolutionScope;
import org.apache.maven.api.Scope;
import org.apache.maven.api.Session;
import org.apache.maven.api.services.*;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.graph.visitor.FilteringDependencyVisitor;
import org.eclipse.aether.util.graph.visitor.PreorderDependencyNodeConsumerVisitor;

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultDependencyResolver implements DependencyResolver {

    @Inject
    public DefaultDependencyResolver() {}

    @Override
    public List<Node> flatten(Session s, Node node, ResolutionScope scope) throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);

        // TODO: v4: refactor with RepositorySystem#flattenDependencyNodes with resolver alpha-3
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = new ArrayList<>();
        DependencyVisitor builder = new PreorderDependencyNodeConsumerVisitor(dependencies::add);
        DependencyFilter filter = getScopeDependencyFilter(scope);
        DependencyVisitor visitor = new FilteringDependencyVisitor(builder, filter);
        root.accept(visitor);
        dependencies.remove(root);

        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getScopeDependencyFilter(ResolutionScope scope) {
        Set<String> scopes = scope.scopes().stream().map(Scope::id).collect(Collectors.toSet());
        return (n, p) -> {
            Dependency d = n.getDependency();
            return d == null || scopes.contains(d.getScope());
        };
    }

    @Override
    public DependencyResolverResult resolve(DependencyResolverRequest request)
            throws DependencyCollectorException, DependencyResolverException, ArtifactResolverException {
        nonNull(request, "request can not be null");
        InternalSession session = InternalSession.from(request.getSession());

        if (request.getProject().isPresent()) {
            List<Artifact> artifacts = resolveDependencies(
                    request.getSession(), request.getProject().get(), request.getResolutionScope());
            List<Path> paths = artifacts.stream()
                    .map(a -> request.getSession()
                            .getService(ArtifactManager.class)
                            .getPath(a)
                            .get())
                    .collect(Collectors.toList());
            return new DefaultDependencyResolverResult(Collections.emptyList(), null, Collections.emptyList(), paths);
        }

        DependencyCollectorResult collectorResult =
                session.getService(DependencyCollector.class).collect(request);
        List<Node> dependencies = flatten(session, collectorResult.getRoot(), request.getResolutionScope());

        List<ArtifactCoordinate> coordinates = dependencies.stream()
                .map(Node::getDependency)
                .filter(Objects::nonNull)
                .map(Artifact::toCoordinate)
                .collect(Collectors.toList());
        Map<Artifact, Path> artifacts = session.resolveArtifacts(coordinates);
        List<Path> paths = dependencies.stream()
                .map(Node::getDependency)
                .filter(Objects::nonNull)
                .map(artifacts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new DefaultDependencyResolverResult(
                collectorResult.getExceptions(), collectorResult.getRoot(), dependencies, paths);
    }

    private List<Artifact> resolveDependencies(Session session, Project project, ResolutionScope scope) {
        Collection<String> toResolve = toScopes(scope);
        try {
            LifecycleDependencyResolver lifecycleDependencyResolver =
                    session.getService(Lookup.class).lookup(LifecycleDependencyResolver.class);
            Set<org.apache.maven.artifact.Artifact> artifacts = lifecycleDependencyResolver.resolveProjectArtifacts(
                    getMavenProject(project),
                    toResolve,
                    toResolve,
                    InternalSession.from(session).getMavenSession(),
                    false,
                    Collections.emptySet());
            return map(artifacts, a -> InternalSession.from(session).getArtifact(RepositoryUtils.toArtifact(a)));
        } catch (LifecycleExecutionException e) {
            throw new DependencyResolverException("Unable to resolve project dependencies", e);
        }
    }

    private MavenProject getMavenProject(Project project) {
        return ((DefaultProject) project).getProject();
    }

    private Collection<String> toScopes(ResolutionScope scope) {
        return map(scope.scopes(), Scope::id);
    }

    static class DefaultDependencyResolverResult implements DependencyResolverResult {
        private final List<Exception> exceptions;
        private final Node root;
        private final List<Node> dependencies;
        private final List<Path> paths;

        DefaultDependencyResolverResult(
                List<Exception> exceptions, Node root, List<Node> dependencies, List<Path> paths) {
            this.exceptions = exceptions;
            this.root = root;
            this.dependencies = dependencies;
            this.paths = paths;
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
        public List<Node> getDependencies() {
            return dependencies;
        }

        @Override
        public List<Path> getPaths() {
            return paths;
        }
    }
}
