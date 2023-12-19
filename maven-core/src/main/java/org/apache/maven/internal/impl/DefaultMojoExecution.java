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
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.graph.DependencyNode;

public class DefaultMojoExecution implements MojoExecution {
    private final InternalSession session;
    private final org.apache.maven.plugin.MojoExecution delegate;

    public DefaultMojoExecution(InternalSession session, org.apache.maven.plugin.MojoExecution delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    public org.apache.maven.plugin.MojoExecution getDelegate() {
        return delegate;
    }

    @Override
    public Plugin getPlugin() {
        return new Plugin() {
            @Override
            public org.apache.maven.api.model.Plugin getModel() {
                return delegate.getPlugin().getDelegate();
            }

            @Override
            public PluginDescriptor getDescriptor() {
                return delegate.getMojoDescriptor().getPluginDescriptor().getPluginDescriptorV4();
            }

            @Override
            public List<Lifecycle> getLifecycles() {
                try {
                    return Collections.unmodifiableList(new ArrayList<>(delegate.getMojoDescriptor()
                            .getPluginDescriptor()
                            .getLifecycleMappings()
                            .values()));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to load plugin lifecycles", e);
                }
            }

            @Override
            public ClassLoader getClassLoader() {
                return delegate.getMojoDescriptor().getRealm();
            }

            @Override
            public Artifact getArtifact() {
                org.apache.maven.artifact.Artifact artifact =
                        delegate.getMojoDescriptor().getPluginDescriptor().getPluginArtifact();
                org.eclipse.aether.artifact.Artifact resolverArtifact = RepositoryUtils.toArtifact(artifact);
                return resolverArtifact != null ? session.getArtifact(resolverArtifact) : null;
            }

            @Override
            public Map<String, Dependency> getDependenciesMap() {
                DependencyNode resolverNode =
                        delegate.getMojoDescriptor().getPluginDescriptor().getDependencyNode();
                DefaultNode node = new DefaultNode(session, resolverNode, false);
                return Collections.unmodifiableMap(node.stream()
                        .filter(Objects::nonNull)
                        .map(Node::getDependency)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(d -> d.getGroupId() + ":" + d.getArtifactId(), d -> d)));
            }
        };
    }

    @Override
    public PluginExecution getModel() {
        return delegate.getPlugin().getExecutions().stream()
                .filter(pe -> Objects.equals(pe.getId(), getExecutionId()))
                .findFirst()
                .map(org.apache.maven.model.PluginExecution::getDelegate)
                .orElse(null);
    }

    @Override
    public MojoDescriptor getDescriptor() {
        return delegate.getMojoDescriptor().getMojoDescriptorV4();
    }

    @Override
    public String getLifecyclePhase() {
        return delegate.getLifecyclePhase();
    }

    @Override
    public String getExecutionId() {
        return delegate.getExecutionId();
    }

    @Override
    public String getGoal() {
        return delegate.getGoal();
    }

    @Override
    public Optional<XmlNode> getConfiguration() {
        return Optional.of(delegate.getConfiguration()).map(Xpp3Dom::getDom);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
