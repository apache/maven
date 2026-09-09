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
import java.util.LinkedHashSet;
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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;

/**
 * Strategy for adding binary file extension exclusions to the maven-resources-plugin
 * when resource filtering is enabled.
 *
 * <p>Maven 4 strictly decodes filtered resources as UTF-8, which causes
 * {@code MalformedInputException} when binary files (e.g., {@code .xlsx}, {@code .docx})
 * are present in a resource directory with {@code <filtering>true</filtering>}.
 * Maven 3 was more lenient and silently handled these files.
 *
 * <p>This strategy scans all {@code <resource>} and {@code <testResource>} blocks.
 * If any resource directory has filtering enabled and the {@code maven-resources-plugin}
 * does not already declare {@code <nonFilteredFileExtensions>}, this strategy adds a
 * list of common binary file extensions to prevent the exception.
 *
 * @see <a href="https://github.com/apache/maven/issues/12455">#12455</a>
 */
@Named
@Singleton
@Priority(17)
public class ResourceFilteringStrategy extends AbstractUpgradeStrategy {

    private static final String RESOURCES = "resources";
    private static final String TEST_RESOURCES = "testResources";
    private static final String RESOURCE = "resource";
    private static final String TEST_RESOURCE = "testResource";
    private static final String FILTERING = "filtering";
    private static final String NON_FILTERED_FILE_EXTENSIONS = "nonFilteredFileExtensions";
    private static final String NON_FILTERED_FILE_EXTENSION = "nonFilteredFileExtension";
    private static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";
    private static final String MAVEN_PLUGINS_GROUP_ID = "org.apache.maven.plugins";

    /**
     * Common binary file extensions that should not be filtered.
     * These extensions cover binary formats frequently found in Maven projects that would
     * cause {@code MalformedInputException} when processed as UTF-8 text.
     */
    static final List<String> BINARY_EXTENSIONS = List.of(
            // Document formats
            "xlsx",
            "xls",
            "docx",
            "doc",
            "pptx",
            "pdf",
            // Archive and package formats
            "zip",
            "tar",
            "gz",
            "jar",
            // Compiled and native binaries
            "class",
            "so",
            "dll",
            "exe",
            // Image formats
            "png",
            "jpg",
            "jpeg",
            "gif",
            "bmp",
            "tiff",
            "ico",
            // Font formats
            "woff",
            "woff2",
            "ttf",
            "eot",
            // Media formats
            "mp3",
            "mp4",
            // Other binary formats
            "swf",
            "ser",
            "keystore",
            "jks",
            "p12",
            "pfx");

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Adding binary file extension exclusions to resource filtering";
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

            context.info(pomPath + " (checking for resource filtering of binary files)");
            context.indent();

