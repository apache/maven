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
package org.apache.maven.configuration.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the runtime expression interpolation used before Maven configures a Mojo.
 */
public final class PluginConfigurationInterpolator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginConfigurationInterpolator.class);

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private PluginConfigurationInterpolator() {}

    /**
     * Pre-interpolates expressions that were not available during model interpolation.
     *
     * @param configuration the plugin configuration
     * @param evaluator the expression evaluator
     * @param session the Maven session for determining model-time properties
     * @return the interpolated configuration
     */
    public static PlexusConfiguration interpolateConfiguration(
            PlexusConfiguration configuration, ExpressionEvaluator evaluator, MavenSession session) {
        if (!(configuration instanceof XmlPlexusConfiguration xmlConfig)) {
            return configuration;
        }

        Set<String> modelTimePropertyNames = collectModelTimePropertyNames(session);
        XmlNode original = xmlConfig.toXmlNode();
        XmlNode interpolated = interpolateXmlNode(original, evaluator, modelTimePropertyNames);
        if (interpolated == original) {
            return configuration;
        }
        return new XmlPlexusConfiguration(interpolated);
    }

    private static Set<String> collectModelTimePropertyNames(MavenSession session) {
        Set<String> names = new HashSet<>();
        Properties userProperties = session.getUserProperties();
        if (userProperties != null) {
            names.addAll(userProperties.stringPropertyNames());
        }
        Properties systemProperties = session.getSystemProperties();
        if (systemProperties != null) {
            names.addAll(systemProperties.stringPropertyNames());
        }
        org.apache.maven.project.MavenProject project = session.getCurrentProject();
        if (project != null && project.getOriginalModel() != null) {
            Properties originalProperties = project.getOriginalModel().getProperties();
            if (originalProperties != null) {
                names.addAll(originalProperties.stringPropertyNames());
            }
        }
        return names;
    }

    private static boolean containsRuntimeExpression(String value, Set<String> modelTimePropertyNames) {
        Matcher matcher = EXPRESSION_PATTERN.matcher(value);
        while (matcher.find()) {
            if (!modelTimePropertyNames.contains(matcher.group(1))) {
                return true;
            }
        }
        return false;
    }

    private static XmlNode interpolateXmlNode(
            XmlNode node, ExpressionEvaluator evaluator, Set<String> modelTimePropertyNames) {
        boolean changed = false;

        String value = node.value();
        String newValue = value;
        if (value != null && value.contains("${") && containsRuntimeExpression(value, modelTimePropertyNames)) {
            try {
                Object evaluated = evaluator.evaluate(value);
                if (evaluated instanceof String evaluatedString && !evaluatedString.equals(value)) {
                    newValue = evaluatedString;
                    changed = true;
                }
            } catch (ExpressionEvaluationException e) {
                LOGGER.debug("Failed to interpolate configuration value '{}': {}", value, e.getMessage());
            }
        }

        Map<String, String> attributes = node.attributes();
        Map<String, String> newAttributes = attributes;
        String defaultValue = attributes.get("default-value");
        if (defaultValue != null
                && defaultValue.contains("${")
                && containsRuntimeExpression(defaultValue, modelTimePropertyNames)) {
            try {
                Object evaluated = evaluator.evaluate(defaultValue);
                if (evaluated instanceof String evaluatedString && !evaluatedString.equals(defaultValue)) {
                    newAttributes = new HashMap<>(attributes);
                    newAttributes.put("default-value", evaluatedString);
                    changed = true;
                }
            } catch (ExpressionEvaluationException e) {
                LOGGER.debug(
                        "Failed to interpolate configuration default-value '{}': {}", defaultValue, e.getMessage());
            }
        }

        List<XmlNode> children = node.children();
        List<XmlNode> newChildren = children;
        for (int i = 0; i < children.size(); i++) {
            XmlNode child = children.get(i);
            XmlNode newChild = interpolateXmlNode(child, evaluator, modelTimePropertyNames);
            if (newChild != child && newChildren == children) {
                newChildren = new ArrayList<>(children.size());
                for (int j = 0; j < i; j++) {
                    newChildren.add(children.get(j));
                }
                changed = true;
            }
            if (newChildren != children) {
                newChildren.add(newChild);
            }
        }

        if (!changed) {
            return node;
        }

        return XmlNode.newBuilder()
                .name(node.name())
                .value(newValue)
                .attributes(newAttributes)
                .children(newChildren)
                .namespaceUri(node.namespaceUri())
                .prefix(node.prefix())
                .inputLocation(node.inputLocation())
                .build();
    }
}
