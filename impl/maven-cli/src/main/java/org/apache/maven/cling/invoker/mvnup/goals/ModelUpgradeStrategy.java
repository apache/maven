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
import java.util.Map;
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.EXECUTIONS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODEL_VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECTS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_0_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_1_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Namespaces.MAVEN_4_0_0_NAMESPACE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Namespaces.MAVEN_4_1_0_NAMESPACE;
import static org.apache.maven.cling.invoker.mvnup.goals.ModelVersionUtils.getSchemaLocationForModelVersion;

/**
 * Strategy for upgrading Maven model versions (e.g., 4.0.0 → 4.1.0).
 * Handles namespace updates, schema location changes, and element conversions.
 */
@Named
@Singleton
@Priority(40)
public class ModelUpgradeStrategy extends AbstractUpgradeStrategy {

    public ModelUpgradeStrategy() {
        // Target model version will be determined from context
    }

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        // Handle --all option (overrides individual options)
        if (options.all().orElse(false)) {
            return true;
        }

        String targetModel = determineTargetModelVersion(context);
        // Only applicable if we're not staying at 4.0.0
        return !MODEL_VERSION_4_0_0.equals(targetModel);
    }

    @Override
    public String getDescription() {
        return "Upgrading POM model version";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        String targetModelVersion = determineTargetModelVersion(context);

        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            String currentVersion = ModelVersionUtils.detectModelVersion(pomDocument);
            context.info(pomPath + " (current: " + currentVersion + ")");
            context.indent();

            try {
                if (currentVersion.equals(targetModelVersion)) {
                    context.success("Already at target version " + targetModelVersion);
                } else if (ModelVersionUtils.canUpgrade(currentVersion, targetModelVersion)) {
                    context.action("Upgrading from " + currentVersion + " to " + targetModelVersion);

                    // Perform the actual upgrade
                    context.indent();
                    try {
                        Document upgradedDocument =
                                performModelUpgrade(pomDocument, context, currentVersion, targetModelVersion);
                        // Update the map with the modified document
                        pomMap.put(pomPath, upgradedDocument);
                    } finally {
                        context.unindent();
                    }
                    context.success("Model upgrade completed");
                    modifiedPoms.add(pomPath);
                } else {
                    // Treat invalid upgrades (including downgrades) as errors, not warnings
                    context.failure("Cannot upgrade from " + currentVersion + " to " + targetModelVersion);
                    errorPoms.add(pomPath);
                }
            } catch (Exception e) {
                context.failure("Model upgrade failed: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Performs the core model upgrade from current version to target version.
     * This includes namespace updates and module conversion using domtrip.
     * Returns the upgraded document.
     */
    private Document performModelUpgrade(
            Document pomDocument, UpgradeContext context, String currentVersion, String targetModelVersion) {
        // Create Editor from domtrip Document
        Editor editor = new Editor(pomDocument);

        // Update model version element
        Element root = editor.root();
        Element modelVersionElement = root.child(MODEL_VERSION).orElse(null);
        if (modelVersionElement != null) {
            editor.setTextContent(modelVersionElement, targetModelVersion);
            context.detail("Updated modelVersion to " + targetModelVersion);
        } else {
            // Create new modelVersion element if it doesn't exist
            DomUtils.insertContentElement(root, MODEL_VERSION, targetModelVersion);
            context.detail("Added modelVersion " + targetModelVersion);
        }

        // Update namespace and schema location
        upgradeNamespaceAndSchemaLocation(editor, context, targetModelVersion);

        // Convert modules to subprojects (for 4.1.0 and higher)
        if (ModelVersionUtils.isVersionGreaterOrEqual(targetModelVersion, MODEL_VERSION_4_1_0)) {
            convertModulesToSubprojects(editor, context);
            upgradeDeprecatedPhases(editor, context);
        }

        // Return the modified document from the editor
        return editor.document();
    }

    /**
     * Updates namespace and schema location for the target model version using domtrip.
     */
    private void upgradeNamespaceAndSchemaLocation(Editor editor, UpgradeContext context, String targetModelVersion) {
        Element root = editor.root();
        if (root == null) {
            return;
        }

        // Update namespace based on target model version
        String targetNamespace = getNamespaceForModelVersion(targetModelVersion);

        // Use element's attribute method to set the namespace declaration
        // This modifies the element in place and marks it as modified
        root.attribute("xmlns", targetNamespace);
        context.detail("Updated namespace to " + targetNamespace);

        // Update schema location if present
        String currentSchemaLocation = root.attribute("xsi:schemaLocation");
        if (currentSchemaLocation != null) {
            String newSchemaLocation = getSchemaLocationForModelVersion(targetModelVersion);
            root.attribute("xsi:schemaLocation", newSchemaLocation);
            context.detail("Updated xsi:schemaLocation");
        }
    }

    /**
     * Converts modules to subprojects for 4.1.0 compatibility using domtrip.
     */
    private void convertModulesToSubprojects(Editor editor, UpgradeContext context) {
        Element root = editor.root();
        if (root == null) {
            return;
        }

        // Convert modules element to subprojects
        Element modulesElement = root.child(MODULES).orElse(null);
        if (modulesElement != null) {
            // domtrip makes this much simpler - just change the element name
            // The formatting and structure are preserved automatically
            modulesElement.name(SUBPROJECTS);
            context.detail("Converted <modules> to <subprojects>");

            // Convert all module children to subproject
            var moduleElements = modulesElement.children(MODULE).toList();
            for (Element moduleElement : moduleElements) {
                moduleElement.name(SUBPROJECT);
            }

            if (!moduleElements.isEmpty()) {
                context.detail("Converted " + moduleElements.size() + " <module> elements to <subproject>");
            }
        }

        // Also check inside profiles
        Element profilesElement = root.child(PROFILES).orElse(null);
        if (profilesElement != null) {
            var profileElements = profilesElement.children(PROFILE).toList();
            for (Element profileElement : profileElements) {
                Element profileModulesElement = profileElement.child(MODULES).orElse(null);
                if (profileModulesElement != null) {
                    profileModulesElement.name(SUBPROJECTS);

                    var profileModuleElements =
                            profileModulesElement.children(MODULE).toList();
                    for (Element moduleElement : profileModuleElements) {
                        moduleElement.name(SUBPROJECT);
                    }

                    if (!profileModuleElements.isEmpty()) {
                        context.detail("Converted " + profileModuleElements.size()
                                + " <module> elements to <subproject> in profiles");
                    }
                }
            }
        }
    }

    /**
     * Determines the target model version from the upgrade context.
     */
    private String determineTargetModelVersion(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.modelVersion().isPresent()) {
            return options.modelVersion().get();
        } else if (options.all().orElse(false)) {
            return MODEL_VERSION_4_1_0;
        } else {
            return MODEL_VERSION_4_0_0;
        }
    }

    /**
     * Gets the namespace URI for a model version.
     */
    private String getNamespaceForModelVersion(String modelVersion) {
        if (MavenPomElements.ModelVersions.MODEL_VERSION_4_2_0.equals(modelVersion)) {
            return MavenPomElements.Namespaces.MAVEN_4_2_0_NAMESPACE;
        } else if (MODEL_VERSION_4_1_0.equals(modelVersion)) {
            return MAVEN_4_1_0_NAMESPACE;
        } else {
            return MAVEN_4_0_0_NAMESPACE;
        }
    }

    /**
     * Upgrades deprecated Maven 3 phase names to Maven 4 equivalents.
     * This replaces pre-/post- phases with before:/after: phases.
     */
    private void upgradeDeprecatedPhases(Editor editor, UpgradeContext context) {
        // Create mapping of deprecated phases to their Maven 4 equivalents
        Map<String, String> phaseUpgrades = createPhaseUpgradeMap();

        Element root = editor.root();
        if (root == null) {
            return;
        }

        int totalUpgrades = 0;

        // Upgrade phases in main build section
        Element buildElement = root.child(BUILD).orElse(null);
        if (buildElement != null) {
            totalUpgrades += upgradePhaseElements(buildElement, phaseUpgrades, context);
        }

        // Upgrade phases in profiles
        Element profilesElement = root.child(PROFILES).orElse(null);
        if (profilesElement != null) {
            var profileElements = profilesElement.children(PROFILE).toList();
            for (Element profileElement : profileElements) {
                Element profileBuildElement = profileElement.child(BUILD).orElse(null);
                if (profileBuildElement != null) {
                    totalUpgrades += upgradePhaseElements(profileBuildElement, phaseUpgrades, context);
                }
            }
        }

        if (totalUpgrades > 0) {
            context.detail("Upgraded " + totalUpgrades + " deprecated phase name(s) to Maven 4 equivalents");
        }
    }

    /**
     * Creates the mapping of deprecated phase names to their Maven 4 equivalents.
     * Uses Maven API constants to ensure consistency with the lifecycle definitions.
     */
    private Map<String, String> createPhaseUpgradeMap() {
        Map<String, String> phaseUpgrades = new HashMap<>();

        // Clean lifecycle aliases
        phaseUpgrades.put("pre-clean", Lifecycle.BEFORE + Lifecycle.Phase.CLEAN);
        phaseUpgrades.put("post-clean", Lifecycle.AFTER + Lifecycle.Phase.CLEAN);

        // Default lifecycle aliases
        phaseUpgrades.put("pre-integration-test", Lifecycle.BEFORE + Lifecycle.Phase.INTEGRATION_TEST);
        phaseUpgrades.put("post-integration-test", Lifecycle.AFTER + Lifecycle.Phase.INTEGRATION_TEST);

        // Site lifecycle aliases
        phaseUpgrades.put("pre-site", Lifecycle.BEFORE + Lifecycle.SITE);
        phaseUpgrades.put("post-site", Lifecycle.AFTER + Lifecycle.SITE);

        return phaseUpgrades;
    }

    /**
     * Upgrades phase elements within a build section.
     */
    private int upgradePhaseElements(Element buildElement, Map<String, String> phaseUpgrades, UpgradeContext context) {
        if (buildElement == null) {
            return 0;
        }

        int upgrades = 0;

        // Check plugins section
        Element pluginsElement = buildElement.child(PLUGINS).orElse(null);
        if (pluginsElement != null) {
            upgrades += upgradePhaseElementsInPlugins(pluginsElement, phaseUpgrades, context);
        }

        // Check pluginManagement section
        Element pluginManagementElement = buildElement.child(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagementElement != null) {
            Element managedPluginsElement =
                    pluginManagementElement.child(PLUGINS).orElse(null);
            if (managedPluginsElement != null) {
                upgrades += upgradePhaseElementsInPlugins(managedPluginsElement, phaseUpgrades, context);
            }
        }

        return upgrades;
    }

    /**
     * Upgrades phase elements within a plugins section.
     */
    private int upgradePhaseElementsInPlugins(
            Element pluginsElement, Map<String, String> phaseUpgrades, UpgradeContext context) {
        int upgrades = 0;

        var pluginElements = pluginsElement.children(PLUGIN).toList();
        for (Element pluginElement : pluginElements) {
            Element executionsElement = pluginElement.child(EXECUTIONS).orElse(null);
            if (executionsElement != null) {
                var executionElements = executionsElement
                        .children(MavenPomElements.Elements.EXECUTION)
                        .toList();
                for (Element executionElement : executionElements) {
                    Element phaseElement = executionElement
                            .child(MavenPomElements.Elements.PHASE)
                            .orElse(null);
                    if (phaseElement != null) {
                        String currentPhase = phaseElement.textContent().trim();
                        String newPhase = phaseUpgrades.get(currentPhase);
                        if (newPhase != null) {
                            phaseElement.textContent(newPhase);
                            context.detail("Upgraded phase: " + currentPhase + " → " + newPhase);
                            upgrades++;
                        }
                    }
                }
            }
        }

        return upgrades;
    }
}