            try {
                boolean modified = fixResourceFiltering(pomDocument, context);

                if (modified) {
                    modifiedPoms.add(pomPath);
                    context.success("Added nonFilteredFileExtensions to maven-resources-plugin");
                } else {
                    context.success("No resource filtering issues found");
                }
            } catch (Exception e) {
                context.failure("Failed to fix resource filtering: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Checks if any resource or testResource block has filtering enabled,
     * and if so, ensures the maven-resources-plugin has nonFilteredFileExtensions configured.
     */
    private boolean fixResourceFiltering(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean modified = false;

        // Check root-level build
        modified |= fixResourceFilteringInBuild(root, pomDocument, context);

        // Check profile-level builds
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles != null) {
            for (Element profile : profiles.childElements(PROFILE).toList()) {
                modified |= fixResourceFilteringInBuild(profile, pomDocument, context);
            }
        }

        return modified;
    }

    /**
     * Checks resources/testResources within a build element (root or profile)
     * and adds nonFilteredFileExtensions if filtering is enabled.
     */
    private boolean fixResourceFilteringInBuild(Element parent, Document pomDocument, UpgradeContext context) {
        Element build = parent.childElement(BUILD).orElse(null);
        if (build == null) {
            return false;
        }

        boolean hasFilteredResources = hasFilteringEnabled(build, RESOURCES, RESOURCE);
        boolean hasFilteredTestResources = hasFilteringEnabled(build, TEST_RESOURCES, TEST_RESOURCE);

        if (!hasFilteredResources && !hasFilteredTestResources) {
            return false;
        }

        // Check if maven-resources-plugin already has nonFilteredFileExtensions configured
        Set<String> existingExtensions = findExistingNonFilteredExtensions(parent);

        if (existingExtensions == null) {
            // No configuration exists at all — add the full list
            addNonFilteredExtensions(parent, pomDocument, BINARY_EXTENSIONS, context);
            return true;
        }

        // Configuration exists — check if any extensions are missing
        Set<String> missingExtensions = new LinkedHashSet<>();
        for (String ext : BINARY_EXTENSIONS) {
            if (!existingExtensions.contains(ext)) {
                missingExtensions.add(ext);
            }
        }

        if (!missingExtensions.isEmpty()) {
            mergeNonFilteredExtensions(parent, missingExtensions, context);
            return true;
        }

        return false;
    }

    /**
     * Checks if any resource/testResource in a build element has filtering enabled.
     */
    boolean hasFilteringEnabled(Element build, String containerName, String elementName) {
        Element container = build.childElement(containerName).orElse(null);
        if (container == null) {
            return false;
        }

        for (Element resource : container.childElements(elementName).toList()) {
            Element filteringElement = resource.childElement(FILTERING).orElse(null);
            if (filteringElement != null && "true".equals(filteringElement.textContentTrimmed())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Finds existing nonFilteredFileExtensions from the maven-resources-plugin configuration.
     * Searches both pluginManagement and direct plugins sections.
     *
     * @return the set of existing extensions, or null if no configuration exists
     */
    Set<String> findExistingNonFilteredExtensions(Element parent) {
        Element build = parent.childElement(BUILD).orElse(null);
        if (build == null) {
            return null;
        }

        // Check pluginManagement first, then direct plugins
        Element pluginElement = findResourcesPlugin(build, true);
        if (pluginElement == null) {
            pluginElement = findResourcesPlugin(build, false);
        }

        if (pluginElement == null) {
            return null;
        }

        Element configuration = pluginElement.childElement(CONFIGURATION).orElse(null);
        if (configuration == null) {
            return null;
        }

        Element extensions =
                configuration.childElement(NON_FILTERED_FILE_EXTENSIONS).orElse(null);
        if (extensions == null) {
            return null;
        }

        Set<String> result = new HashSet<>();
        for (Element ext : extensions.childElements(NON_FILTERED_FILE_EXTENSION).toList()) {
            String value = ext.textContentTrimmed();
            if (value != null && !value.isEmpty()) {
                result.add(value);
            }
        }

        return result;
    }

    /**
     * Finds the maven-resources-plugin element in the build section.
     *
     * @param build the build element
     * @param inPluginManagement true to look in pluginManagement, false for direct plugins
     * @return the plugin element, or null if not found
     */
    private Element findResourcesPlugin(Element build, boolean inPluginManagement) {
        Element pluginsContainer;

        if (inPluginManagement) {
            Element pluginMgmt = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
            if (pluginMgmt == null) {
                return null;
            }
            pluginsContainer = pluginMgmt.childElement(PLUGINS).orElse(null);
        } else {
            pluginsContainer = build.childElement(PLUGINS).orElse(null);
        }

        if (pluginsContainer == null) {
            return null;
        }

        for (Element plugin : pluginsContainer.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childText(ARTIFACT_ID);
            if (MAVEN_RESOURCES_PLUGIN.equals(artifactId)) {
                String groupId = plugin.childText(GROUP_ID);
                if (groupId == null || MAVEN_PLUGINS_GROUP_ID.equals(groupId)) {
                    return plugin;
                }
            }
        }

        return null;
    }

    /**
     * Adds nonFilteredFileExtensions configuration to the maven-resources-plugin
     * in pluginManagement. Creates the pluginManagement/plugins/plugin structure
     * if it does not exist.
     */
    private void addNonFilteredExtensions(
            Element parent, Document pomDocument, List<String> extensions, UpgradeContext context) {
        Element build = parent.childElement(BUILD).orElse(null);
        if (build == null) {
            build = DomUtils.insertNewElement(BUILD, parent);
        }

        // Prefer adding to pluginManagement so child modules inherit the config
        Element pluginManagement = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement == null) {
            pluginManagement = DomUtils.insertNewElement(PLUGIN_MANAGEMENT, build);
        }

        Element plugins = pluginManagement.childElement(PLUGINS).orElse(null);
        if (plugins == null) {
            plugins = DomUtils.insertNewElement(PLUGINS, pluginManagement);
        }

        // Check if maven-resources-plugin already exists in pluginManagement
        Element pluginElement = null;
        for (Element plugin : plugins.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childText(ARTIFACT_ID);
            if (MAVEN_RESOURCES_PLUGIN.equals(artifactId)) {
                String groupId = plugin.childText(GROUP_ID);
                if (groupId == null || MAVEN_PLUGINS_GROUP_ID.equals(groupId)) {
                    pluginElement = plugin;
                    break;
                }
            }
        }

        if (pluginElement == null) {
            pluginElement = DomUtils.insertNewElement(PLUGIN, plugins);
            DomUtils.insertContentElement(pluginElement, ARTIFACT_ID, MAVEN_RESOURCES_PLUGIN);
        }

        Element configuration = pluginElement.childElement(CONFIGURATION).orElse(null);
        if (configuration == null) {
            configuration = DomUtils.insertNewElement(CONFIGURATION, pluginElement);
        }

        Element extensionsElement = DomUtils.insertNewElement(NON_FILTERED_FILE_EXTENSIONS, configuration);
        for (String ext : extensions) {
            DomUtils.insertContentElement(extensionsElement, NON_FILTERED_FILE_EXTENSION, ext);
        }

        context.detail("Added " + NON_FILTERED_FILE_EXTENSIONS
                + " to maven-resources-plugin to prevent MalformedInputException on binary files with Maven 4");
    }

    /**
     * Merges missing extensions into the existing nonFilteredFileExtensions configuration.
     */
    private void mergeNonFilteredExtensions(Element parent, Set<String> missingExtensions, UpgradeContext context) {
        Element build = parent.childElement(BUILD).orElse(null);
        if (build == null) {
            return;
        }

        // Find the existing configuration
        Element pluginElement = findResourcesPlugin(build, true);
        if (pluginElement == null) {
            pluginElement = findResourcesPlugin(build, false);
        }

        if (pluginElement == null) {
            return;
        }

        Element configuration = pluginElement.childElement(CONFIGURATION).orElse(null);
        if (configuration == null) {
            return;
        }

        Element extensionsElement =
                configuration.childElement(NON_FILTERED_FILE_EXTENSIONS).orElse(null);
        if (extensionsElement == null) {
            return;
        }

        for (String ext : missingExtensions) {
            DomUtils.insertContentElement(extensionsElement, NON_FILTERED_FILE_EXTENSION, ext);
        }

        context.detail("Merged " + missingExtensions.size()
                + " missing binary file extensions into existing " + NON_FILTERED_FILE_EXTENSIONS
                + " configuration to prevent MalformedInputException with Maven 4");
    }
}
