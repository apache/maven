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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultLifecycleBindingsInjectorTest {

    @Test
    void mergePluginManagementOnlyActivatesExecutionsFromTheSameLifecycle() {
        InputSource lifecycleSource = inputSource("lifecycle");
        InputSource managementSource = inputSource("plugin-management");

        PluginExecution lifecycleExecution = execution("default-clean", "clean", lifecycleSource);
        Plugin lifecyclePlugin = plugin("lifecycle-version", lifecycleExecution, lifecycleSource);
        lifecyclePlugin.setConfiguration(configuration("shared", "lifecycle", "lifecycle", "default"));

        PluginExecution sameLifecycleExecution = execution("managed-clean", "clean", managementSource);
        PluginExecution crossLifecycleExecution = execution("managed-initialize", "initialize", managementSource);
        PluginExecution defaultPhaseExecution = execution("managed-default-phase", null, managementSource);
        Plugin managedPlugin = plugin(
                "managed-version",
                managementSource,
                sameLifecycleExecution,
                crossLifecycleExecution,
                defaultPhaseExecution);
        managedPlugin.setConfiguration(configuration("shared", "managed", "managed", "configured"));
        managedPlugin.setExtensions(true);
        managedPlugin.setInherited(false);
        managedPlugin.addDependency(dependency("managed-dependency"));

        Model target = modelWithPluginManagement(managedPlugin);
        Model source = modelWithPlugin(lifecyclePlugin);

        new DefaultLifecycleBindingsInjector.LifecycleBindingsMerger(Map.of("clean", "clean", "initialize", "default"))
                .merge(target, source);

        Plugin result = target.getBuild().getPlugins().get(0);
        Xpp3Dom resultConfiguration = (Xpp3Dom) result.getConfiguration();

        assertEquals("managed-version", result.getVersion());
        assertEquals("managed", resultConfiguration.getChild("shared").getValue());
        assertEquals("configured", resultConfiguration.getChild("managed").getValue());
        assertEquals("default", resultConfiguration.getChild("lifecycle").getValue());
        assertEquals(3, result.getExecutions().size());
        assertEquals(
                Set.of("default-clean", "managed-clean", "managed-default-phase"),
                result.getExecutions().stream().map(PluginExecution::getId).collect(Collectors.toSet()));
        assertEquals(
                List.of("managed-dependency"),
                result.getDependencies().stream().map(Dependency::getArtifactId).toList());
        assertEquals("true", result.getExtensions());
        assertEquals("false", result.getInherited());
        assertEquals("plugin-management", result.getLocation("").getSource().getModelId());
        assertEquals(
                "plugin-management", result.getLocation("version").getSource().getModelId());
        assertEquals(
                "lifecycle",
                result.getExecutions().stream()
                        .filter(execution -> "default-clean".equals(execution.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getLocation("")
                        .getSource()
                        .getModelId());
    }

    private static Model modelWithPlugin(Plugin plugin) {
        Build build = new Build();
        build.addPlugin(plugin);
        Model model = new Model();
        model.setBuild(build);
        return model;
    }

    private static Model modelWithPluginManagement(Plugin plugin) {
        PluginManagement management = new PluginManagement();
        management.addPlugin(plugin);
        Build build = new Build();
        build.setPluginManagement(management);
        Model model = new Model();
        model.setBuild(build);
        return model;
    }

    private static Plugin plugin(String version, PluginExecution execution, InputSource source) {
        return plugin(version, source, execution);
    }

    private static Plugin plugin(String version, InputSource source, PluginExecution... executions) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId("maven-clean-plugin");
        plugin.setVersion(version);
        for (PluginExecution execution : executions) {
            plugin.addExecution(execution);
        }
        plugin.setLocation("", new InputLocation(1, 1, source));
        plugin.setLocation("version", new InputLocation(2, 1, source));
        return plugin;
    }

    private static PluginExecution execution(String id, String phase, InputSource source) {
        PluginExecution execution = new PluginExecution();
        execution.setId(id);
        execution.setPhase(phase);
        execution.addGoal("clean");
        execution.setLocation("", new InputLocation(3, 1, source));
        return execution;
    }

    private static Dependency dependency(String artifactId) {
        Dependency dependency = new Dependency();
        dependency.setGroupId("org.apache.maven.test");
        dependency.setArtifactId(artifactId);
        dependency.setVersion("1");
        return dependency;
    }

    private static Xpp3Dom configuration(String name, String value, String secondName, String secondValue) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value);
        configuration.addChild(child);
        child = new Xpp3Dom(secondName);
        child.setValue(secondValue);
        configuration.addChild(child);
        return configuration;
    }

    private static InputSource inputSource(String modelId) {
        InputSource source = new InputSource();
        source.setModelId(modelId);
        return source;
    }
}
