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
package org.apache.maven.model.composition;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles the import of plugin management from other models into the target model.
 */
@Named
@Singleton
public class DefaultPluginManagementImporter implements PluginManagementImporter {

    @Override
    public Model importManagement(
            Model target,
            List<? extends PluginManagement> sources,
            ModelBuildingRequest request,
            ModelProblemCollector problems) {
        if (sources != null && !sources.isEmpty()) {
            Map<String, Plugin> plugins = new LinkedHashMap<>();

            Build build = target.getBuild();
            if (build == null) {
                build = Build.newInstance();
            }

            PluginManagement plgMgmt = build.getPluginManagement();
            if (plgMgmt != null) {
                for (Plugin plugin : plgMgmt.getPlugins()) {
                    plugins.put(plugin.getKey(), plugin);
                }
            } else {
                plgMgmt = PluginManagement.newInstance();
            }

            for (PluginManagement source : sources) {
                for (Plugin plugin : source.getPlugins()) {
                    String key = plugin.getKey();
                    plugins.putIfAbsent(key, plugin);
                }
            }

            return target.withBuild(build.withPluginManagement(plgMgmt.withPlugins(plugins.values())));
        }
        return target;
    }
}
