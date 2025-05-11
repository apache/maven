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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Packaging;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.model.LifecycleBindingsInjector;

/**
 * Handles injection of plugin executions induced by the lifecycle bindings for a packaging.
 *
 */
@Named
@Singleton
public class DefaultLifecycleBindingsInjector implements LifecycleBindingsInjector {

    private final LifecycleBindingsMerger merger = new LifecycleBindingsMerger();

    private final LifecycleRegistry lifecycleRegistry;
    private final PackagingRegistry packagingRegistry;

    @Inject
    public DefaultLifecycleBindingsInjector(LifecycleRegistry lifecycleRegistry, PackagingRegistry packagingRegistry) {
        this.lifecycleRegistry = lifecycleRegistry;
        this.packagingRegistry = packagingRegistry;
    }

    public Model injectLifecycleBindings(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        String packagingId = model.getPackaging();
        Packaging packaging = packagingRegistry.lookup(packagingId).orElse(null);
        if (packaging == null) {
            problems.add(
                    Severity.ERROR, Version.BASE, "Unknown packaging: " + packagingId, model.getLocation("packaging"));
            return model;
        } else {
            Map<String, PluginContainer> plugins = new HashMap<>(packaging.plugins());
            lifecycleRegistry.stream()
                    .filter(lf -> !plugins.containsKey(lf.id()))
                    .forEach(lf -> plugins.put(
                            lf.id(),
                            PluginContainer.newBuilder()
                                    .plugins(lf.phases().stream()
                                            .flatMap(phase -> phase.plugins().stream())
                                            .toList())
                                    .build()));
            Map<Plugin, Plugin> allPlugins = new LinkedHashMap<>();
            plugins.values().stream().flatMap(pc -> pc.getPlugins().stream()).forEach(p -> addPlugin(allPlugins, p));
            Model lifecycleModel = Model.newBuilder()
                    .build(Build.newBuilder().plugins(allPlugins.values()).build())
                    .build();
            return merger.merge(model, lifecycleModel);
        }
    }

    private void addPlugin(Map<Plugin, Plugin> plugins, Plugin plugin) {
        Plugin cur = plugins.putIfAbsent(plugin, plugin);
        if (cur != null) {
            Map<String, PluginExecution> execs = new LinkedHashMap<>();
            cur.getExecutions().forEach(e -> execs.put(e.getId(), e));
            plugin.getExecutions().forEach(e -> {
                int i = 0;
                String id = e.getId();
                while (execs.putIfAbsent(id, e.withId(id)) != null) {
                    id = e.getId() + "-" + (++i);
                }
            });
            Plugin merged = cur.withExecutions(execs.values());
            plugins.put(merged, merged);
        }
    }

    /**
     *  The domain-specific model merger for lifecycle bindings
     */
    protected static class LifecycleBindingsMerger extends MavenModelMerger {

        private static final String PLUGIN_MANAGEMENT = "plugin-management";

        public Model merge(Model target, Model source) {
            Build targetBuild = target.getBuild();
            if (targetBuild == null) {
                targetBuild = Build.newInstance();
            }

            Map<Object, Object> context =
                    Collections.singletonMap(PLUGIN_MANAGEMENT, targetBuild.getPluginManagement());

            Build.Builder builder = Build.newBuilder(targetBuild);
            mergePluginContainer_Plugins(builder, targetBuild, source.getBuild(), false, context);

            return target.withBuild(builder.build());
        }

        @SuppressWarnings({"checkstyle:methodname"})
        @Override
        protected void mergePluginContainer_Plugins(
                PluginContainer.Builder builder,
                PluginContainer target,
                PluginContainer source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            List<Plugin> src = source.getPlugins();
            if (!src.isEmpty()) {
                List<Plugin> tgt = target.getPlugins();

                Map<Object, Plugin> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

                for (Plugin element : tgt) {
                    Object key = getPluginKey().apply(element);
                    merged.put(key, element);
                }

                Map<Object, Plugin> added = new LinkedHashMap<>();

                for (Plugin element : src) {
                    Object key = getPluginKey().apply(element);
                    Plugin existing = merged.get(key);
                    if (existing != null) {
                        element = mergePlugin(existing, element, sourceDominant, context);
                    } else {
                        added.put(key, element);
                    }
                    merged.put(key, element);
                }

                if (!added.isEmpty()) {
                    PluginManagement pluginMgmt = (PluginManagement) context.get(PLUGIN_MANAGEMENT);
                    if (pluginMgmt != null) {
                        for (Plugin managedPlugin : pluginMgmt.getPlugins()) {
                            Object key = getPluginKey().apply(managedPlugin);
                            Plugin addedPlugin = added.get(key);
                            if (addedPlugin != null) {
                                Plugin plugin =
                                        mergePlugin(managedPlugin, addedPlugin, sourceDominant, Collections.emptyMap());
                                merged.put(key, plugin);
                            }
                        }
                    }
                }

                List<Plugin> result = new ArrayList<>(merged.values());

                builder.plugins(result);
            }
        }

        @Override
        protected void mergePluginExecution_Priority(
                PluginExecution.Builder builder,
                PluginExecution target,
                PluginExecution source,
                boolean sourceDominant,
                Map<Object, Object> context) {
            if (target.getPriority() > source.getPriority()) {
                builder.priority(source.getPriority());
                builder.location("priority", source.getLocation("priority"));
            }
        }
        // mergePluginExecution_Priority( builder, target, source, sourceDominant, context );
    }
}
