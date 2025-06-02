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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.MAVEN_4_COMPATIBILITY_REASON;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.MAVEN_PLUGIN_PREFIX;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

/**
 * Strategy for upgrading Maven plugins to recommended versions.
 * Handles plugin version upgrades in build/plugins and build/pluginManagement sections.
 */
@Named
@Singleton
@Priority(10)
public class PluginUpgradeStrategy extends AbstractUpgradeStrategy {

    private static final List<PluginUpgrade> PLUGIN_UPGRADES = List.of(
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-exec-plugin", "3.2.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.0.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID,
                    "maven-remote-resources-plugin",
                    "3.0.0",
                    MAVEN_4_COMPATIBILITY_REASON));

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.plugins(), true);
    }

    @Override
    public String getDescription() {
        return "Upgrading Maven plugins to recommended versions";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking for plugin upgrades)");
            context.indent();

            try {
                boolean hasUpgrades = false;

                // Apply plugin upgrades
                hasUpgrades |= upgradePluginsInDocument(pomDocument, context);
                // Add missing plugin management entries if needed
                hasUpgrades |= addMissingPluginManagement(context, pomDocument);

                if (hasUpgrades) {
                    modifiedPoms.add(pomPath);
                    context.success("Plugin upgrades applied");
                } else {
                    context.success("No plugin upgrades needed");
                }
            } catch (Exception e) {
                context.failure("Failed to upgrade plugins: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Upgrades plugins in the document.
     * Checks both build/plugins and build/pluginManagement/plugins sections.
     * Also checks parent POMs for plugins that need to be managed locally.
     */
    private boolean upgradePluginsInDocument(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Define the plugins that need to be upgraded for Maven 4 compatibility
        Map<String, PluginUpgradeInfo> pluginUpgrades = getPluginUpgradesMap();

        // Check build/plugins
        Element buildElement = root.getChild(UpgradeConstants.XmlElements.BUILD, namespace);
        if (buildElement != null) {
            Element pluginsElement = buildElement.getChild(PLUGINS, namespace);
            if (pluginsElement != null) {
                hasUpgrades |= upgradePluginsInSection(
                        pluginsElement, namespace, pluginUpgrades, pomDocument, BUILD + "/" + PLUGINS, context);
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
            if (pluginManagementElement != null) {
                Element managedPluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
                if (managedPluginsElement != null) {
                    hasUpgrades |= upgradePluginsInSection(
                            managedPluginsElement,
                            namespace,
                            pluginUpgrades,
                            pomDocument,
                            BUILD + "/" + PLUGIN_MANAGEMENT + "/" + PLUGINS,
                            context);
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Adds missing plugin management entries for plugins that need to be managed.
     * This ensures that plugins used in the build have proper version management.
     * Only adds entries for plugins that actually need upgrades or lack version management.
     */
    private boolean addMissingPluginManagement(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Get the plugins that need to be upgraded for Maven 4 compatibility
        Map<String, PluginUpgradeInfo> pluginUpgrades = getPluginUpgradesMap();

        // Convert PluginUpgradeInfo to PluginUpgrade for compatibility with ParentPomResolver
        Map<String, PluginUpgrade> pluginUpgradeMap = new HashMap<>();
        for (Map.Entry<String, PluginUpgradeInfo> entry : pluginUpgrades.entrySet()) {
            PluginUpgradeInfo info = entry.getValue();
            pluginUpgradeMap.put(
                    entry.getKey(),
                    new PluginUpgrade(info.groupId, info.artifactId, info.minVersion, MAVEN_4_COMPATIBILITY_REASON));
        }

        // Check build/plugins section for plugins that need management
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement != null) {
            Element pluginsElement = buildElement.getChild(PLUGINS, namespace);
            if (pluginsElement != null) {
                // Find plugins that need management entries
                List<PluginUpgradeInfo> pluginsNeedingManagement =
                        findPluginsNeedingManagement(pluginsElement, namespace, pluginUpgrades, buildElement);

                if (!pluginsNeedingManagement.isEmpty()) {
                    // Ensure build/pluginManagement/plugins structure exists
                    Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
                    if (pluginManagementElement == null) {
                        pluginManagementElement = JDomUtils.insertNewElement(PLUGIN_MANAGEMENT, buildElement);
                    }

                    Element managedPluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
                    if (managedPluginsElement == null) {
                        managedPluginsElement = JDomUtils.insertNewElement(PLUGINS, pluginManagementElement);
                    }

                    // Add management entries for plugins that need them
                    for (PluginUpgradeInfo upgrade : pluginsNeedingManagement) {
                        addPluginManagementEntry(managedPluginsElement, upgrade, context);
                        hasUpgrades = true;
                    }
                }
            }
        }

        // Check parent POMs for plugins that need to be managed locally
        // This handles the case where plugins are defined in parent POMs but need local management
        // for Maven 4 compatibility
        try {
            hasUpgrades |= ParentPomResolver.checkParentPomsForPlugins(context, pomDocument, pluginUpgradeMap);
        } catch (Exception e) {
            context.debug("Failed to check parent POMs for plugins: " + e.getMessage());
        }

        return hasUpgrades;
    }

    /**
     * Finds plugins in the build/plugins section that need management entries.
     * Only returns plugins that are in the upgrade list, are not already managed,
     * and either lack a version or have a version that needs upgrading.
     */
    private List<PluginUpgradeInfo> findPluginsNeedingManagement(
            Element pluginsElement,
            Namespace namespace,
            Map<String, PluginUpgradeInfo> pluginUpgrades,
            Element buildElement) {
        List<PluginUpgradeInfo> pluginsNeedingManagement = new ArrayList<>();
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
                PluginUpgradeInfo upgrade = pluginUpgrades.get(pluginKey);

                if (upgrade != null) {
                    // Check if this plugin is already managed in pluginManagement
                    if (!isPluginAlreadyManaged(buildElement, namespace, upgrade)) {
                        // Only add management if the plugin has a version that needs upgrading
                        Element versionElement = pluginElement.getChild(VERSION, namespace);
                        if (versionElement != null) {
                            String currentVersion = versionElement.getTextTrim();
                            // Check if version is a property reference or needs upgrading
                            if (currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
                                // Property reference - check if property needs upgrading
                                String propertyName = currentVersion.substring(2, currentVersion.length() - 1);
                                if (propertyNeedsUpgrade(buildElement.getDocument(), propertyName, upgrade)) {
                                    pluginsNeedingManagement.add(upgrade);
                                }
                            } else if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                                // Direct version that needs upgrading
                                pluginsNeedingManagement.add(upgrade);
                            }
                        }
                        // Note: We don't add management for plugins without versions as they may inherit from parent
                    }
                }
            }
        }

        return pluginsNeedingManagement;
    }

    /**
     * Checks if a property needs to be upgraded.
     */
    private boolean propertyNeedsUpgrade(Document pomDocument, String propertyName, PluginUpgradeInfo upgrade) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element propertiesElement = root.getChild(UpgradeConstants.XmlElements.PROPERTIES, namespace);

        if (propertiesElement != null) {
            Element propertyElement = propertiesElement.getChild(propertyName, namespace);
            if (propertyElement != null) {
                String currentVersion = propertyElement.getTextTrim();
                return isVersionBelow(currentVersion, upgrade.minVersion);
            }
        }

        // Property not found - be conservative and don't add management
        // The property might be defined in a parent POM or through other means
        return false;
    }

    /**
     * Checks if a plugin is already managed in the pluginManagement section.
     */
    private boolean isPluginAlreadyManaged(Element buildElement, Namespace namespace, PluginUpgradeInfo upgrade) {
        Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
        if (pluginManagementElement == null) {
            return false;
        }

        Element managedPluginsElement = pluginManagementElement.getChild("plugins", namespace);
        if (managedPluginsElement == null) {
            return false;
        }

        List<Element> managedPluginElements = managedPluginsElement.getChildren(PLUGIN, namespace);
        for (Element managedPluginElement : managedPluginElements) {
            String managedGroupId = getChildText(managedPluginElement, GROUP_ID, namespace);
            String managedArtifactId = getChildText(managedPluginElement, ARTIFACT_ID, namespace);

            // Default groupId for Maven plugins
            if (managedGroupId == null
                    && managedArtifactId != null
                    && managedArtifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                managedGroupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (upgrade.groupId.equals(managedGroupId) && upgrade.artifactId.equals(managedArtifactId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a plugin management entry for a plugin that needs to be managed.
     */
    private void addPluginManagementEntry(
            Element managedPluginsElement, PluginUpgradeInfo upgrade, UpgradeContext context) {
        // Create plugin element using JDomUtils for proper formatting
        Element pluginElement = JDomUtils.insertNewElement(PLUGIN, managedPluginsElement);

        // Add child elements using JDomUtils for proper formatting
        JDomUtils.insertContentElement(pluginElement, GROUP_ID, upgrade.groupId);
        JDomUtils.insertContentElement(pluginElement, ARTIFACT_ID, upgrade.artifactId);
        JDomUtils.insertContentElement(pluginElement, VERSION, upgrade.minVersion);

        context.detail("Added plugin management for " + upgrade.groupId + ":" + upgrade.artifactId + " version "
                + upgrade.minVersion + " (needed for Maven 4 compatibility)");
    }

    /**
     * Returns the map of plugins that need to be upgraded for Maven 4 compatibility.
     */
    private Map<String, PluginUpgradeInfo> getPluginUpgradesMap() {
        Map<String, PluginUpgradeInfo> upgrades = new HashMap<>();
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-compiler-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-exec-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-exec-plugin", "3.2.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-enforcer-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.0.0"));
        upgrades.put(
                "org.codehaus.mojo:flatten-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-shade-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-remote-resources-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-remote-resources-plugin", "3.0.0"));
        return upgrades;
    }

    /**
     * Upgrades plugins in a specific plugins section (either build/plugins or build/pluginManagement/plugins).
     */
    private boolean upgradePluginsInSection(
            Element pluginsElement,
            Namespace namespace,
            Map<String, PluginUpgradeInfo> pluginUpgrades,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {
        boolean hasUpgrades = false;
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
                PluginUpgradeInfo upgrade = pluginUpgrades.get(pluginKey);

                if (upgrade != null) {
                    if (upgradePluginVersion(pluginElement, namespace, upgrade, pomDocument, sectionName, context)) {
                        hasUpgrades = true;
                    }
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Upgrades a specific plugin's version if needed.
     */
    private boolean upgradePluginVersion(
            Element pluginElement,
            Namespace namespace,
            PluginUpgradeInfo upgrade,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {
        Element versionElement = pluginElement.getChild(VERSION, namespace);
        String currentVersion;
        boolean isProperty = false;
        String propertyName = null;

        if (versionElement != null) {
            currentVersion = versionElement.getTextTrim();
            // Check if version is a property reference
            if (currentVersion.startsWith("${") && currentVersion.endsWith("}")) {
                isProperty = true;
                propertyName = currentVersion.substring(2, currentVersion.length() - 1);
            }
        } else {
            // Plugin version might be inherited from parent or pluginManagement
            context.debug("Plugin " + upgrade.groupId + ":" + upgrade.artifactId
                    + " has no explicit version, may inherit from parent");
            return false;
        }

        if (isProperty) {
            // Update property value if it's below minimum version
            return upgradePropertyVersion(pomDocument, propertyName, upgrade, sectionName, context);
        } else {
            // Direct version comparison and upgrade
            if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                versionElement.setText(upgrade.minVersion);
                context.detail("Upgraded " + upgrade.groupId + ":" + upgrade.artifactId + " from " + currentVersion
                        + " to " + upgrade.minVersion + " in " + sectionName);
                return true;
            } else {
                context.debug("Plugin " + upgrade.groupId + ":" + upgrade.artifactId + " version " + currentVersion
                        + " is already >= " + upgrade.minVersion);
            }
        }

        return false;
    }

    /**
     * Upgrades a property value if it represents a plugin version below the minimum.
     */
    private boolean upgradePropertyVersion(
            Document pomDocument,
            String propertyName,
            PluginUpgradeInfo upgrade,
            String sectionName,
            UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element propertiesElement = root.getChild(UpgradeConstants.XmlElements.PROPERTIES, namespace);

        if (propertiesElement != null) {
            Element propertyElement = propertiesElement.getChild(propertyName, namespace);
            if (propertyElement != null) {
                String currentVersion = propertyElement.getTextTrim();
                if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                    propertyElement.setText(upgrade.minVersion);
                    context.detail("Upgraded property " + propertyName + " (for " + upgrade.groupId
                            + ":"
                            + upgrade.artifactId + ") from " + currentVersion + " to " + upgrade.minVersion
                            + " in "
                            + sectionName);
                    return true;
                } else {
                    context.debug("Property " + propertyName + " version " + currentVersion + " is already >= "
                            + upgrade.minVersion);
                }
            } else {
                context.warning("Property " + propertyName + " not found in POM properties");
            }
        } else {
            context.warning("No properties section found in POM for property " + propertyName);
        }

        return false;
    }

    /**
     * Simple version comparison to check if current version is below minimum version.
     * This is a basic implementation that works for most Maven plugin versions.
     */
    private boolean isVersionBelow(String currentVersion, String minVersion) {
        if (currentVersion == null || minVersion == null) {
            return false;
        }

        // Remove any qualifiers like -SNAPSHOT, -alpha, etc. for comparison
        String cleanCurrent = currentVersion.split("-")[0];
        String cleanMin = minVersion.split("-")[0];

        try {
            String[] currentParts = cleanCurrent.split("\\.");
            String[] minParts = cleanMin.split("\\.");

            int maxLength = Math.max(currentParts.length, minParts.length);

            for (int i = 0; i < maxLength; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int minPart = i < minParts.length ? Integer.parseInt(minParts[i]) : 0;

                if (currentPart < minPart) {
                    return true;
                } else if (currentPart > minPart) {
                    return false;
                }
            }

            return false; // Versions are equal
        } catch (NumberFormatException e) {
            // Fallback to string comparison if parsing fails
            return currentVersion.compareTo(minVersion) < 0;
        }
    }

    /**
     * Helper method to get child element text.
     */
    private String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }

    /**
     * Gets the list of plugin upgrades to apply.
     */
    public static List<PluginUpgrade> getPluginUpgrades() {
        return PLUGIN_UPGRADES;
    }

    /**
     * Holds plugin upgrade information for Maven 4 compatibility.
     * This class contains the minimum version requirements for plugins
     * that need to be upgraded to work properly with Maven 4.
     */
    public static class PluginUpgradeInfo {
        /** The Maven groupId of the plugin */
        final String groupId;

        /** The Maven artifactId of the plugin */
        final String artifactId;

        /** The minimum version required for Maven 4 compatibility */
        final String minVersion;

        /**
         * Creates a new plugin upgrade information holder.
         *
         * @param groupId the Maven groupId of the plugin
         * @param artifactId the Maven artifactId of the plugin
         * @param minVersion the minimum version required for Maven 4 compatibility
         */
        PluginUpgradeInfo(String groupId, String artifactId, String minVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.minVersion = minVersion;
        }
    }
}
