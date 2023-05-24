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
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Print warnings if read-only parameters of a plugin are used in configuration.
 *
 * @author Slawomir Jaranowski
 */
@Named
@Singleton
class ReadOnlyPluginParametersValidator extends AbstractMavenPluginDescriptorSourcedParametersValidator {

    @Inject
    ReadOnlyPluginParametersValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected String getParameterLogReason(Parameter parameter) {
        return "is read-only, must not be used in configuration";
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
                .filter(parameter -> !parameter.isEditable())
                .forEach(parameter -> checkParameter(
                        mavenSession, mojoDescriptor, mojoClass, parameter, pomConfiguration, expressionEvaluator));
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
}
