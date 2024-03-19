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

import java.util.List;

import org.apache.maven.plugin.PluginValidationManager;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

import static java.util.Objects.requireNonNull;

/**
 * Detects Plexus Container Default in plugin dependencies.
 *
 * @since 3.9.2
 */
@Singleton
@Named
class PlexusContainerDefaultDependenciesValidator implements MavenPluginDependenciesValidator {

    private final PluginValidationManager pluginValidationManager;

    @Inject
    PlexusContainerDefaultDependenciesValidator(PluginValidationManager pluginValidationManager) {
        this.pluginValidationManager = requireNonNull(pluginValidationManager);
    }

    public void validate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult) {
        doValidate(session, pluginArtifact, artifactDescriptorResult.getDependencies(), true);
    }

    @Override
    public void validate(RepositorySystemSession session, Artifact pluginArtifact, List<Dependency> dependencies) {
        doValidate(session, pluginArtifact, dependencies, false);
    }

    private void doValidate(
            RepositorySystemSession session, Artifact pluginArtifact, List<Dependency> dependencies, boolean direct) {
        boolean pcdPresent = dependencies.stream()
                .filter(d -> "org.codehaus.plexus".equals(d.getArtifact().getGroupId()))
                .anyMatch(d -> "plexus-container-default".equals(d.getArtifact().getArtifactId()));

        if (pcdPresent) {
            pluginValidationManager.reportPluginValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    session,
                    pluginArtifact,
                    (direct ? "Direct" : "Transitive")
                            + " dependencies contain plexus-container-default, which is EOL");
        }
    }
}
