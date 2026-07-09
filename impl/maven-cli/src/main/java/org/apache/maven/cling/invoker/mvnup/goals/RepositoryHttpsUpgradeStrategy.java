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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import eu.maveniverse.domtrip.Document;
import eu.maveniverse.domtrip.Editor;
import eu.maveniverse.domtrip.Element;
import org.apache.maven.api.cli.mvnup.UpgradeOptions;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.cling.invoker.mvnup.UpgradeContext;

import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_REPOSITORIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PLUGIN_REPOSITORY;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILE;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.PROFILES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.REPOSITORIES;
import static eu.maveniverse.domtrip.maven.MavenPomElements.Elements.REPOSITORY;

/**
 * Strategy for upgrading HTTP repository URLs to HTTPS in POM files.
 *
 * <p>Since Maven 3.8.1+, HTTP repositories are blocked by the {@code maven-default-http-blocker}
 * mirror, so any {@code http://} repository URL will cause build failures. This strategy
 * automatically converts {@code http://} URLs to {@code https://}, using canonical HTTPS URLs
 * for well-known repositories.
 *
 * <p>The strategy handles:
 * <ul>
 *   <li>{@code <repositories>/<repository>} URLs</li>
 *   <li>{@code <pluginRepositories>/<pluginRepository>} URLs</li>
 *   <li>{@code <distributionManagement>/<repository>} and {@code <snapshotRepository>} URLs</li>
 *   <li>Profile-scoped repositories in all of the above sections</li>
 * </ul>
 */
@Named
@Singleton
@Priority(15)
public class RepositoryHttpsUpgradeStrategy extends AbstractUpgradeStrategy {

    private static final String HTTP_SCHEME = "http://";

    private static final String DISTRIBUTION_MANAGEMENT = "distributionManagement";
    private static final String SNAPSHOT_REPOSITORY = "snapshotRepository";

    /**
     * Well-known repository URL mappings from HTTP to their canonical HTTPS equivalents.
     * Uses a LinkedHashMap to preserve insertion order for predictable iteration.
     */
    private static final Map<String, String> WELL_KNOWN_URL_MAPPINGS = createWellKnownMappings();

    private static Map<String, String> createWellKnownMappings() {
        Map<String, String> mappings = new LinkedHashMap<>();
        // Apache repository URLs
        mappings.put("http://repository.apache.org/snapshots", "https://repository.apache.org/snapshots");
        mappings.put("http://repository.apache.org/releases", "https://repository.apache.org/releases");
        // Maven Central variants - all map to the canonical HTTPS URL
        mappings.put("http://repo1.maven.org/maven2", "https://repo.maven.apache.org/maven2");
        mappings.put("http://repo.maven.apache.org/maven2", "https://repo.maven.apache.org/maven2");
        mappings.put("http://central.maven.org/maven2", "https://repo.maven.apache.org/maven2");
        return mappings;
    }

    @Override
    public boolean isApplicable(UpgradeContext context) {
        UpgradeOptions options = getOptions(context);
        return isOptionEnabled(options, options.model(), true);
    }

    @Override
    public String getDescription() {
        return "Upgrading HTTP repository URLs to HTTPS";
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

            context.info(pomPath + " (checking for HTTP repository URLs)");
            context.indent();

            try {
                boolean hasUpgrades = upgradeRepositoryUrls(pomDocument, context);

                if (hasUpgrades) {
                    modifiedPoms.add(pomPath);
                    context.success("HTTP repository URLs upgraded to HTTPS");
                } else {
                    context.success("No HTTP repository URLs found");
                }
            } catch (Exception e) {
                context.failure("Failed to upgrade repository URLs: " + e.getMessage());
                errorPoms.add(pomPath);
            } finally {
                context.unindent();
            }
        }

