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

import org.apache.maven.plugin.PluginValidationManager;

/**
 * Common implementations for plugin parameters configuration validation that relies on Mojo descriptor (leaves out
 * core parameters by default).
 *
 * @author Slawomir Jaranowski
 */
abstract class AbstractMavenPluginDescriptorSourcedParametersValidator extends AbstractMavenPluginParametersValidator {

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
            Arrays.asList("mojo.", "pom.", "plugin.", "project.", "session.", "settings.");

    protected AbstractMavenPluginDescriptorSourcedParametersValidator(PluginValidationManager pluginValidationManager) {
        super(pluginValidationManager);
    }

    @Override
    protected boolean isIgnoredProperty(String strValue) {
        if (!strValue.startsWith("${")) {
            return false;
        }

        String propertyName = strValue.replace("${", "").replace("}", "");

        if (IGNORED_PROPERTY_VALUES.contains(propertyName)) {
            return true;
        }

        return IGNORED_PROPERTY_PREFIX.stream().anyMatch(propertyName::startsWith);
    }
}
