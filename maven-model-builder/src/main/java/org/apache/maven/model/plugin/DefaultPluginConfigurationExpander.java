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
package org.apache.maven.model.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles expansion of general build plugin configuration into individual executions.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultPluginConfigurationExpander implements PluginConfigurationExpander {

    @Override
    public void expandPluginConfiguration(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        Build build = model.getBuild();

        if (build != null) {
            expand(build.getPlugins());

            PluginManagement pluginManagement = build.getPluginManagement();

            if (pluginManagement != null) {
                expand(pluginManagement.getPlugins());
            }
        }
    }

    private void expand(List<Plugin> plugins) {
        for (Plugin plugin : plugins) {
            XmlNode pluginConfiguration = plugin.getDelegate().getConfiguration();

            if (pluginConfiguration != null) {
                for (PluginExecution execution : plugin.getExecutions()) {
                    XmlNode executionConfiguration = execution.getDelegate().getConfiguration();
                    executionConfiguration = XmlNode.merge(executionConfiguration, pluginConfiguration);
                    execution.update(execution.getDelegate().withConfiguration(executionConfiguration));
                }
            }
        }
    }
}
