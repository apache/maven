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
import org.eclipse.aether.util.artifact.JavaScopes;

/**
 * Detects Maven3 plugins using maven-compat Maven2 compatibility layer.
 *
 * @since 3.9.3
 */
@Singleton
@Named
class Maven3CompatDependenciesValidator extends AbstractMavenPluginDependenciesValidator {

    @Inject
    Maven3CompatDependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected void doValidate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult) {
        for (org.eclipse.aether.graph.Dependency dependency : artifactDescriptorResult.getDependencies()) {
            if ("org.apache.maven".equals(dependency.getArtifact().getGroupId())
                    && "maven-compat".equals(dependency.getArtifact().getArtifactId())
                    && !JavaScopes.TEST.equals(dependency.getScope())) {
                pluginValidationManager.reportPluginValidationIssue(
                        PluginValidationManager.IssueLocality.EXTERNAL,
                        session,
                        pluginArtifact,
                        "Plugin depends on the deprecated Maven 2.x compatibility layer, which will be not supported in Maven 4.x");
            }
        }
    }
}
