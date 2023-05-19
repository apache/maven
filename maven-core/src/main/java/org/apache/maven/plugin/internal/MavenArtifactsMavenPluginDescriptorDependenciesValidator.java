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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;

/**
 * Detects presence of unwanted Maven3 artifacts in plugin descriptor, possibly caused by multitude of reasons, among
 * them is "wrong scope" dependency declaration as well.
 * <p>
 * Historically, this class was named as "MavenScopeDependenciesValidator" due original intent to check "wrong Maven
 * Artifact scopes". Since then, it turned out that the values validated (the plugin descriptor dependencies, that is
 * produced at plugin build time by maven-plugin-plugin) may be off (for example due maven-plugin-plugin bug), and
 * is potentially not inline with "reality" (actual plugin dependencies).
 * <p>
 * The original intent related check is moved to
 * {@link DefaultPluginDependenciesResolver#resolve(org.apache.maven.model.Plugin, java.util.List, org.eclipse.aether.RepositorySystemSession)}
 * method instead.
 *
 * @since 3.9.3
 */
@Singleton
@Named
class MavenArtifactsMavenPluginDescriptorDependenciesValidator
        extends AbstractMavenPluginDescriptorDependenciesValidator {

    @Inject
    MavenArtifactsMavenPluginDescriptorDependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected void doValidate(MavenSession mavenSession, MojoDescriptor mojoDescriptor) {
        Set<String> mavenArtifacts = mojoDescriptor.getPluginDescriptor().getDependencies().stream()
                .filter(d -> "org.apache.maven".equals(d.getGroupId()))
                .filter(d -> !DefaultPluginValidationManager.EXPECTED_PROVIDED_SCOPE_EXCLUSIONS_GA.contains(
                        d.getGroupId() + ":" + d.getArtifactId()))
                .filter(d -> d.getVersion().startsWith("3."))
                .map(d -> d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion())
                .collect(Collectors.toSet());

        if (!mavenArtifacts.isEmpty()) {
            pluginValidationManager.reportPluginValidationIssue(
                    mavenSession,
                    mojoDescriptor,
                    "Plugin descriptor should not contain these Maven artifacts: " + mavenArtifacts);
        }
    }
}