        return new UpgradeResult(processedPoms, modifiedPoms, errorPoms);
    }

    /**
     * Upgrades all HTTP repository URLs in the POM document to HTTPS.
     *
     * @param pomDocument the POM document to process
     * @param context the upgrade context for logging
     * @return true if any URLs were upgraded
     */
    private boolean upgradeRepositoryUrls(Document pomDocument, UpgradeContext context) {
        Element root = pomDocument.root();
        boolean hasUpgrades = false;

        // Process root-level repositories and pluginRepositories
        hasUpgrades |= upgradeRepositoriesSection(root, REPOSITORIES, REPOSITORY, "repositories", context);
        hasUpgrades |=
                upgradeRepositoriesSection(root, PLUGIN_REPOSITORIES, PLUGIN_REPOSITORY, "pluginRepositories", context);

        // Process distributionManagement
        hasUpgrades |= upgradeDistributionManagement(root, "distributionManagement", context);

        // Process profile-scoped repositories
        hasUpgrades |= upgradeProfileRepositories(root, context);

        return hasUpgrades;
    }

    /**
     * Upgrades repository URLs in a repositories or pluginRepositories section.
     */
    private boolean upgradeRepositoriesSection(
            Element parent, String containerName, String elementType, String sectionLabel, UpgradeContext context) {
        Element container = parent.childElement(containerName).orElse(null);
        if (container == null) {
            return false;
        }

        return container
                .childElements(elementType)
                .map(repo -> upgradeUrlElement(repo, sectionLabel, context))
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Upgrades repository and snapshotRepository URLs in the distributionManagement section.
     */
    private boolean upgradeDistributionManagement(Element parent, String sectionLabel, UpgradeContext context) {
        Element distMgmt = parent.childElement(DISTRIBUTION_MANAGEMENT).orElse(null);
        if (distMgmt == null) {
            return false;
        }

        boolean hasUpgrades = false;

        // Process <distributionManagement>/<repository>
        Element repository = distMgmt.childElement(REPOSITORY).orElse(null);
        if (repository != null) {
            hasUpgrades |= upgradeUrlElement(repository, sectionLabel + "/repository", context);
        }

        // Process <distributionManagement>/<snapshotRepository>
        Element snapshotRepository = distMgmt.childElement(SNAPSHOT_REPOSITORY).orElse(null);
        if (snapshotRepository != null) {
            hasUpgrades |= upgradeUrlElement(snapshotRepository, sectionLabel + "/snapshotRepository", context);
        }

        return hasUpgrades;
    }

    /**
     * Upgrades repository URLs in profile-scoped sections.
     */
    private boolean upgradeProfileRepositories(Element root, UpgradeContext context) {
        Element profiles = root.childElement(PROFILES).orElse(null);
        if (profiles == null) {
            return false;
        }

        return profiles.childElements(PROFILE)
                .map(profile -> {
                    boolean upgraded = false;
                    upgraded |= upgradeRepositoriesSection(
                            profile, REPOSITORIES, REPOSITORY, "profile/repositories", context);
                    upgraded |= upgradeRepositoriesSection(
                            profile, PLUGIN_REPOSITORIES, PLUGIN_REPOSITORY, "profile/pluginRepositories", context);
                    upgraded |= upgradeDistributionManagement(profile, "profile/distributionManagement", context);
                    return upgraded;
                })
                .reduce(false, Boolean::logicalOr);
    }

    /**
     * Upgrades the URL element of a repository from HTTP to HTTPS.
     *
     * @param repositoryElement the repository element containing a url child
     * @param sectionLabel the section label for logging
     * @param context the upgrade context for logging
     * @return true if the URL was upgraded
     */
    private boolean upgradeUrlElement(Element repositoryElement, String sectionLabel, UpgradeContext context) {
        Element urlElement = repositoryElement.childElement("url").orElse(null);
        if (urlElement == null) {
            return false;
        }

        String url = urlElement.textContent();
        if (url == null) {
            return false;
        }

        String trimmedUrl = url.trim();
        if (!trimmedUrl.startsWith(HTTP_SCHEME)) {
            return false;
        }

        String repositoryId = repositoryElement.childText("id");
        String idLabel = repositoryId != null ? repositoryId : "(no id)";

        // Try well-known URL mappings first
        String upgradedUrl = mapWellKnownUrl(trimmedUrl);
        if (upgradedUrl == null) {
            // Generic http -> https conversion
            upgradedUrl = "https://" + trimmedUrl.substring(HTTP_SCHEME.length());
        }

        // Preserve original whitespace around the URL
        String newUrl = url.replace(trimmedUrl, upgradedUrl);
        Editor editor = new Editor(repositoryElement.document());
        editor.setTextContent(urlElement, newUrl);

        context.detail("Upgraded repository URL from " + trimmedUrl + " to " + upgradedUrl + " for " + idLabel + " in "
                + sectionLabel);
        return true;
    }

    /**
     * Maps a well-known HTTP URL to its canonical HTTPS equivalent.
     *
     * @param httpUrl the HTTP URL to map
     * @return the canonical HTTPS URL, or null if no well-known mapping exists
     */
    static String mapWellKnownUrl(String httpUrl) {
        // Normalize: remove trailing slash for comparison
        String normalized = httpUrl.endsWith("/") ? httpUrl.substring(0, httpUrl.length() - 1) : httpUrl;

        for (Map.Entry<String, String> mapping : WELL_KNOWN_URL_MAPPINGS.entrySet()) {
            if (normalized.equals(mapping.getKey()) || normalized.startsWith(mapping.getKey() + "/")) {
                // Replace the matched prefix with the canonical HTTPS URL,
                // preserving any trailing path segments
                String remainder = normalized.substring(mapping.getKey().length());
                String result = mapping.getValue() + remainder;
                // Restore trailing slash if original had one
                if (httpUrl.endsWith("/") && !result.endsWith("/")) {
                    result += "/";
                }
                return result;
            }
        }
        return null;
    }
}
