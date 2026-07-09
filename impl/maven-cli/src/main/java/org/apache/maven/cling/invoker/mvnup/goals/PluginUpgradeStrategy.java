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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PARENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROPERTIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_4_COMPATIBILITY_REASON;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_PLUGIN_PREFIX;

/**
 * Strategy for upgrading Maven plugins to recommended versions. Handles plugin version upgrades in build/plugins and
 * build/pluginManagement sections.
 */
@Named
@Singleton
@Priority(10)
public class PluginUpgradeStrategy extends AbstractUpgradeStrategy {

    private static final List<PluginUpgrade> PLUGIN_UPGRADES = List.of(
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade("org.codehaus.mojo", "exec-maven-plugin", "3.5.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.5.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID,
                    "maven-remote-resources-plugin",
                    "3.0.0",
                    MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-surefire-plugin", "3.5.2", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-failsafe-plugin", "3.5.2", MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID,
                    "maven-surefire-report-plugin",
                    "3.5.2",
                    MAVEN_4_COMPATIBILITY_REASON),
            new PluginUpgrade(
                    "net.alchim31.maven",
                    "scala-maven-plugin",
                    "4.9.5",
                    "Versions before 4.9.5 call add() on immutable lists returned by Maven 4 API"),
            new PluginUpgrade(
                    DEFAULT_MAVEN_PLUGIN_GROUP_ID,
                    "maven-resources-plugin",
                    "3.3.1",
                    "Beta/RC versions compiled against different Maven 4 API signatures"),
            new PluginUpgrade(
                    "org.codehaus.mojo",
                    "jaxb2-maven-plugin",
                    "3.2.0",
                    "Versions before 3.2.0 depend on jaxb-parent:3.0.0 which contains invalid XML rejected by Maven 4"),
            new PluginUpgrade(
                    "io.quarkus", "quarkus-maven-plugin", "3.26.0", "Maven 4 compatibility (Aether API changes)"),
            new PluginUpgrade(
                    "io.quarkus.platform",
                    "quarkus-maven-plugin",
                    "3.26.0",
                    "Maven 4 compatibility (Aether API changes)"),
            new PluginUpgrade(
                    "org.codehaus.gmavenplus",
                    "gmavenplus-plugin",
                    "4.2.0",
                    "Versions before 4.2.0 call mutating methods on immutable lists returned by Maven 4 API"));

    private static final List<PluginUpgrade> PLUGIN_DEPENDENCY_UPGRADES = List.of(new PluginUpgrade(
            "org.codehaus.mojo",
            "extra-enforcer-rules",
            "1.4",
            "Versions before 1.4 use a removed DependencyGraphBuilder API incompatible with Maven 4"));

    @Inject
    public PluginUpgradeStrategy() {}

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

