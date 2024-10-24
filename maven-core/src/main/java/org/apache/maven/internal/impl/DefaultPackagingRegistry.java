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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.Type;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.spi.PackagingProvider;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: this is session scoped as SPI can contribute.
 */
@Named
@Singleton
public class DefaultPackagingRegistry
        extends ExtensibleEnumRegistries.DefaultExtensibleEnumRegistry<Packaging, PackagingProvider>
        implements PackagingRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPackagingRegistry.class);

    private final Lookup lookup;

    private final TypeRegistry typeRegistry;

    @Inject
    public DefaultPackagingRegistry(Lookup lookup, TypeRegistry typeRegistry, List<PackagingProvider> providers) {
        super(providers);
        this.lookup = lookup;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public Optional<Packaging> lookup(String id) {
        id = id.toLowerCase(Locale.ROOT);
        // TODO: we should be able to inject a Map<String, LifecycleMapping> directly,
        // however, SISU visibility filtering can only happen when an explicit
        // lookup is performed. The whole problem here is caused by "project extensions"
        // which are bound to a project's classloader, without any clear definition
        // of a "project scope"
        LifecycleMapping lifecycleMapping =
                lookup.lookupOptional(LifecycleMapping.class, id).orElse(null);
        if (lifecycleMapping == null) {
            return Optional.empty();
        }
        Type type = typeRegistry.lookup(id).orElse(null);
        if (type == null) {
            return Optional.empty();
        }
        return Optional.of(new DefaultPackaging(id, type, getPlugins(lifecycleMapping)));
    }

    private Map<String, PluginContainer> getPlugins(LifecycleMapping lifecycleMapping) {
        Map<String, PluginContainer> lfs = new HashMap<>();
        lifecycleMapping.getLifecycles().forEach((id, lifecycle) -> {
            Map<String, Plugin> plugins = new HashMap<>();
            lifecycle
                    .getLifecyclePhases()
                    .forEach((phase, lifecyclePhase) -> parseLifecyclePhaseDefinitions(plugins, phase, lifecyclePhase));
            lfs.put(id, PluginContainer.newBuilder().plugins(plugins.values()).build());
        });
        return lfs;
    }

    static void parseLifecyclePhaseDefinitions(Map<String, Plugin> plugins, String phase, LifecyclePhase goals) {
        InputLocation location = DefaultLifecycleRegistry.DEFAULT_LIFECYCLE_INPUT_LOCATION;

        List<LifecycleMojo> mojos = goals.getMojos();
        if (mojos != null) {
            for (int i = 0; i < mojos.size(); i++) {
                LifecycleMojo mojo = mojos.get(i);

                // Compute goal coordinates
                String groupId, artifactId, version, goal;
                String[] p = mojo.getGoal().trim().split(":");
                if (p.length == 3) {
                    // <groupId>:<artifactId>:<goal>
                    groupId = p[0];
                    artifactId = p[1];
                    version = null;
                    goal = p[2];
                } else if (p.length == 4) {
                    // <groupId>:<artifactId>:<version>:<goal>
                    groupId = p[0];
                    artifactId = p[1];
                    version = p[2];
                    goal = p[3];
                } else {
                    // invalid
                    LOGGER.warn(
                            "Ignored invalid goal specification '{}' from lifecycle mapping for phase {}",
                            mojo.getGoal(),
                            phase);
                    continue;
                }

                String key = groupId + ":" + artifactId;

                // Build plugin
                List<PluginExecution> execs = new ArrayList<>();
                List<Dependency> deps = new ArrayList<>();

                Plugin existing = plugins.get(key);
                if (existing != null) {
                    if (version == null) {
                        version = existing.getVersion();
                    }
                    execs.addAll(existing.getExecutions());
                    deps.addAll(existing.getDependencies());
                }

                PluginExecution execution = PluginExecution.newBuilder()
                        .id(getExecutionId(existing, goal))
                        .priority(i - mojos.size())
                        .phase(phase)
                        .goals(List.of(goal))
                        .configuration(mojo.getConfiguration())
                        .location("", location)
                        .location("id", location)
                        .location("phase", location)
                        .location("goals", location)
                        .build();
                execs.add(execution);

                if (mojo.getDependencies() != null) {
                    mojo.getDependencies().forEach(d -> deps.add(d.getDelegate()));
                }

                Plugin plugin = Plugin.newBuilder()
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version)
                        .location("", location)
                        .location("groupId", location)
                        .location("artifactId", location)
                        .location("version", location)
                        .executions(execs)
                        .dependencies(deps)
                        .build();

                plugins.put(key, plugin);
            }
        }
    }

    private static String getExecutionId(Plugin plugin, String goal) {
        Set<String> existingIds = plugin != null
                ? plugin.getExecutions().stream().map(PluginExecution::getId).collect(Collectors.toSet())
                : Set.of();
        String base = "default-" + goal;
        String id = base;
        for (int index = 1; existingIds.contains(id); index++) {
            id = base + '-' + index;
        }
        return id;
    }

    private record DefaultPackaging(String id, Type type, Map<String, PluginContainer> plugins) implements Packaging {}
}
