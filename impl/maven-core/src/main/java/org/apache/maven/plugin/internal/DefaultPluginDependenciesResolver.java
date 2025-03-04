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
package org.apache.maven.plugin.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.extension.internal.CoreExports;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;
import org.apache.maven.impl.resolver.RelocatedArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assists in resolving the dependencies of a plugin. <strong>Warning:</strong> This is an internal utility class that
 * is only public for technical reasons, it is not part of the public API. In particular, this class can be changed or
 * deleted without prior notice.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultPluginDependenciesResolver implements PluginDependenciesResolver {
    private static final String REPOSITORY_CONTEXT = org.apache.maven.api.services.RequestTrace.CONTEXT_PLUGIN;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repoSystem;

    private final List<MavenPluginDependenciesValidator> dependenciesValidators;

    private final CoreExports coreExports;

    @Inject
    public DefaultPluginDependenciesResolver(
            RepositorySystem repoSystem,
            List<MavenPluginDependenciesValidator> dependenciesValidators,
            CoreExports coreExports) {
        this.repoSystem = repoSystem;
        this.dependenciesValidators = dependenciesValidators;
        this.coreExports = coreExports;
    }

    private Artifact toArtifact(Plugin plugin, RepositorySystemSession session) {
        return new DefaultArtifact(
                plugin.getGroupId(),
                plugin.getArtifactId(),
                null,
                "jar",
                plugin.getVersion(),
                session.getArtifactTypeRegistry().get("maven-plugin"));
    }

    public Artifact resolve(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException {
        RequestTrace trace = RequestTrace.newChild(null, plugin);

        Artifact pluginArtifact = toArtifact(plugin, session);

        try {
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession(session);
            pluginSession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, false));

            ArtifactDescriptorRequest request =
                    new ArtifactDescriptorRequest(pluginArtifact, repositories, REPOSITORY_CONTEXT);
            request.setTrace(trace);
            ArtifactDescriptorResult result = repoSystem.readArtifactDescriptor(pluginSession, request);

            for (MavenPluginDependenciesValidator dependenciesValidator : dependenciesValidators) {
                dependenciesValidator.validate(session, pluginArtifact, result);
            }

            pluginArtifact = result.getArtifact();

            if (logger.isWarnEnabled() && !result.getRelocations().isEmpty()) {
                String message =
                        pluginArtifact instanceof RelocatedArtifact relocated ? ": " + relocated.getMessage() : "";
                logger.warn(
                        "The artifact {} has been relocated to {}{}",
                        result.getRelocations().get(0),
                        pluginArtifact,
                        message);
            }

            String requiredMavenVersion = (String) result.getProperties().get("prerequisites.maven");
            if (requiredMavenVersion != null) {
                Map<String, String> props = new LinkedHashMap<>(pluginArtifact.getProperties());
                props.put("requiredMavenVersion", requiredMavenVersion);
                pluginArtifact = pluginArtifact.setProperties(props);
            }
        } catch (ArtifactDescriptorException e) {
            throw new PluginResolutionException(plugin, e.getResult().getExceptions(), e);
        }

        try {
            ArtifactRequest request = new ArtifactRequest(pluginArtifact, repositories, REPOSITORY_CONTEXT);
            request.setTrace(trace);
            pluginArtifact = repoSystem.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new PluginResolutionException(plugin, e.getResult().getExceptions(), e);
        }

        return pluginArtifact;
    }

    /**
     * @since 3.3.0
     */
    public DependencyResult resolveCoreExtension(
            Plugin plugin,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return resolveInternal(plugin, null /* pluginArtifact */, dependencyFilter, repositories, session);
    }

    public DependencyResult resolvePlugin(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return resolveInternal(plugin, pluginArtifact, dependencyFilter, repositories, session);
    }

    public DependencyNode resolve(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return resolveInternal(plugin, pluginArtifact, dependencyFilter, repositories, session)
                .getRoot();
    }

    private DependencyResult resolveInternal(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        InternalSession iSession = InternalSession.from(session);
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(iSession, plugin.getDelegate());

        if (pluginArtifact == null) {
            pluginArtifact = toArtifact(plugin, session);
        }

        DependencyFilter collectionFilter = new ScopeDependencyFilter("provided", "test");
        DependencyFilter resolutionFilter = AndDependencyFilter.newInstance(collectionFilter, dependencyFilter);

        DependencyNode node;

        try {
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession(session);
            pluginSession.setDependencySelector(session.getDependencySelector());
            pluginSession.setDependencyGraphTransformer(session.getDependencyGraphTransformer());

            CollectRequest request = new CollectRequest();
            request.setRequestContext(REPOSITORY_CONTEXT);
            request.setRepositories(repositories);
            request.setRoot(new org.eclipse.aether.graph.Dependency(pluginArtifact, null));
            Map<String, org.eclipse.aether.graph.Dependency> core = getCoreExportsAsDependencies(session);
            request.setManagedDependencies(core.values().stream().toList());
            for (Dependency dependency : plugin.getDependencies()) {
                org.eclipse.aether.graph.Dependency pluginDep =
                        RepositoryUtils.toDependency(dependency, session.getArtifactTypeRegistry());
                if (!DependencyScope.SYSTEM.is(pluginDep.getScope())) {
                    pluginDep = pluginDep.setScope(DependencyScope.RUNTIME.id());
                }
                org.eclipse.aether.graph.Dependency managedDep =
                        core.get(pluginDep.getArtifact().getGroupId() + ":"
                                + pluginDep.getArtifact().getArtifactId());
                if (managedDep != null) {
                    // align version if needed
                    if (!Objects.equals(
                            pluginDep.getArtifact().getVersion(),
                            managedDep.getArtifact().getVersion())) {
                        pluginDep = pluginDep.setArtifact(pluginDep
                                .getArtifact()
                                .setVersion(managedDep.getArtifact().getVersion()));
                    }
                    // align scope
                    pluginDep = pluginDep.setScope(managedDep.getScope());
                }
                request.addDependency(pluginDep);
            }

            DependencyRequest depRequest = new DependencyRequest(request, resolutionFilter);
            depRequest.setTrace(trace.trace());

            request.setTrace(RequestTrace.newChild(trace.trace(), depRequest));

            node = repoSystem.collectDependencies(pluginSession, request).getRoot();

            if (logger.isDebugEnabled()) {
                node.accept(new DependencyGraphDumper(logger::debug));
            }

            depRequest.setRoot(node);
            return repoSystem.resolveDependencies(session, depRequest);
        } catch (DependencyCollectionException e) {
            throw new PluginResolutionException(plugin, e.getResult().getExceptions(), e);
        } catch (DependencyResolutionException e) {
            List<Exception> exceptions = Stream.concat(
                            e.getResult().getCollectExceptions().stream(),
                            e.getResult().getArtifactResults().stream()
                                    .filter(r -> !r.isResolved())
                                    .flatMap(r -> r.getExceptions().stream()))
                    .collect(Collectors.toList());
            throw new PluginResolutionException(plugin, exceptions, e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    private static final String CACHE_KEY =
            DefaultPluginDependenciesResolver.class.getName() + "#getCoreExportsAsDependencies";

    @SuppressWarnings("unchecked")
    private Map<String, org.eclipse.aether.graph.Dependency> getCoreExportsAsDependencies(
            RepositorySystemSession session) {
        return (Map<String, org.eclipse.aether.graph.Dependency>)
                session.getData().computeIfAbsent(CACHE_KEY, () -> {
                    HashMap<String, org.eclipse.aether.graph.Dependency> core = new HashMap<>();
                    ClassLoader classLoader = coreExports.getExportedPackages().get("org.apache.maven.*");
                    for (String coreArtifact : coreExports.getExportedArtifacts()) {
                        String[] split = coreArtifact.split(":");
                        if (split.length == 2) {
                            String groupId = split[0];
                            String artifactId = split[1];
                            String version = discoverArtifactVersion(classLoader, groupId, artifactId, null);
                            if (version != null) {
                                core.put(
                                        groupId + ":" + artifactId,
                                        new org.eclipse.aether.graph.Dependency(
                                                new DefaultArtifact(groupId + ":" + artifactId + ":" + version),
                                                DependencyScope.PROVIDED.id()));
                            }
                        }
                    }
                    return Collections.unmodifiableMap(core);
                });
    }

    private static String discoverArtifactVersion(
            ClassLoader classLoader, String groupId, String artifactId, @Nullable String defVal) {
        String version = defVal;
        String resource = "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
        final Properties props = new Properties();
        try (InputStream is = classLoader.getResourceAsStream(resource)) {
            if (is != null) {
                props.load(is);
            }
            version = props.getProperty("version");
        } catch (IOException e) {
            // fall through
        }
        if (version != null) {
            version = version.trim();
            if (version.startsWith("${")) {
                version = defVal;
            }
        }
        return version;
    }
}
