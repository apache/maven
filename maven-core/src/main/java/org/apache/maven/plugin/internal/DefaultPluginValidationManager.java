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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
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

    private static final String MAVEN_PLUGIN_VALIDATION_DEFAULT = "default";

    private enum ValidationLevel {
        DISABLED,
        DEFAULT,
        VERBOSE
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterSessionEnd(MavenSession session) {
        reportSessionCollectedValidationIssues(session);
    }

    private ValidationLevel validationLevel(RepositorySystemSession session) {
        String level = ConfigUtils.getString(session, MAVEN_PLUGIN_VALIDATION_DEFAULT, MAVEN_PLUGIN_VALIDATION_KEY);
        try {
            return ValidationLevel.valueOf(level.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return ValidationLevel.DEFAULT;
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
        if (validationLevel(session) == ValidationLevel.DISABLED) {
            return;
        }
        String pluginKey = pluginKey(pluginArtifact);
        PluginValidationIssues pluginIssues =
                pluginIssues(session).computeIfAbsent(pluginKey, k -> new PluginValidationIssues(pluginKey));
        pluginIssues.reportPluginIssue(null, null, issue);
    }

    @Override
    public void reportPluginValidationIssue(MavenSession mavenSession, MojoDescriptor mojoDescriptor, String issue) {
        if (validationLevel(mavenSession.getRepositorySession()) == ValidationLevel.DISABLED) {
            return;
        }
        String pluginKey = pluginKey(mojoDescriptor);
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues(pluginKey));
        pluginIssues.reportPluginIssue(pluginDeclaration(mojoDescriptor), pluginoccurrence(mavenSession), issue);
    }

    @Override
    public void reportPluginMojoValidationIssue(
            MavenSession mavenSession, MojoDescriptor mojoDescriptor, Class<?> mojoClass, String issue) {
        String pluginKey = pluginKey(mojoDescriptor);
        if (validationLevel(mavenSession.getRepositorySession()) == ValidationLevel.DISABLED) {
            return;
        }
        PluginValidationIssues pluginIssues = pluginIssues(mavenSession.getRepositorySession())
                .computeIfAbsent(pluginKey, k -> new PluginValidationIssues(pluginKey));
        pluginIssues.reportPluginMojoIssue(
                pluginDeclaration(mojoDescriptor),
                pluginoccurrence(mavenSession),
                mojoInfo(mojoDescriptor, mojoClass),
                issue);
    }

    public void reportSessionCollectedValidationIssues(MavenSession mavenSession) {
        ValidationLevel validationLevel = validationLevel(mavenSession.getRepositorySession());
        if (validationLevel == ValidationLevel.DISABLED || !logger.isWarnEnabled()) {
            return;
        }
        ConcurrentHashMap<String, PluginValidationIssues> issuesMap = pluginIssues(mavenSession.getRepositorySession());
        if (!issuesMap.isEmpty()) {
            logger.warn("");
            logger.warn("Plugin issues were detected in build:");
            logger.warn("");
            for (PluginValidationIssues issues : issuesMap.values()) {
                logger.warn("Plugin {}", issues.pluginKey);
                if (validationLevel == ValidationLevel.VERBOSE && !issues.pluginDeclarations.isEmpty()) {
                    logger.warn("  Declared at location(s):");
                    for (String pluginDeclaration : issues.pluginDeclarations) {
                        logger.warn("   * {}", pluginDeclaration);
                    }
                }
                if (validationLevel == ValidationLevel.VERBOSE && !issues.pluginOccurrences.isEmpty()) {
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
            logger.warn("");
            logger.warn(
                    "To fix these issues, please upgrade listed plugins, or notify their maintainers about these issues.");
            logger.warn("");
            logger.warn(
                    "To get more or less details, use 'maven.plugin.validation' user property with one of the values: 'disabled', 'verbose' or (implied) 'default'");
            logger.warn("");
        }
    }

    private String pluginDeclaration(MojoDescriptor mojoDescriptor) {
        InputLocation inputLocation =
                mojoDescriptor.getPluginDescriptor().getPlugin().getLocation("");
        if (inputLocation != null) {
            return inputLocation.toString();
        } else {
            return "unknown";
        }
    }

    private String pluginoccurrence(MavenSession mavenSession) {
        MavenProject prj = mavenSession.getCurrentProject();
        String result = prj.getName() + " " + prj.getVersion();
        File currentPom = prj.getFile();
        if (currentPom != null) {
            File rootBasedir = mavenSession.getTopLevelProject().getBasedir();
            result += " (from " + rootBasedir.toPath().relativize(currentPom.toPath()) + ")";
        }
        return result;
    }

    private String mojoInfo(MojoDescriptor mojoDescriptor, Class<?> mojoClass) {
        return mojoDescriptor.getFullGoalName() + " (class: " + mojoClass.getName() + ")";
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, PluginValidationIssues> pluginIssues(RepositorySystemSession session) {
        return (ConcurrentHashMap<String, PluginValidationIssues>)
                session.getData().computeIfAbsent(ISSUES_KEY, ConcurrentHashMap::new);
    }

    private static class PluginValidationIssues {
        private final String pluginKey;

        private final LinkedHashSet<String> pluginDeclarations;

        private final LinkedHashSet<String> pluginOccurrences;

        private final LinkedHashSet<String> pluginIssues;

        private final LinkedHashMap<String, LinkedHashSet<String>> mojoIssues;

        private PluginValidationIssues(String pluginKey) {
            this.pluginKey = pluginKey;
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
