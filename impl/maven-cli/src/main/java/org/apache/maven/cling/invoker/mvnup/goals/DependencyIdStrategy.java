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
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.CLASSIFIER;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.DEPENDENCY_MANAGEMENT;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.EXCLUSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.EXCLUSIONS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.GROUP_ID;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGINS;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.TYPE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.VERSION;
import static eu.maveniverse.domtrip.maven.MavenPomElements.ModelVersions.MODEL_VERSION_4_2_0;

/**
 * Strategy for collapsing dependency coordinate child elements into compact {@code id} attributes.
 * Only applies to POMs at model version 4.2.0 or higher.
 *
 * <p>Transforms verbose dependency declarations:
 * <pre>{@code
 * <dependency>
 *   <groupId>org.example</groupId>
 *   <artifactId>lib</artifactId>
 *   <version>1.0</version>
 * </dependency>
 * }</pre>
 * into compact form:
 * <pre>{@code
 * <dependency id="org.example:lib:1.0"/>
 * }</pre>
 *
 * <p>Supported formats (trailing {@code :} means version is managed):
 * <ul>
 *   <li>{@code g:a} — version managed</li>
 *   <li>{@code g:a:v} — default type "jar"</li>
 *   <li>{@code g:a:type:} — non-default type, version managed</li>
 *   <li>{@code g:a:type:v} — non-default type</li>
 *   <li>{@code g:a:type:classifier:} — with classifier, version managed</li>
 *   <li>{@code g:a:type:classifier:v} — with classifier</li>
 * </ul>
 *
 * <p>Also collapses exclusions from {@code <groupId>}/{@code <artifactId>} into {@code id="g:a"}.
 */
@Named
@Singleton
@Priority(25)
public class DependencyIdStrategy extends AbstractUpgradeStrategy {

