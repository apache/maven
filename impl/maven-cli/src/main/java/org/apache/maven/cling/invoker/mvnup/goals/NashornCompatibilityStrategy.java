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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.ARTIFACT_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.BUILD;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Plugins.DEFAULT_MAVEN_PLUGIN_GROUP_ID;

/**
 * Strategy for injecting the standalone OpenJDK Nashorn engine as a plugin dependency
 * when {@code maven-antrun-plugin} uses inline JavaScript via {@code <script language="javascript">}.
 *
 * <p>Maven 4 requires JDK 17+, but the built-in Nashorn JavaScript engine was removed in JDK 15.
 * Plugins executing JavaScript in-process (particularly {@code maven-antrun-plugin} with
 * {@code <script language="javascript">}) fail with:
 * <pre>
 * [ERROR] Unable to create javax script engine for javascript
 * </pre>
 *
 * <p>Toolchains cannot help because the script runs in Maven's own JVM, not in a forked process.
 * The standalone OpenJDK Nashorn ({@code org.openjdk.nashorn:nashorn-core}) provides a drop-in
 * replacement that registers via {@code ServiceLoader} and restores JavaScript support.
 *
 * <p>This strategy both <strong>injects</strong> the Nashorn dependency as a quick fix and
 * <strong>warns</strong> that users should consider migrating to GraalVM JavaScript
 * ({@code org.graalvm.js:js-scriptengine}) for long-term support.
 *
 * @see <a href="https://github.com/apache/maven/issues/12988">#12988</a>
 */
@Named
@Singleton
@Priority(19)
public class NashornCompatibilityStrategy extends AbstractUpgradeStrategy {

    static final String ANTRUN_ARTIFACT_ID = "maven-antrun-plugin";
    static final String NASHORN_GROUP_ID = "org.openjdk.nashorn";
    static final String NASHORN_ARTIFACT_ID = "nashorn-core";
    static final String NASHORN_VERSION = "15.4";

    private static final String SCRIPT = "script";
    private static final String LANGUAGE = "language";
    private static final String JAVASCRIPT = "javascript";

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        // Handle --all option (overrides individual options)
        boolean useAll = options.all().orElse(false);
        if (useAll) {
            return true;
        }

        // Apply default behavior: if no specific options are provided, enable by default
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

        // Check if --model is explicitly set (this is a compatibility fix)
        if (options.model().isPresent()) {
            return options.model().get();
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Injecting standalone Nashorn for antrun JavaScript compatibility";
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

            context.info(pomPath + " (checking for antrun JavaScript usage)");
            context.indent();

            try {
                boolean modified = injectNashornForAntrunJavaScript(pomDocument, context);

                if (modified) {
                    context.success("Injected standalone Nashorn dependency for antrun JavaScript");
                    modifiedPoms.add(pomPath);
                } else {
                    context.success("No antrun JavaScript usage found");
                }
            } catch (Exception e) {
                context.failure("Failed to process antrun JavaScript compatibility: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Scans all plugin declarations for maven-antrun-plugin with JavaScript scripts
     * and injects the standalone Nashorn dependency where needed.
     *
     * @return true if any modifications were made
     */
    private boolean injectNashornForAntrunJavaScript(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        List<Element> antrunPlugins = findAntrunPluginsWithJavaScript(root);

        boolean modified = false;
        for (Element pluginElement : antrunPlugins) {
            if (!hasNashornDependency(pluginElement)) {
                context.warning("maven-antrun-plugin uses inline JavaScript which requires a standalone"
                        + " script engine on JDK 17+. Adding org.openjdk.nashorn:nashorn-core as a"
                        + " quick fix. Consider migrating to GraalVM JavaScript"
                        + " (org.graalvm.js:js-scriptengine + org.graalvm.js:js) for long-term support.");
                addNashornDependency(pluginElement, context);
                modified = true;
            } else {
                context.detail("Nashorn dependency already present on maven-antrun-plugin");
            }
        }

        return modified;
    }

    /**
     * Finds all maven-antrun-plugin declarations that contain JavaScript script executions.
     * Searches in build/plugins, build/pluginManagement/plugins, and profile builds.
     */
    List<Element> findAntrunPluginsWithJavaScript(Element root) {
        List<Element> result = new ArrayList<>();

        // Collect all plugin containers: build/plugins, build/pluginManagement/plugins,
        // and the same within profiles
        Stream<Element> pluginContainers = Stream.concat(
                root.childElement(BUILD).stream()
                        .flatMap(build -> Stream.concat(
                                build.childElement(PLUGINS).stream(),
                                build.childElement(PLUGIN_MANAGEMENT).stream()
                                        .flatMap(pm -> pm.childElement(PLUGINS).stream()))),
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
                    if (isAntrunPlugin(pluginElement) && containsJavaScriptScript(pluginElement)) {
                        result.add(pluginElement);
                    }
                }));

        return result;
    }

    /**
     * Checks if a plugin element is maven-antrun-plugin.
     */
    static boolean isAntrunPlugin(Element pluginElement) {
        String artifactId = pluginElement.childText(ARTIFACT_ID);
        if (!ANTRUN_ARTIFACT_ID.equals(artifactId)) {
            return false;
        }

        String groupId = pluginElement.childText(GROUP_ID);
        // If groupId is absent, Maven defaults to org.apache.maven.plugins
        return groupId == null || DEFAULT_MAVEN_PLUGIN_GROUP_ID.equals(groupId);
    }

    /**
     * Recursively searches a plugin element for {@code <script language="javascript">} elements.
     * The script element can appear at any depth within the plugin's configuration or executions.
     */
    static boolean containsJavaScriptScript(Element element) {
        // Check if this element is a <script language="javascript">
        if (SCRIPT.equals(element.name())) {
            String lang = element.attribute(LANGUAGE);
            if (lang != null && JAVASCRIPT.equalsIgnoreCase(lang)) {
                return true;
            }
        }

        // Recursively check children
        return element.childElements().anyMatch(NashornCompatibilityStrategy::containsJavaScriptScript);
    }

    /**
     * Checks if the antrun plugin already has nashorn-core as a dependency.
     */
    static boolean hasNashornDependency(Element pluginElement) {
        return pluginElement
                .childElement(DEPENDENCIES)
                .map(deps -> deps.childElements("dependency").anyMatch(dep -> {
                    String gid = dep.childText(GROUP_ID);
                    String aid = dep.childText(ARTIFACT_ID);
                    return NASHORN_GROUP_ID.equals(gid) && NASHORN_ARTIFACT_ID.equals(aid);
                }))
                .orElse(false);
    }

    /**
     * Adds the standalone Nashorn dependency to the antrun plugin element.
     */
    private void addNashornDependency(Element pluginElement, UpgradeContext context) {
        Element dependenciesElement = pluginElement
                .childElement(DEPENDENCIES)
                .orElseGet(() -> DomUtils.insertNewElement(DEPENDENCIES, pluginElement));

        DomUtils.createDependency(dependenciesElement, NASHORN_GROUP_ID, NASHORN_ARTIFACT_ID, NASHORN_VERSION);

        context.detail("Added " + NASHORN_GROUP_ID + ":" + NASHORN_ARTIFACT_ID + ":" + NASHORN_VERSION
                + " as plugin dependency of " + ANTRUN_ARTIFACT_ID);
    }
}
