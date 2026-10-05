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

import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.AbstractConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.TypeAwareExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Converter for String, CharSequence, StringBuilder, and StringBuffer that properly handles
 * empty configuration elements (e.g. {@code <setting></setting>} or {@code <setting/>}).
 */
class EnhancedStringConverter extends AbstractConfigurationConverter {

    @Override
    public boolean canConvert(Class<?> type) {
        return String.class.equals(type)
                || CharSequence.class.equals(type)
                || StringBuilder.class.equals(type)
                || StringBuffer.class.equals(type);
    }

    @Override
    public Object fromConfiguration(
            ConverterLookup lookup,
            PlexusConfiguration configuration,
            Class<?> type,
            Class<?> enclosingType,
            ClassLoader loader,
            ExpressionEvaluator evaluator,
            ConfigurationListener listener)
            throws ComponentConfigurationException {

        if (configuration.getChildCount() > 0) {
            throw new ComponentConfigurationException(
                    "Basic element '" + configuration.getName() + "' must not contain child elements");
        }

        String value = configuration.getValue();
        Object result = null;

        if (value != null && !value.isEmpty()) {
            try {
                if (evaluator instanceof TypeAwareExpressionEvaluator typeAware) {
                    result = typeAware.evaluate(value, type);
                } else if (evaluator != null) {
                    result = evaluator.evaluate(value);
                } else {
                    result = value;
                }
            } catch (ExpressionEvaluationException e) {
                throw new ComponentConfigurationException(
                        configuration,
                        String.format(
                                "Cannot evaluate expression '%s' for configuration entry '%s'",
                                value, configuration.getName()),
                        e);
            }
        } else if (value != null) {
            // Explicit empty content: <element></element>
            result = "";
        } else {
            // value == null: self-closing tag <element/>
            String defaultValue = configuration.getAttribute("default-value");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                try {
                    if (evaluator instanceof TypeAwareExpressionEvaluator typeAware) {
                        result = typeAware.evaluate(defaultValue, type);
                    } else if (evaluator != null) {
                        result = evaluator.evaluate(defaultValue);
                    } else {
                        result = defaultValue;
                    }
                } catch (ExpressionEvaluationException e) {
                    throw new ComponentConfigurationException(
                            configuration,
                            String.format(
                                    "Cannot evaluate expression '%s' for configuration entry '%s'",
                                    defaultValue, configuration.getName()),
                            e);
                }
            } else {
                result = "";
            }
        }

        if (result == null) {
            result = "";
        }

        String str = result.toString();
        if (StringBuilder.class.equals(type)) {
            return new StringBuilder(str);
        } else if (StringBuffer.class.equals(type)) {
            return new StringBuffer(str);
        }
        return str;
    }
}
