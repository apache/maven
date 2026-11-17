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

import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.plugin.descriptor.MojoDescriptor;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.xml.XmlNode;

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
    private final Optional<String> executionId;
    private final String goal;
    private final Optional<String> lifecyclePhase;
    private final XmlNode configuration;

    public DefaultMojoExecution(InternalMavenSession session, org.apache.maven.plugin.MojoExecution delegate) {
        requireNonNull(session, "session");
        requireNonNull(delegate, "delegate");
        this.descriptor = requireNonNull(delegate.getMojoDescriptor(), "delegate.mojoDescriptor")
                .getMojoDescriptorV4();
        this.executionId = Optional.ofNullable(delegate.getExecutionId());
        this.goal = requireNonNull(delegate.getGoal(), "delegate.goal");
        this.lifecyclePhase = Optional.ofNullable(delegate.getLifecyclePhase());
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
        return new DefaultPlugin(
                session, delegate.getPlugin(), legacyPluginDescriptor, session.getService(Lookup.class));
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
    public Optional<String> executionId() {
        return executionId;
    }

    @Override
    public String goal() {
        return goal;
    }

    @Override
    public Optional<String> lifecyclePhase() {
        return lifecyclePhase;
    }

    @Override
    public Optional<XmlNode> configuration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public String toString() {
        return descriptor.getId() + " {execution: " + executionId.orElse("<cli>") + '}';
    }
}
