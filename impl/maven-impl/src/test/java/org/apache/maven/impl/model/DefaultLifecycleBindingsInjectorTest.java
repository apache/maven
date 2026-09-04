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

import java.util.List;

import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginExecution;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.xml.XmlNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefaultLifecycleBindingsInjectorTest {

    @Test
    void mergePluginManagementDoesNotActivateManagedExecutions() {
        InputSource lifecycleSource = InputSource.of("lifecycle", "lifecycle.xml");
        InputSource managementSource = InputSource.of("plugin-management", "pom.xml");

        PluginExecution lifecycleExecution = execution("default-clean", "clean", lifecycleSource);
        Plugin lifecyclePlugin = plugin("lifecycle-version", lifecycleExecution, lifecycleSource)
                .withConfiguration(configuration("shared", "lifecycle", "lifecycle", "default"));

        PluginExecution managedExecution = execution("custom-clean", "initialize", managementSource);
        Plugin managedPlugin = Plugin.newBuilder(plugin("managed-version", managedExecution, managementSource))
                .configuration(configuration("shared", "managed", "managed", "configured"))
                .extensions("true")
                .inherited("false")
                .dependencies(List.of(Dependency.newBuilder()
                        .groupId("org.apache.maven.test")
                        .artifactId("managed-dependency")
                        .version("1")
                        .build()))
                .build();

        Model target = Model.newBuilder()
                .build(Build.newBuilder()
                        .pluginManagement(PluginManagement.newBuilder()
                                .plugins(List.of(managedPlugin))
                                .build())
                        .build())
                .build();
        Model source = Model.newBuilder()
                .build(Build.newBuilder().plugins(List.of(lifecyclePlugin)).build())
                .build();

        Model resultModel = new DefaultLifecycleBindingsInjector.LifecycleBindingsMerger().merge(target, source);

        Plugin result = resultModel.getBuild().getPlugins().get(0);
        XmlNode resultConfiguration = result.getConfiguration();

        assertEquals("managed-version", result.getVersion());
        assertEquals("managed", resultConfiguration.child("shared").value());
        assertEquals("configured", resultConfiguration.child("managed").value());
        assertEquals("default", resultConfiguration.child("lifecycle").value());
        assertEquals(
                List.of("default-clean"),
                result.getExecutions().stream().map(PluginExecution::getId).toList());
        assertEquals(List.of(), result.getDependencies());
        assertNull(result.getExtensions());
        assertNull(result.getInherited());
        assertEquals("lifecycle", result.getLocation("").getSource().getModelId());
        assertEquals(
                "plugin-management", result.getLocation("version").getSource().getModelId());
        assertEquals(
                "lifecycle",
                result.getExecutions().get(0).getLocation("").getSource().getModelId());
    }

    private static Plugin plugin(String version, PluginExecution execution, InputSource source) {
        return Plugin.newBuilder()
                .artifactId("maven-clean-plugin")
                .version(version)
                .executions(List.of(execution))
                .location("", InputLocation.of(1, 1, source))
                .location("version", InputLocation.of(2, 1, source))
                .build();
    }

    private static PluginExecution execution(String id, String phase, InputSource source) {
        return PluginExecution.newBuilder()
                .id(id)
                .phase(phase)
                .goals(List.of("clean"))
                .location("", InputLocation.of(3, 1, source))
                .build();
    }

    private static XmlNode configuration(String name, String value, String secondName, String secondValue) {
        return XmlNode.newBuilder()
                .name("configuration")
                .children(List.of(XmlNode.newInstance(name, value), XmlNode.newInstance(secondName, secondValue)))
                .build();
    }
}
