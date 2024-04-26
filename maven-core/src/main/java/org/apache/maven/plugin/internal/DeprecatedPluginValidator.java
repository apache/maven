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

import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Print warnings if deprecated mojo or parameters of plugin are used in configuration.
 *
 */
@Singleton
@Named
class DeprecatedPluginValidator extends AbstractMavenPluginDescriptorSourcedParametersValidator {

    private final MessageBuilderFactory messageBuilderFactory;

    @Inject
    DeprecatedPluginValidator(
            PluginValidationManager pluginValidationManager, MessageBuilderFactory messageBuilderFactory) {
        super(pluginValidationManager);
        this.messageBuilderFactory = messageBuilderFactory;
    }

    @Override
    protected String getParameterLogReason(Parameter parameter) {
        return "is deprecated: " + parameter.getDeprecated();
    }

    @Override
    protected void doValidate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator) {
        if (mojoDescriptor.getDeprecated() != null) {
            pluginValidationManager.reportPluginMojoValidationIssue(
                    PluginValidationManager.IssueLocality.INTERNAL,
                    mavenSession,
                    mojoDescriptor,
                    mojoClass,
                    logDeprecatedMojo(mojoDescriptor));
        }

        if (mojoDescriptor.getParameters() != null) {
            mojoDescriptor.getParameters().stream()
                    .filter(parameter -> parameter.getDeprecated() != null)
                    .filter(Parameter::isEditable)
                    .forEach(parameter -> checkParameter(
                            mavenSession, mojoDescriptor, mojoClass, parameter, pomConfiguration, expressionEvaluator));
        }
    }

    private void checkParameter(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            Parameter parameter,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator) {
        PlexusConfiguration config = pomConfiguration.getChild(parameter.getName(), false);

        if (isValueSet(config, expressionEvaluator)) {
            pluginValidationManager.reportPluginMojoValidationIssue(
                    PluginValidationManager.IssueLocality.INTERNAL,
                    mavenSession,
                    mojoDescriptor,
                    mojoClass,
                    formatParameter(parameter));
        }
    }

    private String logDeprecatedMojo(MojoDescriptor mojoDescriptor) {
        return messageBuilderFactory
                .builder()
                .warning("Goal '")
                .warning(mojoDescriptor.getGoal())
                .warning("' is deprecated: ")
                .warning(mojoDescriptor.getDeprecated())
                .toString();
    }
}
