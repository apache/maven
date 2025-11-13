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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECTS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Files.DEFAULT_PARENT_RELATIVE_PATH;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Files.POM_XML;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_1_0;

/**
 * Strategy for applying Maven inference optimizations.
 * For 4.0.0 models: applies limited inference (parent-related only).
 * For 4.1.0+ models: applies full inference optimizations.
 * Removes redundant information that can be inferred by Maven during model building.
 */
@Named
@Singleton
@Priority(30)
public class InferenceStrategy extends AbstractUpgradeStrategy {

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        // Handle --all option (overrides individual options)
        boolean useAll = options.all().orElse(false);
        if (useAll) {
            return true;
        }

        // Check if --infer is explicitly set
        if (options.infer().isPresent()) {
            return options.infer().get();
        }

        // Apply default behavior: if no specific options are provided, enable --infer
        if (options.infer().isEmpty()
                && options.model().isEmpty()
                && options.plugins().isEmpty()
                && options.modelVersion().isEmpty()) {
            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Applying Maven inference optimizations";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        // Compute all GAVs for inference
        Set<Coordinates> allGAVs = computeAllArtifactCoordinates(context, pomMap);

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            String currentVersion = ModelVersionUtils.detectModelVersion(pomDocument);
            context.info(pomPath + " (current: " + currentVersion + ")");
            context.indent();

            try {
                if (!ModelVersionUtils.isEligibleForInference(currentVersion)) {
                    context.warning(
                            "Model version " + currentVersion + " not eligible for inference (requires >= 4.0.0)");
                    continue;
                }

                boolean hasInferences = false;

                // Apply limited parent inference for all eligible models (4.0.0+)
                hasInferences |= applyLimitedParentInference(context, pomDocument);

                // Apply full inference optimizations only for 4.1.0+ models
                if (MODEL_VERSION_4_1_0.equals(currentVersion) || ModelVersionUtils.isNewerThan410(currentVersion)) {
                    hasInferences |= applyFullParentInference(context, pomMap, pomDocument);
                    hasInferences |= applyDependencyInference(context, allGAVs, pomDocument);
                    hasInferences |= applyDependencyInferenceRedundancy(context, pomMap, pomDocument);
                    hasInferences |= applySubprojectsInference(context, pomDocument, pomPath);
                    hasInferences |= applyModelVersionInference(context, pomDocument);
                }

                if (hasInferences) {
                    modifiedPoms.add(pomPath);
                    if (MODEL_VERSION_4_1_0.equals(currentVersion)
                            || ModelVersionUtils.isNewerThan410(currentVersion)) {
                        context.success("Full inference optimizations applied");
                    } else {
                        context.success("Limited inference optimizations applied (parent-related only)");
                    }
                } else {
                    context.success("No inference optimizations needed");
                }
            } catch (Exception e) {
                context.failure("Failed to apply inference optimizations" + ": " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Applies limited parent-related inference optimizations for Maven 4.0.0+ models.
     * Removes redundant child groupId/version that can be inferred from parent.
     */
    private boolean applyLimitedParentInference(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.root();

        // Check if this POM has a parent
        Element parentElement = root.child(PARENT).orElse(null);
        if (parentElement == null) {
            return false;
        }

        // Apply limited inference (child groupId/version removal only)
        return trimParentElementLimited(context, root, parentElement);
    }

    /**
     * Applies full parent-related inference optimizations for Maven 4.1.0+ models.
     * Removes redundant parent elements that can be inferred from relativePath.
     */
    private boolean applyFullParentInference(UpgradeContext context, Map<Path, Document> pomMap, Document pomDocument) {
        Element root = pomDocument.root();

        // Check if this POM has a parent
        Element parentElement = root.child(PARENT).orElse(null);
        if (parentElement == null) {
            return false;
        }

        // Apply full inference (parent element trimming based on relativePath)
        return trimParentElementFull(context, root, parentElement, pomMap);
    }

    /**
     * Applies dependency-related inference optimizations.
     * Removes managed dependencies that point to project artifacts.
     */
    private boolean applyDependencyInference(UpgradeContext context, Set<Coordinates> allGAVs, Document pomDocument) {
        boolean hasChanges = false;
        Element root = pomDocument.root();

        // Check dependencyManagement section
        Element dependencyManagement = root.child(DEPENDENCY_MANAGEMENT).orElse(null);
        if (dependencyManagement != null) {
            Element dependencies = dependencyManagement.child(DEPENDENCIES).orElse(null);
            if (dependencies != null) {
                hasChanges |=
                        removeManagedDependenciesFromSection(context, dependencies, allGAVs, DEPENDENCY_MANAGEMENT);
            }
        }

        // Check profiles for dependencyManagement
        boolean profileChanges = root.child(PROFILES).stream()
                .flatMap(profiles -> profiles.children(PROFILE))
                .map(profile -> profile.child(DEPENDENCY_MANAGEMENT)
                        .flatMap(dm -> dm.child(DEPENDENCIES))
                        .map(deps -> removeManagedDependenciesFromSection(
                                context, deps, allGAVs, "profile dependencyManagement"))
                        .orElse(false))
                .reduce(false, Boolean::logicalOr);

        hasChanges |= profileChanges;

        return hasChanges;
    }

    /**
     * Applies dependency inference redundancy optimizations.
     * Removes redundant groupId/version from regular dependencies that can be inferred from project artifacts.
     */
    private boolean applyDependencyInferenceRedundancy(
            UpgradeContext context, Map<Path, Document> pomMap, Document pomDocument) {
        Element root = pomDocument.root();
        boolean hasChanges = false;

        // Process main dependencies
        Element dependenciesElement = root.child(DEPENDENCIES).orElse(null);
        if (dependenciesElement != null) {
            hasChanges |= removeDependencyInferenceFromSection(context, dependenciesElement, pomMap, DEPENDENCIES);
        }

        // Process profile dependencies
        boolean profileDependencyChanges = root.child(PROFILES).stream()
                .flatMap(profiles -> profiles.children(PROFILE))
                .map(profile -> profile.child(DEPENDENCIES)
                        .map(deps ->
                                removeDependencyInferenceFromSection(context, deps, pomMap, "profile dependencies"))
                        .orElse(false))
                .reduce(false, Boolean::logicalOr);

        hasChanges |= profileDependencyChanges;

        // Process build plugin dependencies
        boolean pluginDependencyChanges = root.child(BUILD).flatMap(build -> build.child(PLUGINS)).stream()
                .flatMap(plugins -> plugins.children(PLUGIN))
                .map(plugin -> plugin.child(DEPENDENCIES)
                        .map(deps -> removeDependencyInferenceFromSection(context, deps, pomMap, "plugin dependencies"))
                        .orElse(false))
                .reduce(false, Boolean::logicalOr);

        hasChanges |= pluginDependencyChanges;

        return hasChanges;
    }

    /**
     * Applies subprojects-related inference optimizations.
     * Removes redundant subprojects lists that match direct children.
     */
    private boolean applySubprojectsInference(UpgradeContext context, Document pomDocument, Path pomPath) {
        boolean hasChanges = false;
        Element root = pomDocument.root();

        // Check main subprojects
        Element subprojectsElement = root.child(SUBPROJECTS).orElse(null);
        if (subprojectsElement != null) {
            if (isSubprojectsListRedundant(subprojectsElement, pomPath)) {
                DomUtils.removeElement(subprojectsElement);
                context.detail("Removed: redundant subprojects list (matches direct children)");
                hasChanges = true;
            }
        }

        // Check profiles for subprojects
        boolean profileSubprojectsChanges = root.child(PROFILES).stream()
                .flatMap(profiles -> profiles.children(PROFILE))
                .map(profile -> profile.child(SUBPROJECTS)
                        .filter(subprojects -> isSubprojectsListRedundant(subprojects, pomPath))
                        .map(subprojects -> {
                            DomUtils.removeElement(subprojects);
                            context.detail(
                                    "Removed: redundant subprojects list from profile (matches direct children)");
                            return true;
                        })
                        .orElse(false))
                .reduce(false, Boolean::logicalOr);

        hasChanges |= profileSubprojectsChanges;

        return hasChanges;
    }

    /**
     * Applies model version inference optimization.
     * Removes modelVersion element when it can be inferred from namespace.
     */
    private boolean applyModelVersionInference(UpgradeContext context, Document pomDocument) {
        String currentVersion = ModelVersionUtils.detectModelVersion(pomDocument);

        // Only remove modelVersion for 4.1.0+ models where it can be inferred from namespace
        if (MODEL_VERSION_4_1_0.equals(currentVersion) || ModelVersionUtils.isNewerThan410(currentVersion)) {

            if (ModelVersionUtils.removeModelVersion(pomDocument)) {
                context.detail("Removed: modelVersion element (can be inferred from namespace)");
                return true;
            }
        }

        return false;
    }

    /**
     * Applies limited parent inference for 4.0.0 models.
     * Only removes child groupId/version when they match parent.
     */
    private boolean trimParentElementLimited(UpgradeContext context, Element root, Element parentElement) {
        boolean hasChanges = false;

        // Get parent GAV
        String parentGroupId = parentElement.childText(MavenPomElements.Elements.GROUP_ID);
        String parentVersion = parentElement.childText(MavenPomElements.Elements.VERSION);

        // Get child GAV
        String childGroupId = root.childText(MavenPomElements.Elements.GROUP_ID);
        String childVersion = root.childText(MavenPomElements.Elements.VERSION);

        // Remove child groupId if it matches parent groupId
        if (childGroupId != null && Objects.equals(childGroupId, parentGroupId)) {
            Element childGroupIdElement = root.child(GROUP_ID).orElse(null);
            if (childGroupIdElement != null) {
                DomUtils.removeElement(childGroupIdElement);
                context.detail("Removed: child groupId (matches parent)");
                hasChanges = true;
            }
        }

        // Remove child version if it matches parent version
        if (childVersion != null && Objects.equals(childVersion, parentVersion)) {
            Element childVersionElement = root.child("version").orElse(null);
            if (childVersionElement != null) {
                DomUtils.removeElement(childVersionElement);
                context.detail("Removed: child version (matches parent)");
                hasChanges = true;
            }
        }

        return hasChanges;
    }

    /**
     * Applies full parent inference for 4.1.0+ models.
     * Removes parent groupId/version/artifactId when they can be inferred.
     */
    private boolean trimParentElementFull(
            UpgradeContext context, Element root, Element parentElement, Map<Path, Document> pomMap) {
        boolean hasChanges = false;

        // Get child GAV before applying any changes
        String childGroupId = root.childText(MavenPomElements.Elements.GROUP_ID);
        String childVersion = root.childText(MavenPomElements.Elements.VERSION);

        // First apply limited inference (child elements) - this removes matching child groupId/version
        hasChanges |= trimParentElementLimited(context, root, parentElement);

        // Only remove parent elements if the parent is in the same reactor (not external)
        if (isParentInReactor(parentElement, pomMap, context)) {
            // Remove parent groupId if child has no explicit groupId
            if (childGroupId == null) {
                Element parentGroupIdElement = parentElement.child(GROUP_ID).orElse(null);
                if (parentGroupIdElement != null) {
                    DomUtils.removeElement(parentGroupIdElement);
                    context.detail("Removed: parent groupId (child has no explicit groupId)");
                    hasChanges = true;
                }
            }

            // Remove parent version if child has no explicit version
            if (childVersion == null) {
                Element parentVersionElement = parentElement.child(VERSION).orElse(null);
                if (parentVersionElement != null) {
                    DomUtils.removeElement(parentVersionElement);
                    context.detail("Removed: parent version (child has no explicit version)");
                    hasChanges = true;
                }
            }

            // Remove parent artifactId if it can be inferred from relativePath
            if (canInferParentArtifactId(parentElement, pomMap)) {
                Element parentArtifactIdElement =
                        parentElement.child(ARTIFACT_ID).orElse(null);
                if (parentArtifactIdElement != null) {
                    DomUtils.removeElement(parentArtifactIdElement);
                    context.detail("Removed: parent artifactId (can be inferred from relativePath)");
                    hasChanges = true;
                }
            }
        }

        return hasChanges;
    }

    /**
     * Determines if the parent is part of the same reactor (multi-module project)
     * vs. an external parent POM by checking if the parent exists in the pomMap.
     */
    private boolean isParentInReactor(Element parentElement, Map<Path, Document> pomMap, UpgradeContext context) {
        // If relativePath is explicitly set to empty, parent is definitely external
        String relativePath = parentElement.childText(MavenPomElements.Elements.RELATIVE_PATH);
        if (relativePath != null && relativePath.trim().isEmpty()) {
            return false;
        }

        // Extract parent GAV
        String parentGroupId = parentElement.childText(MavenPomElements.Elements.GROUP_ID);
        String parentArtifactId = parentElement.childText(MavenPomElements.Elements.ARTIFACT_ID);
        String parentVersion = parentElement.childText(MavenPomElements.Elements.VERSION);

        if (parentGroupId == null || parentArtifactId == null || parentVersion == null) {
            // Cannot determine parent GAV, assume external
            return false;
        }

        Coordinates parentGAV = Coordinates.of(parentGroupId, parentArtifactId, parentVersion);

        // Check if any POM in our reactor matches the parent GAV using GAVUtils
        for (Document pomDocument : pomMap.values()) {
            Coordinates pomGAV =
                    AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(context, pomDocument);
            if (pomGAV != null && pomGAV.equals(parentGAV)) {
                return true;
            }
        }

        // Parent not found in reactor, must be external
        return false;
    }

    /**
     * Determines if parent artifactId can be inferred from relativePath.
     */
    private boolean canInferParentArtifactId(Element parentElement, Map<Path, Document> pomMap) {
        // Get relativePath (default is "../pom.xml" if not specified)
        String relativePath = parentElement.childText(MavenPomElements.Elements.RELATIVE_PATH);
        if (relativePath == null || relativePath.trim().isEmpty()) {
            relativePath = DEFAULT_PARENT_RELATIVE_PATH; // Maven default
        }

        // Only infer artifactId if relativePath is the default and we have multiple POMs
        // indicating this is likely a multi-module project
        return DEFAULT_PARENT_RELATIVE_PATH.equals(relativePath) && pomMap.size() > 1;
    }

    /**
     * Checks if a subprojects list is redundant (matches direct child directories with pom.xml).
     */
    private boolean isSubprojectsListRedundant(Element subprojectsElement, Path pomPath) {
        List<Element> subprojectElements =
                subprojectsElement.children(SUBPROJECT).toList();
        if (subprojectElements.isEmpty()) {
            return true; // Empty list is redundant
        }

        // Get the directory containing this POM
        Path parentDir = pomPath.getParent();
        if (parentDir == null) {
            return false;
        }

        // Get declared subprojects
        Set<String> declaredSubprojects = subprojectElements.stream()
                .map(Element::textContentTrimmed)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toSet());

        // Get list of actual direct child directories with pom.xml
        Set<String> actualSubprojects = new HashSet<>();
        try {
            if (Files.exists(parentDir) && Files.isDirectory(parentDir)) {
                try (Stream<Path> children = Files.list(parentDir)) {
                    children.filter(Files::isDirectory)
                            .filter(dir -> Files.exists(dir.resolve(POM_XML)))
                            .forEach(dir ->
                                    actualSubprojects.add(dir.getFileName().toString()));
                }
            }
        } catch (Exception e) {
            // If we can't read the directory, assume not redundant
            return false;
        }

        // Lists are redundant if they match exactly
        return declaredSubprojects.equals(actualSubprojects);
    }

    /**
     * Helper method to remove managed dependencies from a specific dependencies section.
     */
    private boolean removeManagedDependenciesFromSection(
            UpgradeContext context, Element dependencies, Set<Coordinates> allGAVs, String sectionName) {
        List<Element> dependencyElements = dependencies.children(DEPENDENCY).toList();

        List<Element> projectArtifacts = dependencyElements.stream()
                .filter(dependency -> {
                    String groupId = dependency.childText(MavenPomElements.Elements.GROUP_ID);
                    String artifactId = dependency.childText(MavenPomElements.Elements.ARTIFACT_ID);

                    if (groupId != null && artifactId != null) {
                        boolean isProjectArtifact = allGAVs.stream()
                                .anyMatch(gav -> Objects.equals(gav.groupId(), groupId)
                                        && Objects.equals(gav.artifactId(), artifactId));

                        if (isProjectArtifact) {
                            context.detail("Removed: managed dependency " + groupId + ":" + artifactId + " from "
                                    + sectionName + " (project artifact)");
                            return true;
                        }
                    }
                    return false;
                })
                .toList();

        // Remove project artifacts while preserving formatting
        projectArtifacts.forEach(DomUtils::removeElement);

        return !projectArtifacts.isEmpty();
    }

    /**
     * Helper method to remove dependency inference redundancy from a specific dependencies section.
     */
    private boolean removeDependencyInferenceFromSection(
            UpgradeContext context, Element dependencies, Map<Path, Document> pomMap, String sectionName) {
        List<Element> dependencyElements = dependencies.children(DEPENDENCY).toList();
        boolean hasChanges = false;

        for (Element dependency : dependencyElements) {
            String groupId = dependency.childText(MavenPomElements.Elements.GROUP_ID);
            String artifactId = dependency.childText(MavenPomElements.Elements.ARTIFACT_ID);
            String version = dependency.childText(MavenPomElements.Elements.VERSION);

            if (artifactId != null) {
                // Try to find the dependency POM in our pomMap
                Document dependencyPom = findDependencyPom(context, pomMap, groupId, artifactId);
                if (dependencyPom != null) {
                    // Check if we can infer groupId
                    if (groupId != null && canInferDependencyGroupId(context, dependencyPom, groupId)) {
                        Element groupIdElement = dependency.child(GROUP_ID).orElse(null);
                        if (groupIdElement != null) {
                            DomUtils.removeElement(groupIdElement);
                            context.detail("Removed: " + "dependency groupId " + groupId + " from " + sectionName
                                    + " (can be inferred from project)");
                            hasChanges = true;
                        }
                    }

                    // Check if we can infer version
                    if (version != null && canInferDependencyVersion(context, dependencyPom, version)) {
                        Element versionElement = dependency.child(VERSION).orElse(null);
                        if (versionElement != null) {
                            DomUtils.removeElement(versionElement);
                            context.detail("Removed: " + "dependency version " + version + " from " + sectionName
                                    + " (can be inferred from project)");
                            hasChanges = true;
                        }
                    }
                }
            }
        }

        return hasChanges;
    }

    /**
     * Finds a dependency POM in the pomMap by groupId and artifactId.
     */
    private Document findDependencyPom(
            UpgradeContext context, Map<Path, Document> pomMap, String groupId, String artifactId) {
        return pomMap.values().stream()
                .filter(pomDocument -> {
                    Coordinates gav = AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(
                            context, pomDocument);
                    return gav != null
                            && Objects.equals(gav.groupId(), groupId)
                            && Objects.equals(gav.artifactId(), artifactId);
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines if a dependency version can be inferred from the project artifact.
     */
    private boolean canInferDependencyVersion(UpgradeContext context, Document dependencyPom, String declaredVersion) {
        Coordinates projectGav =
                AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(context, dependencyPom);
        if (projectGav == null || projectGav.version() == null) {
            return false;
        }

        // We can infer the version if the declared version matches the project version
        return Objects.equals(declaredVersion, projectGav.version());
    }

    /**
     * Determines if a dependency groupId can be inferred from the project artifact.
     */
    private boolean canInferDependencyGroupId(UpgradeContext context, Document dependencyPom, String declaredGroupId) {
        Coordinates projectGav =
                AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(context, dependencyPom);
        if (projectGav == null || projectGav.groupId() == null) {
            return false;
        }

        // We can infer the groupId if the declared groupId matches the project groupId
        return Objects.equals(declaredGroupId, projectGav.groupId());
    }
}
