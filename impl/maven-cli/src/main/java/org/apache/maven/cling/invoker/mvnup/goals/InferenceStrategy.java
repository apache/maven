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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Files.DEFAULT_PARENT_RELATIVE_PATH;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Files.POM_XML;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.ModelVersions.MODEL_VERSION_4_1_0;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.RELATIVE_PATH;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.SUBPROJECTS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

/**
 * Strategy for applying Maven 4.1.0+ inference optimizations.
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
        return "Applying Maven 4.1.0+ inference optimizations";
    }

    @Override
    public UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        // Compute all GAVs for inference
        Set<GAV> allGAVs = GAVUtils.computeAllGAVs(context, pomMap);

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
                            "Model version " + currentVersion + " not eligible for inference (requires >= 4.1.0)");
                    continue;
                }

                boolean hasInferences = false;

                // Apply all inference optimizations
                hasInferences |= applyParentInference(context, pomMap, pomDocument);
                hasInferences |= applyDependencyInference(context, allGAVs, pomDocument);
                hasInferences |= applyDependencyInferenceRedundancy(context, pomMap, pomDocument);
                hasInferences |= applySubprojectsInference(context, pomDocument, pomPath);
                hasInferences |= applyModelVersionInference(context, pomDocument);

                if (hasInferences) {
                    modifiedPoms.add(pomPath);
                    context.success("Inference optimizations applied");
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
     * Applies parent-related inference optimizations.
     * Removes redundant groupId/version that can be inferred from parent.
     */
    private boolean applyParentInference(UpgradeContext context, Map<Path, Document> pomMap, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasChanges = false;

        // Check if this POM has a parent
        Element parentElement = root.getChild(PARENT, namespace);
        if (parentElement == null) {
            return false;
        }

        // Determine model version for inference level
        String modelVersion = getChildText(root, "modelVersion", namespace);
        boolean isModel410OrHigher = "4.1.0".equals(modelVersion);

        if (isModel410OrHigher) {
            // Full inference for 4.1.0+ models
            hasChanges |= trimParentElementFull(context, root, parentElement, namespace, pomMap);
        } else {
            // Limited inference for 4.0.0 models
            hasChanges |= trimParentElementLimited(context, root, parentElement, namespace);
        }

        return hasChanges;
    }

    /**
     * Applies dependency-related inference optimizations.
     * Removes managed dependencies that point to project artifacts.
     */
    private boolean applyDependencyInference(UpgradeContext context, Set<GAV> allGAVs, Document pomDocument) {
        boolean hasChanges = false;
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check dependencyManagement section
        Element dependencyManagement = root.getChild("dependencyManagement", namespace);
        if (dependencyManagement != null) {
            Element dependencies = dependencyManagement.getChild("dependencies", namespace);
            if (dependencies != null) {
                hasChanges |= removeManagedDependenciesFromSection(
                        context, dependencies, namespace, allGAVs, "dependencyManagement");
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
                        hasChanges |= removeManagedDependenciesFromSection(
                                context, profileDependencies, namespace, allGAVs, "profile dependencyManagement");
                    }
                }
            }
        }

        return hasChanges;
    }

    /**
     * Applies dependency inference redundancy optimizations.
     * Removes redundant groupId/version from regular dependencies that can be inferred from project artifacts.
     */
    private boolean applyDependencyInferenceRedundancy(
            UpgradeContext context, Map<Path, Document> pomMap, Document pomDocument) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean hasChanges = false;

        // Process main dependencies
        Element dependenciesElement = root.getChild("dependencies", namespace);
        if (dependenciesElement != null) {
            hasChanges |= removeDependencyInferenceFromSection(
                    context, dependenciesElement, namespace, pomMap, "dependencies");
        }

        // Process profile dependencies
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                Element profileDependencies = profileElement.getChild("dependencies", namespace);
                if (profileDependencies != null) {
                    hasChanges |= removeDependencyInferenceFromSection(
                            context, profileDependencies, namespace, pomMap, "profile dependencies");
                }
            }
        }

        // Process build plugin dependencies
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement != null) {
            Element pluginsElement = buildElement.getChild(PLUGINS, namespace);
            if (pluginsElement != null) {
                List<Element> pluginElements = pluginsElement.getChildren(PLUGIN, namespace);
                for (Element pluginElement : pluginElements) {
                    Element pluginDependencies = pluginElement.getChild("dependencies", namespace);
                    if (pluginDependencies != null) {
                        hasChanges |= removeDependencyInferenceFromSection(
                                context, pluginDependencies, namespace, pomMap, "plugin dependencies");
                    }
                }
            }
        }

        return hasChanges;
    }

    /**
     * Applies subprojects-related inference optimizations.
     * Removes redundant subprojects lists that match direct children.
     */
    private boolean applySubprojectsInference(UpgradeContext context, Document pomDocument, Path pomPath) {
        boolean hasChanges = false;
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        // Check main subprojects
        Element subprojectsElement = root.getChild(SUBPROJECTS, namespace);
        if (subprojectsElement != null) {
            if (isSubprojectsListRedundant(subprojectsElement, namespace, pomPath)) {
                removeElementWithFormatting(subprojectsElement);
                context.detail("Removed: redundant subprojects list (matches direct children)");
                hasChanges = true;
            }
        }

        // Check profiles for subprojects
        Element profilesElement = root.getChild("profiles", namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren("profile", namespace);
            for (Element profileElement : profileElements) {
                Element profileSubprojects = profileElement.getChild(SUBPROJECTS, namespace);
                if (profileSubprojects != null) {
                    if (isSubprojectsListRedundant(profileSubprojects, namespace, pomPath)) {
                        removeElementWithFormatting(profileSubprojects);
                        context.detail("Removed: redundant subprojects list from profile (matches direct children)");
                        hasChanges = true;
                    }
                }
            }
        }

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
    private boolean trimParentElementLimited(
            UpgradeContext context, Element root, Element parentElement, Namespace namespace) {
        boolean hasChanges = false;

        // Get parent GAV
        String parentGroupId = getChildText(parentElement, "groupId", namespace);
        String parentVersion = getChildText(parentElement, "version", namespace);

        // Get child GAV
        String childGroupId = getChildText(root, "groupId", namespace);
        String childVersion = getChildText(root, "version", namespace);

        // Remove child groupId if it matches parent groupId
        if (childGroupId != null && Objects.equals(childGroupId, parentGroupId)) {
            Element childGroupIdElement = root.getChild("groupId", namespace);
            if (childGroupIdElement != null) {
                removeElementWithFormatting(childGroupIdElement);
                context.detail("Removed: child groupId (matches parent)");
                hasChanges = true;
            }
        }

        // Remove child version if it matches parent version
        if (childVersion != null && Objects.equals(childVersion, parentVersion)) {
            Element childVersionElement = root.getChild("version", namespace);
            if (childVersionElement != null) {
                removeElementWithFormatting(childVersionElement);
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
            UpgradeContext context,
            Element root,
            Element parentElement,
            Namespace namespace,
            Map<Path, Document> pomMap) {
        boolean hasChanges = false;

        // First apply limited inference (child elements)
        hasChanges |= trimParentElementLimited(context, root, parentElement, namespace);

        // Get child GAV
        String childGroupId = getChildText(root, GROUP_ID, namespace);
        String childVersion = getChildText(root, VERSION, namespace);

        // Remove parent groupId if child has no explicit groupId
        if (childGroupId == null) {
            Element parentGroupIdElement = parentElement.getChild(GROUP_ID, namespace);
            if (parentGroupIdElement != null) {
                removeElementWithFormatting(parentGroupIdElement);
                context.detail("Removed: parent groupId (child has no explicit groupId)");
                hasChanges = true;
            }
        }

        // Remove parent version if child has no explicit version
        if (childVersion == null) {
            Element parentVersionElement = parentElement.getChild(VERSION, namespace);
            if (parentVersionElement != null) {
                removeElementWithFormatting(parentVersionElement);
                context.detail("Removed: parent version (child has no explicit version)");
                hasChanges = true;
            }
        }

        // Remove parent artifactId if it can be inferred from relativePath
        if (canInferParentArtifactId(parentElement, namespace, pomMap)) {
            Element parentArtifactIdElement = parentElement.getChild(ARTIFACT_ID, namespace);
            if (parentArtifactIdElement != null) {
                removeElementWithFormatting(parentArtifactIdElement);
                context.detail("Removed: parent artifactId (can be inferred from relativePath)");
                hasChanges = true;
            }
        }

        return hasChanges;
    }

    /**
     * Determines if parent artifactId can be inferred from relativePath.
     */
    private boolean canInferParentArtifactId(Element parentElement, Namespace namespace, Map<Path, Document> pomMap) {
        // Get relativePath (default is "../pom.xml" if not specified)
        String relativePath = getChildText(parentElement, RELATIVE_PATH, namespace);
        if (relativePath == null || relativePath.trim().isEmpty()) {
            relativePath = DEFAULT_PARENT_RELATIVE_PATH; // Maven default
        }

        // For now, we use a simple heuristic: if relativePath is the default "../pom.xml"
        // and we have parent POMs in our pomMap, we can likely infer the artifactId.
        // A more sophisticated implementation would resolve the actual path and check
        // if the parent POM exists in pomMap.
        return DEFAULT_PARENT_RELATIVE_PATH.equals(relativePath) && !pomMap.isEmpty();
    }

    /**
     * Checks if a subprojects list is redundant (matches direct child directories with pom.xml).
     */
    private boolean isSubprojectsListRedundant(Element subprojectsElement, Namespace namespace, Path pomPath) {
        List<Element> subprojectElements = subprojectsElement.getChildren(SUBPROJECT, namespace);
        if (subprojectElements.isEmpty()) {
            return true; // Empty list is redundant
        }

        // Get the directory containing this POM
        Path parentDir = pomPath.getParent();
        if (parentDir == null) {
            return false;
        }

        // Get declared subprojects
        Set<String> declaredSubprojects = new HashSet<>();
        for (Element subprojectElement : subprojectElements) {
            String subprojectName = subprojectElement.getTextTrim();
            if (subprojectName != null && !subprojectName.isEmpty()) {
                declaredSubprojects.add(subprojectName);
            }
        }

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
            UpgradeContext context, Element dependencies, Namespace namespace, Set<GAV> allGAVs, String sectionName) {
        List<Element> dependencyElements = dependencies.getChildren(DEPENDENCY, namespace);
        List<Element> toRemove = new ArrayList<>();

        for (Element dependency : dependencyElements) {
            String groupId = getChildText(dependency, GROUP_ID, namespace);
            String artifactId = getChildText(dependency, ARTIFACT_ID, namespace);

            if (groupId != null && artifactId != null) {
                // Check if this dependency matches any project artifact
                boolean isProjectArtifact = allGAVs.stream()
                        .anyMatch(gav ->
                                Objects.equals(gav.groupId(), groupId) && Objects.equals(gav.artifactId(), artifactId));

                if (isProjectArtifact) {
                    toRemove.add(dependency);
                    context.detail("Removed: " + "managed dependency " + groupId + ":" + artifactId + " from "
                            + sectionName + " (project artifact)");
                }
            }
        }

        // Remove project artifacts while preserving formatting
        for (Element dependency : toRemove) {
            removeElementWithFormatting(dependency);
        }

        return !toRemove.isEmpty();
    }

    /**
     * Helper method to remove dependency inference redundancy from a specific dependencies section.
     */
    private boolean removeDependencyInferenceFromSection(
            UpgradeContext context,
            Element dependencies,
            Namespace namespace,
            Map<Path, Document> pomMap,
            String sectionName) {
        List<Element> dependencyElements = dependencies.getChildren(DEPENDENCY, namespace);
        boolean hasChanges = false;

        for (Element dependency : dependencyElements) {
            String groupId = getChildText(dependency, GROUP_ID, namespace);
            String artifactId = getChildText(dependency, ARTIFACT_ID, namespace);
            String version = getChildText(dependency, VERSION, namespace);

            if (artifactId != null) {
                // Try to find the dependency POM in our pomMap
                Document dependencyPom = findDependencyPom(context, pomMap, groupId, artifactId);
                if (dependencyPom != null) {
                    // Check if we can infer groupId
                    if (groupId != null && canInferDependencyGroupId(context, dependencyPom, groupId)) {
                        Element groupIdElement = dependency.getChild(GROUP_ID, namespace);
                        if (groupIdElement != null) {
                            removeElementWithFormatting(groupIdElement);
                            context.detail("Removed: " + "dependency groupId " + groupId + " from " + sectionName
                                    + " (can be inferred from project)");
                            hasChanges = true;
                        }
                    }

                    // Check if we can infer version
                    if (version != null && canInferDependencyVersion(context, dependencyPom, version)) {
                        Element versionElement = dependency.getChild(VERSION, namespace);
                        if (versionElement != null) {
                            removeElementWithFormatting(versionElement);
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
        for (Document pomDocument : pomMap.values()) {
            GAV gav = GAVUtils.extractGAVWithParentResolution(context, pomDocument);
            if (gav != null && Objects.equals(gav.groupId(), groupId) && Objects.equals(gav.artifactId(), artifactId)) {
                return pomDocument;
            }
        }
        return null;
    }

    /**
     * Determines if a dependency version can be inferred from the project artifact.
     */
    private boolean canInferDependencyVersion(UpgradeContext context, Document dependencyPom, String declaredVersion) {
        GAV projectGav = GAVUtils.extractGAVWithParentResolution(context, dependencyPom);
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
        GAV projectGav = GAVUtils.extractGAVWithParentResolution(context, dependencyPom);
        if (projectGav == null || projectGav.groupId() == null) {
            return false;
        }

        // We can infer the groupId if the declared groupId matches the project groupId
        return Objects.equals(declaredGroupId, projectGav.groupId());
    }

    /**
     * Helper method to get child text content.
     */
    private String getChildText(Element parent, String childName, Namespace namespace) {
        Element child = parent.getChild(childName, namespace);
        return child != null ? child.getTextTrim() : null;
    }

    /**
     * Removes an element while preserving surrounding formatting.
     */
    private void removeElementWithFormatting(Element element) {
        Element parent = element.getParentElement();
        if (parent != null) {
            int index = parent.indexOf(element);
            parent.removeContent(element);

            // Remove preceding whitespace if it exists
            if (index > 0) {
                Content prevContent = parent.getContent(index - 1);
                if (prevContent instanceof Text text && text.getTextTrim().isEmpty()) {
                    parent.removeContent(prevContent);
                }
            }
        }
    }
}
