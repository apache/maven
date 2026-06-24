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
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.CONFIGURATION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROPERTIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.SOURCE_DIRECTORY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.TEST_SOURCE_DIRECTORY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_1_0;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;

/**
 * Strategy for migrating legacy source configuration to Maven 4.1.0+ {@code <source>} elements.
 *
 * <p>Handles four migration phases:
 * <ol>
 *   <li>Compiler properties ({@code maven.compiler.release}, {@code maven.compiler.source/target})
 *       → {@code <source><targetVersion>}</li>
 *   <li>Compiler plugin configuration ({@code <release>}, {@code <source>/<target>})
 *       → {@code <source><targetVersion>}</li>
 *   <li>Custom source/test directories → {@code <source>} with {@code <directory>}</li>
 *   <li>Resource directories → {@code <source>} with {@code <lang>resources</lang>}</li>
 * </ol>
 */
@Named
@Singleton
@Priority(20)
public class SourceStrategy extends AbstractUpgradeStrategy {

    private static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    private static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";
    private static final String MAVEN_COMPILER_TARGET = "maven.compiler.target";
    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";

    private static final String DEFAULT_SOURCE_DIR = "src/main/java";
    private static final String DEFAULT_TEST_SOURCE_DIR = "src/test/java";
    private static final String DEFAULT_RESOURCE_DIR = "src/main/resources";
    private static final String DEFAULT_TEST_RESOURCE_DIR = "src/test/resources";

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.all().orElse(false)) {
            return true;
        }

        String modelVersion = options.modelVersion().orElse(null);
        return modelVersion != null && ModelVersionUtils.isVersionGreaterOrEqual(modelVersion, MODEL_VERSION_4_1_0);
    }

    @Override
    public String getDescription() {
        return "Migrating source configuration to <source> elements";
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

            String currentVersion = ModelVersionUtils.detectModelVersion(pomDocument);
            context.info(pomPath + " (current: " + currentVersion + ")");
            context.indent();

            try {
                if (!ModelVersionUtils.isVersionGreaterOrEqual(currentVersion, MODEL_VERSION_4_1_0)) {
                    context.success("Skipping (model version " + currentVersion + " < 4.1.0)");
                    continue;
                }

                boolean hasChanges = migrateSources(context, pomDocument);

                if (hasChanges) {
                    modifiedPoms.add(pomPath);
                    context.success("Source configuration migrated to <source> elements");
                } else {
                    context.success("No source configuration to migrate");
                }
            } catch (Exception e) {
                context.failure("Failed to migrate source configuration: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    private boolean migrateSources(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.root();
        boolean hasChanges = false;

        String targetVersion = extractTargetVersionFromProperties(context, root);

        if (targetVersion == null) {
            targetVersion = extractTargetVersionFromCompilerPlugin(context, root);
        } else {
            cleanupCompilerPluginConfig(context, root);
        }

        if (targetVersion != null) {
            Element sourcesElement = ensureSourcesElement(root);
            Element sourceElement = DomUtils.insertNewElement("source", sourcesElement);
            DomUtils.insertContentElement(sourceElement, "targetVersion", targetVersion);
            context.detail("Set targetVersion: " + targetVersion);
            hasChanges = true;
        }

        hasChanges |= migrateSourceDirectories(context, root);
        hasChanges |= migrateResources(context, root);

        cleanupEmptyBuild(root);

        return hasChanges;
    }

    String extractTargetVersionFromProperties(UpgradeContext context, Element root) {
        Element properties = root.childElement(PROPERTIES).orElse(null);
        if (properties == null) {
            return null;
        }

        String targetVersion = null;

        Element releaseElement = properties.childElement(MAVEN_COMPILER_RELEASE).orElse(null);
        if (releaseElement != null) {
            targetVersion = releaseElement.textContent().trim();
            DomUtils.removeElement(releaseElement);
            context.detail("Migrated property: " + MAVEN_COMPILER_RELEASE + " = " + targetVersion);

            // Also remove source/target if present (release takes precedence)
            properties.childElement(MAVEN_COMPILER_SOURCE).ifPresent(e -> {
                DomUtils.removeElement(e);
                context.detail("Removed property: " + MAVEN_COMPILER_SOURCE + " (release takes precedence)");
            });
            properties.childElement(MAVEN_COMPILER_TARGET).ifPresent(e -> {
                DomUtils.removeElement(e);
                context.detail("Removed property: " + MAVEN_COMPILER_TARGET + " (release takes precedence)");
            });
        } else {
            Element sourceElement =
                    properties.childElement(MAVEN_COMPILER_SOURCE).orElse(null);
            Element targetElement =
                    properties.childElement(MAVEN_COMPILER_TARGET).orElse(null);

            if (sourceElement != null && targetElement != null) {
                String sourceValue = sourceElement.textContent().trim();
                String targetValue = targetElement.textContent().trim();

                if (sourceValue.equals(targetValue)) {
                    targetVersion = sourceValue;
                    DomUtils.removeElement(sourceElement);
                    DomUtils.removeElement(targetElement);
                    context.detail("Migrated properties: " + MAVEN_COMPILER_SOURCE + " = " + MAVEN_COMPILER_TARGET
                            + " = " + targetVersion);
                }
            }
        }

        if (targetVersion != null) {
            removeIfEmpty(properties);
        }

        return targetVersion;
    }

    String extractTargetVersionFromCompilerPlugin(UpgradeContext context, Element root) {
        String targetVersion = extractFromPluginSection(
                context,
                root.childElement(BUILD).flatMap(b -> b.childElement(PLUGINS)).orElse(null));

        if (targetVersion == null) {
            targetVersion = extractFromPluginSection(
                    context,
                    root.childElement(BUILD)
                            .flatMap(b -> b.childElement(PLUGIN_MANAGEMENT))
                            .flatMap(pm -> pm.childElement(PLUGINS))
                            .orElse(null));
        }

        return targetVersion;
    }

    private String extractFromPluginSection(UpgradeContext context, Element pluginsElement) {
        if (pluginsElement == null) {
            return null;
        }

        Element compilerPlugin = findCompilerPlugin(pluginsElement);
        if (compilerPlugin == null) {
            return null;
        }

        Element configuration = compilerPlugin.childElement(CONFIGURATION).orElse(null);
        if (configuration == null) {
            return null;
        }

        String targetVersion = null;

        Element releaseElement = configuration.childElement("release").orElse(null);
        if (releaseElement != null) {
            targetVersion = releaseElement.textContent().trim();
            DomUtils.removeElement(releaseElement);
            context.detail("Migrated compiler plugin <release>: " + targetVersion);

            configuration.childElement("source").ifPresent(e -> {
                DomUtils.removeElement(e);
                context.detail("Removed compiler plugin <source> (release takes precedence)");
            });
            configuration.childElement("target").ifPresent(e -> {
                DomUtils.removeElement(e);
                context.detail("Removed compiler plugin <target> (release takes precedence)");
            });
        } else {
            Element sourceElement = configuration.childElement("source").orElse(null);
            Element targetElement = configuration.childElement("target").orElse(null);

            if (sourceElement != null && targetElement != null) {
                String sourceValue = sourceElement.textContent().trim();
                String targetValue = targetElement.textContent().trim();

                if (sourceValue.equals(targetValue)) {
                    targetVersion = sourceValue;
                    DomUtils.removeElement(sourceElement);
                    DomUtils.removeElement(targetElement);
                    context.detail("Migrated compiler plugin <source>/<target>: " + targetVersion);
                }
            }
        }

        if (targetVersion != null) {
            cleanupCompilerPlugin(compilerPlugin, configuration, pluginsElement);
        }

        return targetVersion;
    }

    private Element findCompilerPlugin(Element pluginsElement) {
        return pluginsElement.childElements(PLUGIN).toList().stream()
                .filter(plugin -> {
                    String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
                    String groupId = plugin.childTextTrimmed(GROUP_ID);
                    return MAVEN_COMPILER_PLUGIN.equals(artifactId)
                            && (groupId == null || groupId.isEmpty() || DEFAULT_MAVEN_PLUGIN_GROUP_ID.equals(groupId));
                })
                .findFirst()
                .orElse(null);
    }

    private void cleanupCompilerPluginConfig(UpgradeContext context, Element root) {
        cleanupPluginSectionConfig(
                context,
                root.childElement(BUILD).flatMap(b -> b.childElement(PLUGINS)).orElse(null));
        cleanupPluginSectionConfig(
                context,
                root.childElement(BUILD)
                        .flatMap(b -> b.childElement(PLUGIN_MANAGEMENT))
                        .flatMap(pm -> pm.childElement(PLUGINS))
                        .orElse(null));
    }

    private void cleanupPluginSectionConfig(UpgradeContext context, Element pluginsElement) {
        if (pluginsElement == null) {
            return;
        }
        Element compilerPlugin = findCompilerPlugin(pluginsElement);
        if (compilerPlugin == null) {
            return;
        }
        Element configuration = compilerPlugin.childElement(CONFIGURATION).orElse(null);
        if (configuration == null) {
            return;
        }
        configuration.childElement("release").ifPresent(e -> {
            DomUtils.removeElement(e);
            context.detail("Removed compiler plugin <release> (properties take precedence)");
        });
        configuration.childElement("source").ifPresent(e -> {
            DomUtils.removeElement(e);
            context.detail("Removed compiler plugin <source> (properties take precedence)");
        });
        configuration.childElement("target").ifPresent(e -> {
            DomUtils.removeElement(e);
            context.detail("Removed compiler plugin <target> (properties take precedence)");
        });
        cleanupCompilerPlugin(compilerPlugin, configuration, pluginsElement);
    }

    private void cleanupCompilerPlugin(Element compilerPlugin, Element configuration, Element pluginsElement) {
        removeIfEmpty(configuration);

        boolean hasConfig = compilerPlugin.childElement(CONFIGURATION).isPresent();
        boolean hasExecutions = compilerPlugin.childElement("executions").isPresent();
        boolean hasDeps = compilerPlugin.childElement("dependencies").isPresent();

        if (!hasConfig && !hasExecutions && !hasDeps) {
            Element pluginsParent = pluginsElement.parent() instanceof Element parent ? parent : null;
            DomUtils.removeElement(compilerPlugin);
            removeIfEmpty(pluginsElement);
            if (pluginsParent != null && PLUGIN_MANAGEMENT.equals(pluginsParent.name())) {
                removeIfEmpty(pluginsParent);
            }
        }
    }

    private static void cleanupEmptyBuild(Element root) {
        root.childElement(BUILD).ifPresent(build -> {
            if (!build.childElements().findAny().isPresent()) {
                DomUtils.removeElement(build);
            }
        });
    }

    boolean migrateSourceDirectories(UpgradeContext context, Element root) {
        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            return false;
        }

        boolean hasChanges = false;

        Element sourceDir = buildElement.childElement(SOURCE_DIRECTORY).orElse(null);
        if (sourceDir != null) {
            String dir = sourceDir.textContent().trim();
            DomUtils.removeElement(sourceDir);

            if (!DEFAULT_SOURCE_DIR.equals(dir)) {
                Element sourcesElement = ensureSourcesElement(root);
                Element mainSource = findOrCreateMainJavaSource(sourcesElement);
                DomUtils.insertContentElement(mainSource, "directory", dir);
                context.detail("Migrated sourceDirectory: " + dir);
            } else {
                context.detail("Removed default sourceDirectory");
            }
            hasChanges = true;
        }

        Element testSourceDir = buildElement.childElement(TEST_SOURCE_DIRECTORY).orElse(null);
        if (testSourceDir != null) {
            String dir = testSourceDir.textContent().trim();
            DomUtils.removeElement(testSourceDir);

            if (!DEFAULT_TEST_SOURCE_DIR.equals(dir)) {
                Element sourcesElement = ensureSourcesElement(root);
                Element testSource = DomUtils.insertNewElement("source", sourcesElement);
                DomUtils.insertContentElement(testSource, "scope", "test");
                DomUtils.insertContentElement(testSource, "directory", dir);
                context.detail("Migrated testSourceDirectory: " + dir);
            } else {
                context.detail("Removed default testSourceDirectory");
            }
            hasChanges = true;
        }

        return hasChanges;
    }

    boolean migrateResources(UpgradeContext context, Element root) {
        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            return false;
        }

        boolean hasChanges = false;

        hasChanges |= migrateResourceSection(context, root, buildElement, "resources", "resource", "main");
        hasChanges |= migrateResourceSection(context, root, buildElement, "testResources", "testResource", "test");

        return hasChanges;
    }

    private boolean migrateResourceSection(
            UpgradeContext context,
            Element root,
            Element buildElement,
            String containerName,
            String elementName,
            String scope) {
        Element container = buildElement.childElement(containerName).orElse(null);
        if (container == null) {
            return false;
        }

        List<Element> resources = container.childElements(elementName).toList();

        for (Element resource : resources) {
            if (isDefaultResource(resource, scope)) {
                continue;
            }

            Element sourcesElement = ensureSourcesElement(root);
            Element sourceElement = DomUtils.insertNewElement("source", sourcesElement);

            if ("test".equals(scope)) {
                DomUtils.insertContentElement(sourceElement, "scope", "test");
            }
            DomUtils.insertContentElement(sourceElement, "lang", "resources");

            String directory = resource.childTextTrimmed("directory");
            String defaultDir = "main".equals(scope) ? DEFAULT_RESOURCE_DIR : DEFAULT_TEST_RESOURCE_DIR;
            if (directory != null && !directory.isEmpty() && !defaultDir.equals(directory)) {
                DomUtils.insertContentElement(sourceElement, "directory", directory);
            }

            String filtering = resource.childTextTrimmed("filtering");
            if ("true".equals(filtering)) {
                DomUtils.insertContentElement(sourceElement, "stringFiltering", "true");
            }

            copyIncludesExcludes(resource, sourceElement);

            String targetPath = resource.childTextTrimmed("targetPath");
            if (targetPath != null && !targetPath.isEmpty()) {
                DomUtils.insertContentElement(sourceElement, "targetPath", targetPath);
            }

            context.detail("Migrated " + scope + " resource: " + (directory != null ? directory : defaultDir));
        }

        DomUtils.removeElement(container);
        return true;
    }

    private boolean isDefaultResource(Element resource, String scope) {
        String directory = resource.childTextTrimmed("directory");
        String defaultDir = "main".equals(scope) ? DEFAULT_RESOURCE_DIR : DEFAULT_TEST_RESOURCE_DIR;
        boolean isDefaultDir = directory == null || directory.isEmpty() || defaultDir.equals(directory);
        if (!isDefaultDir) {
            return false;
        }

        String filtering = resource.childTextTrimmed("filtering");
        if ("true".equals(filtering)) {
            return false;
        }

        String targetPath = resource.childTextTrimmed("targetPath");
        if (targetPath != null && !targetPath.isEmpty()) {
            return false;
        }

        if (resource.childElement("includes").isPresent()) {
            return false;
        }
        if (resource.childElement("excludes").isPresent()) {
            return false;
        }

        return true;
    }

    private static void copyIncludesExcludes(Element source, Element target) {
        copyPatternList(source, target, "includes", "include");
        copyPatternList(source, target, "excludes", "exclude");
    }

    private static void copyPatternList(Element source, Element target, String containerName, String elementName) {
        source.childElement(containerName).ifPresent(container -> {
            Element newContainer = DomUtils.insertNewElement(containerName, target);
            container.childElements(elementName).forEach(element -> {
                String value = element.textContent();
                if (value != null && !value.trim().isEmpty()) {
                    DomUtils.insertContentElement(newContainer, elementName, value.trim());
                }
            });
        });
    }

    private Element ensureSourcesElement(Element root) {
        Element buildElement = root.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            buildElement = DomUtils.insertNewElement(BUILD, root);
        }

        Element sourcesElement = buildElement.childElement("sources").orElse(null);
        if (sourcesElement == null) {
            sourcesElement = DomUtils.insertNewElement("sources", buildElement);
        }
        return sourcesElement;
    }

    private Element findOrCreateMainJavaSource(Element sourcesElement) {
        // Look for an existing main/java source element (e.g. one created for targetVersion)
        for (Element source : sourcesElement.childElements("source").toList()) {
            String scope = source.childTextTrimmed("scope");
            String lang = source.childTextTrimmed("lang");
            if ((scope == null || scope.isEmpty() || "main".equals(scope))
                    && (lang == null || lang.isEmpty() || "java".equals(lang))) {
                return source;
            }
        }
        return DomUtils.insertNewElement("source", sourcesElement);
    }

    private static void removeIfEmpty(Element element) {
        if (element != null && !element.childElements().findAny().isPresent()) {
            DomUtils.removeElement(element);
        }
    }
}
