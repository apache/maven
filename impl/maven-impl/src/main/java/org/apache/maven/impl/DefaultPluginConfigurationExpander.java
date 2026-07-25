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
package org.apache.maven.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

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
import org.apache.maven.api.xml.XmlService;

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
            List<Plugin> expandedPlugins = expandPlugin(build.getPlugins());
            PluginManagement pluginManagement = build.getPluginManagement();
            List<Plugin> expandedMgmtPlugins =
                    pluginManagement != null ? expandPlugin(pluginManagement.getPlugins()) : null;

            boolean buildModified = expandedPlugins != build.getPlugins()
                    || (expandedMgmtPlugins != null && expandedMgmtPlugins != pluginManagement.getPlugins());

            if (buildModified) {
                Build.Builder bb = Build.newBuilder(build);
                if (expandedPlugins != build.getPlugins()) {
                    bb.plugins(expandedPlugins);
                }
                if (expandedMgmtPlugins != null && expandedMgmtPlugins != pluginManagement.getPlugins()) {
                    bb.pluginManagement(PluginManagement.newBuilder(pluginManagement)
                            .plugins(expandedMgmtPlugins)
                            .build());
                }
                build = bb.build();
            }
        }
        Reporting reporting = model.getReporting();
        List<ReportPlugin> expandedReportPlugins = null;
        if (reporting != null) {
            expandedReportPlugins = expandReport(reporting.getPlugins());
        }
        boolean modelModified = build != model.getBuild()
                || (expandedReportPlugins != null && expandedReportPlugins != reporting.getPlugins());
        if (modelModified) {
            Model.Builder mb = Model.newBuilder(model);
            if (build != model.getBuild()) {
                mb.build(build);
            }
            if (expandedReportPlugins != null && expandedReportPlugins != reporting.getPlugins()) {
                mb.reporting(Reporting.newBuilder(reporting)
                        .plugins(expandedReportPlugins)
                        .build());
            }
            return mb.build();
        }
        return model;
    }

    private List<Plugin> expandPlugin(List<Plugin> oldPlugins) {
        return map(oldPlugins, plugin -> {
            XmlNode pluginConfiguration = plugin.getConfiguration();
            if (pluginConfiguration != null) {
                return plugin.withExecutions(map(plugin.getExecutions(), execution -> {
                    return execution.withConfiguration(
                            XmlService.merge(execution.getConfiguration(), pluginConfiguration));
                }));
            } else {
                return plugin;
            }
        });
    }

    private List<ReportPlugin> expandReport(List<ReportPlugin> oldPlugins) {
        return map(oldPlugins, plugin -> {
            XmlNode pluginConfiguration = plugin.getConfiguration();
            if (pluginConfiguration != null) {
                return plugin.withReportSets(map(plugin.getReportSets(), report -> {
                    return report.withConfiguration(XmlService.merge(report.getConfiguration(), pluginConfiguration));
                }));
            } else {
                return plugin;
            }
        });
    }

    static <T> List<T> map(List<T> list, UnaryOperator<T> mapper) {
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
