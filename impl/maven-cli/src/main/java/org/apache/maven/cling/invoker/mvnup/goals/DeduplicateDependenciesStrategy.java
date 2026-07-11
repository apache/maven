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
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import eu.maveniverse.domtrip.maven.MavenPomElements;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.MAVEN_PLUGIN_PREFIX;

/**
 * Strategy for deduplicating dependency and plugin declarations in POM files.
 *
 * <p>Maven 4 rejects duplicate {@code <dependency>} entries (same groupId, artifactId,
 * type, and classifier) that Maven 3 silently accepted. This strategy scans each POM's
 * {@code <dependencies>} and {@code <dependencyManagement>/<dependencies>} sections and
 * removes duplicates using last-wins semantics (keeping the last declaration). Note that
 * Maven 3's dependency resolver uses first-wins for same-depth conflicts; last-wins is
 * chosen here intentionally so the most recently added (and presumably most up-to-date)
 * declaration is preserved.
 *
 * <p>The strategy also handles duplicate {@code <plugin>} entries in {@code <plugins>}
 * and {@code <pluginManagement>/<plugins>} sections with the same last-wins semantics
 * (which does match Maven 3's plugin configuration merging behavior).
 *
 * <p>Profile-scoped dependency and plugin sections are processed as well.
 */
@Named
@Singleton
@Priority(19)
public class DeduplicateDependenciesStrategy extends AbstractUpgradeStrategy {

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Deduplicating dependency and plugin declarations";
    }

    @Override
    protected UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking for duplicate declarations)");
            context.indent();

            try {
                boolean hasIssues = false;

                hasIssues |= fixDuplicateDependencies(pomDocument, context);
                hasIssues |= fixDuplicatePlugins(pomDocument, context);

                if (hasIssues) {
                    context.success("Duplicate declarations removed");
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No duplicate declarations found");
                }
            } catch (Exception e) {
                context.failure("Failed to deduplicate declarations: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Fixes duplicate dependencies in dependencies and dependencyManagement sections,
     * including profile-scoped sections.
     */
    private boolean fixDuplicateDependencies(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

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

    /**
     * Fixes duplicate plugins in plugins and pluginManagement sections,
     * including profile-scoped sections.
     */
    private boolean fixDuplicatePlugins(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();

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

    /**
     * Deduplicates dependencies within a specific dependencies section using last-wins semantics.
     * When a duplicate is found, the earlier occurrence is removed and the later one is kept,
     * matching Maven 3's runtime behavior.
     */
    private boolean fixDuplicateDependenciesInSection(
            Element dependenciesElement, UpgradeContext context, String sectionName) {
        List<Element> dependencies =
                dependenciesElement.childElements(DEPENDENCY).toList();
        Map<String, Element> seenDependencies = new HashMap<>();
        List<Element> duplicates = new ArrayList<>();

        for (Element dependency : dependencies) {
            String key = createDependencyKey(dependency);
            Element previous = seenDependencies.put(key, dependency);
            if (previous != null) {
                duplicates.add(previous);
                String keptVersion = dependency.childText(MavenPomElements.Elements.VERSION);
                String versionInfo = keptVersion != null ? " (keeping version " + keptVersion + ")" : "";
                context.detail(
                        "Removed duplicate dependency declaration for " + key + versionInfo + " in " + sectionName);
            }
        }

        duplicates.forEach(DomUtils::removeElement);

        return !duplicates.isEmpty();
    }

    /**
     * Creates a unique key for a dependency based on groupId, artifactId, type, and classifier.
     * Type defaults to "jar" and classifier defaults to empty when not specified.
     */
    private String createDependencyKey(Element dependency) {
        String groupId = dependency.childText(MavenPomElements.Elements.GROUP_ID);
        String artifactId = dependency.childText(MavenPomElements.Elements.ARTIFACT_ID);
        String type = dependency.childText(MavenPomElements.Elements.TYPE);
        String classifier = dependency.childText(MavenPomElements.Elements.CLASSIFIER);

        return groupId + ":" + artifactId + ":" + (type != null ? type : "jar") + ":"
                + (classifier != null ? classifier : "");
    }

    /**
     * Fixes duplicate plugins within a build element, covering both plugins and pluginManagement.
     */
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
     * Deduplicates plugins within a specific plugins section using last-wins semantics.
     */
    private boolean fixDuplicatePluginsInSection(Element pluginsElement, UpgradeContext context, String sectionName) {
        List<Element> plugins = pluginsElement.childElements(PLUGIN).toList();
        Map<String, Element> seenPlugins = new HashMap<>();
        List<Element> duplicates = new ArrayList<>();

        for (Element plugin : plugins) {
            String key = createPluginKey(plugin);
            if (key != null) {
                Element previous = seenPlugins.put(key, plugin);
                if (previous != null) {
                    duplicates.add(previous);
                    String keptVersion = plugin.childText(MavenPomElements.Elements.VERSION);
                    String versionInfo = keptVersion != null ? " (keeping version " + keptVersion + ")" : "";
                    context.detail(
                            "Removed duplicate plugin declaration for " + key + versionInfo + " in " + sectionName);
                }
            }
        }

        duplicates.forEach(DomUtils::removeElement);

        return !duplicates.isEmpty();
    }

    /**
     * Creates a unique key for a plugin based on groupId and artifactId.
     * Defaults groupId to org.apache.maven.plugins for Maven plugins.
     */
    private String createPluginKey(Element plugin) {
        String groupId = plugin.childText(MavenPomElements.Elements.GROUP_ID);
        String artifactId = plugin.childText(MavenPomElements.Elements.ARTIFACT_ID);

        if (groupId == null && artifactId != null && artifactId.startsWith(MAVEN_PLUGIN_PREFIX)) {
            groupId = DEFAULT_MAVEN_PLUGIN_GROUP_ID;
        }

        return (groupId != null && artifactId != null) ? groupId + ":" + artifactId : null;
    }

    private record DependencyContainer(Element element, String sectionName) {}

    private record BuildContainer(Element element, String sectionName) {}
}
