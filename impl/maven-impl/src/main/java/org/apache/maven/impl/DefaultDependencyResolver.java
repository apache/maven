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
package org.apache.maven.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.ProjectManager;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.scope.ResolutionScope;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import static org.apache.maven.impl.ImplUtils.cast;
import static org.apache.maven.impl.ImplUtils.map;
import static org.apache.maven.impl.ImplUtils.nonNull;

@Named
@Singleton
public class DefaultDependencyResolver implements DependencyResolver {

    /**
     * Cache of information about the modules contained in a path element.
     * This cache is created when first needed. It may be never created.
     *
     * <p><b>TODO:</b> This field should not be in this class, because the cache should be global to the session.
     * This field exists here only temporarily, until clarified where to store session-wide caches.</p>
     *
     * @see #moduleCache()
     */
    private PathModularizationCache moduleCache;

    /**
     * Creates an initially empty resolver.
     */
    public DefaultDependencyResolver() {}

    @Nonnull
    @Override
    public DependencyResolverResult collect(@Nonnull DependencyResolverRequest request)
            throws DependencyResolverException, IllegalArgumentException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            Artifact rootArtifact;
            DependencyCoordinates root;
            Collection<DependencyCoordinates> dependencies;
            Collection<DependencyCoordinates> managedDependencies;
            List<RemoteRepository> remoteRepositories;
            if (request.getProject().isPresent()) {
                Project project = request.getProject().get();
                rootArtifact = project.getPomArtifact();
                root = null;
                dependencies = project.getDependencies();
                managedDependencies = project.getManagedDependencies();
                remoteRepositories = request.getRepositories() != null
                        ? request.getRepositories()
                        : session.getService(ProjectManager.class).getRemoteProjectRepositories(project);
            } else {
                rootArtifact = request.getRootArtifact().orElse(null);
                root = request.getRoot().orElse(null);
                dependencies = request.getDependencies();
                managedDependencies = request.getManagedDependencies();
                remoteRepositories =
                        request.getRepositories() != null ? request.getRepositories() : session.getRemoteRepositories();
            }
            ResolutionScope resolutionScope = null;
            if (request.getPathScope() != null) {
                resolutionScope = session.getSession()
                        .getScopeManager()
                        .getResolutionScope(request.getPathScope().id())
                        .orElseThrow();
            }
            CollectRequest collectRequest = new CollectRequest()
                    .setRootArtifact(rootArtifact != null ? session.toArtifact(rootArtifact) : null)
                    .setRoot(root != null ? session.toDependency(root, false) : null)
                    .setDependencies(session.toDependencies(dependencies, false))
                    .setManagedDependencies(session.toDependencies(managedDependencies, true))
                    .setRepositories(session.toRepositories(remoteRepositories))
                    .setRequestContext(trace.context())
                    .setTrace(trace.trace());
            collectRequest.setResolutionScope(resolutionScope);

            RepositorySystemSession systemSession = session.getSession();
            if (request.getVerbose()) {
                systemSession = new DefaultRepositorySystemSession(systemSession)
                        .setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true)
                        .setConfigProperty(DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
            }

            try {
                final CollectResult result =
                        session.getRepositorySystem().collectDependencies(systemSession, collectRequest);
                return new DefaultDependencyResolverResult(
                        null,
                        moduleCache(),
                        result.getExceptions(),
                        session.getNode(result.getRoot(), request.getVerbose()),
                        0);
            } catch (DependencyCollectionException e) {
                throw new DependencyResolverException("Unable to collect dependencies", e);
            }
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    @Nonnull
    @Override
    public List<Node> flatten(@Nonnull Session s, @Nonnull Node node, @Nullable PathScope scope)
            throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = session.getRepositorySystem()
                .flattenDependencyNodes(session.getSession(), root, getScopeDependencyFilter(scope));
        dependencies.remove(root);
        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getScopeDependencyFilter(PathScope scope) {
        if (scope == null) {
            return null;
        }
        Set<String> scopes =
                scope.dependencyScopes().stream().map(DependencyScope::id).collect(Collectors.toSet());
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
    public DependencyResolverResult resolve(DependencyResolverRequest request)
            throws DependencyResolverException, ArtifactResolverException {
        InternalSession session =
                InternalSession.from(nonNull(request, "request").getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            DependencyResolverResult result;
            DependencyResolverResult collectorResult = collect(request);
            List<RemoteRepository> repositories = request.getRepositories() != null
                    ? request.getRepositories()
                    : request.getProject().isPresent()
                            ? session.getService(ProjectManager.class)
                                    .getRemoteProjectRepositories(
                                            request.getProject().get())
                            : session.getRemoteRepositories();
            if (request.getRequestType() == DependencyResolverRequest.RequestType.COLLECT) {
                result = collectorResult;
            } else {
                List<Node> nodes = flatten(session, collectorResult.getRoot(), request.getPathScope());
                List<ArtifactCoordinates> coordinates = nodes.stream()
                        .map(Node::getDependency)
                        .filter(Objects::nonNull)
                        .map(Artifact::toCoordinates)
                        .collect(Collectors.toList());
                Predicate<PathType> filter = request.getPathTypeFilter();
                DefaultDependencyResolverResult resolverResult = new DefaultDependencyResolverResult(
                        null, moduleCache(), collectorResult.getExceptions(), collectorResult.getRoot(), nodes.size());
                if (request.getRequestType() == DependencyResolverRequest.RequestType.FLATTEN) {
                    for (Node node : nodes) {
                        resolverResult.addNode(node);
                    }
                    result = resolverResult;
                } else {
                    ArtifactResolverResult artifactResolverResult =
                            session.getService(ArtifactResolver.class).resolve(session, coordinates, repositories);
                    for (Node node : nodes) {
                        Path path = (node.getArtifact() != null)
                                ? artifactResolverResult
                                        .getResult(node.getArtifact().toCoordinates())
                                        .getPath()
                                : null;
                        try {
                            resolverResult.addDependency(node, node.getDependency(), filter, path);
                        } catch (IOException e) {
                            throw cannotReadModuleInfo(path, e);
                        }
                    }
                    result = resolverResult;
                }
            }
            return result;
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    /**
     * {@return the cache of information about the modules contained in a path element}.
     *
     * <p><b>TODO:</b> This method should not be in this class, because the cache should be global to the session.
     * This method exists here only temporarily, until clarified where to store session-wide caches.</p>
     */
    private PathModularizationCache moduleCache() {
        if (moduleCache == null) {
            moduleCache = new PathModularizationCache();
        }
        return moduleCache;
    }

    private static DependencyResolverException cannotReadModuleInfo(final Path path, final IOException cause) {
        return new DependencyResolverException("Cannot read module information of " + path, cause);
    }
}
