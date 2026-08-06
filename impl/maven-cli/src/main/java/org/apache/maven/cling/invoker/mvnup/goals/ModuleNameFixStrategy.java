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

import javax.lang.model.SourceVersion;

import java.nio.file.Path;
import java.util.HashSet;
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
 * Strategy for fixing invalid {@code Automatic-Module-Name} manifest entries in POM files.
 *
 * <p>Java module names (as validated by {@link SourceVersion#isName(CharSequence)}) must be
 * dot-separated sequences of valid Java identifiers. Dashes ({@code -}) are the most common
 * illegal character because Maven artifact IDs conventionally use them, and projects often
 * copy the artifact ID into the {@code Automatic-Module-Name} manifest entry without
 * sanitizing it.
 *
 * <p>Since {@code maven-archiver 3.3.0}
 * (<a href="https://issues.apache.org/jira/browse/MSHARED-773">MSHARED-773</a>),
 * the archiver validates {@code Automatic-Module-Name} entries and fails the build on
 * invalid names. When {@code mvnup} upgrades {@code maven-jar-plugin} (or other packaging
 * plugins) to versions that pull in the newer archiver, previously-unchecked invalid names
 * surface as build failures.
 *
 * <p>This strategy scans all packaging plugin configurations for
 * {@code <archive><manifestEntries><Automatic-Module-Name>} elements whose values
 * contain dashes, and replaces the dashes with dots to produce a valid module name.
 */
@Named
@Singleton
@Priority(12) // Run after plugin upgrades (10) but before compatibility fixes (20)
public class ModuleNameFixStrategy extends AbstractUpgradeStrategy {

    private static final String ARCHIVE = "archive";
    private static final String MANIFEST_ENTRIES = "manifestEntries";
    private static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";

    /**
     * Plugins that use {@code maven-archiver} and support
     * {@code <configuration><archive><manifestEntries>}.
     */
    private static final Set<String> PACKAGING_PLUGINS = Set.of(
            "maven-jar-plugin",
            "maven-war-plugin",
            "maven-ejb-plugin",
            "maven-ear-plugin",
            "maven-rar-plugin",
            "maven-shade-plugin");

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.plugins(), true);
    }

    @Override
    public String getDescription() {
        return "Fixing invalid Automatic-Module-Name manifest entries";
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

            context.info(pomPath + " (checking Automatic-Module-Name entries)");
            context.indent();

            try {
                boolean modified = fixModuleNames(pomDocument, context);
                if (modified) {
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No invalid Automatic-Module-Name entries found");
                }
            } catch (Exception e) {
                context.failure("Failed to fix module names: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Scans all plugin configurations in the POM for invalid {@code Automatic-Module-Name}
     * entries and fixes them.
     *
     * @return true if any entries were modified
     */
    private boolean fixModuleNames(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean modified = false;

        // Check <build><plugins> and <build><pluginManagement><plugins>
        Element build = root.childElement(BUILD).orElse(null);
        if (build != null) {
            modified |= fixInPluginSection(build.childElement(PLUGINS).orElse(null), context);

            Element pluginManagement = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
            if (pluginManagement != null) {
                modified |= fixInPluginSection(
                        pluginManagement.childElement(PLUGINS).orElse(null), context);
            }
        }

        // Check profiles
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles != null) {
            for (Element profile : profiles.childElements(PROFILE).toList()) {
                Element profileBuild = profile.childElement(BUILD).orElse(null);
                if (profileBuild != null) {
                    modified |= fixInPluginSection(
                            profileBuild.childElement(PLUGINS).orElse(null), context);

                    Element profilePluginMgmt =
                            profileBuild.childElement(PLUGIN_MANAGEMENT).orElse(null);
                    if (profilePluginMgmt != null) {
                        modified |= fixInPluginSection(
                                profilePluginMgmt.childElement(PLUGINS).orElse(null), context);
                    }
                }
            }
        }

        return modified;
    }

    private boolean fixInPluginSection(Element pluginsElement, UpgradeContext context) {
        if (pluginsElement == null) {
            return false;
        }

        boolean modified = false;
        for (Element plugin : pluginsElement.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
            String groupId = plugin.childTextTrimmed(GROUP_ID);

            if (isPackagingPlugin(groupId, artifactId)) {
                modified |= fixInPluginConfig(plugin, context, artifactId);
            }
        }
        return modified;
    }

    private boolean fixInPluginConfig(Element plugin, UpgradeContext context, String pluginArtifactId) {
        Element config = plugin.childElement(CONFIGURATION).orElse(null);
        if (config == null) {
            return false;
        }

        Element archive = config.childElement(ARCHIVE).orElse(null);
        if (archive == null) {
            return false;
        }

        Element manifestEntries = archive.childElement(MANIFEST_ENTRIES).orElse(null);
        if (manifestEntries == null) {
            return false;
        }

        Element moduleNameElement =
                manifestEntries.childElement(AUTOMATIC_MODULE_NAME).orElse(null);
        if (moduleNameElement == null) {
            return false;
        }

        String moduleName = moduleNameElement.textContent();
        if (moduleName == null || moduleName.isBlank()) {
            return false;
        }

        moduleName = moduleName.trim();
        if (SourceVersion.isName(moduleName)) {
            return false; // Already valid
        }

        String fixed = sanitizeModuleName(moduleName);
        if (fixed.equals(moduleName) || !SourceVersion.isName(fixed)) {
            // Cannot fix automatically — warn
            context.failure("Cannot automatically fix invalid Automatic-Module-Name '" + moduleName + "' in "
                    + pluginArtifactId);
            return false;
        }

        moduleNameElement.textContent(fixed);
        context.success(
                "Fixed Automatic-Module-Name in " + pluginArtifactId + ": '" + moduleName + "' → '" + fixed + "'");
        return true;
    }

    /**
     * Checks if the plugin is one of the packaging plugins that support
     * {@code <archive><manifestEntries>}.
     */
    private static boolean isPackagingPlugin(String groupId, String artifactId) {
        if (artifactId == null) {
            return false;
        }
        // Accept both explicit groupId and implicit (null/empty = default)
        if (groupId != null && !groupId.isEmpty() && !"org.apache.maven.plugins".equals(groupId)) {
            return false;
        }
        return PACKAGING_PLUGINS.contains(artifactId);
    }

    /**
     * Sanitizes a module name by replacing dashes with dots.
     *
     * <p>Java module names are dot-separated identifiers. Dashes are the most common
     * illegal character because Maven artifact IDs use them as separators. Replacing
     * dashes with dots is the standard convention (used by the JDK itself when deriving
     * automatic module names from JAR file names).
     *
     * @param moduleName the original (possibly invalid) module name
     * @return the sanitized module name
     */
    static String sanitizeModuleName(String moduleName) {
        // Replace dashes with dots (standard convention for automatic module names)
        String sanitized = moduleName.replace('-', '.');

        // Collapse consecutive dots that may result from replacements like "foo--bar"
        sanitized = sanitized.replaceAll("\\.{2,}", ".");

        // Remove leading/trailing dots
        if (sanitized.startsWith(".")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.endsWith(".")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }

        return sanitized;
    }
}
