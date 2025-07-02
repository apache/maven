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
package org.apache.maven.project;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Simple test to verify that Plugin objects returned by MavenProject.getPlugin() are connected to the project model.
 * This test specifically verifies the fix for the issue where getPlugin() was returning disconnected Plugin objects.
 */
class PluginConnectionSimpleTest {

    @Test
    void testPluginModificationPersistsInModel() {
        // Create a test project with a plugin
        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        Build build = new Build();
        model.setBuild(build);

        // Add a test plugin
        Plugin originalPlugin = new Plugin();
        originalPlugin.setGroupId("org.apache.maven.plugins");
        originalPlugin.setArtifactId("maven-compiler-plugin");
        originalPlugin.setVersion("3.8.1");
        build.addPlugin(originalPlugin);

        MavenProject project = new MavenProject(model);

        // Get the plugin using getPlugin() method
        Plugin retrievedPlugin = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        assertNotNull(retrievedPlugin, "Plugin should be found");
        assertEquals("3.8.1", retrievedPlugin.getVersion(), "Initial version should match");

        // Modify the plugin version
        retrievedPlugin.setVersion("3.11.0");

        // Verify the change persists when getting the plugin again
        Plugin pluginAfterModification = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        assertEquals(
                "3.11.0",
                pluginAfterModification.getVersion(),
                "Version change should persist - this verifies the plugin is connected to the model");

        // Also verify the change is reflected in the build plugins list
        Plugin pluginFromBuildList = project.getBuild().getPlugins().stream()
                .filter(p -> "org.apache.maven.plugins:maven-compiler-plugin".equals(p.getKey()))
                .findFirst()
                .orElse(null);
        assertNotNull(pluginFromBuildList, "Plugin should be found in build plugins list");
        assertEquals(
                "3.11.0", pluginFromBuildList.getVersion(), "Version change should be reflected in build plugins list");
    }

    @Test
    void testPluginConnectionBeforeAndAfterFix() {
        // This test demonstrates the difference between the old broken behavior and the new fixed behavior

        Model model = new Model();
        model.setGroupId("test.group");
        model.setArtifactId("test-artifact");
        model.setVersion("1.0.0");

        Build build = new Build();
        model.setBuild(build);

        Plugin originalPlugin = new Plugin();
        originalPlugin.setGroupId("org.apache.maven.plugins");
        originalPlugin.setArtifactId("maven-surefire-plugin");
        originalPlugin.setVersion("2.22.2");
        build.addPlugin(originalPlugin);

        MavenProject project = new MavenProject(model);

        // The old broken implementation would have done:
        // var plugin = getBuild().getDelegate().getPluginsAsMap().get(pluginKey);
        // return plugin != null ? new Plugin(plugin) : null;
        // This would create a disconnected Plugin that doesn't persist changes.

        // The new fixed implementation does:
        // Find the plugin in the connected plugins list
        Plugin connectedPlugin = project.getPlugin("org.apache.maven.plugins:maven-surefire-plugin");
        assertNotNull(connectedPlugin, "Plugin should be found");

        // Test that modifications persist (this would fail with the old implementation)
        connectedPlugin.setVersion("3.0.0-M7");

        Plugin pluginAfterChange = project.getPlugin("org.apache.maven.plugins:maven-surefire-plugin");
        assertEquals(
                "3.0.0-M7",
                pluginAfterChange.getVersion(),
                "Plugin modifications should persist - this proves the fix is working");
    }
}
