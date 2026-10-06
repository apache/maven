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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Node;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.impl.DefaultNode;
import org.eclipse.aether.graph.DependencyNode;

import static java.util.Objects.requireNonNull;

/**
 * Immutable snapshot of a mojo execution, captured at the point when execution begins
 * (after configuration merging, descriptor resolution, and lifecycle phase assignment are complete).
 * All state is copied at construction time; no reference to the mutable legacy
 * {@link org.apache.maven.plugin.MojoExecution} is retained after construction.
 */
public class DefaultMojoExecution implements MojoExecution {

    private final Plugin plugin;
    private final Optional<PluginExecution> model;
    private final MojoDescriptor descriptor;
    private final String executionId;
    private final String goal;
    private final String lifecyclePhase;
    private final XmlNode configuration;

    public DefaultMojoExecution(InternalMavenSession session, org.apache.maven.plugin.MojoExecution delegate) {
        requireNonNull(session, "session");
        requireNonNull(delegate, "delegate");
        this.descriptor = requireNonNull(delegate.getMojoDescriptor(), "delegate.mojoDescriptor")
                .getMojoDescriptorV4();
        this.executionId = delegate.getExecutionId();
        this.goal = delegate.getGoal();
        this.lifecyclePhase = delegate.getLifecyclePhase();
        this.configuration = delegate.getConfiguration() != null
                ? delegate.getConfiguration().getDom()
                : null;
        this.plugin = buildPlugin(session, delegate);
        this.model = buildModel(delegate);
    }

    private static Plugin buildPlugin(InternalMavenSession session, org.apache.maven.plugin.MojoExecution delegate) {
        org.apache.maven.plugin.descriptor.MojoDescriptor legacyDescriptor = delegate.getMojoDescriptor();
        org.apache.maven.plugin.descriptor.PluginDescriptor legacyPluginDescriptor =
                legacyDescriptor.getPluginDescriptor();
        PluginDescriptor pluginDescriptorV4 = legacyPluginDescriptor.getPluginDescriptorV4();

        ClassLoader classLoader = legacyDescriptor.getRealm();

        org.apache.maven.artifact.Artifact legacyArtifact = legacyPluginDescriptor.getPluginArtifact();
        org.eclipse.aether.artifact.Artifact resolverArtifact = RepositoryUtils.toArtifact(legacyArtifact);
        Artifact artifact = resolverArtifact != null ? session.getArtifact(resolverArtifact) : null;

        DependencyNode resolverNode = legacyPluginDescriptor.getDependencyNode();
        Map<String, Dependency> dependenciesMap = resolverNode != null
                ? Collections.unmodifiableMap(new DefaultNode(session, resolverNode, false)
                        .stream()
                                .filter(Objects::nonNull)
                                .map(Node::getDependency)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(
                                        d -> d.getGroupId() + ":" + d.getArtifactId(), d -> d, (a, b) -> a)))
                : Collections.emptyMap();

        org.apache.maven.api.model.Plugin modelPlugin =
                delegate.getPlugin() != null ? delegate.getPlugin().getDelegate() : null;

        return new Plugin() {
            @Override
            public org.apache.maven.api.model.Plugin getModel() {
                return modelPlugin;
            }

            @Override
            public PluginDescriptor getDescriptor() {
                return pluginDescriptorV4;
            }

            @Override
            public List<Lifecycle> getLifecycles() {
                try {
                    return Collections.unmodifiableList(new ArrayList<>(
                            legacyPluginDescriptor.getLifecycleMappings().values()));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to load plugin lifecycles", e);
                }
            }

            @Override
            public ClassLoader getClassLoader() {
                return classLoader;
            }

            @Override
            public Artifact getArtifact() {
                return artifact;
            }

            @Override
            public Map<String, Dependency> getDependenciesMap() {
                return dependenciesMap;
            }
        };
    }

    private static Optional<PluginExecution> buildModel(org.apache.maven.plugin.MojoExecution delegate) {
        if (delegate.getPlugin() == null) {
            return Optional.empty();
        }
        String id = delegate.getExecutionId();
        return delegate.getPlugin().getExecutions().stream()
                .filter(pe -> Objects.equals(pe.getId(), id))
                .findFirst()
                .map(org.apache.maven.model.PluginExecution::getDelegate);
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public Optional<PluginExecution> model() {
        return model;
    }

    @Override
    public MojoDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public String executionId() {
        return executionId;
    }

    @Override
    public String goal() {
        return goal;
    }

    @Override
    public String lifecyclePhase() {
        return lifecyclePhase;
    }

    @Override
    public Optional<XmlNode> configuration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public String toString() {
        return descriptor.getId() + " {execution: " + executionId + '}';
    }
}
