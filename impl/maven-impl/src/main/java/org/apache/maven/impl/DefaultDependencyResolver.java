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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.maven.api.Version;
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
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.scope.ResolutionScope;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.impl.ImplUtils.cast;
import static org.apache.maven.impl.ImplUtils.map;

@Named
@Singleton
public class DefaultDependencyResolver implements DependencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDependencyResolver.class);

    /**
     * Cache of information about the modules contained in a path element.
     * Keys are the Java versions targeted by the project.
     *
     * <p><b>TODO:</b> This field should not be in this class, because the cache should be global to the session.
     * This field exists here only temporarily, until clarified where to store session-wide caches.</p>
     */
    private final Map<Runtime.Version, PathModularizationCache> moduleCaches;

    /**
     * Creates an initially empty resolver.
     */
    public DefaultDependencyResolver() {
        // TODO: the cache should not be instantiated here, but should rather be session-wide.
        moduleCaches = new ConcurrentHashMap<>();
    }

    /**
     * {@return the cache for the given request}.
     *
     * @param  request the request for which to get the target version
     * @throws IllegalArgumentException if the version string cannot be interpreted as a valid version
     */
    private PathModularizationCache moduleCache(DependencyResolverRequest request) {
        return moduleCaches.computeIfAbsent(getTargetVersion(request), PathModularizationCache::new);
    }

    /**
     * Returns the target version of the given request as a Java version object.
     *
     * @param  request the request for which to get the target version
     * @return the target version as a Java object
     * @throws IllegalArgumentException if the version string cannot be interpreted as a valid version
     */
    static Runtime.Version getTargetVersion(DependencyResolverRequest request) {
        Version target = request.getTargetVersion();
        return (target != null) ? Runtime.Version.parse(target.toString()) : Runtime.version();
    }

    @Nonnull
    @Override
    public DependencyResolverResult collect(@Nonnull DependencyResolverRequest request)
            throws DependencyResolverException, IllegalArgumentException {
        requireNonNull(request, "request");
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
                    .setRepositories(session.toResolvingRepositories(remoteRepositories))
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
                        moduleCache(request),
                        result.getExceptions(),
                        session.getNode(result.getRoot(), request.getVerbose()),
                        0);
            } catch (DependencyCollectionException e) {
                String enhancedMessage = enhanceCollectionError(e, collectRequest);
                throw new DependencyResolverException(enhancedMessage, e);
            }
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    @Nonnull
    @Override
    public List<Node> flatten(@Nonnull Session s, @Nonnull Node node, @Nullable PathScope scope)
            throws DependencyResolverException {
        return flatten(s, node, scope, null);
    }

    private List<Node> flatten(
            @Nonnull Session s, @Nonnull Node node, @Nullable PathScope scope, @Nullable Project project)
            throws DependencyResolverException {
        InternalSession session = InternalSession.from(s);
        DependencyNode root = cast(AbstractNode.class, node, "node").getDependencyNode();
        List<DependencyNode> dependencies = session.getRepositorySystem()
                .flattenDependencyNodes(session.getSession(), root, getDependencyFilter(session, scope, project));
        dependencies.remove(root);
        return map(dependencies, session::getNode);
    }

    private static DependencyFilter getDependencyFilter(InternalSession session, PathScope scope, Project project) {
        DependencyFilter scopeFilter = getScopeDependencyFilter(scope);
        if (isTestSkip(session, project)) {
            DependencyFilter testFilter = (node, parents) -> {
                org.eclipse.aether.graph.Dependency dep = node.getDependency();
                if (dep != null) {
                    return !(isTestArtifact(dep.getArtifact()) && "test".equals(dep.getScope()));
                }
                return true;
            };
            if (scopeFilter != null) {
                return (node, parents) -> scopeFilter.accept(node, parents) && testFilter.accept(node, parents);
            }
            return testFilter;
        }
        return scopeFilter;
    }

    /**
     * Determines whether the specified artifact refers to test classes (e.g. test-jar).
     * <p>
     * Note: This method intentionally duplicates the logic in {@code RepositoryUtils.isTestArtifact}
     * because {@code maven-impl} is part of the Maven 4 API implementation layer and does not depend
     * on {@code maven-core} (which contains {@code RepositoryUtils}).
     *
     * @param artifact the artifact to check, may be {@code null}
     * @return {@code true} if the artifact refers to test classes, {@code false} otherwise
     */
    static boolean isTestArtifact(org.eclipse.aether.artifact.Artifact artifact) {
        return artifact != null
                && ("test-jar".equals(artifact.getProperty(ArtifactProperties.TYPE, ""))
                        || ("jar".equals(artifact.getExtension()) && "tests".equals(artifact.getClassifier())));
    }

    /**
     * Determines whether test execution and compilation are skipped (e.g. via {@code -Dmaven.test.skip}).
     * <p>
     * Note: This method exists separately from {@code TestSkipUtils.isTestSkip} because {@code maven-impl}
     * operates on the Maven 4 API types ({@link InternalSession}, {@link Project}) and cannot depend on
     * {@code maven-core} where {@code TestSkipUtils} resides. Property precedence order follows
     * {@code userProperties > systemProperties > configProperties > projectProperties}.
     *
     * @param session the internal session, may be {@code null}
     * @param project the Maven project, may be {@code null}
     * @return {@code true} if tests are skipped, {@code false} otherwise
     */
    static boolean isTestSkip(InternalSession session, Project project) {
        if (session != null) {
            try {
                if (session.getUserProperties() != null) {
                    String val = session.getUserProperties().get("maven.test.skip");
                    if (val != null) {
                        return Boolean.parseBoolean(val) || val.isEmpty();
                    }
                }
                if (session.getSystemProperties() != null) {
                    String val = session.getSystemProperties().get("maven.test.skip");
                    if (val != null) {
                        return Boolean.parseBoolean(val) || val.isEmpty();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to check test skip from session properties", e);
            }
            try {
                RepositorySystemSession rSession = session.getSession();
                if (rSession != null) {
                    Object val = rSession.getUserProperties().get("maven.test.skip");
                    if (val != null) {
                        String s = String.valueOf(val);
                        return Boolean.parseBoolean(s) || s.isEmpty();
                    }
                    val = rSession.getSystemProperties().get("maven.test.skip");
                    if (val != null) {
                        String s = String.valueOf(val);
                        return Boolean.parseBoolean(s) || s.isEmpty();
                    }
                    val = rSession.getConfigProperties().get("maven.test.skip");
                    if (val != null) {
                        String s = String.valueOf(val);
                        return Boolean.parseBoolean(s) || s.isEmpty();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to check test skip from repository session properties", e);
            }
        }
        if (project != null) {
            try {
                if (project.getModel() != null && project.getModel().getProperties() != null) {
                    String val = project.getModel().getProperties().get("maven.test.skip");
                    if (val != null) {
                        return Boolean.parseBoolean(val) || val.isEmpty();
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to check test skip from project properties", e);
            }
        }
        return false;
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
                InternalSession.from(requireNonNull(request, "request").getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        DependencyResolverResult result;
        try {
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
                List<Node> nodes = flatten(
                        session,
                        collectorResult.getRoot(),
                        request.getPathScope(),
                        request.getProject().orElse(null));
                List<ArtifactCoordinates> coordinates = nodes.stream()
                        .map(Node::getDependency)
                        .filter(Objects::nonNull)
                        .map(Artifact::toCoordinates)
                        .collect(Collectors.toList());
                Predicate<PathType> filter = request.getPathTypeFilter();
                DefaultDependencyResolverResult resolverResult = new DefaultDependencyResolverResult(
                        null,
                        moduleCache(request),
                        collectorResult.getExceptions(),
                        collectorResult.getRoot(),
                        nodes.size());
                if (request.getRequestType() == DependencyResolverRequest.RequestType.FLATTEN) {
                    for (Node node : nodes) {
                        resolverResult.addNode(node);
                    }
                } else {
                    ArtifactResolverResult artifactResolverResult =
                            session.getService(ArtifactResolver.class).resolve(session, coordinates, repositories);
                    for (Node node : nodes) {
                        Path path = (node.artifact() != null)
                                ? artifactResolverResult
                                        .getResult(node.artifact().toCoordinates())
                                        .getPath()
                                : null;
                        try {
                            resolverResult.addDependency(node, node.dependency(), filter, path);
                        } catch (IOException e) {
                            throw cannotReadModuleInfo(path, e);
                        }
                    }
                }
                result = resolverResult;
            }
        } finally {
            RequestTraceHelper.exit(trace);
        }
        return result;
    }

    private static DependencyResolverException cannotReadModuleInfo(final Path path, final IOException cause) {
        return new DependencyResolverException("Cannot read module information of " + path, cause);
    }

    private static boolean containsUnresolvedExpression(String value) {
        return value != null && value.contains("${") && value.contains("}");
    }

    private static String enhanceCollectionError(DependencyCollectionException e, CollectRequest request) {
        if (e.getMessage() != null && e.getMessage().contains("Invalid Collect Request")) {
            StringBuilder enhanced = new StringBuilder();
            enhanced.append("Failed to collect dependencies");

            org.eclipse.aether.graph.Dependency root = request.getRoot();
            if (root != null && root.getArtifact() != null) {
                org.eclipse.aether.artifact.Artifact artifact = root.getArtifact();
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();
                String version = artifact.getVersion();

                if (containsUnresolvedExpression(groupId)
                        || containsUnresolvedExpression(artifactId)
                        || containsUnresolvedExpression(version)) {
                    enhanced.append(" due to unresolved expression(s) in dependency: ")
                            .append(groupId)
                            .append(":")
                            .append(artifactId)
                            .append(":")
                            .append(version)
                            .append(".\n")
                            .append("Please check that all properties are defined in your POM or settings.xml.");
                    return enhanced.toString();
                }
            }

            for (org.eclipse.aether.graph.Dependency dep : request.getDependencies()) {
                if (dep != null && dep.getArtifact() != null) {
                    org.eclipse.aether.artifact.Artifact artifact = dep.getArtifact();
                    String groupId = artifact.getGroupId();
                    String artifactId = artifact.getArtifactId();
                    String version = artifact.getVersion();

                    if (containsUnresolvedExpression(groupId)
                            || containsUnresolvedExpression(artifactId)
                            || containsUnresolvedExpression(version)) {
                        enhanced.append(" due to unresolved expression(s) in dependency: ")
                                .append(groupId)
                                .append(":")
                                .append(artifactId)
                                .append(":")
                                .append(version)
                                .append(".\n")
                                .append("Please check that all properties are defined in your POM or settings.xml.");
                        return enhanced.toString();
                    }
                }
            }
        }
        return e.getMessage();
    }
}
