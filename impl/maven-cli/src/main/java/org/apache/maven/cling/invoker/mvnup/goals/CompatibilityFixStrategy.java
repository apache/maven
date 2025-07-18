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
import java.util.Objects;
import java.util.Set;

import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;

import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Files.DEFAULT_PARENT_RELATIVE_PATH;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.Plugins.MAVEN_PLUGIN_PREFIX;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.COMBINE_APPEND;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.COMBINE_CHILDREN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.COMBINE_MERGE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.COMBINE_OVERRIDE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlAttributes.COMBINE_SELF;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.ARTIFACT_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.BUILD;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.CLASSIFIER;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.DEPENDENCY_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.GROUP_ID;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PARENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGINS;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_MANAGEMENT;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_REPOSITORIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PLUGIN_REPOSITORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.PROFILES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.RELATIVE_PATH;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.REPOSITORIES;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.REPOSITORY;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.TYPE;
import static org.apache.maven.cling.invoker.mvnup.goals.UpgradeConstants.XmlElements.VERSION;

/**
 * Strategy for applying Maven 4 compatibility fixes to POM files.
 * Fixes issues that prevent POMs from being processed by Maven 4.
 */
@Named
@Singleton
@Priority(20)
public class CompatibilityFixStrategy extends AbstractUpgradeStrategy {

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        // Handle --all option (overrides individual options)
        boolean useAll = options.all().orElse(false);
        if (useAll) {
            return true;
        }

        // Apply default behavior: if no specific options are provided, enable --model
        // OR if all options are explicitly disabled, still apply default behavior
        boolean noOptionsSpecified = options.all().isEmpty()
                && options.infer().isEmpty()
                && options.model().isEmpty()
                && options.plugins().isEmpty()
                && options.modelVersion().isEmpty();

        boolean allOptionsDisabled = options.all().map(v -> !v).orElse(false)
                && options.infer().map(v -> !v).orElse(false)
                && options.model().map(v -> !v).orElse(false)
                && options.plugins().map(v -> !v).orElse(false)
                && options.modelVersion().isEmpty();

        if (noOptionsSpecified || allOptionsDisabled) {
            return true;
        }

