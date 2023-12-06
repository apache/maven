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
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Detects Maven3 dependencies scope.
 *
 * @since 3.9.3
 */
@Singleton
@Named
class MavenScopeDependenciesValidator extends AbstractMavenPluginDependenciesValidator {

    @Inject
    MavenScopeDependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected void doValidate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult) {
        Set<String> mavenArtifacts = artifactDescriptorResult.getDependencies().stream()
                .filter(d -> !JavaScopes.PROVIDED.equals(d.getScope()) && !JavaScopes.TEST.equals(d.getScope()))
                .map(org.eclipse.aether.graph.Dependency::getArtifact)
                .filter(a -> "org.apache.maven".equals(a.getGroupId()))
                .filter(a -> !DefaultPluginValidationManager.EXPECTED_PROVIDED_SCOPE_EXCLUSIONS_GA.contains(
                        a.getGroupId() + ":" + a.getArtifactId()))
                .filter(a -> a.getVersion().startsWith("3."))
                .map(a -> a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getVersion())
                .collect(Collectors.toSet());

        if (!mavenArtifacts.isEmpty()) {
            pluginValidationManager.reportPluginValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    session,
                    pluginArtifact,
                    "Plugin should declare Maven artifacts in `provided` scope. If the plugin already declares them in `provided` scope, update the maven-plugin-plugin to latest version. Artifacts found with wrong scope: "
                            + mavenArtifacts);
        }
    }
}
