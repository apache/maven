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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.cling.invoker.mvnup.Goal;
import org.apache.maven.cling.invoker.mvnup.PomDiscovery;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.apache.maven.cling.invoker.mvnup.UpgradeInvokerRequest;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Base class for upgrade goals containing shared functionality.
 * Subclasses only differ in whether they save modifications to disk.
 *
 * <h2>Supported Upgrades</h2>
 *
 * <h3>Model Version Upgrades</h3>
 * <ul>
 *   <li><strong>4.0.0 → 4.1.0</strong>: Upgrades Maven 3.x POMs to Maven 4.1.0 format</li>
 * </ul>
 *
 * <h3>4.0.0 → 4.1.0 Upgrade Process</h3>
 * <ol>
 *   <li><strong>Namespace Update</strong>: Changes namespace from Maven 4.0.0 to 4.1.0 for all elements</li>
 *   <li><strong>Schema Location Update</strong>: Updates xsi:schemaLocation to Maven 4.1.0 XSD</li>
 *   <li><strong>Module Conversion</strong>: Converts {@code <modules>} to {@code <subprojects>} and {@code <module>} to {@code <subproject>}</li>
 *   <li><strong>Model Version Update</strong>: Updates {@code <modelVersion>} to 4.1.0</li>
 * </ol>
 *
 * <h3>Default Behavior</h3>
 * If no specific options are provided, the tool applies {@code --fix-model} and {@code --plugins} by default to ensure Maven 4 compatibility.
 *
 * <h3>All-in-One Option</h3>
 * The {@code --all} option is a convenience flag equivalent to {@code --model 4.1.0 --infer --fix-model --plugins}.
 * It performs a complete upgrade to Maven 4.1.0 with all optimizations, compatibility fixes, and plugin upgrades.
 *
 * <h3>Maven 4 Compatibility Fixes</h3>
 * When {@code --fix-model} option is enabled (or by default), applies fixes for Maven 4 compatibility issues:
 * <ul>
 *   <li><strong>Unsupported combine.children Attributes</strong>: Changes 'override' to 'merge' (Maven 4 only supports 'append' and 'merge')</li>
 *   <li><strong>Unsupported combine.self Attributes</strong>: Changes 'append' to 'merge' (Maven 4 only supports 'override', 'merge', and 'remove')</li>
 *   <li><strong>Duplicate Dependencies</strong>: Removes duplicate dependency declarations that Maven 4 strictly validates</li>
 *   <li><strong>Unsupported Repository Expressions</strong>: Comments out repositories with expressions not supported by Maven 4</li>
 * </ul>
 *
 * <h3>Plugin Upgrades</h3>
 * When {@code --plugins} option is enabled (or by default), upgrades plugins known to fail with Maven 4:
 * <ul>
 *   <li><strong>maven-exec-plugin</strong>: Upgrades to version 3.2.0 or higher</li>
 *   <li><strong>maven-enforcer-plugin</strong>: Upgrades to version 3.0.0 or higher</li>
 *   <li><strong>flatten-maven-plugin</strong>: Upgrades to version 1.2.7 or higher</li>
 *   <li><strong>maven-shade-plugin</strong>: Upgrades to version 3.5.0 or higher</li>
 * </ul>
 * Plugin versions are upgraded in both {@code <build><plugins>} and {@code <build><pluginManagement><plugins>} sections.
 * If a plugin version is defined via a property, the property value is updated instead.
 *
 * <h3>Inference Optimizations (Optional)</h3>
 * When {@code --infer} option is enabled, applies inference optimizations to remove redundant information:
 *
 * <h4>Limited Inference for 4.0.0 Models (Maven 3.x POMs)</h4>
 * <ul>
 *   <li><strong>Child GroupId Removal</strong>: Removes child {@code <groupId>} when it matches parent groupId</li>
 *   <li><strong>Child Version Removal</strong>: Removes child {@code <version>} when it matches parent version</li>
 * </ul>
 *
 * <h4>Full Inference for 4.1.0+ Models</h4>
 * <ul>
 *   <li><strong>ModelVersion Removal</strong>: Removes {@code <modelVersion>} element (inference enabled)</li>
 *   <li><strong>Root Attribute</strong>: Adds {@code root="true"} attribute to root project</li>
 *   <li><strong>Parent Element Trimming</strong>:
 *     <ul>
 *       <li>Removes parent {@code <groupId>} when child has no explicit groupId</li>
 *       <li>Removes parent {@code <version>} when child has no explicit version</li>
 *       <li>Removes parent {@code <artifactId>} when it can be inferred from relativePath</li>
 *     </ul>
 *   </li>
 *   <li><strong>Managed Dependencies Cleanup</strong>: Removes managed dependencies pointing to project artifacts</li>
 *   <li><strong>Dependency Inference</strong>:
 *     <ul>
 *       <li>Removes dependency {@code <version>} when it points to a project artifact</li>
 *       <li>Removes dependency {@code <groupId>} when it points to a project artifact</li>
 *       <li>Applies to main dependencies, profile dependencies, and plugin dependencies</li>
 *     </ul>
 *   </li>
 *   <li><strong>Subprojects List Removal</strong>: Removes redundant {@code <subprojects>} lists that match direct child directories</li>
 * </ul>
 *
 * <h3>Multi-Module Project Support</h3>
 * <ul>
 *   <li><strong>POM Discovery</strong>: Recursively discovers all POM files in the project structure</li>
 *   <li><strong>GAV Resolution</strong>: Computes GroupId, ArtifactId, Version for all project artifacts with parent inheritance</li>
 *   <li><strong>Cross-Module Inference</strong>: Uses knowledge of all project artifacts for intelligent inference decisions</li>
 *   <li><strong>RelativePath Resolution</strong>: Resolves parent POMs via relativePath for artifactId inference</li>
 * </ul>
 *
 * <h3>Format Preservation</h3>
 * <ul>
 *   <li><strong>Whitespace Preservation</strong>: Maintains original formatting when removing elements</li>
 *   <li><strong>Comment Preservation</strong>: Preserves XML comments and processing instructions</li>
 *   <li><strong>Line Separator Handling</strong>: Uses system-appropriate line separators</li>
 * </ul>
 */
public abstract class BaseUpgradeGoal implements Goal {

    /**
     * Executes the upgrade goal.
     * Template method that calls doUpgrade and optionally saves modifications.
     */
    @Override
    public int execute(UpgradeContext context) throws Exception {
        UpgradeInvokerRequest request = (UpgradeInvokerRequest) context.invokerRequest;
        UpgradeOptions options = request.options();

        // Determine target model version
        // Default to 4.0.0 unless --all is specified or explicit --model is provided
        String targetModel;
        if (options.model().isPresent()) {
            targetModel = options.model().get();
        } else if (options.all().orElse(false)) {
            targetModel = "4.1.0";
        } else {
            targetModel = "4.0.0";
        }

        if (!isValidModelVersion(targetModel)) {
            context.logger.error("Invalid target model version: " + targetModel);
            context.logger.error("Supported versions: 4.0.0, 4.1.0");
            return 1;
        }

        // Discover POMs
        context.logger.info("Discovering POM files...");
        Path startingDirectory = options.directory().map(Paths::get).orElse(context.invokerRequest.cwd());

        Map<Path, Document> pomMap;
        try {
            pomMap = PomDiscovery.discoverPoms(startingDirectory);
        } catch (IOException | JDOMException e) {
            context.logger.error("Failed to discover POM files: " + e.getMessage());
            return 1;
        }

        if (pomMap.isEmpty()) {
            context.logger.warn("No POM files found in " + startingDirectory);
            return 0;
        }

        context.logger.info("Found " + pomMap.size() + " POM file(s)");

        // Perform the upgrade logic
        int result = doUpgrade(context, targetModel, pomMap);

        // Save modifications if this is an apply goal
        if (shouldSaveModifications() && result == 0) {
            saveModifications(context, pomMap);
        }

        return result;
    }

