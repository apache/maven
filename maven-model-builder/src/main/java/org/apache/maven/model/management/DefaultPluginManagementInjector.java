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
package org.apache.maven.model.management;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.merge.MavenModelMerger;

/**
 * Handles injection of plugin management into the model.
 *
 * @author Benjamin Bentmann
 */
@SuppressWarnings({"checkstyle:methodname"})
@Named
@Singleton
public class DefaultPluginManagementInjector implements PluginManagementInjector {

    private ManagementModelMerger merger = new ManagementModelMerger();

    @Override
    public void injectManagement(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        merger.mergeManagedBuildPlugins(model);
    }

    /**
     * ManagementModelMerger
     */
    protected static class ManagementModelMerger extends MavenModelMerger {

        public void mergeManagedBuildPlugins(Model model) {
            Build build = model.getBuild();
            if (build != null) {
                PluginManagement pluginManagement = build.getPluginManagement();
                if (pluginManagement != null) {
                    mergePluginContainerPlugins(build, pluginManagement);
                }
            }
        }

        private void mergePluginContainerPlugins(PluginContainer target, PluginContainer source) {
            List<Plugin> src = source.getPlugins();
            if (!src.isEmpty()) {
                List<Plugin> tgt = target.getPlugins();

                Map<Object, Plugin> managedPlugins = new LinkedHashMap<>(src.size() * 2);

                Map<Object, Object> context = Collections.emptyMap();

                for (Plugin element : src) {
                    Object key = getPluginKey(element);
                    managedPlugins.put(key, element);
                }

                for (Plugin element : tgt) {
                    Object key = getPluginKey(element);
                    Plugin managedPlugin = managedPlugins.get(key);
                    if (managedPlugin != null) {
                        mergePlugin(element, managedPlugin, false, context);
                    }
                }
            }
        }

        @Override
        protected void mergePlugin_Executions(
                Plugin target, Plugin source, boolean sourceDominant, Map<Object, Object> context) {
            List<PluginExecution> src = source.getExecutions();
            if (!src.isEmpty()) {
                List<PluginExecution> tgt = target.getExecutions();

                Map<Object, PluginExecution> merged = new LinkedHashMap<>((src.size() + tgt.size()) * 2);

                for (PluginExecution element : src) {
                    Object key = getPluginExecutionKey(element);
                    merged.put(key, element.clone());
                }

                for (PluginExecution element : tgt) {
                    Object key = getPluginExecutionKey(element);
                    PluginExecution existing = merged.get(key);
                    if (existing != null) {
                        mergePluginExecution(element, existing, sourceDominant, context);
                    }
                    merged.put(key, element);
                }

                target.setExecutions(new ArrayList<>(merged.values()));
            }
        }
    }
}
