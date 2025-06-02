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
import java.util.Map;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Inject;
import org.apache.maven.cling.invoker.mvnup.Goal;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Files.MVN_DIRECTORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_1_0;

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
 *   <li><strong>Duplicate Plugins</strong>: Removes duplicate plugin declarations that Maven 4 strictly validates</li>
 *   <li><strong>Unsupported Repository Expressions</strong>: Comments out repositories with expressions not supported by Maven 4</li>
 *   <li><strong>Incorrect Parent Relative Paths</strong>: Fixes parent.relativePath that point to non-existent POMs by searching the project structure</li>
 *   <li><strong>.mvn Directory Creation</strong>: Creates .mvn directory in root when not upgrading to 4.1.0 to avoid root directory warnings</li>
 * </ul>
 *
 * <h3>Plugin Upgrades</h3>
 * When {@code --plugins} option is enabled (or by default), upgrades plugins known to fail with Maven 4:
 * <ul>
 *   <li><strong>maven-exec-plugin</strong>: Upgrades to version 3.2.0 or higher</li>
 *   <li><strong>maven-enforcer-plugin</strong>: Upgrades to version 3.0.0 or higher</li>
 *   <li><strong>flatten-maven-plugin</strong>: Upgrades to version 1.2.7 or higher</li>
 *   <li><strong>maven-shade-plugin</strong>: Upgrades to version 3.5.0 or higher</li>
 *   <li><strong>maven-remote-resources-plugin</strong>: Upgrades to version 3.0.0 or higher</li>
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
public abstract class AbstractUpgradeGoal implements Goal {

    private final StrategyOrchestrator orchestrator;

    @Inject
    public AbstractUpgradeGoal(StrategyOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Executes the upgrade goal.
     * Template method that calls doUpgrade and optionally saves modifications.
     */
    @Override
    public int execute(UpgradeContext context) throws Exception {
        UpgradeOptions options = context.options();

        // Determine target model version
        // Default to 4.0.0 unless --all is specified or explicit --model-version is provided
        String targetModel;
        if (options.modelVersion().isPresent()) {
            targetModel = options.modelVersion().get();
        } else if (options.all().orElse(false)) {
            targetModel = MODEL_VERSION_4_1_0;
        } else {
            targetModel = UpgradeConstants.ModelVersions.MODEL_VERSION_4_0_0;
        }

        if (!ModelVersionUtils.isValidModelVersion(targetModel)) {
            context.failure("Invalid target model version: " + targetModel);
            context.failure("Supported versions: 4.0.0, 4.1.0");
            return 1;
        }

        // Discover POMs
        context.info("Discovering POM files...");
        Path startingDirectory = options.directory().map(Paths::get).orElse(context.invokerRequest.cwd());

        Map<Path, Document> pomMap;
        try {
            pomMap = PomDiscovery.discoverPoms(startingDirectory);
        } catch (IOException | JDOMException e) {
            context.failure("Failed to discover POM files: " + e.getMessage());
            return 1;
        }

        if (pomMap.isEmpty()) {
            context.warning("No POM files found in " + startingDirectory);
            return 0;
        }

        context.info("Found " + pomMap.size() + " POM file(s)");

        // Perform the upgrade logic
        int result = doUpgrade(context, targetModel, pomMap);

        // Save modifications if this is an apply goal
        if (shouldSaveModifications() && result == 0) {
            saveModifications(context, pomMap);
        }

        return result;
    }

    /**
     * Performs the upgrade logic using the strategy pattern.
     * Delegates to StrategyOrchestrator for coordinated strategy execution.
     */
    protected int doUpgrade(UpgradeContext context, String targetModel, Map<Path, Document> pomMap) {
        // Execute strategies using the orchestrator
        try {
            UpgradeResult result = orchestrator.executeStrategies(context, pomMap);

            // Create .mvn directory if needed (when not upgrading to 4.1.0)
            if (!MODEL_VERSION_4_1_0.equals(targetModel)) {
                createMvnDirectoryIfNeeded(context);
            }

            return result.success() ? 0 : 1;
        } catch (Exception e) {
            context.failure("Strategy execution failed: " + e.getMessage());
            return 1;
        }
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
        context.info("");
        context.info("Saving modified POMs...");

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
                context.failure("Failed to save " + pomPath + ": " + e.getMessage());
            }
        }
    }

    /**
     * Creates .mvn directory in the root directory if it doesn't exist and the model isn't upgraded to 4.1.0.
     * This avoids the warning about not being able to find the root directory.
     */
    protected void createMvnDirectoryIfNeeded(UpgradeContext context) {
        context.info("");
        context.info("Creating .mvn directory if needed to avoid root directory warnings...");

        // Find the root directory (starting directory)
        Path startingDirectory = context.options().directory().map(Paths::get).orElse(context.invokerRequest.cwd());

        Path mvnDir = startingDirectory.resolve(MVN_DIRECTORY);

        try {
            if (!Files.exists(mvnDir)) {
                if (shouldSaveModifications()) {
                    Files.createDirectories(mvnDir);
                    context.success("Created .mvn directory at " + mvnDir);
                } else {
                    context.action("Would create .mvn directory at " + mvnDir);
                }
            } else {
                context.success(".mvn directory already exists at " + mvnDir);
            }
        } catch (Exception e) {
            context.failure("Failed to create .mvn directory: " + e.getMessage());
        }
    }
}