    /**
     * Performs the upgrade logic. Subclasses can override for specific behavior.
     */
    protected int doUpgrade(UpgradeContext context, String targetModel, Map<Path, Document> pomMap) {
        UpgradeOptions options = ((UpgradeInvokerRequest) context.invokerRequest).options();

        // Handle --all option (overrides individual options)
        boolean useAll = options.all().orElse(false);
        boolean useInference = useAll || options.infer().orElse(false);
        boolean useFixModel = useAll || options.fixModel().orElse(false);
        boolean usePlugins = useAll || options.plugins().orElse(false);

        // Apply default behavior: if no specific options are provided, enable --fix-model and --plugins
        if (!useAll
                && !options.infer().isPresent()
                && !options.fixModel().isPresent()
                && !options.plugins().isPresent()
                && !options.model().isPresent()) {
            useFixModel = true;
            usePlugins = true;
        }

        // Determine if we should perform model upgrades
        boolean performModelUpgrade = !"4.0.0".equals(targetModel);

        String action = shouldSaveModifications() ? "Upgrading" : "Checking";
        if (performModelUpgrade) {
            context.logger.info(action + " POM model to: " + targetModel);
        } else {
            context.logger.info(action + " POMs for Maven 4 compatibility (keeping current model versions)");
        }

        if (useAll) {
            context.logger.info(
                    "Using --all option (model upgrade + inference + compatibility fixes + plugin upgrades)");
        } else if ((useFixModel && !options.fixModel().isPresent())
                || (usePlugins && !options.plugins().isPresent())) {
            context.logger.info("Applying default behavior: Maven 4 compatibility fixes and plugin upgrades");
        }
        context.logger.info("");

        // Step 1: Analyze each POM and perform upgrade (only if model upgrade is requested)
        int upgradeableCount = 0;
        int alreadyTargetCount = 0;
        int incompatibleCount = 0;

        if (performModelUpgrade) {
            for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
                Path pomPath = entry.getKey();
                Document pomDocument = entry.getValue();

                String currentVersion = detectModelVersion(context, pomDocument);
                context.logger.info("  " + pomPath + " (current: " + currentVersion + ")");

                if (currentVersion.equals(targetModel)) {
                    context.logger.info("    ✓ Already at target version " + targetModel);
                    alreadyTargetCount++;
                } else if (canUpgrade(currentVersion, targetModel)) {
                    context.logger.info("    → " + action + " from " + currentVersion + " to " + targetModel);
                    upgradeableCount++;

                    // Perform the actual upgrade
                    if ("4.0.0".equals(currentVersion) && "4.1.0".equals(targetModel)) {
                        upgradeFrom400To410(context, pomPath, pomDocument);
                    }
                } else {
                    context.logger.warn("    ✗ Cannot upgrade from " + currentVersion + " to " + targetModel);
                    incompatibleCount++;
                }
            }
        } else {
            // When no model upgrade is requested, skip model version analysis entirely
            // Just count the POMs for summary purposes
            alreadyTargetCount = pomMap.size();
        }

        // Step 2: Apply inference if enabled (only for models >= 4.1.0)
        if (useInference) {
            applyInferenceToEligiblePoms(context, pomMap);
        }

        // Step 3: Apply Maven 4 compatibility fixes if enabled
        if (useFixModel) {
            applyMaven4CompatibilityFixes(context, pomMap);
        }

        // Step 4: Apply plugin upgrades if enabled
        if (usePlugins) {
            applyPluginUpgrades(context, pomMap);
        }

        // Summary
        context.logger.info("");
        context.logger.info("Summary:");
        if (performModelUpgrade) {
            context.logger.info("  " + alreadyTargetCount + " POM(s) already at target version " + targetModel);
            context.logger.info("  " + upgradeableCount + " POM(s) "
                    + (shouldSaveModifications() ? "upgraded" : "can be upgraded") + " to " + targetModel);
            context.logger.info("  " + incompatibleCount + " POM(s) cannot be upgraded to " + targetModel);

            if (!shouldSaveModifications() && upgradeableCount > 0) {
                context.logger.info("");
                context.logger.info("To apply the upgrade, run:");
                context.logger.info("  mvnup apply --model=" + targetModel);
            }
        } else {
            // Suggest apply command with the same options that were used for check
            if (!shouldSaveModifications() && (useFixModel || usePlugins)) {
                context.logger.info("");
                context.logger.info("To apply the changes, run:");
                StringBuilder applyCommand = new StringBuilder("  mvnup apply");

                // Include explicit options that were specified
                if (options.fixModel().isPresent()) {
                    applyCommand.append(" --fix-model");
                }
                if (options.plugins().isPresent()) {
                    applyCommand.append(" --plugins");
                }
                if (options.infer().isPresent()) {
                    applyCommand.append(" --infer");
                }

                // If no explicit options were specified, this was default behavior
                if (!options.fixModel().isPresent()
                        && !options.plugins().isPresent()
                        && !options.infer().isPresent()
                        && !options.model().isPresent()) {
                    // Default behavior - no options needed, just "mvnup apply"
                }

                context.logger.info(applyCommand.toString());
            }
        }

