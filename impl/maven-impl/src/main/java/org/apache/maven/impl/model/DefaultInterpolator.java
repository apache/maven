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
package org.apache.maven.impl.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.utils.StringInterpolator;
import org.apache.maven.utils.StringInterpolatorException;

@Named
@Singleton
public class DefaultInterpolator implements Interpolator {

    @Override
    public void interpolate(
            Map<String, String> map,
            UnaryOperator<String> callback,
            BinaryOperator<String> postprocessor,
            boolean defaultsToEmpty) {
        Map<String, String> org = new HashMap<>(map);
        for (String name : map.keySet()) {
            map.compute(
                    name,
                    (k, value) -> interpolate(
                            value,
                            name,
                            null,
                            v -> {
                                String r = org.get(v);
                                if (r == null && callback != null) {
                                    r = callback.apply(v);
                                }
                                return r;
                            },
                            postprocessor,
                            defaultsToEmpty));
        }
    }

    @Override
    public String interpolate(
            String val, UnaryOperator<String> callback, BinaryOperator<String> postprocessor, boolean defaultsToEmpty) {
        return interpolate(val, null, null, callback, postprocessor, defaultsToEmpty);
    }

    @Nullable
    public String interpolate(
            @Nullable String val,
            @Nullable String currentKey,
            @Nullable Set<String> cycleMap,
            @Nullable UnaryOperator<String> callback,
            @Nullable BinaryOperator<String> postprocessor,
            boolean defaultsToEmpty) {
        return substVars(val, currentKey, cycleMap, null, callback, postprocessor, defaultsToEmpty);
    }

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     * @param callback Callback for substitution
     */
    public void performSubstitution(Map<String, String> properties, UnaryOperator<String> callback) {
        performSubstitution(properties, callback, true);
    }

    /**
     * Perform substitution on a property set
     *
     * @param properties the property set to perform substitution on
     * @param callback the callback to obtain substitution values
     * @param defaultsToEmptyString sets an empty string if a replacement value is not found, leaves intact otherwise
     */
    public void performSubstitution(
            Map<String, String> properties, UnaryOperator<String> callback, boolean defaultsToEmptyString) {
        Map<String, String> org = new HashMap<>(properties);
        for (String name : properties.keySet()) {
            properties.compute(
                    name, (k, value) -> substVars(value, name, null, org, callback, null, defaultsToEmptyString));
        }
    }

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * {@code ${&lt;prop-name&gt;}}, where {@code &lt;prop-name&gt;}
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from innermost to outermost. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws InterpolatorException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public String substVars(String val, String currentKey, Set<String> cycleMap, Map<String, String> configProps) {
        return substVars(val, currentKey, cycleMap, configProps, null);
    }

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * {@code ${&lt;prop-name&gt;}}, where {@code &lt;prop-name&gt;}
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from innermost to outermost. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @param callback the callback to obtain substitution values
     * @return The value of the specified string after system property substitution.
     * @throws InterpolatorException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public String substVars(
            String val,
            String currentKey,
            Set<String> cycleMap,
            Map<String, String> configProps,
            UnaryOperator<String> callback) {
        return substVars(val, currentKey, cycleMap, configProps, callback, null, false);
    }

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * {@code ${&lt;prop-name&gt;}}, where {@code &lt;prop-name&gt;}
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from innermost to outermost. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @param callback the callback to obtain substitution values
     * @param defaultsToEmptyString sets an empty string if a replacement value is not found, leaves intact otherwise
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
     **/
    public static String substVars(
            String val,
            String currentKey,
            Set<String> cycleMap,
            Map<String, String> configProps,
            UnaryOperator<String> callback,
            BinaryOperator<String> postprocessor,
            boolean defaultsToEmptyString) {
        try {
            return StringInterpolator.substVars(
                    val, currentKey, cycleMap, configProps, callback, postprocessor, defaultsToEmptyString);
        } catch (StringInterpolatorException e) {
            throw new InterpolatorException(e.getMessage(), e);
        }
    }

    /**
     * Escapes special characters to prevent interpolation.
     *
     * @param val the string to escape
     * @return the escaped string
     */
    @Nullable
    public static String escape(@Nullable String val) {
        return StringInterpolator.escape(val);
    }

    /**
     * Unescapes previously escaped characters.
     *
     * @param val the string to unescape
     * @return the unescaped string
     */
    @Nullable
    public static String unescape(@Nullable String val) {
        return StringInterpolator.unescape(val);
    }
}
