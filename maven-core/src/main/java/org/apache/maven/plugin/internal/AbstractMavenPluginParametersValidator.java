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

import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.slf4j.Logger;

/**
 * Common implementations for plugin parameters configuration validation.
 *
 * @author Slawomir Jaranowski
 */
abstract class AbstractMavenPluginParametersValidator implements MavenPluginConfigurationValidator {

    // plugin author can provide @Parameter( property = "session" ) in this case property will always evaluate
    // so, we need ignore those

    // source org.apache.maven.plugin.PluginParameterExpressionEvaluator
    private static final List<String> IGNORED_PROPERTY_VALUES = Arrays.asList(
            "basedir",
            "executedProject",
            "localRepository",
            "mojo",
            "mojoExecution",
            "plugin",
            "project",
            "reactorProjects",
            "session",
            "settings");

    private static final List<String> IGNORED_PROPERTY_PREFIX =
            Arrays.asList("mojo.", "plugin.", "project.", "session.", "settings.");

    protected abstract Logger getLogger();

    protected static boolean isValueSet(PlexusConfiguration config, ExpressionEvaluator expressionEvaluator) {
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

    private static boolean isIgnoredProperty(String strValue) {
        if (!strValue.startsWith("${")) {
            return false;
        }

        String propertyName = strValue.replace("${", "").replace("}", "");

        if (IGNORED_PROPERTY_VALUES.contains(propertyName)) {
            return true;
        }

        return IGNORED_PROPERTY_PREFIX.stream().anyMatch(propertyName::startsWith);
    }

    protected abstract String getParameterLogReason(Parameter parameter);

    protected void logParameter(Parameter parameter) {
        MessageBuilder messageBuilder = MessageUtils.buffer()
                .warning("Parameter '")
                .warning(parameter.getName())
                .warning('\'');

        if (parameter.getExpression() != null) {
            String userProperty = parameter.getExpression().replace("${", "'").replace('}', '\'');
            messageBuilder.warning(" (user property ").warning(userProperty).warning(")");
        }

        messageBuilder.warning(" ").warning(getParameterLogReason(parameter));

        getLogger().warn(messageBuilder.toString());
    }
}
