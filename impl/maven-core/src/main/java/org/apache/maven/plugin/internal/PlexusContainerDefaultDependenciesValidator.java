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

import org.apache.maven.plugin.PluginValidationManager;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Detects Plexus Container Default in plugins.
 *
 * @since 3.9.2
 */
@Singleton
@Named
class PlexusContainerDefaultDependenciesValidator extends AbstractMavenPluginDependenciesValidator {

    @Inject
    PlexusContainerDefaultDependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    protected void doValidate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult) {
        boolean pcdPresent = artifactDescriptorResult.getDependencies().stream()
                .filter(d -> "org.codehaus.plexus".equals(d.getArtifact().getGroupId()))
                .anyMatch(d -> "plexus-container-default".equals(d.getArtifact().getArtifactId()));

        if (pcdPresent) {
            pluginValidationManager.reportPluginValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    session,
                    pluginArtifact,
                    "Plugin depends on plexus-container-default, which is EOL");
        }
    }
}
