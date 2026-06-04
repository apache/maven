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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Comment;
import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.Coordinates;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Attributes.COMBINE_APPEND;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Attributes.COMBINE_CHILDREN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Attributes.COMBINE_MERGE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Attributes.COMBINE_OVERRIDE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Attributes.COMBINE_SELF;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PARENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_REPOSITORIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_REPOSITORY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROPERTIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.RELATIVE_PATH;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.REPOSITORIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.REPOSITORY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Files.DEFAULT_PARENT_RELATIVE_PATH;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_PLUGIN_PREFIX;

/**
 * Strategy for applying Maven 4 compatibility fixes to POM files.
 * Fixes issues that prevent POMs from being processed by Maven 4.
 */
@Named
@Singleton
@Priority(20)
public class CompatibilityFixStrategy extends AbstractUpgradeStrategy {

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static final Set<String> VALID_COMBINE_SELF_VALUES = Set.of(COMBINE_OVERRIDE, COMBINE_MERGE, "remove");

    private static final Set<String> VALID_COMBINE_CHILDREN_VALUES = Set.of(COMBINE_APPEND, COMBINE_MERGE);

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

        Set<String> allDefinedProperties = collectAllDefinedProperties(pomMap);

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking for Maven 4 compatibility issues)");
            context.indent();

            try {
                boolean hasIssues = false;

                hasIssues |= fixUnsupportedCombineChildrenAttributes(pomDocument, context);
                hasIssues |= fixUnsupportedCombineSelfAttributes(pomDocument, context);
                hasIssues |= fixDuplicateDependencies(pomDocument, context);
                hasIssues |= fixDuplicatePlugins(pomDocument, context);
                hasIssues |= fixUnsupportedRepositoryExpressions(pomDocument, context);
                hasIssues |= fixIncorrectParentRelativePaths(pomDocument, pomPath, pomMap, context);
                hasIssues |= fixUndefinedPropertyExpressions(pomDocument, allDefinedProperties, context);
                hasIssues |= fixUndefinedPropertyExpressionsInRepositories(pomDocument, allDefinedProperties, context);

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
     * Maven 4 only supports 'append' and 'merge' (default is merge).
     * Invalid values are removed entirely since Maven 3 silently ignored them.
     */
    private boolean fixUnsupportedCombineChildrenAttributes(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        List<Element> invalidElements = findElementsWithInvalidAttribute(
                        root, COMBINE_CHILDREN, VALID_COMBINE_CHILDREN_VALUES)
                .toList();

        for (Element element : invalidElements) {
            String invalidValue = element.attribute(COMBINE_CHILDREN);
            element.removeAttribute(COMBINE_CHILDREN);
            context.detail(
                    "Fixed: removed invalid " + COMBINE_CHILDREN + "='" + invalidValue + "' from " + element.name());
        }

        return !invalidElements.isEmpty();
    }

    /**
     * Fixes unsupported combine.self attribute values.
     * Maven 4 only supports 'override', 'merge', and 'remove' (default is merge).
     * Invalid values are removed entirely since Maven 3 silently ignored them.
     */
    private boolean fixUnsupportedCombineSelfAttributes(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        List<Element> invalidElements = findElementsWithInvalidAttribute(root, COMBINE_SELF, VALID_COMBINE_SELF_VALUES)
                .toList();

        for (Element element : invalidElements) {
            String invalidValue = element.attribute(COMBINE_SELF);
            element.removeAttribute(COMBINE_SELF);
            context.detail("Fixed: removed invalid " + COMBINE_SELF + "='" + invalidValue + "' from " + element.name());
        }

        return !invalidElements.isEmpty();
    }

    /**
     * Fixes duplicate dependencies in dependencies and dependencyManagement sections.
     */
    private boolean fixDuplicateDependencies(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        // Collect all dependency containers to process
        Stream<DependencyContainer> dependencyContainers = Stream.concat(
                // Root level dependencies
                Stream.of(
                                new DependencyContainer(
                                        root.childElement(DEPENDENCIES).orElse(null), DEPENDENCIES),
                                new DependencyContainer(
                                        root.childElement(DEPENDENCY_MANAGEMENT)
                                                .flatMap(dm -> dm.childElement(DEPENDENCIES))
                                                .orElse(null),
                                        DEPENDENCY_MANAGEMENT))
                        .filter(container -> container.element != null),
                // Profile dependencies
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .flatMap(profile -> Stream.of(
                                        new DependencyContainer(
                                                profile.childElement(DEPENDENCIES)
                                                        .orElse(null),
                                                "profile dependencies"),
                                        new DependencyContainer(
                                                profile.childElement(DEPENDENCY_MANAGEMENT)
                                                        .flatMap(dm -> dm.childElement(DEPENDENCIES))
                                                        .orElse(null),
                                                "profile dependencyManagement"))
                                .filter(container -> container.element != null)));

        return dependencyContainers
                .map(container -> fixDuplicateDependenciesInSection(container.element, context, container.sectionName))
                .reduce(false, Boolean::logicalOr);
    }

    private static class DependencyContainer {
        final Element element;
        final String sectionName;

        DependencyContainer(Element element, String sectionName) {
            this.element = element;
            this.sectionName = sectionName;
        }
    }

    /**
     * Fixes duplicate plugins in plugins and pluginManagement sections.
     */
    private boolean fixDuplicatePlugins(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        // Collect all build elements to process
        Stream<BuildContainer> buildContainers = Stream.concat(
                // Root level build
                Stream.of(new BuildContainer(root.childElement(BUILD).orElse(null), BUILD))
                        .filter(container -> container.element != null),
                // Profile builds
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .map(profile ->
                                new BuildContainer(profile.childElement(BUILD).orElse(null), "profile build"))
                        .filter(container -> container.element != null));

        return buildContainers
                .map(container -> fixPluginsInBuildElement(container.element, context, container.sectionName))
                .reduce(false, Boolean::logicalOr);
    }

    private static class BuildContainer {
        final Element element;
        final String sectionName;

        BuildContainer(Element element, String sectionName) {
            this.element = element;
            this.sectionName = sectionName;
        }
    }

    /**
     * Fixes unsupported repository URL expressions.
     */
    private boolean fixUnsupportedRepositoryExpressions(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        // Collect all repository containers to process
        Stream<Element> repositoryContainers = Stream.concat(
                // Root level repositories
                Stream.of(
                                root.childElement(REPOSITORIES).orElse(null),
                                root.childElement(PLUGIN_REPOSITORIES).orElse(null))
                        .filter(Objects::nonNull),
                // Profile repositories
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .flatMap(profile -> Stream.of(
                                        profile.childElement(REPOSITORIES).orElse(null),
                                        profile.childElement(PLUGIN_REPOSITORIES)
                                                .orElse(null))
                                .filter(Objects::nonNull)));

        return repositoryContainers
                .map(container -> fixRepositoryExpressions(container, pomDocument, context))
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Fixes incorrect parent relative paths.
     */
    private boolean fixIncorrectParentRelativePaths(
            Document pomDocument, Path pomPath, Map<Path, Document> pomMap, UpgradeContext context) {
        Element root = pomDocument.root();

        Element parentElement = root.childElement(PARENT).orElse(null);
        if (parentElement == null) {
            return false; // No parent to fix
        }

        Element relativePathElement = parentElement.childElement(RELATIVE_PATH).orElse(null);
        String currentRelativePath =
                relativePathElement != null ? relativePathElement.textContent().trim() : DEFAULT_PARENT_RELATIVE_PATH;

        // Try to find the correct parent POM
        String parentGroupId = parentElement.childText(MavenPomElements.Elements.GROUP_ID);
        String parentArtifactId = parentElement.childText(MavenPomElements.Elements.ARTIFACT_ID);
        String parentVersion = parentElement.childText(MavenPomElements.Elements.VERSION);

        Path correctParentPath = findParentPomInMap(context, parentGroupId, parentArtifactId, parentVersion, pomMap);
        if (correctParentPath != null) {
            try {
                Path correctRelativePath = pomPath.getParent().relativize(correctParentPath);
                String correctRelativePathStr = correctRelativePath.toString().replace('\\', '/');

                if (!correctRelativePathStr.equals(currentRelativePath)) {
                    // Update or create relativePath element using DomUtils convenience method
                    DomUtils.updateOrCreateChildElement(parentElement, RELATIVE_PATH, correctRelativePathStr);
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

    private Set<String> collectAllDefinedProperties(Map<Path, Document> pomMap) {
        Set<String> properties = new HashSet<>();
        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            collectPropertiesFromDom(entry.getValue(), properties);
        }
        return properties;
    }

    private void collectPropertiesFromDom(Document document, Set<String> properties) {
        Element root = document.root();

        root.childElement(PROPERTIES)
                .ifPresent(propsElement -> propsElement.childElements().forEach(child -> properties.add(child.name())));

        root.childElement(PROFILES)
                .ifPresent(profiles -> profiles.childElements(PROFILE)
                        .forEach(profile -> profile.childElement(PROPERTIES)
                                .ifPresent(propsElement ->
                                        propsElement.childElements().forEach(child -> properties.add(child.name())))));
    }

    /**
     * Fixes dependencies with undefined property expressions by commenting them out.
     */
    private boolean fixUndefinedPropertyExpressions(
            Document pomDocument, Set<String> allDefinedProperties, UpgradeContext context) {
        Element root = pomDocument.root();

        Stream<DependencyContainer> dependencyContainers = Stream.concat(
                Stream.of(
                                new DependencyContainer(
                                        root.childElement(DEPENDENCIES).orElse(null), DEPENDENCIES),
                                new DependencyContainer(
                                        root.childElement(DEPENDENCY_MANAGEMENT)
                                                .flatMap(dm -> dm.childElement(DEPENDENCIES))
                                                .orElse(null),
                                        DEPENDENCY_MANAGEMENT))
                        .filter(container -> container.element != null),
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .flatMap(profile -> Stream.of(
                                        new DependencyContainer(
                                                profile.childElement(DEPENDENCIES)
                                                        .orElse(null),
                                                "profile dependencies"),
                                        new DependencyContainer(
                                                profile.childElement(DEPENDENCY_MANAGEMENT)
                                                        .flatMap(dm -> dm.childElement(DEPENDENCIES))
                                                        .orElse(null),
                                                "profile dependencyManagement"))
                                .filter(container -> container.element != null)));

        return dependencyContainers
                .map(container -> fixUndefinedPropertyExpressionsInSection(
                        container.element, allDefinedProperties, pomDocument, context, container.sectionName))
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Fixes repositories with undefined property expressions by commenting them out.
     */
    private boolean fixUndefinedPropertyExpressionsInRepositories(
            Document pomDocument, Set<String> allDefinedProperties, UpgradeContext context) {
        Element root = pomDocument.root();

        Stream<RepositoryContainer> repositoryContainers = Stream.concat(
                Stream.of(
                                new RepositoryContainer(
                                        root.childElement(REPOSITORIES).orElse(null), REPOSITORY, REPOSITORIES),
                                new RepositoryContainer(
                                        root.childElement(PLUGIN_REPOSITORIES).orElse(null),
                                        PLUGIN_REPOSITORY,
                                        PLUGIN_REPOSITORIES))
                        .filter(c -> c.element != null),
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .flatMap(profile -> Stream.of(
                                        new RepositoryContainer(
                                                profile.childElement(REPOSITORIES)
                                                        .orElse(null),
                                                REPOSITORY,
                                                "profile repositories"),
                                        new RepositoryContainer(
                                                profile.childElement(PLUGIN_REPOSITORIES)
                                                        .orElse(null),
                                                PLUGIN_REPOSITORY,
                                                "profile pluginRepositories"))
                                .filter(c -> c.element != null)));

        return repositoryContainers
                .map(c -> fixUndefinedPropertyExpressionsInRepositorySection(
                        c.element, c.elementType, allDefinedProperties, pomDocument, context, c.sectionName))
                .reduce(false, Boolean::logicalOr);
    }

    private record RepositoryContainer(Element element, String elementType, String sectionName) {}

    private boolean fixUndefinedPropertyExpressionsInRepositorySection(
            Element repositoriesElement,
            String elementType,
            Set<String> allDefinedProperties,
            Document pomDocument,
            UpgradeContext context,
            String sectionName) {
        boolean fixed = false;
        List<Element> repositories =
                repositoriesElement.childElements(elementType).toList();
        Editor editor = new Editor(pomDocument);

        for (Element repository : repositories) {
            Set<String> undefinedProps = findUndefinedPropertiesInRepository(repository, allDefinedProperties);
            if (!undefinedProps.isEmpty()) {
                String propLabel = undefinedProps.size() > 1 ? "properties" : "property";
                String propsStr = "'" + String.join("', '", undefinedProps) + "'";

                Comment comment = editor.commentOutElement(repository);
                String elementXml = comment.content().trim();
                comment.content(
                        " mvnup: commented out - undefined " + propLabel + " " + propsStr + "\n" + elementXml + " ");

                context.detail("Fixed: Commented out " + elementType + " with undefined " + propLabel + " " + propsStr
                        + " in " + sectionName);
                fixed = true;
            }
        }

        return fixed;
    }

    private Set<String> findUndefinedPropertiesInRepository(Element repository, Set<String> allDefinedProperties) {
        Set<String> undefinedProperties = new HashSet<>();

        String id = repository.childText("id");
        String url = repository.childText("url");

        collectUndefinedExpressions(id, allDefinedProperties, undefinedProperties);
        collectUndefinedExpressions(url, allDefinedProperties, undefinedProperties);

        return undefinedProperties;
    }

    /**
     * Fixes undefined property expressions in a specific dependencies section.
     */
    private boolean fixUndefinedPropertyExpressionsInSection(
            Element dependenciesElement,
            Set<String> allDefinedProperties,
            Document pomDocument,
            UpgradeContext context,
            String sectionName) {
        boolean fixed = false;
        List<Element> dependencies =
                dependenciesElement.childElements(DEPENDENCY).toList();
        Editor editor = new Editor(pomDocument);

        for (Element dependency : dependencies) {
            Set<String> undefinedProps = findUndefinedProperties(dependency, allDefinedProperties);
            if (!undefinedProps.isEmpty()) {
                String propLabel = undefinedProps.size() > 1 ? "properties" : "property";
                String propsStr = "'" + String.join("', '", undefinedProps) + "'";

                Comment comment = editor.commentOutElement(dependency);
                String elementXml = comment.content().trim();
                comment.content(
                        " mvnup: commented out - undefined " + propLabel + " " + propsStr + "\n" + elementXml + " ");

                context.detail("Fixed: Commented out dependency with undefined " + propLabel + " " + propsStr + " in "
                        + sectionName);
                fixed = true;
            }
        }

        return fixed;
    }

    /**
     * Finds undefined property expressions in a dependency's coordinate fields.
     */
    private Set<String> findUndefinedProperties(Element dependency, Set<String> allDefinedProperties) {
        Set<String> undefinedProperties = new HashSet<>();

        String groupId = dependency.childText(MavenPomElements.Elements.GROUP_ID);
        String artifactId = dependency.childText(MavenPomElements.Elements.ARTIFACT_ID);
        String version = dependency.childText(MavenPomElements.Elements.VERSION);

        collectUndefinedExpressions(groupId, allDefinedProperties, undefinedProperties);
        collectUndefinedExpressions(artifactId, allDefinedProperties, undefinedProperties);
        collectUndefinedExpressions(version, allDefinedProperties, undefinedProperties);

        return undefinedProperties;
    }

    private void collectUndefinedExpressions(String value, Set<String> allDefinedProperties, Set<String> result) {
        if (value == null) {
            return;
        }
        Matcher matcher = EXPRESSION_PATTERN.matcher(value);
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            if (!isWellKnownProperty(propertyName) && !allDefinedProperties.contains(propertyName)) {
                result.add(propertyName);
            }
        }
    }

    private static boolean isWellKnownProperty(String propertyName) {
        if (propertyName.startsWith("project.")
                || propertyName.startsWith("pom.")
                || propertyName.startsWith("env.")
                || propertyName.startsWith("settings.")
                || propertyName.startsWith("maven.")) {
            return true;
        }
        if (propertyName.startsWith("java.")
                || propertyName.startsWith("os.")
                || propertyName.startsWith("user.")
                || propertyName.startsWith("file.")
                || propertyName.startsWith("line.")
                || propertyName.startsWith("path.")
                || propertyName.startsWith("sun.")) {
            return true;
        }
        return "basedir".equals(propertyName)
                || "revision".equals(propertyName)
                || "sha1".equals(propertyName)
                || "changelist".equals(propertyName);
    }

    /**
     * Recursively finds all elements with a specific attribute value.
     */
    private Stream<Element> findElementsWithAttribute(Element element, String attributeName, String attributeValue) {
        return Stream.concat(
                // Check current element
                Stream.of(element).filter(e -> {
                    String attr = e.attribute(attributeName);
                    return attr != null && attributeValue.equals(attr);
                }),
                // Recursively check children
                element.childElements()
                        .flatMap(child -> findElementsWithAttribute(child, attributeName, attributeValue)));
    }

    /**
     * Recursively finds all elements with an attribute whose value is not in the set of valid values.
     */
    private Stream<Element> findElementsWithInvalidAttribute(
            Element element, String attributeName, Set<String> validValues) {
        return Stream.concat(
                Stream.of(element).filter(e -> {
                    String attr = e.attribute(attributeName);
                    return attr != null && !validValues.contains(attr);
                }),
                element.childElements()
                        .flatMap(child -> findElementsWithInvalidAttribute(child, attributeName, validValues)));
    }

    /**
     * Helper methods extracted from BaseUpgradeGoal for compatibility fixes.
     */
    private boolean fixDuplicateDependenciesInSection(
            Element dependenciesElement, UpgradeContext context, String sectionName) {
        List<Element> dependencies =
                dependenciesElement.childElements(DEPENDENCY).toList();
        Map<String, Element> seenDependencies = new HashMap<>();

        List<Element> duplicates = dependencies.stream()
                .filter(dependency -> {
                    String key = createDependencyKey(dependency);
                    if (seenDependencies.containsKey(key)) {
                        context.detail("Fixed: Removed duplicate dependency: " + key + " in " + sectionName);
                        return true; // This is a duplicate
                    } else {
                        seenDependencies.put(key, dependency);
                        return false; // This is the first occurrence
                    }
                })
                .toList();

        // Remove duplicates while preserving formatting
        duplicates.forEach(DomUtils::removeElement);

        return !duplicates.isEmpty();
    }

    private String createDependencyKey(Element dependency) {
        String groupId = dependency.childText(MavenPomElements.Elements.GROUP_ID);
        String artifactId = dependency.childText(MavenPomElements.Elements.ARTIFACT_ID);
        String type = dependency.childText(MavenPomElements.Elements.TYPE);
        String classifier = dependency.childText(MavenPomElements.Elements.CLASSIFIER);

        return groupId + ":" + artifactId + ":" + (type != null ? type : "jar") + ":"
                + (classifier != null ? classifier : "");
    }

    private boolean fixPluginsInBuildElement(Element buildElement, UpgradeContext context, String sectionName) {
        boolean fixed = false;

        Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
        if (pluginsElement != null) {
            fixed |= fixDuplicatePluginsInSection(pluginsElement, context, sectionName + "/" + PLUGINS);
        }

        Element pluginManagementElement =
                buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagementElement != null) {
            Element managedPluginsElement =
                    pluginManagementElement.childElement(PLUGINS).orElse(null);
            if (managedPluginsElement != null) {
                fixed |= fixDuplicatePluginsInSection(
                        managedPluginsElement, context, sectionName + "/" + PLUGIN_MANAGEMENT + "/" + PLUGINS);
            }
        }

        return fixed;
    }

    /**
     * Fixes duplicate plugins within a specific plugins section.
     */
    private boolean fixDuplicatePluginsInSection(Element pluginsElement, UpgradeContext context, String sectionName) {
        List<Element> plugins = pluginsElement.childElements(PLUGIN).toList();
        Map<String, Element> seenPlugins = new HashMap<>();

        List<Element> duplicates = plugins.stream()
                .filter(plugin -> {
                    String key = createPluginKey(plugin);
                    if (key != null) {
                        if (seenPlugins.containsKey(key)) {
                            context.detail("Fixed: Removed duplicate plugin: " + key + " in " + sectionName);
                            return true; // This is a duplicate
                        } else {
                            seenPlugins.put(key, plugin);
                        }
                    }
                    return false; // This is the first occurrence or invalid plugin
                })
                .toList();

        // Remove duplicates while preserving formatting
        duplicates.forEach(DomUtils::removeElement);

        return !duplicates.isEmpty();
    }

    private String createPluginKey(Element plugin) {
        String groupId = plugin.childText(MavenPomElements.Elements.GROUP_ID);
        String artifactId = plugin.childText(MavenPomElements.Elements.ARTIFACT_ID);

        // Default groupId for Maven plugins
        if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
            groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
        }

        return (groupId != null && artifactId != null) ? groupId + ":" + artifactId : null;
    }

    private boolean fixRepositoryExpressions(
            Element repositoriesElement, Document pomDocument, UpgradeContext context) {
        if (repositoriesElement == null) {
            return false;
        }

        boolean fixed = false;
        String elementType = repositoriesElement.name().equals(REPOSITORIES) ? REPOSITORY : PLUGIN_REPOSITORY;
        List<Element> repositories =
                repositoriesElement.childElements(elementType).toList();

        for (Element repository : repositories) {
            Element urlElement = repository.childElement("url").orElse(null);
            if (urlElement != null) {
                String url = urlElement.textContent().trim();
                String fixedUrl =
                        url.replace("${basedir}", "${project.basedir}").replace("${pom.basedir}", "${project.basedir}");
                if (!fixedUrl.equals(url)) {
                    urlElement.textContent(fixedUrl);
                    String repositoryId = repository.childText("id");
                    context.detail("Fixed: replaced deprecated expression in " + elementType + " URL (id: "
                            + repositoryId + "): " + url + " → " + fixedUrl);
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
                    Coordinates gav = AbstractUpgradeStrategy.extractArtifactCoordinatesWithParentResolution(
                            context, entry.getValue());
                    return gav != null
                            && Objects.equals(gav.groupId(), groupId)
                            && Objects.equals(gav.artifactId(), artifactId)
                            && (version == null || Objects.equals(gav.version(), version));
                })
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
