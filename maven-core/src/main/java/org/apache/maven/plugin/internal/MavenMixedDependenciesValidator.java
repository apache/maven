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
import org.codehaus.plexus.component.repository.ComponentDependency;

/**
 * Detects mixed Maven versions in plugins.
 *
 * @since 3.9.2
 */
@Singleton
@Named
class MavenMixedDependenciesValidator extends AbstractMavenPluginDependenciesValidator {

    @Inject
    MavenMixedDependenciesValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected void doValidate(MavenSession mavenSession, MojoDescriptor mojoDescriptor) {
        Set<String> mavenVersions = mojoDescriptor.getPluginDescriptor().getDependencies().stream()
                .filter(d -> "org.apache.maven".equals(d.getGroupId()))
                .filter(d -> !"maven-archiver".equals(d.getArtifactId()))
                .map(ComponentDependency::getVersion)
                .collect(Collectors.toSet());

        if (mavenVersions.size() > 1) {
            pluginValidationManager.reportPluginValidationIssue(
                    mavenSession, mojoDescriptor, "Plugin mixes multiple Maven versions: " + mavenVersions);
        }
    }
}
