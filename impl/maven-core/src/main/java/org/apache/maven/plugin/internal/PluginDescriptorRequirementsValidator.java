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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Verify that plugin descriptor does not contain Plexus Component requirements.
 */
@Singleton
@Named
class PluginDescriptorRequirementsValidator implements MavenPluginConfigurationValidator {

    protected final PluginValidationManager pluginValidationManager;

    @Inject
    PluginDescriptorRequirementsValidator(PluginValidationManager pluginValidationManager) {
        this.pluginValidationManager = pluginValidationManager;
    }

    @Override
    public void validate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator) {
        if (!mojoDescriptor.getRequirements().isEmpty()) {
            pluginValidationManager.reportPluginMojoValidationIssue(
                    PluginValidationManager.IssueLocality.EXTERNAL,
                    mavenSession,
                    mojoDescriptor,
                    mojoClass,
                    "Plugin uses Plexus Component requirements (@Component annotation). "
                            + "Use Maven 4 Dependency Injection (for v4 plugins) or JSR 330 annotations (for v3 plugins) to inject dependencies instead.");
        }
    }
}
