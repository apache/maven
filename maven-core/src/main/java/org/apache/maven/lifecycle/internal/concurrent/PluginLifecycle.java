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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

class PluginLifecycle implements Lifecycle {
    private final org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle lifecycleOverlay;
    private final PluginDescriptor pluginDescriptor;

    PluginLifecycle(
            org.apache.maven.api.plugin.descriptor.lifecycle.Lifecycle lifecycleOverlay,
            PluginDescriptor pluginDescriptor) {
        this.lifecycleOverlay = lifecycleOverlay;
        this.pluginDescriptor = pluginDescriptor;
    }

    @Override
    public String id() {
        return lifecycleOverlay.getId();
    }

    @Override
    public Collection<Phase> phases() {
        return lifecycleOverlay.getPhases().stream()
                .map(phase -> new Phase() {
                    @Override
                    public String name() {
                        return phase.getId();
                    }

                    @Override
                    public List<Plugin> plugins() {
                        return Collections.singletonList(Plugin.newBuilder()
                                .groupId(pluginDescriptor.getGroupId())
                                .artifactId(pluginDescriptor.getArtifactId())
                                .version(pluginDescriptor.getVersion())
                                .configuration(phase.getConfiguration())
                                .executions(phase.getExecutions().stream()
                                        .map(exec -> org.apache.maven.api.model.PluginExecution.newBuilder()
                                                .goals(exec.getGoals())
                                                .configuration(exec.getConfiguration())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build());
                    }

                    @Override
                    public Collection<Link> links() {
                        return Collections.emptyList();
                    }

                    @Override
                    public List<Phase> phases() {
                        return Collections.emptyList();
                    }

                    @Override
                    public Stream<Phase> allPhases() {
                        return Stream.concat(Stream.of(this), phases().stream().flatMap(Phase::allPhases));
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Alias> aliases() {
        return Collections.emptyList();
    }
}
