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
package org.apache.maven.cling.invoker.mvnup.goals;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.apache.maven.cling.invoker.mvnup.jdom.JDomPomCfg;
import org.apache.maven.cling.invoker.mvnup.jdom.JDomUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

/**
 * Utility class for resolving and analyzing parent POMs for plugin upgrades.
 * This class handles downloading parent POMs from Maven Central and checking
 * if they contain plugins that need to be managed locally.
 */
public class ParentPomResolver {

    /**
     * Checks parent POMs for plugins that need to be managed locally.
     * Downloads parent POMs from Maven Central and checks if they contain
     * any of the target plugins that need version management.
     */
    public static boolean checkParentPomsForPlugins(
            UpgradeContext context, Document pomDocument, Map<String, BaseUpgradeGoal.PluginUpgrade> pluginUpgrades) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Get parent information
        Element parentElement = root.getChild("parent", namespace);
        if (parentElement == null) {
            return false; // No parent to check
        }

        String parentGroupId = getChildText(parentElement, "groupId", namespace);
        String parentArtifactId = getChildText(parentElement, "artifactId", namespace);
        String parentVersion = getChildText(parentElement, "version", namespace);

        if (parentGroupId == null || parentArtifactId == null || parentVersion == null) {
            context.logger.debug("      Parent POM has incomplete coordinates, skipping parent plugin check");
            return false;
        }

        try {
            // Download and parse parent POM
            Document parentPom = downloadParentPom(context, parentGroupId, parentArtifactId, parentVersion);
            if (parentPom == null) {
                return false;
            }

            // Check if parent contains any of our target plugins
            Set<String> parentPlugins = findPluginsInParentPom(context, parentPom, pluginUpgrades.keySet());

            if (!parentPlugins.isEmpty()) {
                // Add plugin management entries for plugins found in parent
                hasUpgrades |= addPluginManagementForParentPlugins(context, pomDocument, parentPlugins, pluginUpgrades);
            }

        } catch (Exception e) {
            context.logger.debug("      Failed to check parent POM for plugins: " + e.getMessage());
        }

