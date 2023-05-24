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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Assists in resolving the dependencies of a plugin. <strong>Warning:</strong> This is an internal utility class that
 * is only public for technical reasons, it is not part of the public API. In particular, this class can be changed or
 * deleted without prior notice.
 *
 * @since 3.0
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultPluginDependenciesResolver implements PluginDependenciesResolver {
    private static final String REPOSITORY_CONTEXT = "plugin";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repoSystem;

    private final List<MavenPluginDependenciesValidator> dependenciesValidators;

    private final String mavenVersion;

    private final Set<String> mavenGoneCoreGAs;

    private final Set<String> mavenCoreGAs;

    private final Map<Dependency, Set<String>> otherCoreGAVs;

    private final List<Dependency> mavenManagedDependencies;

    private final Dependency mavenCompat;

    private final List<Exclusion> mavenGlobalExclusions;

    @Inject
    public DefaultPluginDependenciesResolver(
            RepositorySystem repoSystem,
            List<MavenPluginDependenciesValidator> dependenciesValidators,
            RuntimeInformation runtimeInformation) {
        this.repoSystem = repoSystem;
        this.dependenciesValidators = dependenciesValidators;
        this.mavenVersion = runtimeInformation.getMavenVersion();
        this.mavenGoneCoreGAs = Collections.unmodifiableSet(Stream.of(
                        "org.apache.maven:maven-artifact-manager",
                        "org.apache.maven:maven-plugin-descriptor",
                        "org.apache.maven:maven-plugin-registry",
                        "org.apache.maven:maven-profile",
                        "org.apache.maven:maven-project",
                        "org.apache.maven:maven-toolchain")
                .collect(Collectors.toSet()));
        this.mavenCoreGAs = Collections.unmodifiableSet(Stream.of(
                        "org.apache.maven:maven-artifact",
                        "org.apache.maven:maven-builder-support",
                        "org.apache.maven:maven-compat",
                        "org.apache.maven:maven-core",
                        "org.apache.maven:maven-embedder",
                        "org.apache.maven:maven-model",
                        "org.apache.maven:maven-model-builder",
                        "org.apache.maven:maven-model-transform",
                        "org.apache.maven:maven-plugin-api",
                        "org.apache.maven:maven-repository-metadata",
                        "org.apache.maven:maven-resolver-provider",
                        "org.apache.maven:maven-settings",
                        "org.apache.maven:maven-settings-builder",
                        "org.apache.maven:maven-slf4j-provider",
                        "org.apache.maven:maven-slf4j-wrapper",
                        "org.apache.maven:maven-toolchain-builder",
                        "org.apache.maven:maven-toolchain-model")
                .collect(Collectors.toSet()));

        // here we "align" other deps by fixing their version (for runtime scope), and rest are (should be) provided
        // anyway
        Map<Dependency, Set<String>> otherCoreGAVs = new HashMap<>();
        otherCoreGAVs.put(
                new Dependency(
                        new DefaultArtifact("org.eclipse.sisu:org.eclipse.sisu.inject:0.3.5"), JavaScopes.RUNTIME),
                Collections.singleton("org.sonatype.sisu:sisu-inject-bean"));
        otherCoreGAVs.put(
                new Dependency(new DefaultArtifact("com.google.inject:guice:5.1.0"), JavaScopes.RUNTIME),
                Collections.singleton("org.sonatype.sisu:sisu-guice"));

        otherCoreGAVs.put(
                new Dependency(
                        new DefaultArtifact("org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.5"), JavaScopes.PROVIDED),
                new HashSet<>(Arrays.asList(
                        "org.sonatype.spice:spice-inject-plexus",
                        "org.sonatype.sisu:sisu-inject-plexus",
                        "org.codehaus.plexus:plexus-container-default",
                        "plexus:plexus-container-default")));
        otherCoreGAVs.put(
                new Dependency(
                        new DefaultArtifact("org.codehaus.plexus:plexus-classworlds:2.6.0"), JavaScopes.PROVIDED),
                Collections.singleton("classworlds:classworlds"));
        this.otherCoreGAVs = Collections.unmodifiableMap(otherCoreGAVs);

        List<Dependency> mavenCoreDependencies = Collections.unmodifiableList(mavenCoreGAs.stream()
                .map(s -> new Dependency(new DefaultArtifact(s + ":" + mavenVersion), JavaScopes.PROVIDED))
                .collect(Collectors.toList()));
        this.mavenCompat = mavenCoreDependencies.stream()
                .filter(d -> "maven-compat".equals(d.getArtifact().getArtifactId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("maven-compat not found among Maven Core dependencies"));

        this.mavenManagedDependencies = Collections.unmodifiableList(
                Streams.concat(mavenCoreDependencies.stream(), otherCoreGAVs.keySet().stream())
                        .collect(Collectors.toList()));

        this.mavenGlobalExclusions = Collections.unmodifiableList(Streams.concat(
                        mavenGoneCoreGAs.stream(),
                        otherCoreGAVs.values().stream().flatMap(Collection::stream))
                .map(s -> {
                    int idx = s.indexOf(':');
                    String g = s.substring(0, idx);
                    String a = s.substring(idx + 1);
                    return new Exclusion(g, a, "*", "*");
                })
                .collect(Collectors.toList()));
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

    private ArtifactDescriptorResult readArtifactDescriptor(
            RequestTrace trace, Plugin plugin, RepositorySystemSession session, List<RemoteRepository> repositories)
            throws ArtifactDescriptorException {
        Artifact pluginArtifact = toArtifact(plugin, session);
        DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession(session);
        pluginSession.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, false));

        ArtifactDescriptorRequest request =
                new ArtifactDescriptorRequest(pluginArtifact, repositories, REPOSITORY_CONTEXT);
        request.setTrace(trace);
        return repoSystem.readArtifactDescriptor(pluginSession, request);
    }

    public Artifact resolve(Plugin plugin, List<RemoteRepository> repositories, RepositorySystemSession session)
            throws PluginResolutionException {
        RequestTrace trace = RequestTrace.newChild(null, plugin);
        Artifact pluginArtifact;
        try {
            ArtifactDescriptorResult result = readArtifactDescriptor(trace, plugin, session, repositories);

            pluginArtifact = result.getArtifact();

            for (MavenPluginDependenciesValidator dependenciesValidator : dependenciesValidators) {
                dependenciesValidator.validate(session, pluginArtifact, result);
            }

            if (logger.isWarnEnabled()) {
                if (!result.getRelocations().isEmpty()) {
                    String message = pluginArtifact instanceof org.apache.maven.repository.internal.RelocatedArtifact
                            ? ((org.apache.maven.repository.internal.RelocatedArtifact) pluginArtifact).getMessage()
                            : null;
                    logger.warn("The artifact " + result.getRelocations().get(0) + " has been relocated to "
                            + pluginArtifact + (message != null ? ": " + message : ""));
                }
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
    public DependencyNode resolveCoreExtension(
            Plugin plugin,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return resolveInternal(plugin, null /* pluginArtifact */, dependencyFilter, repositories, session);
    }

    public DependencyNode resolve(
            Plugin plugin,
            Artifact pluginArtifact,
            DependencyFilter dependencyFilter,
            List<RemoteRepository> repositories,
            RepositorySystemSession session)
            throws PluginResolutionException {
        return resolveInternal(plugin, pluginArtifact, dependencyFilter, repositories, session);
    }

    private DependencyNode resolveInternal(
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

        DependencySelector dependencySelector = session.getDependencySelector();
        DependencyFilter resolutionFilter =
                AndDependencyFilter.newInstance(new ScopeDependencyFilter("provided", "test"), dependencyFilter);

        DependencyNode node;

        try {
            DefaultRepositorySystemSession pluginSession = new DefaultRepositorySystemSession(session);
            pluginSession.setDependencySelector(dependencySelector);
            pluginSession.setDependencyGraphTransformer(session.getDependencyGraphTransformer());

            CollectRequest request = new CollectRequest();
            request.setRequestContext(REPOSITORY_CONTEXT);
            request.setRepositories(repositories);
            request.setManagedDependencies(mavenManagedDependencies);
            Dependency rootDependency = new Dependency(pluginArtifact, null).setExclusions(mavenGlobalExclusions);
            request.setRoot(rootDependency);

            // plugin dependencies from POM
            ArtifactDescriptorResult descriptor = readArtifactDescriptor(trace, plugin, session, repositories);
            List<Dependency> dependencies = new ArrayList<>(descriptor.getDependencies());

            // extra plugin deps in project/build/plugins/plugin/dependencies
            for (org.apache.maven.model.Dependency dependency : plugin.getDependencies()) {
                Dependency pluginDep = RepositoryUtils.toDependency(dependency, session.getArtifactTypeRegistry());
                if (!JavaScopes.SYSTEM.equals(pluginDep.getScope())) {
                    pluginDep = pluginDep.setScope(JavaScopes.RUNTIME);
                }
                dependencies.add(pluginDep);
            }

            // alter them all: if any "core" dependency, set proper scope
            boolean mavenCompatAdded = false;
            for (Dependency dependency : dependencies) {
                String ga = dependency.getArtifact().getGroupId() + ":"
                        + dependency.getArtifact().getArtifactId();
                if (mavenCoreGAs.contains(ga)) {
                    dependency = dependency
                            .setScope(JavaScopes.PROVIDED)
                            .setArtifact(dependency.getArtifact().setVersion(mavenVersion));
                }
                Dependency otherDependency = otherGav(ga);
                if (otherDependency != null) {
                    request.addDependency(otherDependency);
                } else if (!mavenGoneCoreGAs.contains(ga)) {
                    request.addDependency(dependency);
                } else if (!mavenCompatAdded) {
                    request.addDependency(mavenCompat);
                    mavenCompatAdded = true;
                }
            }

            DependencyRequest depRequest = new DependencyRequest(request, resolutionFilter);
            depRequest.setTrace(trace);

            request.setTrace(RequestTrace.newChild(trace, depRequest));

            node = repoSystem.collectDependencies(pluginSession, request).getRoot();

            if (logger.isDebugEnabled()) {
                node.accept(new GraphLogger());
            }

            depRequest.setRoot(node);
            repoSystem.resolveDependencies(session, depRequest);
        } catch (ArtifactDescriptorException | DependencyCollectionException e) {
            throw new PluginResolutionException(plugin, e);
        } catch (DependencyResolutionException e) {
            throw new PluginResolutionException(plugin, e.getCause());
        }

        return node;
    }

    private Dependency otherGav(String ga) {
        for (Map.Entry<Dependency, Set<String>> entry : otherCoreGAVs.entrySet()) {
            if (entry.getValue().contains(ga)
                    || (entry.getKey().getArtifact().getGroupId() + ":"
                                    + entry.getKey().getArtifact().getArtifactId())
                            .equals(ga)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Keep this class in sync with org.apache.maven.project.DefaultProjectDependenciesResolver.GraphLogger
    class GraphLogger implements DependencyVisitor {

        private String indent = "";

        public boolean visitEnter(DependencyNode node) {
            StringBuilder buffer = new StringBuilder(128);
            buffer.append(indent);
            Dependency dep = node.getDependency();
            if (dep != null) {
                org.eclipse.aether.artifact.Artifact art = dep.getArtifact();

                buffer.append(art);
                if (StringUtils.isNotEmpty(dep.getScope())) {
                    buffer.append(':').append(dep.getScope());
                }

                if (dep.isOptional()) {
                    buffer.append(" (optional)");
                }

                // TODO We currently cannot tell which <dependencyManagement> section contained the management
                //      information. When the resolver provides this information, these log messages should be updated
                //      to contain it.
                if ((node.getManagedBits() & DependencyNode.MANAGED_SCOPE) == DependencyNode.MANAGED_SCOPE) {
                    final String premanagedScope = DependencyManagerUtils.getPremanagedScope(node);
                    buffer.append(" (scope managed from ");
                    buffer.append(Objects.toString(premanagedScope, "default"));
                    buffer.append(')');
                }

                if ((node.getManagedBits() & DependencyNode.MANAGED_VERSION) == DependencyNode.MANAGED_VERSION) {
                    final String premanagedVersion = DependencyManagerUtils.getPremanagedVersion(node);
                    buffer.append(" (version managed from ");
                    buffer.append(Objects.toString(premanagedVersion, "default"));
                    buffer.append(')');
                }

                if ((node.getManagedBits() & DependencyNode.MANAGED_OPTIONAL) == DependencyNode.MANAGED_OPTIONAL) {
                    final Boolean premanagedOptional = DependencyManagerUtils.getPremanagedOptional(node);
                    buffer.append(" (optionality managed from ");
                    buffer.append(Objects.toString(premanagedOptional, "default"));
                    buffer.append(')');
                }

                if ((node.getManagedBits() & DependencyNode.MANAGED_EXCLUSIONS) == DependencyNode.MANAGED_EXCLUSIONS) {
                    final Collection<org.eclipse.aether.graph.Exclusion> premanagedExclusions =
                            DependencyManagerUtils.getPremanagedExclusions(node);

                    buffer.append(" (exclusions managed from ");
                    buffer.append(Objects.toString(premanagedExclusions, "default"));
                    buffer.append(')');
                }

                if ((node.getManagedBits() & DependencyNode.MANAGED_PROPERTIES) == DependencyNode.MANAGED_PROPERTIES) {
                    final Map<String, String> premanagedProperties =
                            DependencyManagerUtils.getPremanagedProperties(node);

                    buffer.append(" (properties managed from ");
                    buffer.append(Objects.toString(premanagedProperties, "default"));
                    buffer.append(')');
                }
            }

            logger.debug(buffer.toString());
            indent += "   ";
            return true;
        }

        public boolean visitLeave(DependencyNode node) {
            indent = indent.substring(0, indent.length() - 3);
            return true;
        }
    }
}
