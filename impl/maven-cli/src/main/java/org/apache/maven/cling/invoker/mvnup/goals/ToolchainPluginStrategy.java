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
import java.util.Map;
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;
import org.apache.maven.impl.JdkSourceLevelSupport;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.CONFIGURATION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROPERTIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;

/**
 * Strategy for adding the {@code maven-toolchains-plugin} with the {@code select-jdk-toolchain}
 * goal when the project's required {@code --source}/{@code --release} level is no longer supported
 * by the running JDK.
 *
 * <p>This strategy detects the project's source level from:
 * <ol>
 *   <li>{@code maven.compiler.release} property</li>
 *   <li>{@code maven.compiler.source} property</li>
 *   <li>Compiler plugin {@code <configuration><release>} or {@code <source>}</li>
 * </ol>
 *
 * <p>If the running JDK does not support the detected source level (per JEP 182 retirement
 * schedule), and the {@code maven-toolchains-plugin} is not already configured with the
 * {@code select-jdk-toolchain} goal, this strategy adds it so that the plugin's built-in
 * JDK discovery mechanism can find a compatible JDK at build time.
 *
 * @see <a href="https://openjdk.org/jeps/182">JEP 182: Policy for Retiring javac -source and -target Options</a>
 */
@Named
@Singleton
@Priority(15)
public class ToolchainPluginStrategy extends AbstractUpgradeStrategy {

    static final String MAVEN_TOOLCHAINS_PLUGIN = "maven-toolchains-plugin";
    static final String TOOLCHAINS_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    static final String SELECT_JDK_TOOLCHAIN_GOAL = "select-jdk-toolchain";

    /**
     * Minimum toolchains-plugin version that contains the {@code select-jdk-toolchain} goal.
     * Older versions (e.g. 1.1) only have {@code help} and {@code toolchain} goals, so adding
     * a {@code select-jdk-toolchain} execution without upgrading would break the build.
     */
    static final String TOOLCHAINS_PLUGIN_MIN_VERSION = "3.2.0";

