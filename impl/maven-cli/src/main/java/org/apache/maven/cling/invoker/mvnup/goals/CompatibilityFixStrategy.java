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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.MODULES;
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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SUBPROJECTS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
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

    /**
     * Known incompatible plugins where even the latest version fails with Maven 4.
     * Maps plugin key (groupId:artifactId) to a description of the incompatibility.
     * <p>
     * Note: plugins that have a fixed version available should be added to
     * {@link PluginUpgradeStrategy} instead, so mvnup can auto-upgrade them.
     */
    private static final Map<String, String> KNOWN_INCOMPATIBLE_PLUGINS = Map.of();

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
        allDefinedProperties.addAll(collectEffectiveProperties(context, pomMap));

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
                hasIssues |= fixUnsupportedRepositoryExpressions(pomDocument, context);
                hasIssues |= fixDeprecatedPropertyExpressions(pomDocument, context);
                hasIssues |= fixIncorrectParentRelativePaths(pomDocument, pomPath, pomMap, context);
                hasIssues |= fixUndefinedPropertyExpressions(pomDocument, allDefinedProperties, context);
                hasIssues |= fixUndefinedPropertyExpressionsInRepositories(pomDocument, allDefinedProperties, context);

                // Warning-only checks: emit warnings for issues that cannot be auto-fixed
                // These do not modify the POM and do not affect hasIssues
                warnAboutIncompatiblePlugins(pomDocument, context);
                warnAboutPropertyInterpolatedModulePaths(pomDocument, context);
                warnAboutCiFriendlyMissingDependencyVersions(pomDocument, context);

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

    private Set<String> collectEffectiveProperties(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<String> properties = new HashSet<>();
        for (Path pomPath : pomMap.keySet()) {
            try {
                org.apache.maven.api.model.Model effectiveModel = buildEffectiveModel(pomPath);
                properties.addAll(effectiveModel.getProperties().keySet());
            } catch (Exception e) {
                context.debug("Failed to build effective model for " + pomPath + ": " + e.getMessage());
            }
        }
        return properties;
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

    /**
     * Fixes deprecated Maven 2/3 shorthand property expressions throughout the POM.
     * Replaces ${version}, ${groupId}, and ${artifactId} with their ${project.*} equivalents,
     * which are the only forms supported in Maven 4.
     */
    private boolean fixDeprecatedPropertyExpressions(Document pomDocument, UpgradeContext context) {
        return fixDeprecatedPropertyExpressionsInElement(pomDocument.root(), context);
    }

    private boolean fixDeprecatedPropertyExpressionsInElement(Element element, UpgradeContext context) {
        boolean fixed = false;

        List<Element> children = element.childElements().toList();

        if (children.isEmpty()) {
            String text = element.textContent();
            if (text != null && !text.isEmpty()) {
                String fixedText = replaceDeprecatedExpressions(text);
                if (!fixedText.equals(text)) {
                    element.textContent(fixedText);
                    context.detail("Fixed: replaced deprecated property expression in <" + element.name() + ">: "
                            + text.trim() + " → " + fixedText.trim());
                    fixed = true;
                }
            }
        } else {
            for (Element child : children) {
                fixed |= fixDeprecatedPropertyExpressionsInElement(child, context);
            }
        }

        return fixed;
    }

    private static String replaceDeprecatedExpressions(String text) {
        return text.replace("${version}", "${project.version}")
                .replace("${groupId}", "${project.groupId}")
                .replace("${artifactId}", "${project.artifactId}");
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

    // ---- Warning-only checks for Maven 4 compatibility issues that cannot be auto-fixed ----

    /**
     * Warns about plugins known to be incompatible with Maven 4 even at their latest version.
     * These plugins call methods on immutable API objects or use removed internal APIs
     * and require upstream fixes before they can work with Maven 4.
     *
     * @see <a href="https://github.com/apache/maven/issues/12432">#12432</a>
     */
    private void warnAboutIncompatiblePlugins(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        Stream<Element> pluginContainers = Stream.concat(
                // Root level build
                root.childElement(BUILD).stream()
                        .flatMap(build -> Stream.concat(
                                build.childElement(PLUGINS).stream(),
                                build.childElement(PLUGIN_MANAGEMENT).stream()
                                        .flatMap(pm -> pm.childElement(PLUGINS).stream()))),
                // Profile builds
                root.childElement(PROFILES).stream()
                        .flatMap(profiles -> profiles.childElements(PROFILE))
                        .flatMap(profile -> profile.childElement(BUILD).stream())
                        .flatMap(build -> Stream.concat(
                                build.childElement(PLUGINS).stream(),
                                build.childElement(PLUGIN_MANAGEMENT).stream()
                                        .flatMap(pm -> pm.childElement(PLUGINS).stream()))));

        pluginContainers.forEach(pluginsElement -> pluginsElement
                .childElements(PLUGIN)
                .forEach(pluginElement -> {
                    String groupId = pluginElement.childText(MavenPomElements.Elements.GROUP_ID);
                    String artifactId = pluginElement.childText(MavenPomElements.Elements.ARTIFACT_ID);

                    if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
                        groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
                    }

                    if (groupId != null && artifactId != null) {
                        String pluginKey = groupId + ":" + artifactId;
                        String warning = KNOWN_INCOMPATIBLE_PLUGINS.get(pluginKey);
                        if (warning != null) {
                            context.warning("Known Maven 4 incompatibility: " + pluginKey + " — " + warning);
                        }
                    }
                }));
    }

    /**
     * Warns about {@code <module>} (or {@code <subproject>}) elements that contain property
     * expressions ({@code ${...}}). Maven 4 validates module paths during POM parsing,
     * before profiles can set property values, so these paths are rejected as non-existent.
     *
     * @see <a href="https://github.com/apache/maven/issues/12434">#12434</a>
     */
    private void warnAboutPropertyInterpolatedModulePaths(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        // Check root-level modules/subprojects
        Stream.concat(
                        root.childElement(MODULES).stream().flatMap(m -> m.childElements(MODULE)),
                        root.childElement(SUBPROJECTS).stream().flatMap(s -> s.childElements(SUBPROJECT)))
                .forEach(moduleElement -> {
                    String path = moduleElement.textContentTrimmed();
                    if (path != null && EXPRESSION_PATTERN.matcher(path).find()) {
                        context.warning("Module path '" + path
                                + "' contains a property expression. Maven 4 validates module paths"
                                + " before property interpolation, which will cause a build failure"
                                + " if the expression cannot be resolved at parse time.");
                    }
                });

        // Check profile-level modules/subprojects
        root.childElement(PROFILES)
                .ifPresent(profiles -> profiles.childElements(PROFILE).forEach(profile -> {
                    Stream.concat(
                                    profile.childElement(MODULES).stream().flatMap(m -> m.childElements(MODULE)),
                                    profile.childElement(SUBPROJECTS).stream()
                                            .flatMap(s -> s.childElements(SUBPROJECT)))
                            .forEach(moduleElement -> {
                                String path = moduleElement.textContentTrimmed();
                                if (path != null
                                        && EXPRESSION_PATTERN.matcher(path).find()) {
                                    context.warning("Profile module path '" + path
                                            + "' contains a property expression. Maven 4 validates module"
                                            + " paths before profile-driven property interpolation, which"
                                            + " will cause a build failure when no profile is active.");
                                }
                            });
                }));
    }

    /**
     * Warns about CI-friendly projects using {@code ${revision}} (or {@code ${sha1}},
     * {@code ${changelist}}) where child module dependencies lack explicit {@code <version>}
     * elements and rely on {@code dependencyManagement} inherited from the parent.
     * Maven 4 validates dependency completeness before fully resolving the parent's
     * {@code dependencyManagement} chain when the parent's own version is a CI-friendly
     * expression.
     *
     * @see <a href="https://github.com/apache/maven/issues/12435">#12435</a>
     */
    private void warnAboutCiFriendlyMissingDependencyVersions(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

        // Only check if this POM has a parent with a CI-friendly version
        Element parentElement = root.childElement(PARENT).orElse(null);
        if (parentElement == null) {
            return;
        }
        String parentVersion = parentElement.childText(VERSION);
        if (parentVersion == null || !isCiFriendlyExpression(parentVersion)) {
            return;
        }

        // Check for dependencies without explicit versions
        List<String> versionlessDeps = root.childElement(DEPENDENCIES).stream()
                .flatMap(deps -> deps.childElements(DEPENDENCY))
                .filter(dep -> dep.childElement(VERSION).isEmpty())
                .map(dep -> {
                    String gid = dep.childText(MavenPomElements.Elements.GROUP_ID);
                    String aid = dep.childText(MavenPomElements.Elements.ARTIFACT_ID);
                    return (gid != null ? gid : "?") + ":" + (aid != null ? aid : "?");
                })
                .toList();

        if (!versionlessDeps.isEmpty()) {
            context.warning("This module uses CI-friendly version '" + parentVersion
                    + "' in its parent and has " + versionlessDeps.size()
                    + " dependencies without explicit <version> elements (e.g., "
                    + versionlessDeps.get(0)
                    + "). Maven 4 may fail with 'dependencies.dependency.version is missing'"
                    + " because dependency management inheritance is validated before the"
                    + " CI-friendly parent version is fully resolved.");
        }
    }

    /**
     * Checks if a version string is a CI-friendly expression.
     */
    private static boolean isCiFriendlyExpression(String version) {
        return version.contains("${revision}") || version.contains("${sha1}") || version.contains("${changelist}");
    }
}
