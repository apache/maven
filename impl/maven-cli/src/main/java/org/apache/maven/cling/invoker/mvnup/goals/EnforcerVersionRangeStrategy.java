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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;

/**
 * Strategy for widening {@code requireMavenVersion} ranges in the {@code maven-enforcer-plugin}
 * to allow Maven 4.
 *
 * <p>Many projects use the {@code maven-enforcer-plugin} with a {@code requireMavenVersion} rule
 * that has an exclusive upper bound at version 4 (e.g., {@code [3.8.8,4)}, {@code [3,4)}),
 * which blocks Maven 4 builds. This strategy widens such ranges to allow Maven 4 by changing
 * the upper bound to 5 (e.g., {@code [3.8.8,4)} becomes {@code [3.8.8,5)}).
 *
 * <p>The strategy handles:
 * <ul>
 *   <li>{@code <configuration><rules><requireMavenVersion>} in plugin declarations</li>
 *   <li>{@code <executions><execution><configuration><rules><requireMavenVersion>} in executions</li>
 *   <li>Both {@code build/plugins} and {@code build/pluginManagement/plugins} sections</li>
 *   <li>Profile-scoped enforcer plugin declarations</li>
 * </ul>
 */
@Named
@Singleton
@Priority(18)
public class EnforcerVersionRangeStrategy extends AbstractUpgradeStrategy {

    private static final String ENFORCER_ARTIFACT_ID = "maven-enforcer-plugin";

    private static final String RULES = "rules";
    private static final String REQUIRE_MAVEN_VERSION = "requireMavenVersion";
    private static final String EXECUTIONS = "executions";
    private static final String EXECUTION = "execution";

    /**
     * Pattern to match Maven version ranges with an exclusive upper bound at any 4.x version.
     * Captures:
     *   Group 1: opening bracket ([ or ()
     *   Group 2: lower bound (e.g., 3.8.8, 3, 3.6.3)
     *   Group 3: upper bound starting with 4 (e.g., 4, 4.0, 4.0.0, 4.1, 4.2.1)
     *
     * Examples matched: [3.8.8,4), [3,4), (3.6.3,4), [3.8.8,4.0), [3.8.8,4.0.0), [3.8.8,4.1)
     * Examples not matched: [3.8.8,), 3.8.8, [3.8.8,5), [3.8.8,4]
     */
    static final Pattern MAVEN4_EXCLUSIVE_UPPER_BOUND = Pattern.compile("^(\\[|\\()(.+?),\\s*(4(?:\\.\\d+)*)\\s*\\)$");

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Widening RequireMavenVersion ranges to allow Maven 4";
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

            context.info(pomPath + " (checking for RequireMavenVersion range restrictions)");
            context.indent();

