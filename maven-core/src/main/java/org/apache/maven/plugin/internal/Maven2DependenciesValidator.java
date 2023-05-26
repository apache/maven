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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.PluginValidationManager;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Detects Maven2 plugins.
 *
 * @since 3.9.2
 */
@Singleton
@Named
class Maven2DependenciesValidator extends AbstractMavenPluginDependenciesValidator {

    @Inject
    Maven2DependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected void doValidate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult) {
        Set<String> maven2Versions = artifactDescriptorResult.getDependencies().stream()
                .map(Dependency::getArtifact)
                .filter(d -> "org.apache.maven".equals(d.getGroupId()))
                .filter(d -> !DefaultPluginValidationManager.EXPECTED_PROVIDED_SCOPE_EXCLUSIONS_GA.contains(
                        d.getGroupId() + ":" + d.getArtifactId()))
                .map(Artifact::getVersion)
                .filter(v -> v.startsWith("2."))
                .collect(Collectors.toSet());

        if (!maven2Versions.isEmpty()) {
            pluginValidationManager.reportPluginValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    session,
                    pluginArtifact,
                    "Plugin is a Maven 2.x plugin, which will be not supported in Maven 4.x");
        }
    }
}