        try {
            // Phase 1: Write all modifications to temp directory (keeping project structure)
            Path tempDir = createTempProjectStructure(context, pomMap);

            // Phase 2: For each POM, build effective model using the session and analyze plugins
            PluginAnalysisResults analysisResults = analyzePluginsUsingEffectiveModels(context, pomMap, tempDir);

            // Collect locally declared plugin keys so we can add comments for remote-parent overrides
            Set<String> localPluginKeys = collectLocallyDeclaredPluginKeys(pomMap);

            // Phase 3: Add plugin management and direct overrides to the last local parent in hierarchy
            for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
                Path pomPath = entry.getKey();
                Document pomDocument = entry.getValue();
                processedPoms.add(pomPath);

                context.info(pomPath + " (checking for plugin upgrades)");
                context.indent();

                try {
                    boolean hasUpgrades = false;

                    // Apply direct plugin upgrades in the document
                    hasUpgrades |= upgradePluginsInDocument(pomDocument, context);

                    // Add plugin management based on effective model analysis
                    Set<String> pluginsForManagement =
                            analysisResults.pluginsNeedingManagement().get(pomPath);
                    if (pluginsForManagement != null && !pluginsForManagement.isEmpty()) {
                        hasUpgrades |= addPluginManagementForEffectivePlugins(
                                context, pomDocument, pluginsForManagement, localPluginKeys);
                        context.detail("Added plugin management to " + pomPath + " (target parent for "
                                + pluginsForManagement.size() + " plugins)");
                    }

                    // Add direct plugin overrides in build/plugins for inherited plugins
                    // whose versions cannot be overridden via pluginManagement alone
                    Set<String> pluginsForDirectOverride =
                            analysisResults.pluginsNeedingDirectOverride().get(pomPath);
                    if (pluginsForDirectOverride != null && !pluginsForDirectOverride.isEmpty()) {
                        hasUpgrades |= addDirectPluginOverrides(
                                context, pomDocument, pluginsForDirectOverride, localPluginKeys);
                    }

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

            // Clean up temp directory
            cleanupTempDirectory(tempDir);

        } catch (Exception e) {
            context.failure("Failed to create temp project structure: " + e.getMessage());
            // Mark all POMs as errors
            errorPoms.addAll(pomMap.keySet());
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Upgrades plugins in the document. Checks both build/plugins and build/pluginManagement/plugins sections. Only
     * processes plugins explicitly defined in the current POM document.
     */
    private boolean upgradePluginsInDocument(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean hasUpgrades = false;

        // Define the plugins that need to be upgraded for Maven 4 compatibility
        Map<String, PluginUpgradeInfo> pluginUpgrades = getPluginUpgradesMap();

        // Check build/plugins
        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement != null) {
            Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
            if (pluginsElement != null) {
                hasUpgrades |= upgradePluginsInSection(
                        pluginsElement, pluginUpgrades, pomDocument, BUILD + "/" + PLUGINS, context);
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement =
                    buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
            if (pluginManagementElement != null) {
                Element managedPluginsElement =
                        pluginManagementElement.childElement(PLUGINS).orElse(null);
                if (managedPluginsElement != null) {
                    hasUpgrades |= upgradePluginsInSection(
                            managedPluginsElement,
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
     * Returns the map of plugins that need to be upgraded for Maven 4 compatibility.
     */
    private Map<String, PluginUpgradeInfo> getPluginUpgradesMap() {
        Map<String, PluginUpgradeInfo> upgrades = new HashMap<>();
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-compiler-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-compiler-plugin", "3.2"));
        upgrades.put(
                "org.codehaus.mojo:exec-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "exec-maven-plugin", "3.5.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-enforcer-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-enforcer-plugin", "3.5.0"));
        upgrades.put(
                "org.codehaus.mojo:flatten-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-shade-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-shade-plugin", "3.5.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-remote-resources-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-remote-resources-plugin", "3.0.0"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-surefire-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-surefire-plugin", "3.5.2"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-failsafe-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-failsafe-plugin", "3.5.2"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-surefire-report-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-surefire-report-plugin", "3.5.2"));
        upgrades.put(
                "net.alchim31.maven:scala-maven-plugin",
                new PluginUpgradeInfo("net.alchim31.maven", "scala-maven-plugin", "4.9.5"));
        upgrades.put(
                DEFAULT_MAVEN_PLUGIN_GROUP_ID + ":maven-resources-plugin",
                new PluginUpgradeInfo(DEFAULT_MAVEN_PLUGIN_GROUP_ID, "maven-resources-plugin", "3.3.1"));
        upgrades.put(
                "org.codehaus.mojo:jaxb2-maven-plugin",
                new PluginUpgradeInfo("org.codehaus.mojo", "jaxb2-maven-plugin", "3.2.0"));
        upgrades.put(
                "io.quarkus:quarkus-maven-plugin",
                new PluginUpgradeInfo("io.quarkus", "quarkus-maven-plugin", "3.26.0"));
        upgrades.put(
                "io.quarkus.platform:quarkus-maven-plugin",
                new PluginUpgradeInfo("io.quarkus.platform", "quarkus-maven-plugin", "3.26.0"));
        upgrades.put(
                "org.codehaus.gmavenplus:gmavenplus-plugin",
                new PluginUpgradeInfo("org.codehaus.gmavenplus", "gmavenplus-plugin", "4.2.0"));
        return upgrades;
    }

    /**
     * Upgrades plugins in a specific plugins section (either build/plugins or build/pluginManagement/plugins).
     */
    private boolean upgradePluginsInSection(
            Element pluginsElement,
            Map<String, PluginUpgradeInfo> pluginUpgrades,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {

        return pluginsElement
                .childElements(PLUGIN)
                .map(pluginElement -> {
                    boolean upgraded = false;
                    String groupId = getChildText(pluginElement, GROUP_ID);
                    String artifactId = getChildText(pluginElement, ARTIFACT_ID);

                    // Default groupId for Maven plugins
                    if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                        groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
                    }

                    if (groupId != null && artifactId != null) {
                        String pluginKey = groupId + ":" + artifactId;
                        PluginUpgradeInfo upgrade = pluginUpgrades.get(pluginKey);

                        if (upgrade != null) {
                            upgraded = upgradePluginVersion(pluginElement, upgrade, pomDocument, sectionName, context);
                        }
                    }

                    upgraded |= upgradePluginDependencies(pluginElement, pomDocument, sectionName, context);

                    return upgraded;
                })
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Upgrades a specific plugin's version if needed.
     */
    private boolean upgradePluginVersion(
            Element pluginElement,
            PluginUpgradeInfo upgrade,
            Document pomDocument,
            String sectionName,
            UpgradeContext context) {
        Element versionElement = pluginElement.childElement(VERSION).orElse(null);
        String currentVersion;
        boolean isProperty = false;
        String propertyName = null;

        if (versionElement != null) {
            currentVersion = versionElement.textContentTrimmed();
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

        // For Quarkus plugins, check the platform version before upgrading.
        // Upgrading quarkus-maven-plugin to 3.x when the project uses Quarkus 2.x
        // causes NoSuchMethodError and build failures.
        if (isQuarkusPlugin(upgrade.groupId, upgrade.artifactId)) {
            String platformVersion = detectQuarkusPlatformVersion(pomDocument);
            if (platformVersion != null) {
                int majorVersion = extractMajorVersion(platformVersion);
                if (majorVersion >= 0 && majorVersion < 3) {
                    context.warning("Skipping quarkus-maven-plugin upgrade: project uses Quarkus platform "
                            + majorVersion + ".x (" + platformVersion
                            + ") which is incompatible with plugin 3.x");
                    return false;
                }
            } else {
                context.warning("Could not determine Quarkus platform version — if the project uses "
                        + "Quarkus 2.x, the plugin upgrade may cause build failures");
            }
        }

        if (isProperty) {
            // For Quarkus plugins, check if the property is shared with a Quarkus BOM
            if (isQuarkusPlugin(upgrade.groupId, upgrade.artifactId)
                    && isPropertyUsedByQuarkusBom(pomDocument, propertyName)) {
                return decoupleQuarkusPluginVersion(
                        pomDocument, versionElement, propertyName, upgrade, sectionName, context);
            }
            // Update property value if it's below minimum version
            return upgradePropertyVersion(pomDocument, propertyName, upgrade, sectionName, context);
        } else {
            // Direct version comparison and upgrade
            if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                Editor editor = new Editor(pomDocument);
                editor.setTextContent(versionElement, upgrade.minVersion);
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
        Editor editor = new Editor(pomDocument);
        Element root = editor.root();
        Element propertiesElement = root.childElement(PROPERTIES).orElse(null);

        if (propertiesElement != null) {
            Element propertyElement =
                    propertiesElement.childElement(propertyName).orElse(null);
            if (propertyElement != null) {
                String currentVersion = propertyElement.textContentTrimmed();
                if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                    editor.setTextContent(propertyElement, upgrade.minVersion);
                    context.detail(
                            "Upgraded property " + propertyName + " (for " + upgrade.groupId + ":" + upgrade.artifactId
                                    + ") from " + currentVersion + " to " + upgrade.minVersion + " in " + sectionName);
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
     * Upgrades plugin dependencies (e.g., extra-enforcer-rules inside maven-enforcer-plugin).
     */
    private boolean upgradePluginDependencies(
            Element pluginElement, Document pomDocument, String sectionName, UpgradeContext context) {
        Element dependenciesElement = pluginElement.childElement(DEPENDENCIES).orElse(null);
        if (dependenciesElement == null) {
            return false;
        }

        Map<String, PluginUpgradeInfo> depUpgrades = getPluginDependencyUpgradesMap();

        return dependenciesElement
                .childElements(DEPENDENCY)
                .map(depElement -> {
                    String groupId = getChildText(depElement, GROUP_ID);
                    String artifactId = getChildText(depElement, ARTIFACT_ID);

                    if (groupId != null && artifactId != null) {
                        String depKey = groupId + ":" + artifactId;
                        PluginUpgradeInfo upgrade = depUpgrades.get(depKey);

                        if (upgrade != null) {
                            return upgradePluginVersion(
                                    depElement, upgrade, pomDocument, sectionName + "/plugin/dependencies", context);
                        }
                    }
                    return false;
                })
                .reduce(false, Boolean::logicalOr);
    }

    private Map<String, PluginUpgradeInfo> getPluginDependencyUpgradesMap() {
        return PLUGIN_DEPENDENCY_UPGRADES.stream()
                .collect(Collectors.toMap(
                        upgrade -> upgrade.groupId() + ":" + upgrade.artifactId(),
                        upgrade ->
                                new PluginUpgradeInfo(upgrade.groupId(), upgrade.artifactId(), upgrade.minVersion())));
    }

    /**
     * Simple version comparison to check if current version is below minimum version. This is a basic implementation
     * that works for most Maven plugin versions.
     */
    private boolean isVersionBelow(String currentVersion, String minVersion) {
        if (currentVersion == null || minVersion == null) {
            return false;
        }
        return getSession().parseVersion(currentVersion).compareTo(getSession().parseVersion(minVersion)) < 0;
    }

    /**
     * Helper method to get child element text.
     */
    private String getChildText(Element parent, String childName) {
        Element child = parent.childElement(childName).orElse(null);
        return child != null ? child.textContentTrimmed() : null;
    }

    /**
     * Gets the list of plugin upgrades to apply.
     */
    public static List<PluginUpgrade> getPluginUpgrades() {
        return PLUGIN_UPGRADES;
    }

    /**
     * Analyzes plugins using effective models built from the temp directory. Returns analysis results with two maps:
     * plugins needing pluginManagement entries and plugins needing direct build/plugins overrides.
     */
    private PluginAnalysisResults analyzePluginsUsingEffectiveModels(
            UpgradeContext context, Map<Path, Document> pomMap, Path tempDir) {
        Map<Path, Set<String>> managementResult = new HashMap<>();
        Map<Path, Set<String>> directOverrideResult = new HashMap<>();
        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgradesAsMap();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path originalPomPath = entry.getKey();

            try {
                // Find the corresponding temp POM path
                Path commonRoot = findCommonRoot(pomMap.keySet());
                Path relativePath = commonRoot.relativize(originalPomPath);
                Path tempPomPath = tempDir.resolve(relativePath);

                // Build effective model using Maven 4 API
                PluginAnalysis analysis = analyzeEffectiveModelForPlugins(context, tempPomPath, pluginUpgrades);

                // Determine where to add plugin management (last local parent)
                Path targetPom =
                        findLastLocalParentForPluginManagement(context, tempPomPath, pomMap, tempDir, commonRoot);

                if (targetPom != null) {
                    managementResult
                            .computeIfAbsent(targetPom, k -> new HashSet<>())
                            .addAll(analysis.needsManagement());
                    directOverrideResult
                            .computeIfAbsent(targetPom, k -> new HashSet<>())
                            .addAll(analysis.needsDirectOverride());

                    if (!analysis.needsManagement().isEmpty()) {
                        context.debug("Will add plugin management to " + targetPom + " for plugins: "
                                + analysis.needsManagement());
                    }
                    if (!analysis.needsDirectOverride().isEmpty()) {
                        context.debug("Will add direct plugin overrides to " + targetPom + " for plugins: "
                                + analysis.needsDirectOverride());
                    }
                }

            } catch (Exception e) {
                context.warning("Failed to analyze effective model for " + originalPomPath + ": " + e.getMessage());
            }
        }

        return new PluginAnalysisResults(managementResult, directOverrideResult);
    }

    /**
     * Converts PluginUpgradeInfo map to PluginUpgrade map for compatibility.
     */
    private Map<String, PluginUpgrade> getPluginUpgradesAsMap() {
        return PLUGIN_UPGRADES.stream()
                .collect(Collectors.toMap(
                        upgrade -> upgrade.groupId() + ":" + upgrade.artifactId(), upgrade -> upgrade));
    }

    private PluginAnalysis analyzeEffectiveModelForPlugins(
            UpgradeContext context, Path tempPomPath, Map<String, PluginUpgrade> pluginUpgrades) {
        Model effectiveModel = buildEffectiveModel(tempPomPath);
        return analyzePluginsFromEffectiveModel(context, effectiveModel, pluginUpgrades);
    }

    /**
     * Analyzes plugins from the effective model and determines which ones need upgrades. Separates plugins into those
     * overridable via pluginManagement and those requiring a direct build/plugins entry (because the version is set
     * explicitly in an inherited parent's build/plugins, not via pluginManagement).
     */
    private PluginAnalysis analyzePluginsFromEffectiveModel(
            UpgradeContext context, Model effectiveModel, Map<String, PluginUpgrade> pluginUpgrades) {
        Set<String> needsManagement = new HashSet<>();
        Set<String> needsDirectOverride = new HashSet<>();

        Build build = effectiveModel.getBuild();
        if (build != null) {
            // Collect managed plugin versions for comparison
            Map<String, String> managedVersions = new HashMap<>();
            PluginManagement pluginManagement = build.getPluginManagement();
            if (pluginManagement != null) {
                for (Plugin plugin : pluginManagement.getPlugins()) {
                    String pluginKey = getPluginKey(plugin);
                    managedVersions.put(pluginKey, plugin.getVersion());
                }
            }

            // Check build/plugins - these are the actual plugins used in the build
            for (Plugin plugin : build.getPlugins()) {
                String pluginKey = getPluginKey(plugin);
                PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
                if (upgrade != null) {
                    String effectiveVersion = plugin.getVersion();
                    if (isVersionBelow(effectiveVersion, upgrade.minVersion())) {
                        needsManagement.add(pluginKey);
                        String managedVersion = managedVersions.get(pluginKey);
                        if (managedVersion == null || !managedVersion.equals(effectiveVersion)) {
                            // Version differs from pluginManagement (or not in PM at all):
                            // the parent sets an explicit version in build/plugins that
                            // pluginManagement alone cannot override
                            needsDirectOverride.add(pluginKey);
                            context.debug("Plugin " + pluginKey + " version " + effectiveVersion
                                    + " has explicit version in inherited build/plugins"
                                    + " — needs direct override to " + upgrade.minVersion());
                        } else {
                            context.debug("Plugin " + pluginKey + " version " + effectiveVersion
                                    + " is managed via pluginManagement — needs upgrade to " + upgrade.minVersion());
                        }
                    }
                }
            }

            // Check build/pluginManagement/plugins for managed-only plugins
            if (pluginManagement != null) {
                for (Plugin plugin : pluginManagement.getPlugins()) {
                    String pluginKey = getPluginKey(plugin);
                    PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
                    if (upgrade != null && !needsManagement.contains(pluginKey)) {
                        String effectiveVersion = plugin.getVersion();
                        if (isVersionBelow(effectiveVersion, upgrade.minVersion())) {
                            needsManagement.add(pluginKey);
                            context.debug("Managed plugin " + pluginKey + " version " + effectiveVersion
                                    + " needs upgrade to " + upgrade.minVersion());
                        }
                    }
                }
            }
        }

        return new PluginAnalysis(needsManagement, needsDirectOverride);
    }

    /**
     * Gets the plugin key (groupId:artifactId) for a plugin, handling default groupId.
     */
    private String getPluginKey(Plugin plugin) {
        String groupId = plugin.getGroupId();
        String artifactId = plugin.getArtifactId();

        // Default groupId for Maven plugins
        if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
            groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
        }

        return groupId + ":" + artifactId;
    }

    /**
     * Finds the last local parent in the hierarchy where plugin management should be added. This implements the
     * algorithm: start with the effective model, check if parent is in pomMap, if so continue to its parent, else
     * that's the target.
     */
    private Path findLastLocalParentForPluginManagement(
            UpgradeContext context, Path tempPomPath, Map<Path, Document> pomMap, Path tempDir, Path commonRoot) {

        Model effectiveModel = buildEffectiveModel(tempPomPath);

        // Convert the temp path back to the original path
        Path relativePath = tempDir.relativize(tempPomPath);
        Path currentOriginalPath = commonRoot.resolve(relativePath);

        // Start with current POM as the candidate
        Path lastLocalParent = currentOriginalPath;

        // Walk up the parent hierarchy
        Model currentModel = effectiveModel;
        while (currentModel.getParent() != null) {
            Parent parent = currentModel.getParent();

            // Check if this parent is in our local pomMap
            Path parentPath = findParentInPomMap(parent, pomMap);
            if (parentPath != null) {
                // Parent is local, so it becomes our new candidate
                lastLocalParent = parentPath;

                Path parentTempPath = tempDir.resolve(commonRoot.relativize(parentPath));
                currentModel = buildEffectiveModel(parentTempPath);
            } else {
                // Parent is external, stop here
                break;
            }
        }

        context.debug("Last local parent for " + currentOriginalPath + " is " + lastLocalParent);
        return lastLocalParent;
    }

    /**
     * Finds a parent POM in the pomMap based on its coordinates.
     */
    private Path findParentInPomMap(Parent parent, Map<Path, Document> pomMap) {
        String parentGroupId = parent.getGroupId();
        String parentArtifactId = parent.getArtifactId();
        String parentVersion = parent.getVersion();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Document doc = entry.getValue();
            Element root = doc.root();

            // Extract GAV from this POM
            String groupId = getChildText(root, GROUP_ID);
            String artifactId = getChildText(root, ARTIFACT_ID);
            String version = getChildText(root, VERSION);

            // Handle inheritance from parent
            Element parentElement = root.childElement(PARENT).orElse(null);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = getChildText(parentElement, GROUP_ID);
                }
                if (version == null) {
                    version = getChildText(parentElement, VERSION);
                }
            }

            // Check if this POM matches the parent coordinates
            if (parentGroupId.equals(groupId) && parentArtifactId.equals(artifactId) && parentVersion.equals(version)) {
                return entry.getKey();
            }
        }

        return null; // Parent not found in local project
    }

    /**
     * Adds plugin management entries for plugins found through effective model analysis.
     */
    private boolean addPluginManagementForEffectivePlugins(
            UpgradeContext context, Document pomDocument, Set<String> pluginKeys, Set<String> localPluginKeys) {

        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgradesAsMap();
        boolean hasUpgrades = false;

        Element root = pomDocument.root();

        // Ensure build/pluginManagement/plugins structure exists
        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            buildElement = DomUtils.insertNewElement(BUILD, root);
        }

        Element pluginManagementElement =
                buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagementElement == null) {
            pluginManagementElement = DomUtils.insertNewElement(PLUGIN_MANAGEMENT, buildElement);
        }

        Element managedPluginsElement =
                pluginManagementElement.childElement(PLUGINS).orElse(null);
        if (managedPluginsElement == null) {
            managedPluginsElement = DomUtils.insertNewElement(PLUGINS, pluginManagementElement);
        }

        // Add plugin management entries for each plugin
        for (String pluginKey : pluginKeys) {
            PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                // Check if plugin is already managed
                if (!isPluginAlreadyManagedInElement(managedPluginsElement, upgrade)) {
                    boolean fromRemoteParent = !localPluginKeys.contains(pluginKey);
                    addPluginManagementEntryFromUpgrade(managedPluginsElement, upgrade, context, fromRemoteParent);
                    hasUpgrades = true;
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Checks if a plugin is already managed in the given plugins element.
     */
    private boolean isPluginAlreadyManagedInElement(Element pluginsElement, PluginUpgrade upgrade) {
        List<Element> pluginElements = pluginsElement.childElements(PLUGIN).toList();
        for (Element pluginElement : pluginElements) {
            String groupId = getChildText(pluginElement, GROUP_ID);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (upgrade.groupId().equals(groupId) && upgrade.artifactId().equals(artifactId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a plugin management entry from a PluginUpgrade.
     */
    private void addPluginManagementEntryFromUpgrade(
            Element managedPluginsElement, PluginUpgrade upgrade, UpgradeContext context, boolean fromRemoteParent) {
        Element plugin = DomUtils.createPlugin(
                managedPluginsElement, upgrade.groupId(), upgrade.artifactId(), upgrade.minVersion());

        if (fromRemoteParent) {
            new Editor(managedPluginsElement.document())
                    .insertCommentBefore(plugin, " Override version inherited from parent ");
            context.detail("Added plugin management for " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                    + upgrade.minVersion() + " (overrides version inherited from parent)");
        } else {
            context.detail("Added plugin management for " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                    + upgrade.minVersion() + " (found through effective model analysis)");
        }
    }

    /**
     * Adds direct plugin entries in build/plugins for plugins inherited from remote parents. This is necessary when a
     * parent POM sets an explicit version in its build/plugins that pluginManagement alone cannot override.
     */
    private boolean addDirectPluginOverrides(
            UpgradeContext context, Document pomDocument, Set<String> pluginKeys, Set<String> localPluginKeys) {
        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgradesAsMap();
        boolean hasUpgrades = false;

        Element root = pomDocument.root();

        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            buildElement = DomUtils.insertNewElement(BUILD, root);
        }

        Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
        if (pluginsElement == null) {
            pluginsElement = DomUtils.insertNewElement(PLUGINS, buildElement);
        }

        for (String pluginKey : pluginKeys) {
            PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);
            if (upgrade != null) {
                if (!isPluginAlreadyManagedInElement(pluginsElement, upgrade)) {
                    Element plugin = DomUtils.createPlugin(
                            pluginsElement, upgrade.groupId(), upgrade.artifactId(), upgrade.minVersion());
                    if (!localPluginKeys.contains(pluginKey)) {
                        new Editor(pluginsElement.document())
                                .insertCommentBefore(plugin, " Override version inherited from parent ");
                    }
                    hasUpgrades = true;
                    context.detail("Added " + upgrade.groupId() + ":" + upgrade.artifactId() + " version "
                            + upgrade.minVersion() + " in build/plugins (overrides version locked by parent)");
                }
            }
        }

        return hasUpgrades;
    }

    private Set<String> collectLocallyDeclaredPluginKeys(Map<Path, Document> pomMap) {
        Set<String> localPluginKeys = new HashSet<>();
        for (Document doc : pomMap.values()) {
            Element root = doc.root();
            Element buildElement = root.childElement(BUILD).orElse(null);
            if (buildElement != null) {
                Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
                if (pluginsElement != null) {
                    collectPluginKeysFromElement(pluginsElement, localPluginKeys);
                }
                Element pmElement = buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
                if (pmElement != null) {
                    Element managedPluginsElement =
                            pmElement.childElement(PLUGINS).orElse(null);
                    if (managedPluginsElement != null) {
                        collectPluginKeysFromElement(managedPluginsElement, localPluginKeys);
                    }
                }
            }
        }
        return localPluginKeys;
    }

    private void collectPluginKeysFromElement(Element pluginsElement, Set<String> keys) {
        pluginsElement.childElements(PLUGIN).forEach(pluginElement -> {
            String groupId = getChildText(pluginElement, GROUP_ID);
            String artifactId = getChildText(pluginElement, ARTIFACT_ID);
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }
            if (groupId != null && artifactId != null) {
                keys.add(groupId + ":" + artifactId);
            }
        });
    }

    private record PluginAnalysis(Set<String> needsManagement, Set<String> needsDirectOverride) {}

    private record PluginAnalysisResults(
            Map<Path, Set<String>> pluginsNeedingManagement, Map<Path, Set<String>> pluginsNeedingDirectOverride) {}

    /**
     * Checks if the given plugin is a Quarkus Maven plugin.
     */
    private boolean isQuarkusPlugin(String groupId, String artifactId) {
        return "quarkus-maven-plugin".equals(artifactId)
                && ("io.quarkus".equals(groupId) || "io.quarkus.platform".equals(groupId));
    }

    /**
     * Detects the Quarkus platform version used by the project.
     *
     * <p>Checks the following sources in order:
     * <ol>
     *   <li>{@code <dependencyManagement>} for {@code io.quarkus.platform:quarkus-bom}
     *       or {@code io.quarkus:quarkus-bom} — extracts the version (resolving property references)</li>
     *   <li>Properties: {@code quarkus.platform.version}, {@code quarkus.version},
     *       {@code quarkus-plugin.version}</li>
     * </ol>
     *
     * @param pomDocument the POM document to inspect
     * @return the detected Quarkus platform version string, or {@code null} if not found
     */
    String detectQuarkusPlatformVersion(Document pomDocument) {
        Element root = pomDocument.root();

        // 1. Check dependencyManagement for Quarkus BOM
        Element depManagement = root.childElement(DEPENDENCY_MANAGEMENT).orElse(null);
        if (depManagement != null) {
            Element dependencies = depManagement.childElement(DEPENDENCIES).orElse(null);
            if (dependencies != null) {
                String bomVersion = dependencies
                        .childElements(DEPENDENCY)
                        .filter(dep -> {
                            String gid = getChildText(dep, GROUP_ID);
                            String aid = getChildText(dep, ARTIFACT_ID);
                            return ("io.quarkus.platform".equals(gid) || "io.quarkus".equals(gid))
                                    && "quarkus-bom".equals(aid);
                        })
                        .map(dep -> getChildText(dep, VERSION))
                        .filter(v -> v != null && !v.isEmpty())
                        .findFirst()
                        .orElse(null);

                if (bomVersion != null) {
                    // Resolve property reference if needed
                    String resolved = resolvePropertyValue(root, bomVersion);
                    if (resolved != null) {
                        return resolved;
                    }
                }
            }
        }

        // 2. Check well-known properties
        Element propertiesElement = root.childElement(PROPERTIES).orElse(null);
        if (propertiesElement != null) {
            for (String propName : List.of("quarkus.platform.version", "quarkus.version", "quarkus-plugin.version")) {
                Element prop = propertiesElement.childElement(propName).orElse(null);
                if (prop != null) {
                    String value = prop.textContentTrimmed();
                    if (value != null && !value.isEmpty() && !value.startsWith("${")) {
                        return value;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Resolves a version string that may be a property reference (e.g., {@code ${quarkus.version}}).
     * Returns the resolved value, or the original string if not a property reference,
     * or {@code null} if the property cannot be resolved.
     */
    private String resolvePropertyValue(Element root, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (!value.startsWith("${") || !value.endsWith("}")) {
            return value;
        }
        String propertyName = value.substring(2, value.length() - 1);
        Element propertiesElement = root.childElement(PROPERTIES).orElse(null);
        if (propertiesElement != null) {
            Element prop = propertiesElement.childElement(propertyName).orElse(null);
            if (prop != null) {
                String resolved = prop.textContentTrimmed();
                if (resolved != null && !resolved.isEmpty() && !resolved.startsWith("${")) {
                    return resolved;
                }
            }
        }
        return null;
    }

    /**
     * Extracts the major version number from a version string (e.g., "2" from "2.16.7.Final").
     *
     * @return the major version number, or -1 if it cannot be parsed
     */
    private int extractMajorVersion(String version) {
        if (version == null || version.isEmpty()) {
            return -1;
        }
        String[] parts = version.split("\\.");
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Checks if a property is used as the version of a Quarkus BOM in dependencyManagement.
     * Quarkus BOMs are identified by having groupId io.quarkus or io.quarkus.platform,
     * type "pom", and scope "import".
     */
    private boolean isPropertyUsedByQuarkusBom(Document pomDocument, String propertyName) {
        Element root = pomDocument.root();
        String propertyRef = "${" + propertyName + "}";

        Element depManagement = root.childElement(DEPENDENCY_MANAGEMENT).orElse(null);
        if (depManagement == null) {
            return false;
        }
        Element dependencies = depManagement.childElement(DEPENDENCIES).orElse(null);
        if (dependencies == null) {
            return false;
        }

        return dependencies.childElements(DEPENDENCY).anyMatch(dep -> {
            String groupId = getChildText(dep, GROUP_ID);
            String version = getChildText(dep, VERSION);
            String type = getChildText(dep, "type");
            String scope = getChildText(dep, "scope");
            return ("io.quarkus".equals(groupId) || "io.quarkus.platform".equals(groupId))
                    && "pom".equals(type)
                    && "import".equals(scope)
                    && propertyRef.equals(version);
        });
    }

    /**
     * Decouples the Quarkus plugin version from a shared BOM property.
     * Introduces a new property for the plugin version and updates the plugin's version element,
     * leaving the BOM property unchanged.
     */
    private boolean decoupleQuarkusPluginVersion(
            Document pomDocument,
            Element versionElement,
            String sharedPropertyName,
            PluginUpgradeInfo upgrade,
            String sectionName,
            UpgradeContext context) {

        // Resolve the current version from the shared property
        Editor editor = new Editor(pomDocument);
        Element root = editor.root();
        Element propertiesElement = root.childElement(PROPERTIES).orElse(null);
        String currentVersion = null;
        if (propertiesElement != null) {
            Element sharedProp =
                    propertiesElement.childElement(sharedPropertyName).orElse(null);
            if (sharedProp != null) {
                currentVersion = sharedProp.textContentTrimmed();
            }
        }

        if (currentVersion == null) {
            // Property is inherited from parent — we cannot resolve its actual value here,
            // so skip decoupling to avoid introducing a potentially unnecessary property
            // that could downgrade an already-sufficient inherited version.
            context.debug("Shared property " + sharedPropertyName
                    + " not found in current POM (may be inherited) — skipping version decoupling");
            return false;
        }

        if (!isVersionBelow(currentVersion, upgrade.minVersion)) {
            context.debug("Quarkus plugin version (via shared property " + sharedPropertyName + ") " + currentVersion
                    + " is already >= " + upgrade.minVersion);
            return false;
        }

        // Introduce a new property for the plugin version
        String newPropertyName = "quarkus-plugin.version";
        if (propertiesElement == null) {
            propertiesElement = DomUtils.insertNewElement(PROPERTIES, root);
        }

        // Add the new property if it doesn't already exist
        Element existingProp = propertiesElement.childElement(newPropertyName).orElse(null);
        if (existingProp != null) {
            // Property already exists — update its value
            editor.setTextContent(existingProp, upgrade.minVersion);
        } else {
            DomUtils.insertContentElement(propertiesElement, newPropertyName, upgrade.minVersion);
        }

        // Update the plugin's version element to reference the new property
        editor.setTextContent(versionElement, "${" + newPropertyName + "}");

        context.detail("Decoupled " + upgrade.groupId + ":" + upgrade.artifactId + " version from shared property "
                + sharedPropertyName + ": introduced " + newPropertyName + "=" + upgrade.minVersion + " in "
                + sectionName);

        // Emit version gap warning
        if (currentVersion != null) {
            emitVersionGapWarning(context, currentVersion, upgrade.minVersion);
        }

        return true;
    }

    /**
     * Emits a warning if the Quarkus platform BOM version is significantly older than the plugin version.
     * Mismatched plugin and platform versions may cause unexpected behavior because the plugin
     * is designed and tested against a specific platform version.
     */
    private void emitVersionGapWarning(UpgradeContext context, String platformVersion, String pluginVersion) {
        // Only warn when there's a meaningful gap (different minor version)
        String platformMinor = extractMinorVersion(platformVersion);
        String pluginMinor = extractMinorVersion(pluginVersion);

        if (platformMinor != null && pluginMinor != null && !platformMinor.equals(pluginMinor)) {
            context.warning("quarkus-maven-plugin upgraded to " + pluginVersion
                    + " for Maven 4 compatibility. Your Quarkus platform is still at " + platformVersion
                    + ". Consider upgrading the platform to match — mismatched plugin and platform"
                    + " versions may cause unexpected behavior.");
        }
    }

    /**
     * Extracts the minor version component (e.g., "26" from "3.26.0").
     */
    private String extractMinorVersion(String version) {
        if (version == null) {
            return null;
        }
        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }

    /**
     * Holds plugin upgrade information for Maven 4 compatibility. This class contains the minimum version requirements
     * for plugins that need to be upgraded to work properly with Maven 4.
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
         * @param groupId
         *            the Maven groupId of the plugin
         * @param artifactId
         *            the Maven artifactId of the plugin
         * @param minVersion
         *            the minimum version required for Maven 4 compatibility
         */
        PluginUpgradeInfo(String groupId, String artifactId, String minVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.minVersion = minVersion;
        }
    }
}