    private static final String DEFAULT_TYPE = "jar";

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);

        if (options.all().orElse(false)) {
            return true;
        }

        String modelVersion = options.modelVersion().orElse(null);
        return MODEL_VERSION_4_2_0.equals(modelVersion);
    }

    @Override
    public String getDescription() {
        return "Collapsing dependency coordinates into id attributes";
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
                if (!MODEL_VERSION_4_2_0.equals(currentVersion) && !ModelVersionUtils.isNewerThan410(currentVersion)) {
                    context.success("Skipping (model version " + currentVersion + " < 4.2.0)");
                    continue;
                }
                if (!MODEL_VERSION_4_2_0.equals(currentVersion)) {
                    context.success("Skipping (model version " + currentVersion + " is not 4.2.0)");
                    continue;
                }

                boolean hasChanges = collapseDependencies(context, pomDocument);

                if (hasChanges) {
                    modifiedPoms.add(pomPath);
                    context.success("Dependency coordinates collapsed into id attributes");
                } else {
                    context.success("No dependencies to collapse");
                }
            } catch (Exception e) {
                context.failure("Failed to collapse dependency coordinates: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    private boolean collapseDependencies(UpgradeContext context, Document pomDocument) {
        Element root = pomDocument.root();
        boolean hasChanges = false;

        // Process <dependencies>
        hasChanges |= processDependenciesSection(
                context, root.childElement(DEPENDENCIES).orElse(null));

        // Process <dependencyManagement><dependencies>
        hasChanges |= root.childElement(DEPENDENCY_MANAGEMENT)
                .flatMap(dm -> dm.childElement(DEPENDENCIES))
                .map(deps -> processDependenciesSection(context, deps))
                .orElse(false);

        // Process <profiles><profile> dependencies and dependencyManagement
        hasChanges |= root.childElement(PROFILES).stream()
                .flatMap(profiles -> profiles.childElements(PROFILE))
                .map(profile -> {
                    boolean profileChanges = false;
                    profileChanges |= profile.childElement(DEPENDENCIES)
                            .map(deps -> processDependenciesSection(context, deps))
                            .orElse(false);
                    profileChanges |= profile.childElement(DEPENDENCY_MANAGEMENT)
                            .flatMap(dm -> dm.childElement(DEPENDENCIES))
                            .map(deps -> processDependenciesSection(context, deps))
                            .orElse(false);
                    return profileChanges;
                })
                .reduce(false, Boolean::logicalOr);

        // Process <build><plugins><plugin><dependencies>
        hasChanges |= root.childElement(BUILD).flatMap(build -> build.childElement(PLUGINS)).stream()
                .flatMap(plugins -> plugins.childElements(PLUGIN))
                .map(plugin -> plugin.childElement(DEPENDENCIES)
                        .map(deps -> processDependenciesSection(context, deps))
                        .orElse(false))
                .reduce(false, Boolean::logicalOr);

        return hasChanges;
    }

    private boolean processDependenciesSection(UpgradeContext context, Element dependenciesElement) {
        if (dependenciesElement == null) {
            return false;
        }

        List<Element> dependencyElements =
                dependenciesElement.childElements(DEPENDENCY).toList();
        boolean hasChanges = false;

        for (Element dependency : dependencyElements) {
            hasChanges |= collapseDependency(context, dependency);
            hasChanges |= collapseExclusions(context, dependency);
        }

        return hasChanges;
    }

    private boolean collapseDependency(UpgradeContext context, Element dependency) {
        if (dependency.attribute("id") != null) {
            return false;
        }

        String groupId = dependency.childText(GROUP_ID);
        String artifactId = dependency.childText(ARTIFACT_ID);
        String version = dependency.childText(VERSION);

        if (groupId == null || artifactId == null) {
            return false;
        }

        String type = dependency.childText(TYPE);
        String classifier = dependency.childText(CLASSIFIER);

        String id = buildIdValue(groupId, artifactId, version, type, classifier);
        dependency.attribute("id", id);

        removeChildElement(dependency, GROUP_ID);
        removeChildElement(dependency, ARTIFACT_ID);
        if (version != null) {
            removeChildElement(dependency, VERSION);
        }

        if (classifier != null) {
            removeChildElement(dependency, CLASSIFIER);
            removeChildElement(dependency, TYPE);
        } else if (type != null && !DEFAULT_TYPE.equals(type)) {
            removeChildElement(dependency, TYPE);
        } else if (type != null) {
            removeChildElement(dependency, TYPE);
        }

        context.detail("Collapsed: " + id);
        return true;
    }

    private boolean collapseExclusions(UpgradeContext context, Element dependency) {
        Element exclusionsElement = dependency.childElement(EXCLUSIONS).orElse(null);
        if (exclusionsElement == null) {
            return false;
        }

        List<Element> exclusionElements =
                exclusionsElement.childElements(EXCLUSION).toList();
        boolean hasChanges = false;

        for (Element exclusion : exclusionElements) {
            hasChanges |= collapseExclusion(context, exclusion);
        }

        return hasChanges;
    }

    private boolean collapseExclusion(UpgradeContext context, Element exclusion) {
        if (exclusion.attribute("id") != null) {
            return false;
        }

        String groupId = exclusion.childText(GROUP_ID);
        String artifactId = exclusion.childText(ARTIFACT_ID);

        if (groupId == null || artifactId == null) {
            return false;
        }

        String id = groupId + ":" + artifactId;
        exclusion.attribute("id", id);

        removeChildElement(exclusion, GROUP_ID);
        removeChildElement(exclusion, ARTIFACT_ID);

        context.detail("Collapsed exclusion: " + id);
        return true;
    }

    static String buildIdValue(String groupId, String artifactId, String version, String type, String classifier) {
        if (classifier != null && !classifier.isEmpty()) {
            String effectiveType = (type != null && !type.isEmpty()) ? type : DEFAULT_TYPE;
            if (version != null) {
                return groupId + ":" + artifactId + ":" + effectiveType + ":" + classifier + ":" + version;
            } else {
                return groupId + ":" + artifactId + ":" + effectiveType + ":" + classifier + ":";
            }
        } else if (type != null && !type.isEmpty() && !DEFAULT_TYPE.equals(type)) {
            if (version != null) {
                return groupId + ":" + artifactId + ":" + type + ":" + version;
            } else {
                return groupId + ":" + artifactId + ":" + type + ":";
            }
        } else if (version != null) {
            return groupId + ":" + artifactId + ":" + version;
        } else {
            return groupId + ":" + artifactId;
        }
    }

    private static void removeChildElement(Element parent, String childName) {
        parent.childElement(childName).ifPresent(DomUtils::removeElement);
    }
}
