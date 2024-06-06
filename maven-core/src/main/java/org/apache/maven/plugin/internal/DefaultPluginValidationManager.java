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
package org.apache.maven.plugin.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public final class DefaultPluginValidationManager extends AbstractEventSpy implements PluginValidationManager {
    /**
     * The collection of "G:A" combinations that do NOT belong to Maven Core, hence, should be excluded from
     * "expected in provided scope" type of checks.
     */
    static final Collection<String> EXPECTED_PROVIDED_SCOPE_EXCLUSIONS_GA =
            Collections.unmodifiableCollection(Arrays.asList(
                    "org.apache.maven:maven-archiver", "org.apache.maven:maven-jxr", "org.apache.maven:plexus-utils"));

    private static final String ISSUES_KEY = DefaultPluginValidationManager.class.getName() + ".issues";

    private static final String PLUGIN_EXCLUDES_KEY = DefaultPluginValidationManager.class.getName() + ".excludes";

    private static final String MAVEN_PLUGIN_VALIDATION_KEY = "maven.plugin.validation";

    private static final String MAVEN_PLUGIN_VALIDATION_EXCLUDES_KEY = "maven.plugin.validation.excludes";

    private static final ValidationReportLevel DEFAULT_VALIDATION_LEVEL = ValidationReportLevel.INLINE;

    private static final Collection<ValidationReportLevel> INLINE_VALIDATION_LEVEL = Collections.unmodifiableCollection(
            Arrays.asList(ValidationReportLevel.INLINE, ValidationReportLevel.BRIEF));

    private enum ValidationReportLevel {
        NONE, // mute validation completely (validation issue collection still happens, it is just not reported!)
        INLINE, // inline, each "internal" problem one line next to mojo invocation
        SUMMARY, // at end, list of plugin GAVs along with ANY validation issues
        BRIEF, // each "internal" problem one line next to mojo invocation
        // and at end list of plugin GAVs along with "external" issues
        VERBOSE // at end, list of plugin GAVs along with detailed report of ANY validation issues
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void onEvent(Object event) {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (executionEvent.getType() == ExecutionEvent.Type.SessionStarted) {
                RepositorySystemSession repositorySystemSession =
                        executionEvent.getSession().getRepositorySession();
                validationReportLevel(repositorySystemSession); // this will parse and store it in session.data
                validationPluginExcludes(repositorySystemSession);
            } else if (executionEvent.getType() == ExecutionEvent.Type.SessionEnded) {
                reportSessionCollectedValidationIssues(executionEvent.getSession());
            }
        }
    }

    private List<?> validationPluginExcludes(RepositorySystemSession session) {
        return (List<?>) session.getData().computeIfAbsent(PLUGIN_EXCLUDES_KEY, () -> parsePluginExcludes(session));
    }

    private List<String> parsePluginExcludes(RepositorySystemSession session) {
        String excludes = ConfigUtils.getString(session, null, MAVEN_PLUGIN_VALIDATION_EXCLUDES_KEY);
        if (excludes == null || excludes.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(excludes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private ValidationReportLevel validationReportLevel(RepositorySystemSession session) {
        return (ValidationReportLevel) session.getData()
                .computeIfAbsent(ValidationReportLevel.class, () -> parseValidationReportLevel(session));
    }

    private ValidationReportLevel parseValidationReportLevel(RepositorySystemSession session) {
        String level = ConfigUtils.getString(session, null, MAVEN_PLUGIN_VALIDATION_KEY);
        if (level == null || level.isEmpty()) {
            return DEFAULT_VALIDATION_LEVEL;
        }
        try {
            return ValidationReportLevel.valueOf(level.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Invalid value specified for property {}: '{}'. Supported values are (case insensitive): {}",
                    MAVEN_PLUGIN_VALIDATION_KEY,
                    level,
                    Arrays.toString(ValidationReportLevel.values()));
            return DEFAULT_VALIDATION_LEVEL;
        }
    }

    private String pluginKey(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    private String pluginKey(MojoDescriptor mojoDescriptor) {
        PluginDescriptor pd = mojoDescriptor.getPluginDescriptor();
        return pluginKey(pd.getGroupId(), pd.getArtifactId(), pd.getVersion());
    }

    private String pluginKey(Artifact pluginArtifact) {
        return pluginKey(pluginArtifact.getGroupId(), pluginArtifact.getArtifactId(), pluginArtifact.getVersion());
    }

    private void mayReportInline(RepositorySystemSession session, IssueLocality locality, String issue) {
        if (locality == IssueLocality.INTERNAL) {
            ValidationReportLevel validationReportLevel = validationReportLevel(session);
            if (INLINE_VALIDATION_LEVEL.contains(validationReportLevel)) {
                logger.warn(" {}", issue);
            }
        }
    }

    @Override
    public void reportPluginValidationIssue(
            IssueLocality locality, RepositorySystemSession session, Artifact pluginArtifact, String issue) {
        String pluginKey = pluginKey(pluginArtifact);
        if (validationPluginExcludes(session).contains(pluginKey)) {
            return;
        }
        PluginValidationIssues pluginIssues =
                pluginIssues(session).computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginIssue(locality, null, issue);
        mayReportInline(session, locality, issue);
    }

    @Override
    public void reportPluginValidationIssue(
            IssueLocality locality, MavenSession mavenSession, MojoDescriptor mojoDescriptor, String issue) {
        String pluginKey = pluginKey(mojoDescriptor);
        if (validationPluginExcludes(mavenSession.getRepositorySession()).contains(pluginKey)) {
            return;
        }
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginIssue(locality, pluginDeclaration(mavenSession, mojoDescriptor), issue);
        mayReportInline(mavenSession.getRepositorySession(), locality, issue);
    }

    @Override
    public void reportPluginMojoValidationIssue(
            IssueLocality locality,
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            String issue) {
        String pluginKey = pluginKey(mojoDescriptor);
        if (validationPluginExcludes(mavenSession.getRepositorySession()).contains(pluginKey)) {
            return;
        }
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginMojoIssue(
                locality, pluginDeclaration(mavenSession, mojoDescriptor), mojoInfo(mojoDescriptor, mojoClass), issue);
        mayReportInline(mavenSession.getRepositorySession(), locality, issue);
    }

    private void reportSessionCollectedValidationIssues(MavenSession mavenSession) {
        if (!logger.isWarnEnabled()) {
            return; // nothing can be reported
        }
        ValidationReportLevel validationReportLevel = validationReportLevel(mavenSession.getRepositorySession());
        if (validationReportLevel == ValidationReportLevel.NONE
                || validationReportLevel == ValidationReportLevel.INLINE) {
            return; // we were asked to not report anything OR reporting already happened inline
        }
        ConcurrentHashMap<String, PluginValidationIssues> issuesMap = pluginIssues(mavenSession.getRepositorySession());
        EnumSet<IssueLocality> issueLocalitiesToReport = validationReportLevel == ValidationReportLevel.SUMMARY
                        || validationReportLevel == ValidationReportLevel.VERBOSE
                ? EnumSet.allOf(IssueLocality.class)
                : EnumSet.of(IssueLocality.EXTERNAL);

        if (hasAnythingToReport(issuesMap, issueLocalitiesToReport)) {
            logger.warn("");
            logger.warn("Plugin {} validation issues were detected in following plugin(s)", issueLocalitiesToReport);
            logger.warn("");

            // Sorting the plugins
            List<Map.Entry<String, PluginValidationIssues>> sortedEntries = new ArrayList<>(issuesMap.entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER));

            for (Map.Entry<String, PluginValidationIssues> entry : sortedEntries) {
                PluginValidationIssues issues = entry.getValue();
                if (!hasAnythingToReport(issues, issueLocalitiesToReport)) {
                    continue;
                }
                logger.warn(" * {}", entry.getKey());
                if (validationReportLevel == ValidationReportLevel.VERBOSE) {
                    if (!issues.pluginDeclarations.isEmpty()) {
                        logger.warn("  Declared at location(s):");
                        for (String pluginDeclaration : issues.pluginDeclarations) {
                            logger.warn("   * {}", pluginDeclaration);
                        }
                    }
                    if (!issues.pluginIssues.isEmpty()) {
                        for (IssueLocality issueLocality : issueLocalitiesToReport) {
                            Set<String> pluginIssues = issues.pluginIssues.get(issueLocality);
                            if (pluginIssues != null && !pluginIssues.isEmpty()) {
                                logger.warn("  Plugin {} issue(s):", issueLocality);
                                for (String pluginIssue : pluginIssues) {
                                    logger.warn("   * {}", pluginIssue);
                                }
                            }
                        }
                    }
                    if (!issues.mojoIssues.isEmpty()) {
                        for (IssueLocality issueLocality : issueLocalitiesToReport) {
                            Map<String, LinkedHashSet<String>> mojoIssues = issues.mojoIssues.get(issueLocality);
                            if (mojoIssues != null && !mojoIssues.isEmpty()) {
                                logger.warn("  Mojo {} issue(s):", issueLocality);
                                for (String mojoInfo : mojoIssues.keySet()) {
                                    logger.warn("   * Mojo {}", mojoInfo);
                                    for (String mojoIssue : mojoIssues.get(mojoInfo)) {
                                        logger.warn("     - {}", mojoIssue);
                                    }
                                }
                            }
                        }
                    }
                    logger.warn("");
                }
            }
            logger.warn("");
            if (validationReportLevel == ValidationReportLevel.VERBOSE) {
                logger.warn(
                        "Fix reported issues by adjusting plugin configuration or by upgrading above listed plugins. If no upgrade available, please notify plugin maintainers about reported issues.");
            }
            logger.warn(
                    "For more or less details, use 'maven.plugin.validation' property with one of the values (case insensitive): {}",
                    Arrays.toString(ValidationReportLevel.values()));
            logger.warn("");
        }
    }

    private boolean hasAnythingToReport(
            Map<String, PluginValidationIssues> issuesMap, EnumSet<IssueLocality> issueLocalitiesToReport) {
        for (PluginValidationIssues issues : issuesMap.values()) {
            if (hasAnythingToReport(issues, issueLocalitiesToReport)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnythingToReport(PluginValidationIssues issues, EnumSet<IssueLocality> issueLocalitiesToReport) {
        for (IssueLocality issueLocality : issueLocalitiesToReport) {
            Set<String> pluginIssues = issues.pluginIssues.get(issueLocality);
            if (pluginIssues != null && !pluginIssues.isEmpty()) {
                return true;
            }
            Map<String, LinkedHashSet<String>> mojoIssues = issues.mojoIssues.get(issueLocality);
            if (mojoIssues != null && !mojoIssues.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String pluginDeclaration(MavenSession mavenSession, MojoDescriptor mojoDescriptor) {
        InputLocation inputLocation =
                mojoDescriptor.getPluginDescriptor().getPlugin().getLocation("");
        if (inputLocation != null && inputLocation.getSource() != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(inputLocation.getSource().getModelId());
            String location = inputLocation.getSource().getLocation();
            if (location != null) {
                if (location.contains("://")) {
                    stringBuilder.append(" (").append(location).append(")");
                } else {
                    Path topLevelBasedir =
                            mavenSession.getTopLevelProject().getBasedir().toPath();
                    Path locationPath =
                            new File(location).toPath().toAbsolutePath().normalize();
                    if (locationPath.startsWith(topLevelBasedir)) {
                        locationPath = topLevelBasedir.relativize(locationPath);
                    }
                    stringBuilder.append(" (").append(locationPath).append(")");
                }
            }
            stringBuilder.append(" @ line ").append(inputLocation.getLineNumber());
            return stringBuilder.toString();
        } else {
            return "unknown";
        }
    }

    private String mojoInfo(MojoDescriptor mojoDescriptor, Class<?> mojoClass) {
        return mojoDescriptor.getFullGoalName() + " (" + mojoClass.getName() + ")";
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, PluginValidationIssues> pluginIssues(RepositorySystemSession session) {
        return (ConcurrentHashMap<String, PluginValidationIssues>)
                session.getData().computeIfAbsent(ISSUES_KEY, ConcurrentHashMap::new);
    }

    private static class PluginValidationIssues {
        private final LinkedHashSet<String> pluginDeclarations;

        private final HashMap<IssueLocality, LinkedHashSet<String>> pluginIssues;

        private final HashMap<IssueLocality, LinkedHashMap<String, LinkedHashSet<String>>> mojoIssues;

        private PluginValidationIssues() {
            this.pluginDeclarations = new LinkedHashSet<>();
            this.pluginIssues = new HashMap<>();
            this.mojoIssues = new HashMap<>();
        }

        private synchronized void reportPluginIssue(
                IssueLocality issueLocality, String pluginDeclaration, String issue) {
            if (pluginDeclaration != null) {
                pluginDeclarations.add(pluginDeclaration);
            }
            pluginIssues
                    .computeIfAbsent(issueLocality, k -> new LinkedHashSet<>())
                    .add(issue);
        }

        private synchronized void reportPluginMojoIssue(
                IssueLocality issueLocality, String pluginDeclaration, String mojoInfo, String issue) {
            if (pluginDeclaration != null) {
                pluginDeclarations.add(pluginDeclaration);
            }
            mojoIssues
                    .computeIfAbsent(issueLocality, k -> new LinkedHashMap<>())
                    .computeIfAbsent(mojoInfo, k -> new LinkedHashSet<>())
                    .add(issue);
        }
    }
}
