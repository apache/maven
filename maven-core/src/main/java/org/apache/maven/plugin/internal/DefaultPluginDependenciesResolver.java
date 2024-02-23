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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.DependencyScope;
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
    private static final String REPOSITORY_CONTEXT = "plugin";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repoSystem;

    private final List<MavenPluginDependenciesValidator> dependenciesValidators;

    @Inject
    public DefaultPluginDependenciesResolver(
            RepositorySystem repoSystem, List<MavenPluginDependenciesValidator> dependenciesValidators) {
        this.repoSystem = repoSystem;
        this.dependenciesValidators = dependenciesValidators;
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
                String message = pluginArtifact instanceof org.apache.maven.repository.internal.RelocatedArtifact
                        ? ": " + ((org.apache.maven.repository.internal.RelocatedArtifact) pluginArtifact).getMessage()
                        : "";
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
            throw new PluginResolutionException(plugin, e);
        }

        try {
            ArtifactRequest request = new ArtifactRequest(pluginArtifact, repositories, REPOSITORY_CONTEXT);
            request.setTrace(trace);
            pluginArtifact = repoSystem.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new PluginResolutionException(plugin, e);
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
        RequestTrace trace = RequestTrace.newChild(null, plugin);

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
            for (Dependency dependency : plugin.getDependencies()) {
                org.eclipse.aether.graph.Dependency pluginDep =
                        RepositoryUtils.toDependency(dependency, session.getArtifactTypeRegistry());
                if (!DependencyScope.SYSTEM.is(pluginDep.getScope())) {
                    pluginDep = pluginDep.setScope(DependencyScope.RUNTIME.id());
                }
                request.addDependency(pluginDep);
            }

            DependencyRequest depRequest = new DependencyRequest(request, resolutionFilter);
            depRequest.setTrace(trace);

            request.setTrace(RequestTrace.newChild(trace, depRequest));

            node = repoSystem.collectDependencies(pluginSession, request).getRoot();

            if (logger.isDebugEnabled()) {
                node.accept(new DependencyGraphDumper(logger::debug));
            }

            depRequest.setRoot(node);
            return repoSystem.resolveDependencies(session, depRequest);
        } catch (DependencyCollectionException e) {
            throw new PluginResolutionException(plugin, e);
        } catch (DependencyResolutionException e) {
            throw new PluginResolutionException(plugin, e.getCause());
        }
    }
}