        // Check if --model is explicitly set (and not part of "all disabled" scenario)
        if (options.model().isPresent()) {
            return options.model().get();
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Applying Maven 4 compatibility fixes";
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

            context.info(pomPath + " (checking for Maven 4 compatibility issues)");
            context.indent();

            try {
                boolean hasIssues = false;

                // Apply all compatibility fixes
                hasIssues |= fixUnsupportedCombineChildrenAttributes(pomDocument, context);
                hasIssues |= fixUnsupportedCombineSelfAttributes(pomDocument, context);
                hasIssues |= fixDuplicateDependencies(pomDocument, context);
                hasIssues |= fixDuplicatePlugins(pomDocument, context);
                hasIssues |= fixUnsupportedRepositoryExpressions(pomDocument, context);
                hasIssues |= fixIncorrectParentRelativePaths(pomDocument, pomPath, pomMap, context);

                if (hasIssues) {
                    context.success("Maven 4 compatibility issues fixed");
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No Maven 4 compatibility issues found");
                }
            } catch (Exception e) {
                context.failure("Failed to fix Maven 4 compatibility issues" + ": " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Fixes unsupported combine.children attribute values.
     * Maven 4 only supports 'append' and 'merge', not 'override'.
     */
    private boolean fixUnsupportedCombineChildrenAttributes(Document pomDocument, UpgradeContext context) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();

        // Find all elements with combine.children="override" and change to "merge"
        List<Element> elementsWithCombineChildren = findElementsWithAttribute(root, COMBINE_CHILDREN, COMBINE_OVERRIDE);
        for (Element element : elementsWithCombineChildren) {
            element.getAttribute(COMBINE_CHILDREN).setValue(COMBINE_MERGE);
            context.detail("Fixed: " + COMBINE_CHILDREN + "='" + COMBINE_OVERRIDE + "' → '" + COMBINE_MERGE + "' in "
                    + element.getName());
            fixed = true;
        }

        return fixed;
    }

    /**
     * Fixes unsupported combine.self attribute values.
     * Maven 4 only supports 'override', 'merge', and 'remove' (default is merge), not 'append'.
     */
    private boolean fixUnsupportedCombineSelfAttributes(Document pomDocument, UpgradeContext context) {
        boolean fixed = false;
        Element root = pomDocument.getRootElement();

        // Find all elements with combine.self="append" and change to "merge"
        List<Element> elementsWithCombineSelf = findElementsWithAttribute(root, COMBINE_SELF, COMBINE_APPEND);
        for (Element element : elementsWithCombineSelf) {
            element.getAttribute(COMBINE_SELF).setValue(COMBINE_MERGE);
            context.detail("Fixed: " + COMBINE_SELF + "='" + COMBINE_APPEND + "' → '" + COMBINE_MERGE + "' in "
                    + element.getName());
            fixed = true;
        }

        return fixed;
    }

    /**
     * Fixes duplicate dependencies in dependencies and dependencyManagement sections.
     */
    private boolean fixDuplicateDependencies(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean fixed = false;

        // Fix main dependencies
        Element dependenciesElement = root.getChild(DEPENDENCIES, namespace);
        if (dependenciesElement != null) {
            fixed |= fixDuplicateDependenciesInSection(dependenciesElement, namespace, context, DEPENDENCIES);
        }

        // Fix dependencyManagement
        Element dependencyManagementElement = root.getChild(DEPENDENCY_MANAGEMENT, namespace);
        if (dependencyManagementElement != null) {
            Element managedDependenciesElement = dependencyManagementElement.getChild(DEPENDENCIES, namespace);
            if (managedDependenciesElement != null) {
                fixed |= fixDuplicateDependenciesInSection(
                        managedDependenciesElement, namespace, context, DEPENDENCY_MANAGEMENT);
            }
        }

        // Fix profile dependencies
        Element profilesElement = root.getChild(PROFILES, namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren(PROFILE, namespace);
            for (Element profileElement : profileElements) {
                Element profileDependencies = profileElement.getChild(DEPENDENCIES, namespace);
                if (profileDependencies != null) {
                    fixed |= fixDuplicateDependenciesInSection(
                            profileDependencies, namespace, context, "profile dependencies");
                }

                Element profileDepMgmt = profileElement.getChild(DEPENDENCY_MANAGEMENT, namespace);
                if (profileDepMgmt != null) {
                    Element profileManagedDeps = profileDepMgmt.getChild(DEPENDENCIES, namespace);
                    if (profileManagedDeps != null) {
                        fixed |= fixDuplicateDependenciesInSection(
                                profileManagedDeps, namespace, context, "profile dependencyManagement");
                    }
                }
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugins in plugins and pluginManagement sections.
     */
    private boolean fixDuplicatePlugins(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean fixed = false;

        // Fix build plugins
        Element buildElement = root.getChild(BUILD, namespace);
        if (buildElement != null) {
            fixed |= fixPluginsInBuildElement(buildElement, namespace, context, BUILD);
        }

        // Fix profile plugins
        Element profilesElement = root.getChild(PROFILES, namespace);
        if (profilesElement != null) {
            for (Element profileElement : profilesElement.getChildren(PROFILE, namespace)) {
                Element profileBuildElement = profileElement.getChild(BUILD, namespace);
                if (profileBuildElement != null) {
                    fixed |= fixPluginsInBuildElement(profileBuildElement, namespace, context, "profile build");
                }
            }
        }

        return fixed;
    }

    /**
     * Fixes unsupported repository URL expressions.
     */
    private boolean fixUnsupportedRepositoryExpressions(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();
        boolean fixed = false;

        // Fix repositories
        fixed |= fixRepositoryExpressions(root.getChild(REPOSITORIES, namespace), namespace, context);

        // Fix pluginRepositories
        fixed |= fixRepositoryExpressions(root.getChild(PLUGIN_REPOSITORIES, namespace), namespace, context);

        // Fix repositories and pluginRepositories in profiles
        Element profilesElement = root.getChild(PROFILES, namespace);
        if (profilesElement != null) {
            List<Element> profileElements = profilesElement.getChildren(PROFILE, namespace);
            for (Element profileElement : profileElements) {
                fixed |= fixRepositoryExpressions(profileElement.getChild(REPOSITORIES, namespace), namespace, context);
                fixed |= fixRepositoryExpressions(
                        profileElement.getChild(PLUGIN_REPOSITORIES, namespace), namespace, context);
            }
        }

        return fixed;
    }

    /**
     * Fixes incorrect parent relative paths.
     */
    private boolean fixIncorrectParentRelativePaths(
            Document pomDocument, Path pomPath, Map<Path, Document> pomMap, UpgradeContext context) {
        Element root = pomDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Element parentElement = root.getChild(PARENT, namespace);
        if (parentElement == null) {
            return false; // No parent to fix
        }

        Element relativePathElement = parentElement.getChild(RELATIVE_PATH, namespace);
        String currentRelativePath =
                relativePathElement != null ? relativePathElement.getTextTrim() : DEFAULT_PARENT_RELATIVE_PATH;

        // Try to find the correct parent POM
        String parentGroupId = getChildText(parentElement, GROUP_ID, namespace);
        String parentArtifactId = getChildText(parentElement, ARTIFACT_ID, namespace);
        String parentVersion = getChildText(parentElement, VERSION, namespace);

        Path correctParentPath = findParentPomInMap(context, parentGroupId, parentArtifactId, parentVersion, pomMap);
        if (correctParentPath != null) {
            try {
                Path correctRelativePath = pomPath.getParent().relativize(correctParentPath);
                String correctRelativePathStr = correctRelativePath.toString().replace('\\', '/');

                if (!correctRelativePathStr.equals(currentRelativePath)) {
                    // Update relativePath element
                    if (relativePathElement == null) {
                        relativePathElement = new Element(RELATIVE_PATH, namespace);
                        Element insertAfter = parentElement.getChild(VERSION, namespace);
                        if (insertAfter == null) {
                            insertAfter = parentElement.getChild(ARTIFACT_ID, namespace);
                        }
                        if (insertAfter != null) {
                            parentElement.addContent(parentElement.indexOf(insertAfter) + 1, relativePathElement);
                        } else {
                            parentElement.addContent(relativePathElement);
                        }
                    }
                    relativePathElement.setText(correctRelativePathStr);
                    context.detail("Fixed: " + "relativePath corrected from '" + currentRelativePath + "' to '"
                            + correctRelativePathStr + "'");
                    return true;
                }
            } catch (Exception e) {
                context.failure("Failed to compute correct relativePath" + ": " + e.getMessage());
            }
        }

        return false;
    }

    /**
     * Recursively finds all elements with a specific attribute value.
     */
    private List<Element> findElementsWithAttribute(Element element, String attributeName, String attributeValue) {
        List<Element> result = new ArrayList<>();

        // Check current element
        Attribute attr = element.getAttribute(attributeName);
        if (attr != null && attributeValue.equals(attr.getValue())) {
            result.add(element);
        }

        // Recursively check children
        for (Element child : element.getChildren()) {
            result.addAll(findElementsWithAttribute(child, attributeName, attributeValue));
        }

        return result;
    }

    /**
     * Helper methods extracted from BaseUpgradeGoal for compatibility fixes.
     */
    private boolean fixDuplicateDependenciesInSection(
            Element dependenciesElement, Namespace namespace, UpgradeContext context, String sectionName) {
        boolean fixed = false;
        List<Element> dependencies = dependenciesElement.getChildren(DEPENDENCY, namespace);
        Map<String, Element> seenDependencies = new HashMap<>();
        List<Element> toRemove = new ArrayList<>();

        for (Element dependency : dependencies) {
            String groupId = getChildText(dependency, GROUP_ID, namespace);
            String artifactId = getChildText(dependency, ARTIFACT_ID, namespace);
            String type = getChildText(dependency, TYPE, namespace);
            String classifier = getChildText(dependency, CLASSIFIER, namespace);

            // Create a key for uniqueness check
            String key = groupId + ":" + artifactId + ":" + (type != null ? type : "jar") + ":"
                    + (classifier != null ? classifier : "");

            if (seenDependencies.containsKey(key)) {
                // Found duplicate - remove it
                toRemove.add(dependency);
                context.detail("Fixed: " + "Removed duplicate dependency: " + key + " in " + sectionName);
                fixed = true;
            } else {
                seenDependencies.put(key, dependency);
            }
        }

        // Remove duplicates while preserving formatting
        for (Element duplicate : toRemove) {
            removeElementWithFormatting(duplicate);
        }

        return fixed;
    }

    private boolean fixPluginsInBuildElement(
            Element buildElement, Namespace namespace, UpgradeContext context, String sectionName) {
        boolean fixed = false;

        Element pluginsElement = buildElement.getChild(PLUGINS, namespace);
        if (pluginsElement != null) {
            fixed |= fixDuplicatePluginsInSection(pluginsElement, namespace, context, sectionName + "/" + PLUGINS);
        }

        Element pluginManagementElement = buildElement.getChild(PLUGIN_MANAGEMENT, namespace);
        if (pluginManagementElement != null) {
            Element managedPluginsElement = pluginManagementElement.getChild(PLUGINS, namespace);
            if (managedPluginsElement != null) {
                fixed |= fixDuplicatePluginsInSection(
                        managedPluginsElement,
                        namespace,
                        context,
                        sectionName + "/" + PLUGIN_MANAGEMENT + "/" + PLUGINS);
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugins within a specific plugins section.
     */
    private boolean fixDuplicatePluginsInSection(
            Element pluginsElement, Namespace namespace, UpgradeContext context, String sectionName) {
        boolean fixed = false;
        List<Element> plugins = pluginsElement.getChildren(PLUGIN, namespace);
        Map<String, Element> seenPlugins = new HashMap<>();
        List<Element> toRemove = new ArrayList<>();

        for (Element plugin : plugins) {
            String groupId = getChildText(plugin, GROUP_ID, namespace);
            String artifactId = getChildText(plugin, ARTIFACT_ID, namespace);

            // Default groupId for Maven plugins
            if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
            }

            if (groupId != null && artifactId != null) {
                // Create a key for uniqueness check (groupId:artifactId)
                String key = groupId + ":" + artifactId;

                if (seenPlugins.containsKey(key)) {
                    // Found duplicate - remove it
                    toRemove.add(plugin);
                    context.detail("Fixed: " + "Removed duplicate plugin: " + key + " in " + sectionName);
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

    private boolean fixRepositoryExpressions(Element repositoriesElement, Namespace namespace, UpgradeContext context) {
        if (repositoriesElement == null) {
            return false;
        }

        boolean fixed = false;
        String elementType = repositoriesElement.getName().equals(REPOSITORIES) ? REPOSITORY : PLUGIN_REPOSITORY;
        List<Element> repositories = repositoriesElement.getChildren(elementType, namespace);

        for (Element repository : repositories) {
            Element urlElement = repository.getChild("url", namespace);
            if (urlElement != null) {
                String url = urlElement.getTextTrim();
                if (url.contains("${")
                        && !url.contains("${project.basedir}")
                        && !url.contains("${project.rootDirectory}")) {
                    String repositoryId = getChildText(repository, "id", namespace);
                    context.warning("Found unsupported expression in " + elementType + " URL (id: " + repositoryId
                            + "): " + url);
                    context.warning(
                            "Maven 4 only supports ${project.basedir} and ${project.rootDirectory} expressions in repository URLs");

                    // Comment out the problematic repository
                    Comment comment =
                            new Comment(" Repository disabled due to unsupported expression in URL: " + url + " ");
                    Element parent = repository.getParentElement();
                    parent.addContent(parent.indexOf(repository), comment);
                    removeElementWithFormatting(repository);

                    context.detail("Fixed: " + "Commented out " + elementType + " with unsupported URL expression (id: "
                            + repositoryId + ")");
                    fixed = true;
                }
            }
        }

        return fixed;
    }

    private Path findParentPomInMap(
            UpgradeContext context, String groupId, String artifactId, String version, Map<Path, Document> pomMap) {
        return pomMap.entrySet().stream()
                .filter(entry -> {
                    GAV gav = GAVUtils.extractGAVWithParentResolution(context, entry.getValue());
                    return gav != null
                            && Objects.equals(gav.groupId(), groupId)
                            && Objects.equals(gav.artifactId(), artifactId)
                            && (version == null || Objects.equals(gav.version(), version));
                })
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String getChildText(Element parent, String elementName, Namespace namespace) {
        Element element = parent.getChild(elementName, namespace);
        return element != null ? element.getTextTrim() : null;
    }

    /**
     * Removes an element while preserving formatting by also removing preceding whitespace.
     */
    private void removeElementWithFormatting(Element element) {
        Element parent = element.getParentElement();
        if (parent != null) {
            int index = parent.indexOf(element);

            // Remove the element
            parent.removeContent(element);

            // Try to remove preceding whitespace/newline
            if (index > 0) {
                Content prevContent = parent.getContent(index - 1);
                if (prevContent instanceof Text textContent) {
                    String text = textContent.getText();
                    // If it's just whitespace and newlines, remove it
                    if (text.trim().isEmpty() && text.contains("\n")) {
                        parent.removeContent(prevContent);
                    }
                }
            }
        }
    }
}