    private static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    private static final String MAVEN_COMPILER_SOURCE = "maven.compiler.source";
    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.all().orElse(false)) {
            return true;
        }

        // Default behavior: run when no specific options are provided,
        // or when all options are explicitly disabled (safety net — toolchain
        // setup should always run as it prevents build failures)
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

        // Run when --model is explicitly set
        if (options.model().isPresent()) {
            return options.model().get();
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Adding maven-toolchains-plugin for JDK source level compatibility";
    }

    @Override
    protected UpgradeResult doApply(UpgradeContext context, Map<Path, Document> pomMap) {
        Set<Path> processedPoms = new HashSet<>();
        Set<Path> modifiedPoms = new HashSet<>();
        Set<Path> errorPoms = new HashSet<>();

        int runningJdkMajor = getRunningJdkMajor();

        for (Map.Entry<Path, Document> entry : pomMap.entrySet()) {
            Path pomPath = entry.getKey();
            Document pomDocument = entry.getValue();
            processedPoms.add(pomPath);

            context.info(pomPath + " (checking JDK source level compatibility)");
            context.indent();

            try {
                int sourceLevel = detectSourceLevel(pomDocument);
                if (sourceLevel <= 0) {
                    context.success("No source level configured");
                    continue;
                }

                if (JdkSourceLevelSupport.supportsSourceLevel(runningJdkMajor, sourceLevel)) {
                    context.success("Running JDK " + runningJdkMajor + " supports --source " + sourceLevel);
                    continue;
                }

                if (hasToolchainsPluginWithSelectGoal(pomDocument)) {
                    context.success(
                            "maven-toolchains-plugin with " + SELECT_JDK_TOOLCHAIN_GOAL + " goal already configured");
                    continue;
                }

                int latestJdk = JdkSourceLevelSupport.latestJdkForSourceLevel(sourceLevel);
                addToolchainsPlugin(pomDocument, latestJdk);
                modifiedPoms.add(pomPath);
                context.success("Added maven-toolchains-plugin with " + SELECT_JDK_TOOLCHAIN_GOAL + " goal (--source "
                        + sourceLevel + " requires JDK <= " + latestJdk + ")");
            } catch (Exception e) {
                context.failure("Failed to add toolchains plugin: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Detects the project's required source level from properties or compiler plugin configuration.
     *
     * @return the source level as a major version, or {@code -1} if none is configured
     */
    int detectSourceLevel(Document pomDocument) {
        Element root = pomDocument.root();

        // Check properties: maven.compiler.release takes precedence
        Element properties = root.childElement(PROPERTIES).orElse(null);
        if (properties != null) {
            Element releaseElement =
                    properties.childElement(MAVEN_COMPILER_RELEASE).orElse(null);
            if (releaseElement != null) {
                int level = JdkSourceLevelSupport.normalizeSourceLevel(
                        releaseElement.textContent().trim());
                if (level > 0) {
                    return level;
                }
            }

            Element sourceElement =
                    properties.childElement(MAVEN_COMPILER_SOURCE).orElse(null);
            if (sourceElement != null) {
                int level = JdkSourceLevelSupport.normalizeSourceLevel(
                        sourceElement.textContent().trim());
                if (level > 0) {
                    return level;
                }
            }
        }

        // Check compiler plugin configuration
        return detectSourceLevelFromCompilerPlugin(root);
    }

    private int detectSourceLevelFromCompilerPlugin(Element root) {
        Element build = root.childElement(BUILD).orElse(null);
        if (build == null) {
            return -1;
        }

        // Check <build><plugins>
        int level = detectFromPluginSection(build.childElement(PLUGINS).orElse(null));
        if (level > 0) {
            return level;
        }

        // Check <build><pluginManagement><plugins>
        Element pluginManagement = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement != null) {
            level = detectFromPluginSection(
                    pluginManagement.childElement(PLUGINS).orElse(null));
        }

        return level;
    }

    private int detectFromPluginSection(Element pluginsElement) {
        if (pluginsElement == null) {
            return -1;
        }

        for (Element plugin : pluginsElement.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
            String groupId = plugin.childTextTrimmed(GROUP_ID);
            if (MAVEN_COMPILER_PLUGIN.equals(artifactId)
                    && (groupId == null || groupId.isEmpty() || TOOLCHAINS_PLUGIN_GROUP_ID.equals(groupId))) {
                Element config = plugin.childElement(CONFIGURATION).orElse(null);
                if (config != null) {
                    // <release> takes precedence
                    Element releaseNode = config.childElement("release").orElse(null);
                    if (releaseNode != null
                            && releaseNode.textContent() != null
                            && !releaseNode.textContent().isBlank()) {
                        int level = JdkSourceLevelSupport.normalizeSourceLevel(
                                releaseNode.textContent().trim());
                        if (level > 0) {
                            return level;
                        }
                    }
                    Element sourceNode = config.childElement("source").orElse(null);
                    if (sourceNode != null
                            && sourceNode.textContent() != null
                            && !sourceNode.textContent().isBlank()) {
                        int level = JdkSourceLevelSupport.normalizeSourceLevel(
                                sourceNode.textContent().trim());
                        if (level > 0) {
                            return level;
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Checks whether the POM already has the {@code maven-toolchains-plugin}
     * configured with the {@code select-jdk-toolchain} goal.
     */
    boolean hasToolchainsPluginWithSelectGoal(Document pomDocument) {
        Element root = pomDocument.root();
        Element build = root.childElement(BUILD).orElse(null);
        if (build == null) {
            return false;
        }

        // Check both <plugins> and <pluginManagement><plugins>
        if (hasSelectGoalInPluginSection(build.childElement(PLUGINS).orElse(null))) {
            return true;
        }

        Element pluginManagement = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement != null) {
            return hasSelectGoalInPluginSection(
                    pluginManagement.childElement(PLUGINS).orElse(null));
        }

        return false;
    }

    private boolean hasSelectGoalInPluginSection(Element pluginsElement) {
        if (pluginsElement == null) {
            return false;
        }

        for (Element plugin : pluginsElement.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
            String groupId = plugin.childTextTrimmed(GROUP_ID);
            if (MAVEN_TOOLCHAINS_PLUGIN.equals(artifactId)
                    && (groupId == null || groupId.isEmpty() || TOOLCHAINS_PLUGIN_GROUP_ID.equals(groupId))) {
                // Check if it has the select-jdk-toolchain goal in any execution
                Element executions = plugin.childElement("executions").orElse(null);
                if (executions != null) {
                    for (Element execution :
                            executions.childElements("execution").toList()) {
                        Element goals = execution.childElement("goals").orElse(null);
                        if (goals != null) {
                            for (Element goal : goals.childElements("goal").toList()) {
                                if (SELECT_JDK_TOOLCHAIN_GOAL.equals(goal.textContentTrimmed())) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Adds the {@code maven-toolchains-plugin} with {@code select-jdk-toolchain} goal
     * to the POM's {@code <build><plugins>} section, configured with a version constraint
     * so the plugin selects a JDK that supports the project's source level.
     *
     * <p>If the plugin already exists in the POM (e.g. with the older {@code toolchain} goal),
     * the {@code select-jdk-toolchain} execution is appended to the existing entry and its
     * version is upgraded to at least {@value #TOOLCHAINS_PLUGIN_MIN_VERSION} — the first
     * version that contains the {@code select-jdk-toolchain} goal.
     *
     * @param pomDocument the POM document to modify
     * @param maxJdkVersion the latest JDK major version that supports the source level
     */
    void addToolchainsPlugin(Document pomDocument, int maxJdkVersion) {
        Element root = pomDocument.root();
        Element build = root.childElement(BUILD).orElseGet(() -> DomUtils.insertNewElement(BUILD, root));
        Element plugins = build.childElement(PLUGINS).orElseGet(() -> DomUtils.insertNewElement(PLUGINS, build));

        // Look for an existing toolchains-plugin entry (may have only the older 'toolchain' goal)
        Element existingPlugin = findToolchainsPlugin(plugins);
        final Element plugin;
        if (existingPlugin != null) {
            // Existing plugin — ensure version is >= 3.2.0
            ensureMinimumVersion(pomDocument, existingPlugin);
            plugin = existingPlugin;
        } else {
            // New plugin entry — set version to the minimum that has select-jdk-toolchain
            plugin = DomUtils.createPlugin(
                    plugins, TOOLCHAINS_PLUGIN_GROUP_ID, MAVEN_TOOLCHAINS_PLUGIN, TOOLCHAINS_PLUGIN_MIN_VERSION);
        }

        // Also upgrade version in pluginManagement if present there
        Element pluginManagement = build.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement != null) {
            Element managedPlugins = pluginManagement.childElement(PLUGINS).orElse(null);
            if (managedPlugins != null) {
                Element managedPlugin = findToolchainsPlugin(managedPlugins);
                if (managedPlugin != null) {
                    ensureMinimumVersion(pomDocument, managedPlugin);
                }
            }
        }

        Element executions =
                plugin.childElement("executions").orElseGet(() -> DomUtils.insertNewElement("executions", plugin));
        Element execution = DomUtils.insertNewElement("execution", executions);
        Element goals = DomUtils.insertNewElement("goals", execution);
        DomUtils.insertContentElement(goals, "goal", SELECT_JDK_TOOLCHAIN_GOAL);
        Element configuration = DomUtils.insertNewElement(CONFIGURATION, execution);
        DomUtils.insertContentElement(configuration, "version", "(," + maxJdkVersion + "]");
    }

    /**
     * Finds an existing {@code maven-toolchains-plugin} element in a plugins section.
     *
     * @return the plugin element, or {@code null} if not found
     */
    private Element findToolchainsPlugin(Element pluginsElement) {
        for (Element plugin : pluginsElement.childElements(PLUGIN).toList()) {
            String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
            String groupId = plugin.childTextTrimmed(GROUP_ID);
            if (MAVEN_TOOLCHAINS_PLUGIN.equals(artifactId)
                    && (groupId == null || groupId.isEmpty() || TOOLCHAINS_PLUGIN_GROUP_ID.equals(groupId))) {
                return plugin;
            }
        }
        return null;
    }

    /**
     * Ensures the plugin element has a version >= {@value #TOOLCHAINS_PLUGIN_MIN_VERSION}.
     * If the version is missing or below the minimum, it is set/upgraded.
     */
    void ensureMinimumVersion(Document pomDocument, Element plugin) {
        Element versionElement = plugin.childElement(VERSION).orElse(null);
        if (versionElement == null) {
            // No version element — add one with the minimum version
            DomUtils.insertContentElement(plugin, VERSION, TOOLCHAINS_PLUGIN_MIN_VERSION);
        } else {
            String currentVersion = versionElement.textContentTrimmed();
            if (currentVersion != null && !currentVersion.startsWith("${")) {
                if (isVersionBelow(currentVersion, TOOLCHAINS_PLUGIN_MIN_VERSION)) {
                    new Editor(pomDocument).setTextContent(versionElement, TOOLCHAINS_PLUGIN_MIN_VERSION);
                }
            }
        }
    }

    /**
     * Checks if {@code currentVersion} is below {@code minVersion} using Maven version semantics.
     */
    static boolean isVersionBelow(String currentVersion, String minVersion) {
        return new ComparableVersion(currentVersion).compareTo(new ComparableVersion(minVersion)) < 0;
    }

    /**
     * Returns the major version of the running JDK.
     * Extracted as a method so tests can override it.
     */
    int getRunningJdkMajor() {
        return JdkSourceLevelSupport.getRunningJdkMajor();
    }
}