        return 0;
    }

    /**
     * Applies inference to all POMs that are eligible (model version >= 4.1.0).
     * This is done as a second step after any upgrades have been performed.
     */
    protected void applyInferenceToEligiblePoms(UpgradeContext context, Map<Path, Document> pomMap) {
        context.logger.info("");
        context.logger.info("Applying inference to eligible POMs (model >= 4.1.0)...");

        int inferenceAppliedCount = 0;
        int ineligibleCount = 0;

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            String currentVersion = detectModelVersion(context, pomDocument);

            if (isModelVersionEligibleForInference(currentVersion)) {
                context.logger.info("  " + pomPath + " (applying inference to " + currentVersion + ")");
                try {
                    applyInference(context, pomDocument, pomPath, pomMap);
                    inferenceAppliedCount++;
                    context.logger.info("    ✓ Inference applied successfully");
                } catch (Exception e) {
                    context.logger.error("    ✗ Inference failed: " + e.getMessage());
                }
            } else {
                context.logger.info(
                        "  " + pomPath + " (skipping - model " + currentVersion + " not eligible for inference)");
                ineligibleCount++;
            }
        }

        context.logger.info("");
        context.logger.info("Inference Summary:");
        context.logger.info("  " + inferenceAppliedCount + " POM(s) had inference applied");
        context.logger.info("  " + ineligibleCount + " POM(s) were not eligible for inference");
    }

    /**
     * Applies Maven 4 compatibility fixes to all POMs.
     * This fixes issues that prevent POMs from being processed by Maven 4.
     */
    protected void applyMaven4CompatibilityFixes(UpgradeContext context, Map<Path, Document> pomMap) {
        context.logger.info("");
        context.logger.info("Applying Maven 4 compatibility fixes...");

        int fixedCount = 0;
        int noFixesNeededCount = 0;

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            context.logger.info("  " + pomPath + " (checking for Maven 4 compatibility issues)");

            boolean hasIssues = false;
            try {
                // Fix unsupported combine.children attributes
                if (fixUnsupportedCombineChildrenAttributes(context, pomDocument)) {
                    hasIssues = true;
                }

                // Fix unsupported combine.self attributes
                if (fixUnsupportedCombineSelfAttributes(context, pomDocument)) {
                    hasIssues = true;
                }

                // Fix duplicate dependencies
                if (fixDuplicateDependencies(context, pomDocument)) {
                    hasIssues = true;
                }

                // Fix duplicate plugins
                if (fixDuplicatePlugins(context, pomDocument)) {
                    hasIssues = true;
                }

                // Fix unsupported expressions in repository URLs
                if (fixUnsupportedRepositoryExpressions(context, pomDocument)) {
                    hasIssues = true;
                }

                // Fix incorrect parent relative paths
                if (fixIncorrectParentRelativePaths(context, pomDocument, pomPath, pomMap)) {
                    hasIssues = true;
                }

                if (hasIssues) {
                    fixedCount++;
                    context.logger.info("    ✓ Maven 4 compatibility issues fixed");
                } else {
                    noFixesNeededCount++;
                    context.logger.info("    ✓ No Maven 4 compatibility issues found");
                }
            } catch (Exception e) {
                context.logger.error("    ✗ Failed to fix Maven 4 compatibility issues: " + e.getMessage());
            }
        }

        context.logger.info("");
        context.logger.info("Maven 4 Compatibility Fixes Summary:");
        context.logger.info("  " + fixedCount + " POM(s) had compatibility issues fixed");
        context.logger.info("  " + noFixesNeededCount + " POM(s) had no compatibility issues");
    }

    /**
     * Applies plugin upgrades to all POMs.
     * This upgrades plugins known to fail with Maven 4 to their minimum compatible versions.
     */
    protected void applyPluginUpgrades(UpgradeContext context, Map<Path, Document> pomMap) {
        context.logger.info("");
        context.logger.info("Applying plugin upgrades for Maven 4 compatibility...");

        int upgradedCount = 0;
        int noUpgradesNeededCount = 0;

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            context.logger.info("  " + pomPath + " (checking for plugin upgrades)");

            boolean hasUpgrades = false;
            try {
                // Upgrade plugins in build/plugins and build/pluginManagement/plugins
                if (upgradePluginsInPom(context, pomDocument)) {
                    hasUpgrades = true;
                }

                if (hasUpgrades) {
                    upgradedCount++;
                    context.logger.info("    ✓ Plugin upgrades applied");
                } else {
                    noUpgradesNeededCount++;
                    context.logger.info("    ✓ No plugin upgrades needed");
                }
            } catch (Exception e) {
                context.logger.error("    ✗ Failed to upgrade plugins: " + e.getMessage());
            }
        }

        context.logger.info("");
        context.logger.info("Plugin Upgrades Summary:");
        context.logger.info("  " + upgradedCount + " POM(s) had plugins upgraded");
        context.logger.info("  " + noUpgradesNeededCount + " POM(s) had no plugin upgrades needed");
    }

    /**
     * Determines if a model version is eligible for inference.
     * - 4.1.0+ models: Full inference support
     * - 4.0.0 models: Limited inference support (parent groupId/version inheritance only)
     */
    protected boolean isModelVersionEligibleForInference(String modelVersion) {
        // 4.1.0+ models have full inference support
        if ("4.1.0".equals(modelVersion)) {
            return true;
        }
        // Future versions: || "4.2.0".equals(modelVersion)

        // 4.0.0 models (Maven 3.x POMs) have limited inference support when explicitly requested
        if ("4.0.0".equals(modelVersion)) {
            return true;
        }

        return false;
    }

    /**
     * Determines whether modifications should be saved to disk.
     * Apply goals return true, Check goals return false.
     */
    protected abstract boolean shouldSaveModifications();

    /**
     * Saves the modified documents to disk.
     */
    protected void saveModifications(UpgradeContext context, Map<Path, Document> pomMap) {
        context.logger.info("");
        context.logger.info("Saving modified POMs...");

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document document = entry.getValue();
            try {
                String content = Files.readString(entry.getKey(), StandardCharsets.UTF_8);
                int startIndex = content.indexOf("<" + document.getRootElement().getName());
                String head = startIndex >= 0 ? content.substring(0, startIndex) : "";
                String lastTag = document.getRootElement().getName() + ">";
                int endIndex = content.lastIndexOf(lastTag);
                String tail = endIndex >= 0 ? content.substring(endIndex + lastTag.length()) : "";
                Format format = Format.getRawFormat();
                format.setLineSeparator(System.lineSeparator());
                XMLOutputter out = new XMLOutputter(format);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (OutputStream outputStream = output) {
                    outputStream.write(head.getBytes(StandardCharsets.UTF_8));
                    out.output(document.getRootElement(), outputStream);
                    outputStream.write(tail.getBytes(StandardCharsets.UTF_8));
                }
                String newBody = output.toString(StandardCharsets.UTF_8);
                Files.writeString(pomPath, newBody, StandardCharsets.UTF_8);
            } catch (Exception e) {
                context.logger.error("Failed to save " + pomPath + ": " + e.getMessage());
            }
        }
    }

    /**
     * Detects the current model version from a POM document.
     * The explicit modelVersion element takes precedence over namespace URI.
     */
    protected String detectModelVersion(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        String explicitVersion = null;
        String namespaceVersion = null;

        // Check explicit modelVersion element first (takes precedence)
        Element modelVersionElement = root.getChild("modelVersion", namespace);
        if (modelVersionElement != null) {
            explicitVersion = modelVersionElement.getTextTrim();
        }

        // Check namespace URI for 4.1.0+ models
        if (namespace != null && namespace.getURI() != null) {
            String namespaceUri = namespace.getURI();
            if (namespaceUri.contains("4.1.0")) {
                namespaceVersion = "4.1.0";
            }
            // Future versions can be detected here
            // if (namespaceUri.contains("4.2.0")) {
            //     namespaceVersion = "4.2.0";
            // }
        }

        // Explicit version takes precedence
        if (explicitVersion != null && !explicitVersion.isEmpty()) {
            // Check for mismatch between explicit version and namespace
            if (namespaceVersion != null && !explicitVersion.equals(namespaceVersion)) {
                context.logger.warn("Model version mismatch in POM - explicit: " + explicitVersion
                        + ", namespace suggests: " + namespaceVersion + ". Using explicit version.");
            }
            return explicitVersion;
        }

        // Fall back to namespace-inferred version
        if (namespaceVersion != null) {
            return namespaceVersion;
        }

        // Default to 4.0.0 with warning
        context.logger.warn("No model version found in POM, falling back to 4.0.0");
        return "4.0.0";
    }

    /**
     * Determines if an upgrade from one model version to another is supported.
     */
    protected boolean canUpgrade(String currentVersion, String targetVersion) {
        // Define supported upgrade paths
        if ("4.0.0".equals(currentVersion) && "4.1.0".equals(targetVersion)) {
            return true;
        }
        // Future upgrade paths can be added here:
        // if ("4.1.0".equals(currentVersion) && "4.2.0".equals(targetVersion)) {
        //     return true;
        // }

        return false;
    }

    /**
     * Validates if a model version is supported.
     */
    protected boolean isValidModelVersion(String version) {
        return "4.0.0".equals(version) || "4.1.0".equals(version);
        // Future versions: || "4.2.0".equals(version)
    }

    /**
     * Upgrades a POM from model version 4.0.0 to 4.1.0.
     * Note: Inference is now handled separately in a second step.
     */
    protected void upgradeFrom400To410(UpgradeContext context, Path pomPath, Document pomDocument) {
        String action = shouldSaveModifications() ? "Upgrading" : "Analyzing";
        context.logger.info("      " + action + " " + pomPath.getFileName() + " from 4.0.0 to 4.1.0:");

        try {
            // Perform the 4.0.0 → 4.1.0 upgrade
            performModelUpgrade(context, pomDocument);

            String result = shouldSaveModifications() ? "completed successfully" : "completed";
            context.logger.info("        ✓ " + action + " " + result);

        } catch (Exception e) {
            String result = shouldSaveModifications() ? "failed" : "failed";
            context.logger.error("        ✗ " + action + " " + result + ": " + e.getMessage());
        }
    }

    /**
     * Performs the core 4.0.0 → 4.1.0 model upgrade.
     * This includes namespace updates and module conversion.
     */
    protected void performModelUpgrade(UpgradeContext context, Document pomDocument) {
        context.logger.info("        Performing 4.0.0 → 4.1.0 model upgrade...");

        // Update namespace and schema location to 4.1.0
        upgradeNamespaceAndSchemaLocation(context, pomDocument);

        // Convert modules to subprojects
        convertModulesToSubprojects(context, pomDocument);

        // Update modelVersion to 4.1.0 (may be removed later during inference step)
        updateModelVersion(context, pomDocument);
    }

    /**
     * Applies inference optimizations based on the model version.
     * - 4.1.0+ models: Full inference support
     * - 4.0.0 models: Limited inference support (parent groupId/version inheritance only)
     */
    protected void applyInference(
            UpgradeContext context, Document pomDocument, Path pomPath, Map<Path, Document> pomMap) {
        String modelVersion = detectModelVersion(context, pomDocument);

        if ("4.0.0".equals(modelVersion)) {
            applyLimitedInferenceFor400(context, pomDocument, pomPath, pomMap);
        } else {
            applyFullInferenceFor410Plus(context, pomDocument, pomPath, pomMap);
        }
    }

    /**
     * Applies limited inference optimizations to a 4.0.0 model (Maven 3.x POM).
     * Maven 4 supports limited inference for parent groupId and version inheritance when loading 4.0.0 models.
     */
    protected void applyLimitedInferenceFor400(
            UpgradeContext context, Document pomDocument, Path pomPath, Map<Path, Document> pomMap) {
        context.logger.info("        Applying limited inference optimizations for 4.0.0 models...");

        // Get all GAVs for inference from the entire multi-module project
        Set<GAV> allGAVs = computeAllGAVs(context, pomMap);

        // Apply limited parent element trimming (only groupId/version inheritance, no artifactId inference)
        trimParentElementLimited400(context, pomDocument, pomPath, allGAVs, pomMap);
    }

    /**
     * Applies full inference optimizations to a 4.1.0+ model.
     * This includes all Maven 4.1.0+ inference capabilities.
     */
    protected void applyFullInferenceFor410Plus(
            UpgradeContext context, Document pomDocument, Path pomPath, Map<Path, Document> pomMap) {
        context.logger.info("        Applying full inference optimizations for 4.1.0+ models...");

        // Get all GAVs for inference from the entire multi-module project
        Set<GAV> allGAVs = computeAllGAVs(context, pomMap);

        // Determine if this is the root project
        boolean isRootProject = isRootProject(context, pomPath);

        // Remove modelVersion (inference removes redundant information)
        removeModelVersion(context, pomDocument);

        // Add root attribute to root project only
        if (isRootProject) {
            addRootAttribute(context, pomDocument);
        }

        // Trim parent element (now with full pomMap knowledge for artifactId inference)
        trimParentElement(context, pomDocument, pomPath, allGAVs, pomMap);

        // Remove managed dependencies pointing to project artifacts
        removeManagedDependencies(context, pomDocument, allGAVs);

        // Remove redundant dependency information that can be inferred
        removeDependencyInferenceRedundancy(context, pomDocument, pomPath, pomMap);

        // Remove redundant modules/subprojects list
        removeRedundantModulesList(context, pomDocument, pomPath);
    }

    /**
     * Updates namespace and schema location to 4.1.0 for all elements.
     */
    protected void upgradeNamespaceAndSchemaLocation(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();

        // Update namespace to 4.1.0 for all elements recursively
        Namespace newNamespace = Namespace.getNamespace("http://maven.apache.org/POM/4.1.0");
        updateNamespaceRecursively(root, newNamespace);
        context.logger.info("        • Updated namespace to Maven 4.1.0 for all elements");

        // Update xsi:schemaLocation if present
        Namespace xsiNamespace = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        org.jdom2.Attribute schemaLocationAttr = root.getAttribute("schemaLocation", xsiNamespace);
        if (schemaLocationAttr != null) {
            String newSchemaLocation =
                    "http://maven.apache.org/POM/4.1.0 " + "http://maven.apache.org/xsd/maven-4.1.0.xsd";
            schemaLocationAttr.setValue(newSchemaLocation);
            context.logger.info("        • Updated xsi:schemaLocation to Maven 4.1.0");
        }
    }

    /**
     * Recursively updates the namespace for an element and all its children.
     */
    protected void updateNamespaceRecursively(Element element, Namespace newNamespace) {
        // Update the namespace for this element
        element.setNamespace(newNamespace);

        // Recursively update all child elements
        List<Element> children = element.getChildren();
        for (Element child : children) {
            updateNamespaceRecursively(child, newNamespace);
        }
    }

    /**
     * Updates modelVersion to 4.1.0.
     */
    protected void updateModelVersion(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Element modelVersionElement = root.getChild("modelVersion", namespace);
        if (modelVersionElement != null) {
            modelVersionElement.setText("4.1.0");
            context.logger.info("        • Updated modelVersion to 4.1.0");
        }
    }

    /**
     * Removes modelVersion element (for inference).
     */
    protected void removeModelVersion(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Element modelVersionElement = root.getChild("modelVersion", namespace);
        if (modelVersionElement != null) {
            removeElementWithFormatting(modelVersionElement);
            context.logger.info("        • Removed modelVersion (inference enabled)");
        }
    }

    /**
     * Removes an element while preserving formatting by also removing
     * leading whitespace and line separators.
     */
    protected void removeElementWithFormatting(Element element) {
        Element parent = element.getParentElement();
        if (parent != null) {
            int index = parent.indexOf(element);

            // Remove the element
            parent.removeContent(element);

            // Try to remove preceding whitespace/newline
            if (index > 0) {
                org.jdom2.Content prevContent = parent.getContent(index - 1);
                if (prevContent instanceof org.jdom2.Text) {
                    org.jdom2.Text textContent = (org.jdom2.Text) prevContent;
                    String text = textContent.getText();
                    // If it's just whitespace and newlines, remove it
                    if (text.trim().isEmpty() && text.contains("\n")) {
                        parent.removeContent(prevContent);
                    }
                }
            }
        }
    }

    /**
     * Converts modules to subprojects and module to subproject.
     */
    protected void convertModulesToSubprojects(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Convert modules to subprojects in main model
        convertModulesElement(root, namespace);

        // Convert modules to subprojects in profiles
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                convertModulesElement(profileElement, namespace);
            }
        }

        context.logger.info("        • Converted modules to subprojects");
    }

    /**
     * Converts a modules element to subprojects while preserving formatting.
     */
    protected void convertModulesElement(Element parent, Namespace namespace) {
        Element modulesElement = parent.getChild("modules", namespace);
        if (modulesElement != null) {
            // Simply rename the element and its children to preserve formatting
            modulesElement.setName("subprojects");

            // Convert each module to subproject
            List<Element> moduleElements = modulesElement.getChildren("module", namespace);
            for (Element moduleElement : moduleElements) {
                moduleElement.setName("subproject");
            }
        }
    }

    /**
     * Computes all GAVs from all POMs in the multi-module project for inference.
     * This includes resolving parent inheritance and relative path parents.
     */
    protected Set<GAV> computeAllGAVs(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<GAV> gavs = new HashSet<>();

        context.logger.info("        Computing GAVs for inference from " + pomMap.size() + " POM(s)...");

        // Extract GAV from all POMs in the project
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();

            GAV gav = extractGAVWithParentResolution(pomDocument, context);
            if (gav != null) {
                gavs.add(gav);
                context.logger.debug("        Found GAV: " + gav + " from " + pomPath);
            }
        }

        context.logger.info("        Computed " + gavs.size() + " GAV(s) for inference");
        return gavs;
    }

    /**
     * Determines if the given POM is the root project.
     */
    protected boolean isRootProject(UpgradeContext context, Path pomPath) {
        // Root project is typically the one without a parent or at the top level
        // For now, simple heuristic: check if it's in the starting directory
        Path startDir = context.invokerRequest.cwd();
        return pomPath.getParent().equals(startDir);
    }

    /**
     * Adds root=true attribute to the root project.
     */
    protected void addRootAttribute(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        root.setAttribute("root", "true");
        context.logger.info("        • Added root=true attribute");
    }

    /**
     * Applies limited inference for 4.0.0 models (Maven 3.x POMs).
     * Maven 4 supports limited inference when loading 4.0.0 models where child groupId/version can be inferred from parent:
     * - If child.groupId matches parent.groupId, we can remove child.groupId (it will be inferred from parent)
     * - If child.version matches parent.version, we can remove child.version (it will be inferred from parent)
     * - Parent elements are always kept in 4.0.0 models (no relativePath inference)
     */
    protected void trimParentElementLimited400(
            UpgradeContext context, Document pomDocument, Path pomPath, Set<GAV> allGAVs, Map<Path, Document> pomMap) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element parentElement = root.getChild("parent", namespace);

        if (parentElement == null) {
            return;
        }

        // Get parent GAV for comparison
        String parentGroupId = getChildText(parentElement, "groupId", namespace);
        String parentVersion = getChildText(parentElement, "version", namespace);

        // Check if we can remove child groupId
        // Maven 4 logic for 4.0.0 models: if child.groupId == null, use parent.groupId
        // Reverse: if child.groupId == parent.groupId, we can remove child.groupId (it will be inferred)
        Element childGroupIdElement = root.getChild("groupId", namespace);
        if (childGroupIdElement != null && parentGroupId != null) {
            String childGroupId = childGroupIdElement.getTextTrim();
            if (childGroupId.equals(parentGroupId)) {
                removeElementWithFormatting(childGroupIdElement);
                context.logger.info("        • Removed child groupId (will be inferred from parent)");
            }
        }

        // Check if we can remove child version
        // Maven 4 logic for 4.0.0 models: if child.version == null, use parent.version
        // Reverse: if child.version == parent.version, we can remove child.version (it will be inferred)
        Element childVersionElement = root.getChild("version", namespace);
        if (childVersionElement != null && parentVersion != null) {
            String childVersion = childVersionElement.getTextTrim();
            if (childVersion.equals(parentVersion)) {
                removeElementWithFormatting(childVersionElement);
                context.logger.info("        • Removed child version (will be inferred from parent)");
            }
        }

        // Note: In 4.0.0 models, parent elements are always kept (no relativePath inference)
        // and child artifactId is always required (cannot be inferred from parent)
    }

    /**
     * Trims parent element by removing redundant information that can be inferred (Maven 4.1.0+).
     * This reverses Maven's parent inference logic:
     * - If child has no explicit groupId, it inherits from parent -> we can remove parent groupId
     * - If child has no explicit version, it inherits from parent -> we can remove parent version
     * - Parent GAV can be inferred from relativePath if missing
     */
    protected void trimParentElement(
            UpgradeContext context, Document pomDocument, Path pomPath, Set<GAV> allGAVs, Map<Path, Document> pomMap) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element parentElement = root.getChild("parent", namespace);

        if (parentElement == null) {
            return;
        }

        // Check if we can remove parent groupId
        // Maven logic: if child.groupId == null, use parent.groupId
        // Reverse: if child.groupId == null, we can remove parent.groupId (it will be inferred)
        Element childGroupIdElement = root.getChild("groupId", namespace);
        if (childGroupIdElement == null) {
            Element parentGroupIdElement = parentElement.getChild("groupId", namespace);
            if (parentGroupIdElement != null) {
                removeElementWithFormatting(parentGroupIdElement);
                context.logger.info("        • Removed parent groupId (will be inferred from relativePath)");
            }
        }

        // Check if we can remove parent version
        // Maven logic: if child.version == null, use parent.version
        // Reverse: if child.version == null, we can remove parent.version (it will be inferred)
        Element childVersionElement = root.getChild("version", namespace);
        if (childVersionElement == null) {
            Element parentVersionElement = parentElement.getChild("version", namespace);
            if (parentVersionElement != null) {
                removeElementWithFormatting(parentVersionElement);
                context.logger.info("        • Removed parent version (will be inferred from relativePath)");
            }
        }

        // Check if we can remove parent artifactId
        // Maven logic: parent.artifactId is always required for resolution, but if relativePath
        // points to a POM in the multi-module project and we can resolve the GAV, the artifactId can be inferred
        Element parentArtifactIdElement = parentElement.getChild("artifactId", namespace);
        if (parentArtifactIdElement != null && canInferParentArtifactId(context, parentElement, pomPath, pomMap)) {
            removeElementWithFormatting(parentArtifactIdElement);
            context.logger.info("        • Removed parent artifactId (will be inferred from relativePath)");
        }

        // Note: The artifactId inference logic (canInferParentArtifactId) specifically checks
        // if the parent POM is available in our pomMap. This distinguishes between:
        // - Internal parents (in pomMap): artifactId can be inferred from relativePath
        // - External parents (not in pomMap): artifactId must be kept explicitly
    }

    /**
     * Determines if parent artifactId can be inferred from relativePath.
     * This is possible when:
     * 1. Parent has a relativePath (explicit or default "../pom.xml")
     * 2. The relativePath points to a POM that exists in our pomMap
     * 3. We can resolve the parent POM and extract its artifactId
     */
    protected boolean canInferParentArtifactId(
            UpgradeContext context, Element parentElement, Path currentPomPath, Map<Path, Document> pomMap) {
        Namespace namespace = parentElement.getNamespace();

        // Get relativePath (default is "../pom.xml" if not specified)
        String relativePath = getChildText(parentElement, "relativePath", namespace);
        if (relativePath == null || relativePath.trim().isEmpty()) {
            relativePath = "../pom.xml"; // Maven default
        }

        // Resolve the parent POM path
        Path parentPomPath = resolveParentPomPath(currentPomPath, relativePath);
        if (parentPomPath == null) {
            return false;
        }

        // Check if the parent POM exists in our pomMap
        Document parentPomDocument = pomMap.get(parentPomPath);
        if (parentPomDocument == null) {
            // Try to find it by normalizing the path
            for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
                try {
                    if (java.nio.file.Files.isSameFile(entry.getKey(), parentPomPath)) {
                        parentPomDocument = entry.getValue();
                        break;
                    }
                } catch (Exception e) {
                    // Ignore and continue
                }
            }
        }

        if (parentPomDocument == null) {
            return false;
        }

        // Extract artifactId from the parent POM
        Element parentRoot = parentPomDocument.getRootElement();
        String parentArtifactId = getChildText(parentRoot, "artifactId", parentRoot.getNamespace());

        // We can infer if the parent POM has an artifactId
        boolean canInfer = parentArtifactId != null && !parentArtifactId.trim().isEmpty();

        if (canInfer && context != null) {
            context.logger.debug(
                    "        Can infer parent artifactId '" + parentArtifactId + "' from " + parentPomPath);
        }

        return canInfer;
    }

    /**
     * Resolves the parent POM path from a child POM path and relativePath.
     */
    protected Path resolveParentPomPath(Path childPomPath, String relativePath) {
        try {
            Path childDir = childPomPath.getParent();
            if (childDir == null) {
                return null;
            }

            // Handle null or empty relativePath (default to ../pom.xml)
            if (relativePath == null || relativePath.trim().isEmpty()) {
                relativePath = "../pom.xml";
            }

            Path resolvedPath = childDir.resolve(relativePath).normalize();

            // If relativePath doesn't end with .xml, assume it's a directory and append pom.xml
            if (!relativePath.endsWith(".xml")) {
                resolvedPath = resolvedPath.resolve("pom.xml");
            }

            return resolvedPath;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Removes redundant dependency information that can be inferred by Maven.
     * This reverses Maven's dependency inference logic from DefaultModelBuilder.inferDependencyVersion().
     *
     * Maven can infer:
     * - Dependency version from project artifacts in the same reactor
     * - Dependency groupId from project artifacts in the same reactor
     *
     * This method removes such redundant information when the dependency points to a project
     * artifact that exists in our pomMap.
     */
    protected void removeDependencyInferenceRedundancy(
            UpgradeContext context, Document pomDocument, Path pomPath, Map<Path, Document> pomMap) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Process main dependencies
        Element dependenciesElement = root.getChild("dependencies", namespace);
        if (dependenciesElement != null) {
            removeDependencyInferenceFromSection(
                    context, dependenciesElement, namespace, pomPath, pomMap, "dependencies");
        }

        // Process profile dependencies
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                Element profileDependencies = profileElement.getChild("dependencies", namespace);
                if (profileDependencies != null) {
                    removeDependencyInferenceFromSection(
                            context, profileDependencies, namespace, pomPath, pomMap, "profile dependencies");
                }
            }
        }

        // Process build plugin dependencies
        Element buildElement = root.getChild("build", namespace);
        if (buildElement != null) {
            Element pluginsElement = buildElement.getChild("plugins", namespace);
            if (pluginsElement != null) {
                List<Element> pluginElements = pluginsElement.getChildren("plugin", namespace);
                for (Element pluginElement : pluginElements) {
                    Element pluginDependencies = pluginElement.getChild("dependencies", namespace);
                    if (pluginDependencies != null) {
                        removeDependencyInferenceFromSection(
                                context, pluginDependencies, namespace, pomPath, pomMap, "plugin dependencies");
                    }
                }
            }
        }
    }

    /**
     * Helper method to remove dependency inference redundancy from a specific dependencies section.
     */
    protected void removeDependencyInferenceFromSection(
            UpgradeContext context,
            Element dependencies,
            Namespace namespace,
            Path pomPath,
            Map<Path, Document> pomMap,
            String sectionName) {
        List<Element> dependencyElements = dependencies.getChildren("dependency", namespace);

        for (Element dependency : dependencyElements) {
            String groupId = getChildText(dependency, "groupId", namespace);
            String artifactId = getChildText(dependency, "artifactId", namespace);
            String version = getChildText(dependency, "version", namespace);

            if (artifactId != null) {
                // Try to find the dependency's POM in our pomMap
                Document dependencyPom = findDependencyPomInMap(groupId, artifactId, pomMap);
                if (dependencyPom != null) {
                    // We found the dependency in our project - we can infer information

                    // Check if we can remove the version (Maven can infer it from the project artifact)
                    if (version != null && canInferDependencyVersion(dependencyPom, version)) {
                        Element versionElement = dependency.getChild("version", namespace);
                        if (versionElement != null) {
                            removeElementWithFormatting(versionElement);
                            context.logger.info("        • Removed dependency version for " + groupId + ":" + artifactId
                                    + " from " + sectionName + " (can be inferred from project)");
                        }
                    }

                    // Check if we can remove the groupId (Maven can infer it from the project artifact)
                    if (groupId != null && canInferDependencyGroupId(dependencyPom, groupId)) {
                        Element groupIdElement = dependency.getChild("groupId", namespace);
                        if (groupIdElement != null) {
                            removeElementWithFormatting(groupIdElement);
                            context.logger.info("        • Removed dependency groupId for " + artifactId + " from "
                                    + sectionName + " (can be inferred from project)");
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds a dependency's POM document in the pomMap by groupId and artifactId.
     */
    protected Document findDependencyPomInMap(String groupId, String artifactId, Map<Path, Document> pomMap) {
        for (Document pomDocument : pomMap.values()) {
            GAV gav = extractGAVWithParentResolution(pomDocument, null);
            if (gav != null
                    && java.util.Objects.equals(gav.artifactId, artifactId)
                    && (groupId == null || java.util.Objects.equals(gav.groupId, groupId))) {
                return pomDocument;
            }
        }
        return null;
    }

    /**
     * Determines if a dependency version can be inferred from the project artifact.
     * Maven logic: if the dependency points to a project artifact, the version can be inferred.
     */
    protected boolean canInferDependencyVersion(Document dependencyPom, String declaredVersion) {
        GAV projectGav = extractGAVWithParentResolution(dependencyPom, null);
        if (projectGav == null || projectGav.version == null) {
            return false;
        }

        // We can infer the version if the declared version matches the project version
        // This ensures we don't remove version information that's intentionally different
        return java.util.Objects.equals(declaredVersion, projectGav.version);
    }

    /**
     * Determines if a dependency groupId can be inferred from the project artifact.
     * Maven logic: if the dependency points to a project artifact, the groupId can be inferred.
     */
    protected boolean canInferDependencyGroupId(Document dependencyPom, String declaredGroupId) {
        GAV projectGav = extractGAVWithParentResolution(dependencyPom, null);
        if (projectGav == null || projectGav.groupId == null) {
            return false;
        }

        // We can infer the groupId if the declared groupId matches the project groupId
        // This ensures we don't remove groupId information that's intentionally different
        return java.util.Objects.equals(declaredGroupId, projectGav.groupId);
    }

    /**
     * Removes managed dependencies that point to project artifacts.
     * In Maven 4.1.0+, managed dependencies for project artifacts can be inferred automatically
     * because the version can be determined from the project structure.
     */
    protected void removeManagedDependencies(UpgradeContext context, Document pomDocument, Set<GAV> allGAVs) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check dependencyManagement section
        Element dependencyManagement = root.getChild("dependencyManagement", namespace);
        if (dependencyManagement != null) {
            Element dependencies = dependencyManagement.getChild("dependencies", namespace);
            if (dependencies != null) {
                removeManagedDependenciesFromSection(context, dependencies, namespace, allGAVs, "dependencyManagement");
            }
        }

        // Check profiles for dependencyManagement
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                Element profileDependencyManagement = profileElement.getChild("dependencyManagement", namespace);
                if (profileDependencyManagement != null) {
                    Element profileDependencies = profileDependencyManagement.getChild("dependencies", namespace);
                    if (profileDependencies != null) {
                        removeManagedDependenciesFromSection(
                                context, profileDependencies, namespace, allGAVs, "profile dependencyManagement");
                    }
                }
            }
        }
    }

    /**
     * Helper method to remove managed dependencies from a specific dependencies section.
     */
    protected void removeManagedDependenciesFromSection(
            UpgradeContext context, Element dependencies, Namespace namespace, Set<GAV> allGAVs, String sectionName) {
        List<Element> dependencyElements = dependencies.getChildren("dependency", namespace);
        List<Element> toRemove = new ArrayList<>();

        for (Element dependency : dependencyElements) {
            String groupId = getChildText(dependency, "groupId", namespace);
            String artifactId = getChildText(dependency, "artifactId", namespace);

            if (groupId != null && artifactId != null) {
                // Check if this dependency matches any project artifact (ignoring version)
                boolean isProjectArtifact = allGAVs.stream()
                        .anyMatch(gav -> java.util.Objects.equals(gav.groupId, groupId)
                                && java.util.Objects.equals(gav.artifactId, artifactId));

                if (isProjectArtifact) {
                    toRemove.add(dependency);
                }
            }
        }

        // Remove the identified dependencies
        for (Element dependency : toRemove) {
            removeElementWithFormatting(dependency);
            String groupId = getChildText(dependency, "groupId", namespace);
            String artifactId = getChildText(dependency, "artifactId", namespace);
            context.logger.info("        • Removed managed dependency " + groupId + ":" + artifactId + " from "
                    + sectionName + " (project artifact)");
        }
    }

    /**
     * Removes redundant subprojects list when it matches direct child directories.
     * In Maven 4.1.0+, subprojects can be auto-discovered from direct child directories.
     */
    protected void removeRedundantModulesList(UpgradeContext context, Document pomDocument, Path pomPath) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check main subprojects
        Element subprojectsElement = root.getChild("subprojects", namespace);
        if (subprojectsElement != null) {
            if (isSubprojectsListRedundant(context, subprojectsElement, namespace, pomPath)) {
                removeElementWithFormatting(subprojectsElement);
                context.logger.info("        • Removed redundant subprojects list (matches direct children)");
            }
        }

        // Check profiles for subprojects
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                Element profileSubprojects = profileElement.getChild("subprojects", namespace);
                if (profileSubprojects != null) {
                    if (isSubprojectsListRedundant(context, profileSubprojects, namespace, pomPath)) {
                        removeElementWithFormatting(profileSubprojects);
                        context.logger.info(
                                "        • Removed redundant subprojects list from profile (matches direct children)");
                    }
                }
            }
        }
    }

    /**
     * Checks if a subprojects list is redundant (matches direct child directories with pom.xml).
     */
    protected boolean isSubprojectsListRedundant(
            UpgradeContext context, Element subprojectsElement, Namespace namespace, Path pomPath) {
        List<Element> subprojectElements = subprojectsElement.getChildren("subproject", namespace);
        if (subprojectElements.isEmpty()) {
            return true; // Empty list is redundant
        }

        // Get the directory containing this POM
        Path parentDir = pomPath.getParent();
        if (parentDir == null) {
            return false;
        }

        // Get list of declared subprojects
        Set<String> declaredSubprojects = new HashSet<>();
        for (Element subprojectElement : subprojectElements) {
            String subprojectPath = subprojectElement.getTextTrim();
            if (subprojectPath != null && !subprojectPath.isEmpty()) {
                // Normalize path (remove trailing pom.xml if present)
                if (subprojectPath.endsWith("/pom.xml")) {
                    subprojectPath = subprojectPath.substring(0, subprojectPath.length() - "/pom.xml".length());
                } else if (subprojectPath.endsWith("\\pom.xml")) {
                    subprojectPath = subprojectPath.substring(0, subprojectPath.length() - "\\pom.xml".length());
                }
                declaredSubprojects.add(subprojectPath);
            }
        }

        // Get list of actual direct child directories with pom.xml
        Set<String> actualSubprojects = new HashSet<>();
        try {
            if (java.nio.file.Files.exists(parentDir) && java.nio.file.Files.isDirectory(parentDir)) {
                try (java.util.stream.Stream<Path> children = java.nio.file.Files.list(parentDir)) {
                    children.filter(java.nio.file.Files::isDirectory)
                            .filter(dir -> java.nio.file.Files.exists(dir.resolve("pom.xml")))
                            .forEach(dir ->
                                    actualSubprojects.add(dir.getFileName().toString()));
                }
            }
        } catch (Exception e) {
            // If we can't read the directory, assume not redundant
            if (context != null) {
                context.logger.debug(
                        "        Could not read directory " + parentDir + " for subprojects check: " + e.getMessage());
            }
            return false;
        }

        // Compare declared vs actual - they should match exactly for the list to be redundant
        return declaredSubprojects.equals(actualSubprojects);
    }

    /**
     * Extracts GAV from a POM document with parent inheritance resolution.
     * If groupId or version are missing, they are inherited from the parent element.
     */
    protected GAV extractGAVWithParentResolution(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        String groupId = getChildText(root, "groupId", namespace);
        String artifactId = getChildText(root, "artifactId", namespace);
        String version = getChildText(root, "version", namespace);

        // Simple parent inheritance
        if (groupId == null || version == null) {
            Element parentElement = root.getChild("parent", namespace);
            if (parentElement != null) {
                if (groupId == null) {
                    groupId = getChildText(parentElement, "groupId", namespace);
                }
                if (version == null) {
                    version = getChildText(parentElement, "version", namespace);
                }
            }
        }

        if (groupId != null && artifactId != null && version != null) {
            return new GAV(groupId, artifactId, version);
        }

        return null;
    }

    protected GAV extractParentGAVFromRelativePath(Document pomDocument, UpgradeContext context) {
        // Simplified implementation
        return null;
    }

    /**
     * Helper method to get child element text.
     */
    protected String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }

    /**
     * Upgrades plugins in a POM document.
     * Checks both build/plugins and build/pluginManagement/plugins sections.
     * Also checks parent POMs for plugins that need to be managed locally.
     */
    protected boolean upgradePluginsInPom(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasUpgrades = false;

        // Define the plugins that need to be upgraded for Maven 4 compatibility
        Map<String, PluginUpgrade> pluginUpgrades = getPluginUpgrades();

        // Check build/plugins
        Element buildElement = root.getChild("build", namespace);
        if (buildElement != null) {
            Element pluginsElement = buildElement.getChild("plugins", namespace);
            if (pluginsElement != null) {
                hasUpgrades |= upgradePluginsInSection(
                        context, pluginsElement, namespace, pluginUpgrades, pomDocument, "build/plugins");
            }

            // Check build/pluginManagement/plugins
            Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
            if (pluginManagementElement != null) {
                Element managedPluginsElement = pluginManagementElement.getChild("plugins", namespace);
                if (managedPluginsElement != null) {
                    hasUpgrades |= upgradePluginsInSection(
                            context,
                            managedPluginsElement,
                            namespace,
                            pluginUpgrades,
                            pomDocument,
                            "build/pluginManagement/plugins");
                }
            }
        }

        // Check parent POMs for plugins that need to be managed locally
        hasUpgrades |= ParentPomResolver.checkParentPomsForPlugins(context, pomDocument, pluginUpgrades);

        return hasUpgrades;
    }

    /**
     * Returns the map of plugins that need to be upgraded for Maven 4 compatibility.
     */
    protected Map<String, PluginUpgrade> getPluginUpgrades() {
        Map<String, PluginUpgrade> upgrades = new HashMap<>();
        upgrades.put(
                "org.apache.maven.plugins:maven-exec-plugin",
                new PluginUpgrade("org.apache.maven.plugins", "maven-exec-plugin", "3.2.0"));
        upgrades.put(
                "org.apache.maven.plugins:maven-enforcer-plugin",
                new PluginUpgrade("org.apache.maven.plugins", "maven-enforcer-plugin", "3.0.0"));
        upgrades.put(
                "org.codehaus.mojo:flatten-maven-plugin",
                new PluginUpgrade("org.codehaus.mojo", "flatten-maven-plugin", "1.2.7"));
        upgrades.put(
                "org.apache.maven.plugins:maven-shade-plugin",
                new PluginUpgrade("org.apache.maven.plugins", "maven-shade-plugin", "3.5.0"));
        return upgrades;
    }

    /**
     * Upgrades plugins in a specific plugins section (either build/plugins or build/pluginManagement/plugins).
     */
    protected boolean upgradePluginsInSection(
            UpgradeContext context,
            Element pluginsElement,
            Namespace namespace,
            Map<String, PluginUpgrade> pluginUpgrades,
            Document pomDocument,
            String sectionName) {
        boolean hasUpgrades = false;
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
                PluginUpgrade upgrade = pluginUpgrades.get(pluginKey);

                if (upgrade != null) {
                    if (upgradePluginVersion(context, pluginElement, namespace, upgrade, pomDocument, sectionName)) {
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
    protected boolean upgradePluginVersion(
            UpgradeContext context,
            Element pluginElement,
            Namespace namespace,
            PluginUpgrade upgrade,
            Document pomDocument,
            String sectionName) {
        Element versionElement = pluginElement.getChild("version", namespace);
        String currentVersion = null;
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
            context.logger.debug("      Plugin " + upgrade.groupId + ":" + upgrade.artifactId
                    + " has no explicit version, may inherit from parent");
            return false;
        }

        if (currentVersion != null) {
            if (isProperty) {
                // Update property value if it's below minimum version
                return upgradePropertyVersion(context, pomDocument, propertyName, upgrade, sectionName);
            } else {
                // Direct version comparison and upgrade
                if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                    versionElement.setText(upgrade.minVersion);
                    context.logger.info("      • Upgraded " + upgrade.groupId + ":" + upgrade.artifactId + " from "
                            + currentVersion + " to " + upgrade.minVersion + " in " + sectionName);
                    return true;
                } else {
                    context.logger.debug("      Plugin " + upgrade.groupId + ":" + upgrade.artifactId + " version "
                            + currentVersion + " is already >= " + upgrade.minVersion);
                }
            }
        }

        return false;
    }

    /**
     * Upgrades a property value if it represents a plugin version below the minimum.
     */
    protected boolean upgradePropertyVersion(
            UpgradeContext context,
            Document pomDocument,
            String propertyName,
            PluginUpgrade upgrade,
            String sectionName) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element propertiesElement = root.getChild("properties", namespace);

        if (propertiesElement != null) {
            Element propertyElement = propertiesElement.getChild(propertyName, namespace);
            if (propertyElement != null) {
                String currentVersion = propertyElement.getTextTrim();
                if (isVersionBelow(currentVersion, upgrade.minVersion)) {
                    propertyElement.setText(upgrade.minVersion);
                    context.logger.info("      • Upgraded property " + propertyName + " (for " + upgrade.groupId + ":"
                            + upgrade.artifactId + ") from " + currentVersion + " to " + upgrade.minVersion + " in "
                            + sectionName);
                    return true;
                } else {
                    context.logger.debug("      Property " + propertyName + " version " + currentVersion
                            + " is already >= " + upgrade.minVersion);
                }
            } else {
                context.logger.warn("      Property " + propertyName + " not found in POM properties");
            }
        } else {
            context.logger.warn("      No properties section found in POM for property " + propertyName);
        }

        return false;
    }

    /**
     * Simple version comparison to check if current version is below minimum version.
     * This is a basic implementation that works for most Maven plugin versions.
     */
    protected boolean isVersionBelow(String currentVersion, String minVersion) {
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
     * Simple holder for plugin upgrade information.
     */
    protected static class PluginUpgrade {
        final String groupId;
        final String artifactId;
        final String minVersion;

        PluginUpgrade(String groupId, String artifactId, String minVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.minVersion = minVersion;
        }
    }

    /**
     * Simple GAV (GroupId, ArtifactId, Version) holder for tracking project artifacts.
     */
    protected static class GAV {
        final String groupId;
        final String artifactId;
        final String version;

        GAV(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            GAV gav = (GAV) obj;
            return java.util.Objects.equals(groupId, gav.groupId)
                    && java.util.Objects.equals(artifactId, gav.artifactId)
                    && java.util.Objects.equals(version, gav.version);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(groupId, artifactId, version);
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + (version != null ? version : "null");
        }

        /**
         * Checks if this GAV matches another GAV ignoring version.
         */
        public boolean matchesIgnoringVersion(GAV other) {
            return java.util.Objects.equals(groupId, other.groupId)
                    && java.util.Objects.equals(artifactId, other.artifactId);
        }
    }

    /**
     * Fixes unsupported combine.children attribute values.
     * Maven 4 only supports 'append' and 'merge' (default), not 'override'.
     */
    protected boolean fixUnsupportedCombineChildrenAttributes(UpgradeContext context, Document pomDocument) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();

        // Find all elements with combine.children="override" and change to "merge"
        List<Element> elementsWithCombineChildren = findElementsWithAttribute(root, "combine.children", "override");
        for (Element element : elementsWithCombineChildren) {
            element.getAttribute("combine.children").setValue("merge");
            context.logger.info("      • Fixed combine.children='override' → 'merge' in " + element.getName());
            fixed = true;
        }

        return fixed;
    }

    /**
     * Fixes unsupported combine.self attribute values.
     * Maven 4 only supports 'override', 'merge', and 'remove' (default is merge), not 'append'.
     */
    protected boolean fixUnsupportedCombineSelfAttributes(UpgradeContext context, Document pomDocument) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();

        // Find all elements with combine.self="append" and change to "merge"
        List<Element> elementsWithCombineSelf = findElementsWithAttribute(root, "combine.self", "append");
        for (Element element : elementsWithCombineSelf) {
            element.getAttribute("combine.self").setValue("merge");
            context.logger.info("      • Fixed combine.self='append' → 'merge' in " + element.getName());
            fixed = true;
        }

        return fixed;
    }

    /**
     * Fixes duplicate dependency declarations.
     * Maven 4 enforces stricter validation for duplicate dependencies.
     */
    protected boolean fixDuplicateDependencies(UpgradeContext context, Document pomDocument) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check dependencies in main project
        fixed |= fixDuplicateDependenciesInElement(context, root, namespace);

        // Check dependencies in profiles
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                fixed |= fixDuplicateDependenciesInElement(context, profileElement, namespace);
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate dependencies within a specific element (project or profile).
     */
    protected boolean fixDuplicateDependenciesInElement(UpgradeContext context, Element parent, Namespace namespace) {
        boolean fixed = false;
        Element dependenciesElement = parent.getChild("dependencies", namespace);

        if (dependenciesElement != null) {
            List<Element> dependencies = dependenciesElement.getChildren("dependency", namespace);
            Map<String, Element> seenDependencies = new HashMap<>();
            List<Element> toRemove = new ArrayList<>();

            for (Element dependency : dependencies) {
                String groupId = getChildText(dependency, "groupId", namespace);
                String artifactId = getChildText(dependency, "artifactId", namespace);
                String type = getChildText(dependency, "type", namespace);
                String classifier = getChildText(dependency, "classifier", namespace);

                // Create a key for uniqueness check
                String key = groupId + ":" + artifactId + ":" + (type != null ? type : "jar") + ":"
                        + (classifier != null ? classifier : "");

                if (seenDependencies.containsKey(key)) {
                    // Found duplicate - remove it
                    toRemove.add(dependency);
                    context.logger.info("      • Removed duplicate dependency: " + key);
                    fixed = true;
                } else {
                    seenDependencies.put(key, dependency);
                }
            }

            // Remove duplicates while preserving formatting
            for (Element duplicate : toRemove) {
                removeElementWithFormatting(duplicate);
            }
        }

        return fixed;
    }

    /**
     * Fixes unsupported expressions in repository URLs.
     * Maven 4 only supports expressions starting with 'project.basedir' or 'project.rootDirectory'.
     */
    protected boolean fixUnsupportedRepositoryExpressions(UpgradeContext context, Document pomDocument) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check repositories
        fixed |= fixRepositoryExpressions(context, root.getChild("repositories", namespace), namespace);

        // Check pluginRepositories
        fixed |= fixRepositoryExpressions(context, root.getChild("pluginRepositories", namespace), namespace);

        // Check repositories in profiles
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                fixed |= fixRepositoryExpressions(
                        context, profileElement.getChild("repositories", namespace), namespace);
                fixed |= fixRepositoryExpressions(
                        context, profileElement.getChild("pluginRepositories", namespace), namespace);
            }
        }

        return fixed;
    }

    /**
     * Fixes expressions in repository URLs within a repositories or pluginRepositories element.
     */
    protected boolean fixRepositoryExpressions(
            UpgradeContext context, Element repositoriesElement, Namespace namespace) {
        if (repositoriesElement == null) {
            return false;
        }

        boolean fixed = false;
        String elementType = repositoriesElement.getName().equals("repositories") ? "repository" : "pluginRepository";
        List<Element> repositories = repositoriesElement.getChildren(elementType, namespace);

        for (Element repository : repositories) {
            Element urlElement = repository.getChild("url", namespace);
            if (urlElement != null) {
                String url = urlElement.getTextTrim();
                if (url.contains("${")
                        && !url.contains("${project.basedir}")
                        && !url.contains("${project.rootDirectory}")) {
                    String repositoryId = getChildText(repository, "id", namespace);
                    context.logger.warn("      • Found unsupported expression in " + elementType + " URL (id: "
                            + repositoryId + "): " + url);
                    context.logger.warn(
                            "        Maven 4 only supports ${project.basedir} and ${project.rootDirectory} expressions in repository URLs");

                    // Comment out the problematic repository
                    org.jdom2.Comment comment = new org.jdom2.Comment(
                            " Repository disabled due to unsupported expression in URL: " + url + " ");
                    Element parent = repository.getParentElement();
                    parent.addContent(parent.indexOf(repository), comment);
                    removeElementWithFormatting(repository);

                    context.logger.info("      • Commented out " + elementType
                            + " with unsupported URL expression (id: " + repositoryId + ")");
                    fixed = true;
                }
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugin declarations.
     * Maven 4 enforces stricter validation for duplicate plugins in the same section.
     */
    protected boolean fixDuplicatePlugins(UpgradeContext context, Document pomDocument) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check main build plugins
        Element buildElement = root.getChild("build", namespace);
        if (buildElement != null) {
            fixed |= fixPluginsInBuildElement(context, buildElement, namespace, "build");
        }

        // Check profile plugins
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            for (Element profileElement : profilesElement.getChildren("profile", namespace)) {
                Element profileBuildElement = profileElement.getChild("build", namespace);
                if (profileBuildElement != null) {
                    fixed |= fixPluginsInBuildElement(context, profileBuildElement, namespace, "profile build");
                }
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugins in a build element (both plugins and pluginManagement).
     */
    protected boolean fixPluginsInBuildElement(
            UpgradeContext context, Element buildElement, Namespace namespace, String prefix) {
        boolean fixed = false;

        Element pluginsElement = buildElement.getChild("plugins", namespace);
        if (pluginsElement != null) {
            fixed |= fixDuplicatePluginsInSection(context, pluginsElement, namespace, prefix + "/plugins");
        }

        Element pluginManagementElement = buildElement.getChild("pluginManagement", namespace);
        if (pluginManagementElement != null) {
            Element managedPluginsElement = pluginManagementElement.getChild("plugins", namespace);
            if (managedPluginsElement != null) {
                fixed |= fixDuplicatePluginsInSection(
                        context, managedPluginsElement, namespace, prefix + "/pluginManagement/plugins");
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugins within a specific plugins section.
     */
    protected boolean fixDuplicatePluginsInSection(
            UpgradeContext context, Element pluginsElement, Namespace namespace, String sectionName) {
        boolean fixed = false;
        List<Element> plugins = pluginsElement.getChildren("plugin", namespace);
        Map<String, Element> seenPlugins = new HashMap<>();
        List<Element> toRemove = new ArrayList<>();

        for (Element plugin : plugins) {
            String groupId = getChildText(plugin, "groupId", namespace);
            String artifactId = getChildText(plugin, "artifactId", namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith("maven-")) {
                groupId = "org.apache.maven.plugins";
            }

            if (groupId != null && artifactId != null) {
                // Create a key for uniqueness check (groupId:artifactId)
                String key = groupId + ":" + artifactId;

                if (seenPlugins.containsKey(key)) {
                    // Found duplicate - remove it
                    toRemove.add(plugin);
                    context.logger.info("      • Removed duplicate plugin: " + key + " in " + sectionName);
                    fixed = true;
                } else {
                    seenPlugins.put(key, plugin);
                }
            }
        }

        // Remove duplicates while preserving formatting
        for (Element duplicate : toRemove) {
            removeElementWithFormatting(duplicate);
        }

        return fixed;
    }

    /**
     * Fixes incorrect parent relative paths by searching for the correct parent POM in the project structure.
     */
    protected boolean fixIncorrectParentRelativePaths(
            UpgradeContext context, Document pomDocument, Path pomPath, Map<Path, Document> pomMap) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        Element parentElement = root.getChild("parent", namespace);

        if (parentElement == null) {
            return false;
        }

        // Get current relativePath (default is "../pom.xml")
        Element relativePathElement = parentElement.getChild("relativePath", namespace);
        String currentRelativePath = relativePathElement != null ? relativePathElement.getTextTrim() : "../pom.xml";
        if (currentRelativePath == null || currentRelativePath.trim().isEmpty()) {
            currentRelativePath = "../pom.xml";
        }

        // Check if current path is valid
        Path resolvedParentPath = resolveParentPomPath(pomPath, currentRelativePath);
        if (resolvedParentPath != null && Files.exists(resolvedParentPath)) {
            return false; // Path is correct
        }

        // Find correct parent in pomMap
        String parentGroupId = getChildText(parentElement, "groupId", namespace);
        String parentArtifactId = getChildText(parentElement, "artifactId", namespace);
        String parentVersion = getChildText(parentElement, "version", namespace);

        if (parentGroupId == null || parentArtifactId == null) {
            return false;
        }

        Path correctParentPath = findParentPomInMap(parentGroupId, parentArtifactId, parentVersion, pomMap);
        if (correctParentPath == null) {
            return false;
        }

        String correctRelativePath = calculateRelativePath(pomPath, correctParentPath);
        if (correctRelativePath == null || correctRelativePath.equals(currentRelativePath)) {
            return false;
        }

        // Update relativePath element
        if (relativePathElement == null) {
            relativePathElement = new Element("relativePath", namespace);
            Element insertAfter = parentElement.getChild("version", namespace);
            if (insertAfter == null) {
                insertAfter = parentElement.getChild("artifactId", namespace);
            }
            if (insertAfter != null) {
                parentElement.addContent(parentElement.indexOf(insertAfter) + 1, relativePathElement);
            } else {
                parentElement.addContent(relativePathElement);
            }
        }

        relativePathElement.setText(correctRelativePath);
        context.logger.info("      • Fixed parent relativePath from '" + currentRelativePath + "' to '"
                + correctRelativePath + "'");
        return true;
    }

    /**
     * Finds a parent POM in the pomMap by GAV coordinates.
     */
    protected Path findParentPomInMap(String groupId, String artifactId, String version, Map<Path, Document> pomMap) {
        return pomMap.entrySet().stream()
                .filter(entry -> {
                    GAV gav = extractGAVWithParentResolution(entry.getValue(), null);
                    return gav != null
                            && java.util.Objects.equals(gav.groupId, groupId)
                            && java.util.Objects.equals(gav.artifactId, artifactId)
                            && (version == null || java.util.Objects.equals(gav.version, version));
                })
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Calculates the relative path from a child POM to a parent POM.
     */
    protected String calculateRelativePath(Path childPomPath, Path parentPomPath) {
        try {
            Path childDir = childPomPath.getParent();
            return childDir != null
                    ? childDir.relativize(parentPomPath).toString().replace('\\', '/')
                    : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Recursively finds all elements with a specific attribute value.
     */
    protected List<Element> findElementsWithAttribute(Element element, String attributeName, String attributeValue) {
        List<Element> result = new ArrayList<>();

        // Check current element
        org.jdom2.Attribute attr = element.getAttribute(attributeName);
        if (attr != null && attributeValue.equals(attr.getValue())) {
            result.add(element);
        }

        // Recursively check children
        for (Element child : element.getChildren()) {
            result.addAll(findElementsWithAttribute(child, attributeName, attributeValue));
        }

        return result;
    }
}
