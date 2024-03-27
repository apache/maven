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
package org.apache.maven.internal.impl.model;

import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.model.*;

/**
 * Handles injection of plugin executions induced by the lifecycle bindings for a packaging.
 *
 */
@Named
@Singleton
public class DefaultLifecycleBindingsInjector implements LifecycleBindingsInjector {

    private final LifecycleBindingsMerger merger = new LifecycleBindingsMerger();

    private final PackagingRegistry packagingRegistry;

    @Inject
    public DefaultLifecycleBindingsInjector(PackagingRegistry packagingRegistry) {
        this.packagingRegistry = packagingRegistry;
    }

    public Model injectLifecycleBindings(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        String packagingId = model.getPackaging();
        Packaging packaging = packagingRegistry.lookup(packagingId).orElse(null);
        if (packaging == null) {
            problems.add(
                    Severity.ERROR, Version.BASE, "Unknown packaging: " + packaging, model.getLocation("packaging"));
            return model;
        } else {
            Model lifecycleModel = Model.newBuilder()
                    .build(Build.newBuilder()
                            .plugins(packaging.plugins().getPlugins())
                            .build())
                    .build();
            return merger.merge(model, lifecycleModel);
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

            Map<Object, Object> context = Collections.singletonMap(
                    PLUGIN_MANAGEMENT, target.getBuild().getPluginManagement());

            Build.Builder builder = Build.newBuilder(target.getBuild());
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