        return hasUpgrades;
    }

    /**
     * Downloads a parent POM from Maven Central.
     */
    public static Document downloadParentPom(
            UpgradeContext context, String groupId, String artifactId, String version) {
        try {
            // Construct Maven Central URL
            String groupPath = groupId.replace('.', '/');
            String url = String.format(
                    "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom",
                    groupPath, artifactId, version, artifactId, version);

            context.logger.debug("      Downloading parent POM from: " + url);

            // Download and parse POM
            java.net.URL pomUrl = new java.net.URL(url);
            try (java.io.InputStream inputStream = pomUrl.openStream()) {
                SAXBuilder saxBuilder = new SAXBuilder();
                return saxBuilder.build(inputStream);
            }

        } catch (Exception e) {
            context.logger.debug("      Could not download parent POM " + groupId + ":" + artifactId + ":" + version
                    + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds plugins in parent POM that match our target plugins.
     */
    public static Set<String> findPluginsInParentPom(
            UpgradeContext context, Document parentPom, Set<String> targetPlugins) {
        Set<String> foundPlugins = new HashSet<>();
        Element root = parentPom.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check build/plugins and build/pluginManagement/plugins in parent
        Element buildElement = root.getChild("build", namespace);
        if (buildElement != null) {
            // Check build/plugins
            Element pluginsElement = buildElement.getChild("plugins", namespace);
            if (pluginsElement != null) {
                foundPlugins.addAll(findTargetPluginsInSection(pluginsElement, namespace, targetPlugins));
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
            if (pluginManagementElement != null) {
                Element managedPluginsElement = pluginManagementElement.getChild("plugins", namespace);
                if (managedPluginsElement != null) {
                    foundPlugins.addAll(findTargetPluginsInSection(managedPluginsElement, namespace, targetPlugins));
                }
            }
        }

        return foundPlugins;
    }

    /**
     * Finds target plugins in a specific plugins section.
     */
    public static Set<String> findTargetPluginsInSection(
            Element pluginsElement, Namespace namespace, Set<String> targetPlugins) {
        Set<String> foundPlugins = new HashSet<>();
        List<Element> pluginElements = pluginsElement.getChildren("plugin", namespace);

        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, "groupId", namespace);
            String artifactId = getChildText(pluginElement, "artifactId", namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith("maven-")) {
                groupId = "org.apache.maven.plugins";
            }

            if (groupId != null && artifactId != null) {
                String pluginKey = groupId + ":" + artifactId;
                if (targetPlugins.contains(pluginKey)) {
                    foundPlugins.add(pluginKey);
                }
            }
        }

        return foundPlugins;
    }

    /**
     * Adds plugin management entries for plugins found in parent POMs.
     */
    public static boolean addPluginManagementForParentPlugins(
            UpgradeContext context,
            Document pomDocument,
            Set<String> parentPlugins,
            Map<String, BaseUpgradeGoal.PluginUpgrade> pluginUpgrades) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Ensure build/pluginManagement/plugins structure exists using proper JDom utilities
        JDomPomCfg pomConfig = new JDomPomCfg();

        Element buildElement = root.getChild("build", namespace);
        if (buildElement == null) {
            buildElement = JDomUtils.insertNewElement(pomConfig, "build", root);
        }

        Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
        if (pluginManagementElement == null) {
            pluginManagementElement = JDomUtils.insertNewElement(pomConfig, "pluginManagement", buildElement);
        }

        Element pluginsElement = pluginManagementElement.getChild("plugins", namespace);
        if (pluginsElement == null) {
            pluginsElement = JDomUtils.insertNewElement(pomConfig, "plugins", pluginManagementElement);
        }

        // Add plugin management entries for each parent plugin
        for (String pluginKey : parentPlugins) {
            BaseUpgradeGoal.PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                // Check if plugin is already managed
                if (!isPluginAlreadyManaged(pluginsElement, namespace, upgrade)) {
                    addPluginManagementEntry(context, pluginsElement, namespace, upgrade, pomConfig);
                    hasUpgrades = true;
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Checks if a plugin is already managed in pluginManagement.
     */
    public static boolean isPluginAlreadyManaged(
            Element pluginsElement, Namespace namespace, BaseUpgradeGoal.PluginUpgrade upgrade) {
        List<Element> pluginElements = pluginsElement.getChildren("plugin", namespace);

        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, "groupId", namespace);
            String artifactId = getChildText(pluginElement, "artifactId", namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith("maven-")) {
                groupId = "org.apache.maven.plugins";
            }

            if (upgrade.groupId.equals(groupId) && upgrade.artifactId.equals(artifactId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a plugin management entry for a plugin found in parent POM.
     */
    public static void addPluginManagementEntry(
            UpgradeContext context,
            Element pluginsElement,
            Namespace namespace,
            BaseUpgradeGoal.PluginUpgrade upgrade,
            JDomPomCfg pomConfig) {

        // Create plugin element using JDomUtils for proper formatting
        Element pluginElement = JDomUtils.insertNewElement(pomConfig, "plugin", pluginsElement);

        // Add child elements using JDomUtils for proper formatting
        JDomUtils.insertContentElement(pomConfig, pluginElement, "groupId", upgrade.groupId);
        JDomUtils.insertContentElement(pomConfig, pluginElement, "artifactId", upgrade.artifactId);
        JDomUtils.insertContentElement(pomConfig, pluginElement, "version", upgrade.minVersion);

        context.logger.info("      • Added plugin management for " + upgrade.groupId + ":" + upgrade.artifactId
                + " version " + upgrade.minVersion + " (found in parent POM)");
    }

    /**
     * Helper method to get child text content safely.
     */
    private static String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }
}
