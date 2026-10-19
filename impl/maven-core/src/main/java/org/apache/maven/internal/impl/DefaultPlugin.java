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
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.Node;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.PluginRealm;
import org.apache.maven.api.plugin.descriptor.PluginDescriptor;
import org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.impl.DefaultNode;
import org.eclipse.aether.graph.DependencyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

final class DefaultPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPlugin.class);

    private final org.apache.maven.api.model.Plugin model;
    private final PluginDescriptor descriptor;
    private final List<Lifecycle> lifecycles;
    private final PluginRealm realm;
    private final Artifact artifact;
    private final Map<String, Dependency> dependencies;

    DefaultPlugin(
            InternalMavenSession session,
            org.apache.maven.model.Plugin model,
            org.apache.maven.plugin.descriptor.PluginDescriptor descriptor,
            Lookup lookup) {
        requireNonNull(session, "session");
        requireNonNull(descriptor, "descriptor");

        this.model = model != null ? model.getDelegate() : null;
        this.descriptor = requireNonNull(descriptor.getPluginDescriptorV4(), "descriptor.pluginDescriptorV4");
        this.realm = new DefaultPluginRealm(descriptor.getClassRealm(), lookup);

        org.apache.maven.artifact.Artifact legacyArtifact = descriptor.getPluginArtifact();
        org.eclipse.aether.artifact.Artifact resolverArtifact = RepositoryUtils.toArtifact(legacyArtifact);
        this.artifact = resolverArtifact != null ? session.getArtifact(resolverArtifact) : null;

        DependencyNode resolverNode = descriptor.getDependencyNode();
        this.dependencies = resolverNode != null
                ? Collections.unmodifiableMap(new DefaultNode(session, resolverNode, false)
                        .stream()
                                .filter(Objects::nonNull)
                                .map(Node::getDependency)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(
                                        d -> d.getGroupId() + ":" + d.getArtifactId(), d -> d, (a, b) -> a)))
                : Collections.emptyMap();

        List<Lifecycle> lifecycleMappings;
        try {
            lifecycleMappings = Collections.unmodifiableList(
                    new ArrayList<>(descriptor.getLifecycleMappings().values()));
        } catch (Exception e) {
            LOGGER.debug("Unable to load lifecycle mappings for {}: {}", descriptor.getId(), e.getMessage());
            lifecycleMappings = Collections.emptyList();
        }
        this.lifecycles = lifecycleMappings;
    }

    @Override
    public org.apache.maven.api.model.Plugin getModel() {
        return model;
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public List<Lifecycle> getLifecycles() {
        return lifecycles;
    }

    @Override
    public PluginRealm getRealm() {
        return realm;
    }

    @Override
    public Artifact getArtifact() {
        return artifact;
    }

    @Override
    public Map<String, Dependency> getDependenciesMap() {
        return dependencies;
    }
}
