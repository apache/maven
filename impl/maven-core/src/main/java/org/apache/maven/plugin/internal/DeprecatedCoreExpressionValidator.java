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

import java.util.HashMap;
import java.util.Objects;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Print warnings if deprecated core parameters are used in mojo.
 *
 * @since 3.9.1
 */
@Singleton
@Named
class DeprecatedCoreExpressionValidator extends AbstractMavenPluginParametersValidator {
    private static final HashMap<String, String> DEPRECATED_CORE_PARAMETERS;

    private static final String ARTIFACT_REPOSITORY_REASON =
            "ArtifactRepository type is deprecated and its use in Mojos should be avoided.";

    static {
        HashMap<String, String> deprecatedCoreParameters = new HashMap<>();
        deprecatedCoreParameters.put("${localRepository}", ARTIFACT_REPOSITORY_REASON);
        deprecatedCoreParameters.put("${session.localRepository}", ARTIFACT_REPOSITORY_REASON);
        DEPRECATED_CORE_PARAMETERS = deprecatedCoreParameters;
    }

    @Inject
    DeprecatedCoreExpressionValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected String getParameterLogReason(Parameter parameter) {
        return "uses deprecated parameter expression '" + parameter.getDefaultValue() + "': "
                + DEPRECATED_CORE_PARAMETERS.get(parameter.getDefaultValue());
    }

    @Override
    protected void doValidate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator) {
        if (mojoDescriptor.getParameters() == null) {
            return;
        }

        mojoDescriptor.getParameters().stream()
                .filter(this::isDeprecated)
                .map(this::formatParameter)
                .forEach(m -> pluginValidationManager.reportPluginMojoValidationIssue(
                        PluginValidationManager.IssueLocality.EXTERNAL, mavenSession, mojoDescriptor, mojoClass, m));
    }

    private boolean isDeprecated(Parameter parameter) {
        return Objects.equals(
                        org.apache.maven.artifact.repository.ArtifactRepository.class.getName(), parameter.getType())
                && DEPRECATED_CORE_PARAMETERS.containsKey(parameter.getDefaultValue());
    }
}
