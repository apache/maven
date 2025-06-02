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

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.MAVEN_PLUGIN_PREFIX;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

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
            UpgradeContext context, Document pomDocument, Map<String, PluginUpgrade> pluginUpgrades) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Get parent information
        Element parentElement = root.getChild(PARENT, namespace);
        if (parentElement == null) {
            return false; // No parent to check
        }

        String parentGroupId = getChildText(parentElement, GROUP_ID, namespace);
        String parentArtifactId = getChildText(parentElement, ARTIFACT_ID, namespace);
        String parentVersion = getChildText(parentElement, VERSION, namespace);

        if (parentGroupId == null || parentArtifactId == null || parentVersion == null) {
            context.debug("Parent POM has incomplete coordinates, skipping parent plugin check");
            return false;
        }

        try {
            // Download and parse parent POM
            Document parentPom = downloadParentPom(context, parentGroupId, parentArtifactId, parentVersion);
            if (parentPom == null) {
                return false;
            }

            // Check if parent contains any of our target plugins
            Set<String> parentPlugins = findPluginsInParentPom(parentPom, pluginUpgrades.keySet());

            if (!parentPlugins.isEmpty()) {
                // Add plugin management entries for plugins found in parent
                hasUpgrades = addPluginManagementForParentPlugins(context, pomDocument, parentPlugins, pluginUpgrades);
            }

        } catch (Exception e) {
            context.debug("Failed to check parent POM for plugins: " + e.getMessage());
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

            context.debug("Downloading parent POM from: " + url);

            // Download and parse POM
            URL pomUrl = new URL(url);
            try (InputStream inputStream = pomUrl.openStream()) {
                SAXBuilder saxBuilder = new SAXBuilder();
                return saxBuilder.build(inputStream);
            }

        } catch (Exception e) {
            context.debug("Could not download parent POM " + groupId + ":" + artifactId + ":" + version + " - "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Finds plugins in parent POM that match our target plugins.
     */
    public static Set<String> findPluginsInParentPom(Document parentPom, Set<String> targetPlugins) {
        Set<String> foundPlugins = new HashSet<>();
        Element root = parentPom.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check build/plugins and build/pluginManagement/plugins in parent
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement != null) {
            // Check build/plugins
            Element pluginsElement = buildElement.getChild(PLUGINS, namespace);
            if (pluginsElement != null) {
                foundPlugins.addAll(findTargetPluginsInSection(pluginsElement, namespace, targetPlugins));
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
            if (pluginManagementElement != null) {
                Element managedPluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
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
        List<Element> pluginElements = pluginsElement.getChildren(PLUGIN, namespace);

        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, GROUP_ID, namespace);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID, namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
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
            Map<String, PluginUpgrade> pluginUpgrades) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Ensure build/pluginManagement/plugins structure exists using proper JDom utilities
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement == null) {
            buildElement = JDomUtils.insertNewElement(BUILD, root);
        }

        Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
        if (pluginManagementElement == null) {
            pluginManagementElement = JDomUtils.insertNewElement(PLUGIN_MANAGEMENT, buildElement);
        }

        Element pluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
        if (pluginsElement == null) {
            pluginsElement = JDomUtils.insertNewElement(PLUGINS, pluginManagementElement);
        }

        // Add plugin management entries for each parent plugin
        for (String pluginKey : parentPlugins) {
            PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                // Check if plugin is already managed
                Element existingPlugin = findExistingManagedPlugin(pluginsElement, namespace, upgrade);
                if (existingPlugin == null) {
                    // Plugin not managed - add new entry
                    addPluginManagementEntry(context, pluginsElement, upgrade);
                    hasUpgrades = true;
                } else {
                    // Plugin already managed - check if it needs version upgrade
                    if (upgradeExistingPluginManagement(context, existingPlugin, namespace, upgrade)) {
                        hasUpgrades = true;
                    }
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Finds an existing managed plugin element.
     */
    public static Element findExistingManagedPlugin(
            Element pluginsElement, Namespace namespace, PluginUpgrade upgrade) {
        List<Element> pluginElements = pluginsElement.getChildren(PLUGIN, namespace);

        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, GROUP_ID, namespace);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID, namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (upgrade.groupId().equals(groupId) && upgrade.artifactId().equals(artifactId)) {
                return pluginElement;
            }
        }

        return null;
    }

    /**
     * Upgrades an existing plugin management entry if needed.
     */
    public static boolean upgradeExistingPluginManagement(
            UpgradeContext context, Element pluginElement, Namespace namespace, PluginUpgrade upgrade) {
        Element versionElement = pluginElement.getChild(VERSION, namespace);

        if (versionElement == null) {
            // No version element - add one
            JDomUtils.insertContentElement(pluginElement, VERSION, upgrade.minVersion());
            context.detail("Added version " + upgrade.minVersion() + " to plugin management for " + upgrade.groupId()
                    + ":" + upgrade.artifactId() + " (found in parent POM)");
            return true;
        } else {
            String currentVersion = versionElement.getTextTrim();
            if (currentVersion == null || currentVersion.isEmpty()) {
                // Empty version - set it
                versionElement.setText(upgrade.minVersion());
                context.detail("Set version " + upgrade.minVersion() + " for plugin management " + upgrade.groupId()
                        + ":" + upgrade.artifactId() + " (found in parent POM)");
                return true;
            } else {
                // Version exists - check if it needs upgrading
                if (isVersionBelow(currentVersion, upgrade.minVersion())) {
                    versionElement.setText(upgrade.minVersion());
                    context.detail("Upgraded plugin management " + upgrade.groupId() + ":"
                            + upgrade.artifactId() + " from " + currentVersion + " to " + upgrade.minVersion()
                            + " (found in parent POM)");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Compares two version strings to determine if the first is below the second.
     */
    private static boolean isVersionBelow(String currentVersion, String targetVersion) {
        // Simple version comparison - this could be enhanced with a proper version comparison library
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] targetParts = targetVersion.split("\\.");

            int maxLength = Math.max(currentParts.length, targetParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                int targetPart = i < targetParts.length ? parseVersionPart(targetParts[i]) : 0;

                if (currentPart < targetPart) {
                    return true;
                } else if (currentPart > targetPart) {
                    return false;
                }
            }

            return false; // Versions are equal
        } catch (Exception e) {
            // If version parsing fails, assume upgrade is needed
            return true;
        }
    }

    /**
     * Parses a version part, handling qualifiers like SNAPSHOT.
     */
    private static int parseVersionPart(String part) {
        try {
            // Remove qualifiers like -SNAPSHOT, -alpha, etc.
            String numericPart = part.split("-")[0];
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Adds a plugin management entry for a plugin found in parent POM.
     */
    public static void addPluginManagementEntry(UpgradeContext context, Element pluginsElement, PluginUpgrade upgrade) {

        // Create plugin element using JDomUtils for proper formatting
        Element pluginElement = JDomUtils.insertNewElement(PLUGIN, pluginsElement);

        // Add child elements using JDomUtils for proper formatting
        JDomUtils.insertContentElement(pluginElement, GROUP_ID, upgrade.groupId());
        JDomUtils.insertContentElement(pluginElement, ARTIFACT_ID, upgrade.artifactId());
        JDomUtils.insertContentElement(pluginElement, VERSION, upgrade.minVersion());

        context.detail("Added plugin management for " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                + upgrade.minVersion() + " (found in parent POM)");
    }

    /**
     * Helper method to get child text content safely.
     */
    private static String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }
}