            try {
                boolean hasUpgrades = widenEnforcerVersionRanges(pomDocument, context);

                if (hasUpgrades) {
                    modifiedPoms.add(pomPath);
                    context.success("RequireMavenVersion ranges widened to allow Maven 4");
                } else {
                    context.success("No RequireMavenVersion range restrictions found");
                }
            } catch (Exception e) {
                context.failure("Failed to widen RequireMavenVersion ranges: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Finds and widens all RequireMavenVersion ranges in the POM document.
     */
    private boolean widenEnforcerVersionRanges(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean hasUpgrades = false;

        // Process root-level build/plugins and build/pluginManagement/plugins
        hasUpgrades |= processPluginSections(root, context);

        // Process profile-scoped plugins
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles != null) {
            for (Element profile : profiles.childElements(PROFILE).toList()) {
                hasUpgrades |= processPluginSections(profile, context);
            }
        }

        return hasUpgrades;
    }

    /**
     * Processes both build/plugins and build/pluginManagement/plugins sections.
     */
    private boolean processPluginSections(Element parent, UpgradeContext context) {
        Element buildElement = parent.childElement(BUILD).orElse(null);
        if (buildElement == null) {
            return false;
        }

        boolean hasUpgrades = false;

        // Check build/plugins
        Element pluginsElement = buildElement.childElement(PLUGINS).orElse(null);
        if (pluginsElement != null) {
            hasUpgrades |= processPluginsForEnforcer(pluginsElement, context);
        }

        // Check build/pluginManagement/plugins
        Element pluginManagement = buildElement.childElement(PLUGIN_MANAGEMENT).orElse(null);
        if (pluginManagement != null) {
            Element managedPlugins = pluginManagement.childElement(PLUGINS).orElse(null);
            if (managedPlugins != null) {
                hasUpgrades |= processPluginsForEnforcer(managedPlugins, context);
            }
        }

        return hasUpgrades;
    }

    /**
     * Finds enforcer plugin elements and processes their requireMavenVersion rules.
     */
    private boolean processPluginsForEnforcer(Element pluginsElement, UpgradeContext context) {
        return pluginsElement
                .childElements(PLUGIN)
                .filter(this::isEnforcerPlugin)
                .map(plugin -> processEnforcerPlugin(plugin, context))
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Checks whether a plugin element is the maven-enforcer-plugin.
     */
    private boolean isEnforcerPlugin(Element plugin) {
        String artifactId = plugin.childTextTrimmed(ARTIFACT_ID);
        if (!ENFORCER_ARTIFACT_ID.equals(artifactId)) {
            return false;
        }
        String groupId = plugin.childTextTrimmed(GROUP_ID);
        return groupId == null || groupId.isEmpty() || DEFAULT_MAVEN_PLUGIN_GROUP_ID.equals(groupId);
    }

    /**
     * Processes an enforcer plugin element, checking both top-level configuration
     * and per-execution configurations for requireMavenVersion rules.
     */
    private boolean processEnforcerPlugin(Element plugin, UpgradeContext context) {
        boolean hasUpgrades = false;

        // Check top-level <configuration><rules><requireMavenVersion>
        Element configuration = plugin.childElement(CONFIGURATION).orElse(null);
        if (configuration != null) {
            hasUpgrades |= processRulesElement(configuration, context);
        }

        // Check <executions><execution><configuration><rules><requireMavenVersion>
        Element executionsElement = plugin.childElement(EXECUTIONS).orElse(null);
        if (executionsElement != null) {
            for (Element execution : executionsElement.childElements(EXECUTION).toList()) {
                Element execConfig = execution.childElement(CONFIGURATION).orElse(null);
                if (execConfig != null) {
                    hasUpgrades |= processRulesElement(execConfig, context);
                }
            }
        }

        return hasUpgrades;
    }

    /**
     * Processes a configuration element's rules for requireMavenVersion.
     */
    private boolean processRulesElement(Element configuration, UpgradeContext context) {
        Element rules = configuration.childElement(RULES).orElse(null);
        if (rules == null) {
            return false;
        }

        Element requireMavenVersion = rules.childElement(REQUIRE_MAVEN_VERSION).orElse(null);
        if (requireMavenVersion == null) {
            return false;
        }

        Element versionElement = requireMavenVersion.childElement(VERSION).orElse(null);
        if (versionElement == null) {
            return false;
        }

        String versionRange = versionElement.textContentTrimmed();
        if (versionRange == null || versionRange.isEmpty()) {
            return false;
        }

        String widened = widenVersionRange(versionRange);
        if (widened != null) {
            Editor editor = new Editor(versionElement.document());
            editor.setTextContent(versionElement, widened);
            context.detail(
                    "Widened RequireMavenVersion range from " + versionRange + " to " + widened + " to allow Maven 4");
            return true;
        }

        return false;
    }

    /**
     * Widens a Maven version range that has an exclusive upper bound at 4.x.
     *
     * @param versionRange the version range string (e.g., "[3.8.8,4)")
     * @return the widened range (e.g., "[3.8.8,5)"), or null if no widening is needed
     */
    static String widenVersionRange(String versionRange) {
        Matcher matcher = MAVEN4_EXCLUSIVE_UPPER_BOUND.matcher(versionRange);
        if (matcher.matches()) {
            String openBracket = matcher.group(1);
            String lowerBound = matcher.group(2);
            return openBracket + lowerBound + ",5)";
        }
        return null;
    }
}
