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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named
public final class DefaultPluginValidationManager extends AbstractMavenLifecycleParticipant
        implements PluginValidationManager {

    private static final String ISSUES_KEY = DefaultPluginValidationManager.class.getName() + ".issues";

    private static final String MAVEN_PLUGIN_VALIDATION_KEY = "maven.plugin.validation";

    private enum ValidationReportLevel {
        NONE, // mute validation completely (validation issue collection still happens, it is just not reported!)
        INLINE, // each problem one line, inline, next to mojo invocation, repeated as many times as mojo is executed
        BRIEF, // one line with count of plugins in the build having validation issues
        DEFAULT, // list of plugin GAVs in the build having validation issues
        VERBOSE // detailed report (GAV, declaration, use and problems) of plugins in the build having validation issues
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterSessionEnd(MavenSession session) {
        reportSessionCollectedValidationIssues(session);
    }

    private ValidationReportLevel validationLevel(RepositorySystemSession session) {
        String level = ConfigUtils.getString(session, null, MAVEN_PLUGIN_VALIDATION_KEY);
        if (level == null || level.isEmpty()) {
            return ValidationReportLevel.DEFAULT;
        }
        try {
            return ValidationReportLevel.valueOf(level.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Invalid value specified for property {}: '{}'. Supported values are (case insensitive): {}",
                    MAVEN_PLUGIN_VALIDATION_KEY,
                    level,
                    Arrays.toString(ValidationReportLevel.values()));
            return ValidationReportLevel.DEFAULT;
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

    @Override
    public void reportPluginValidationIssue(RepositorySystemSession session, Artifact pluginArtifact, String issue) {
        String pluginKey = pluginKey(pluginArtifact);
        PluginValidationIssues pluginIssues =
                pluginIssues(session).computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginIssue(null, null, issue);
        ValidationReportLevel validationLevel = validationLevel(session);
        if (validationLevel == ValidationReportLevel.INLINE) {
            logger.warn("{}", issue);
        }
    }

    @Override
    public void reportPluginValidationIssue(MavenSession mavenSession, MojoDescriptor mojoDescriptor, String issue) {
        String pluginKey = pluginKey(mojoDescriptor);
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginIssue(
                pluginDeclaration(mavenSession, mojoDescriptor), pluginOccurrence(mavenSession), issue);
        ValidationReportLevel validationLevel = validationLevel(mavenSession.getRepositorySession());
        if (validationLevel == ValidationReportLevel.INLINE) {
            logger.warn("{}", issue);
        }
    }

    @Override
    public void reportPluginMojoValidationIssue(
            MavenSession mavenSession, MojoDescriptor mojoDescriptor, Class<?> mojoClass, String issue) {
        String pluginKey = pluginKey(mojoDescriptor);
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues());
        pluginIssues.reportPluginMojoIssue(
                pluginDeclaration(mavenSession, mojoDescriptor),
                pluginOccurrence(mavenSession),
                mojoInfo(mojoDescriptor, mojoClass),
                issue);
        ValidationReportLevel validationLevel = validationLevel(mavenSession.getRepositorySession());
        if (validationLevel == ValidationReportLevel.INLINE) {
            logger.warn("{}", issue);
        }
    }

    private void reportSessionCollectedValidationIssues(MavenSession mavenSession) {
        if (!logger.isWarnEnabled()) {
            return; // nothing can be reported
        }
        ValidationReportLevel validationLevel = validationLevel(mavenSession.getRepositorySession());
        if (validationLevel == ValidationReportLevel.NONE || validationLevel == ValidationReportLevel.INLINE) {
            return; // we were asked to not report anything OR reporting already happened inline
        }
        ConcurrentHashMap<String, PluginValidationIssues> issuesMap = pluginIssues(mavenSession.getRepositorySession());
        if (!issuesMap.isEmpty()) {

            logger.warn("");
            logger.warn("Plugin validation issues were detected in {} plugin(s)", issuesMap.size());
            logger.warn("");
            if (validationLevel == ValidationReportLevel.BRIEF) {
                return;
            }

            for (Map.Entry<String, PluginValidationIssues> entry : issuesMap.entrySet()) {
                logger.warn(" * {}", entry.getKey());
                if (validationLevel == ValidationReportLevel.VERBOSE) {
                    PluginValidationIssues issues = entry.getValue();
                    if (!issues.pluginDeclarations.isEmpty()) {
                        logger.warn("  Declared at location(s):");
                        for (String pluginDeclaration : issues.pluginDeclarations) {
                            logger.warn("   * {}", pluginDeclaration);
                        }
                    }
                    if (!issues.pluginOccurrences.isEmpty()) {
                        logger.warn("  Used in module(s):");
                        for (String pluginOccurrence : issues.pluginOccurrences) {
                            logger.warn("   * {}", pluginOccurrence);
                        }
                    }
                    if (!issues.pluginIssues.isEmpty()) {
                        logger.warn("  Plugin issue(s):");
                        for (String pluginIssue : issues.pluginIssues) {
                            logger.warn("   * {}", pluginIssue);
                        }
                    }
                    if (!issues.mojoIssues.isEmpty()) {
                        logger.warn("  Mojo issue(s):");
                        for (String mojoInfo : issues.mojoIssues.keySet()) {
                            logger.warn("   * Mojo {}", mojoInfo);
                            for (String mojoIssue : issues.mojoIssues.get(mojoInfo)) {
                                logger.warn("     - {}", mojoIssue);
                            }
                        }
                    }
                    logger.warn("");
                }
            }
            logger.warn("");
            if (validationLevel == ValidationReportLevel.VERBOSE) {
                logger.warn(
                        "Fix reported issues by adjusting plugin configuration or by upgrading above listed plugins. If no upgrade available, please notify plugin maintainers about reported issues.");
            }
            logger.warn(
                    "For more or less details, use 'maven.plugin.validation' property with one of the values (case insensitive): {}",
                    Arrays.toString(ValidationReportLevel.values()));
            logger.warn("");
        }
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
                    File rootBasedir = mavenSession.getTopLevelProject().getBasedir();
                    File locationFile = new File(location);
                    if (location.startsWith(rootBasedir.getPath())) {
                        stringBuilder
                                .append(" (")
                                .append(rootBasedir.toPath().relativize(locationFile.toPath()))
                                .append(")");
                    } else {
                        stringBuilder.append(" (").append(location).append(")");
                    }
                }
            }
            stringBuilder.append(" @ line ").append(inputLocation.getLineNumber());
            return stringBuilder.toString();
        } else {
            return "unknown";
        }
    }

    private String pluginOccurrence(MavenSession mavenSession) {
        MavenProject prj = mavenSession.getCurrentProject();
        String result = prj.getGroupId() + ":" + prj.getArtifactId() + ":" + prj.getVersion();
        File currentPom = prj.getFile();
        if (currentPom != null) {
            File rootBasedir = mavenSession.getTopLevelProject().getBasedir();
            result += " (" + rootBasedir.toPath().relativize(currentPom.toPath()) + ")";
        }
        return result;
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

        private final LinkedHashSet<String> pluginOccurrences;

        private final LinkedHashSet<String> pluginIssues;

        private final LinkedHashMap<String, LinkedHashSet<String>> mojoIssues;

        private PluginValidationIssues() {
            this.pluginDeclarations = new LinkedHashSet<>();
            this.pluginOccurrences = new LinkedHashSet<>();
            this.pluginIssues = new LinkedHashSet<>();
            this.mojoIssues = new LinkedHashMap<>();
        }

        private synchronized void reportPluginIssue(String pluginDeclaration, String pluginOccurrence, String issue) {
            if (pluginDeclaration != null) {
                pluginDeclarations.add(pluginDeclaration);
            }
            if (pluginOccurrence != null) {
                pluginOccurrences.add(pluginOccurrence);
            }
            pluginIssues.add(issue);
        }

        private synchronized void reportPluginMojoIssue(
                String pluginDeclaration, String pluginOccurrence, String mojoInfo, String issue) {
            if (pluginDeclaration != null) {
                pluginDeclarations.add(pluginDeclaration);
            }
            if (pluginOccurrence != null) {
                pluginOccurrences.add(pluginOccurrence);
            }
            mojoIssues.computeIfAbsent(mojoInfo, k -> new LinkedHashSet<>()).add(issue);
        }
    }
}
