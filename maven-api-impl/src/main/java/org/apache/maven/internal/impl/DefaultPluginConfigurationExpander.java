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
import java.util.List;
import java.util.function.Function;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.PluginConfigurationExpander;
import org.apache.maven.api.xml.XmlNode;

/**
 * Handles expansion of general build plugin configuration into individual executions.
 *
 */
@Named
@Singleton
public class DefaultPluginConfigurationExpander implements PluginConfigurationExpander {

    @Override
    public Model expandPluginConfiguration(Model model, ModelBuilderRequest request, ModelProblemCollector problems) {
        Build build = model.getBuild();
        if (build != null) {
            build = build.withPlugins(expandPlugin(build.getPlugins()));
            PluginManagement pluginManagement = build.getPluginManagement();
            if (pluginManagement != null) {
                build = build.withPluginManagement(
                        pluginManagement.withPlugins(expandPlugin(pluginManagement.getPlugins())));
            }
            model = model.withBuild(build);
        }
        Reporting reporting = model.getReporting();
        if (reporting != null) {
            expandReport(reporting.getPlugins());
        }
        return model.withBuild(build);
    }

    private List<Plugin> expandPlugin(List<Plugin> oldPlugins) {
        return map(oldPlugins, plugin -> {
            XmlNode pluginConfiguration = plugin.getConfiguration();
            if (pluginConfiguration != null) {
                return plugin.withExecutions(map(
                        plugin.getExecutions(),
                        execution -> execution.withConfiguration(
                                XmlNode.merge(execution.getConfiguration(), pluginConfiguration))));
            } else {
                return plugin;
            }
        });
    }

    private List<ReportPlugin> expandReport(List<ReportPlugin> oldPlugins) {
        return map(oldPlugins, plugin -> {
            XmlNode pluginConfiguration = plugin.getConfiguration();
            if (pluginConfiguration != null) {
                return plugin.withReportSets(map(
                        plugin.getReportSets(),
                        report -> report.withConfiguration(
                                XmlNode.merge(report.getConfiguration(), pluginConfiguration))));
            } else {
                return plugin;
            }
        });
    }

    static <T> List<T> map(List<T> list, Function<T, T> mapper) {
        List<T> newList = list;
        for (int i = 0; i < newList.size(); i++) {
            T oldT = newList.get(i);
            T newT = mapper.apply(oldT);
            if (newT != oldT) {
                if (newList == list) {
                    newList = new ArrayList<>(list);
                }
                newList.set(i, newT);
            }
        }
        return newList;
    }
}
