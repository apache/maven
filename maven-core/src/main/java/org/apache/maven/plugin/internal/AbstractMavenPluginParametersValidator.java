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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.PluginValidationManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import static java.util.Objects.requireNonNull;

/**
 * Common implementations for plugin parameters configuration validation.
 *
 * @author Slawomir Jaranowski
 */
abstract class AbstractMavenPluginParametersValidator implements MavenPluginConfigurationValidator {

    protected final PluginValidationManager pluginValidationManager;

    protected AbstractMavenPluginParametersValidator(PluginValidationManager pluginValidationManager) {
        this.pluginValidationManager = requireNonNull(pluginValidationManager);
    }

    protected boolean isValueSet(PlexusConfiguration config, ExpressionEvaluator expressionEvaluator) {
        if (config == null) {
            return false;
        }

        // there are sub items ... so configuration is declared
        if (config.getChildCount() > 0) {
            return true;
        }

        String strValue = config.getValue();

        if (strValue == null || strValue.isEmpty()) {
            return false;
        }

        if (isIgnoredProperty(strValue)) {
            return false;
        }

        // for declaration like @Parameter( property = "config.property" )
        // the value will contain ${config.property}

        try {
            return expressionEvaluator.evaluate(strValue) != null;
        } catch (ExpressionEvaluationException e) {
            // not important
            // will be reported during Mojo fields populate
        }

        // fallback - in case of error in expressionEvaluator
        return false;
    }

    @Override
    public final void validate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator) {
        doValidate(mavenSession, mojoDescriptor, mojoClass, pomConfiguration, expressionEvaluator);
    }

    protected abstract void doValidate(
            MavenSession mavenSession,
            MojoDescriptor mojoDescriptor,
            Class<?> mojoClass,
            PlexusConfiguration pomConfiguration,
            ExpressionEvaluator expressionEvaluator);

    protected boolean isIgnoredProperty(String strValue) {
        return false;
    }

    protected abstract String getParameterLogReason(Parameter parameter);

    protected String formatParameter(Parameter parameter) {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Parameter '")
                .append(parameter.getName())
                .append('\'');

        if (parameter.getExpression() != null) {
            String userProperty = parameter.getExpression().replace("${", "'").replace('}', '\'');
            stringBuilder.append(" (user property ").append(userProperty).append(")");
        }

        stringBuilder.append(" ").append(getParameterLogReason(parameter));

        return stringBuilder.toString();
    }
}
