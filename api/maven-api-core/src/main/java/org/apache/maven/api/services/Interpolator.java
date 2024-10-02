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
package org.apache.maven.api.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * The Interpolator service provides methods for variable substitution in strings and maps.
 * It allows for the replacement of placeholders (e.g., ${variable}) with their corresponding values.
 *
 * @since 4.0.0
 */
@Experimental
public interface Interpolator extends Service {

    /**
     * Interpolates the values in the given map using the provided callback function.
     * This method defaults to setting empty strings for unresolved placeholders.
     *
     * @param properties The map containing key-value pairs to be interpolated.
     * @param callback The function to resolve variable values not found in the map.
     */
    default void interpolate(@Nonnull Map<String, String> properties, @Nullable Function<String, String> callback) {
        interpolate(properties, callback, null, true);
    }

    /**
     * Interpolates the values in the given map using the provided callback function.
     *
     * @param map The map containing key-value pairs to be interpolated.
     * @param callback The function to resolve variable values not found in the map.
     * @param defaultsToEmpty If true, unresolved placeholders are replaced with empty strings. If false, they are left unchanged.
     */
    default void interpolate(
            @Nonnull Map<String, String> map, @Nullable Function<String, String> callback, boolean defaultsToEmpty) {
        interpolate(map, callback, null, defaultsToEmpty);
    }

    /**
     * Interpolates the values in the given map using the provided callback function.
     *
     * @param map The map containing key-value pairs to be interpolated.
     * @param callback The function to resolve variable values not found in the map.
     * @param defaultsToEmpty If true, unresolved placeholders are replaced with empty strings.  If false, they are left unchanged.
     */
    void interpolate(
            @Nonnull Map<String, String> map,
            @Nullable Function<String, String> callback,
            @Nullable BiFunction<String, String, String> postprocessor,
            boolean defaultsToEmpty);

    /**
     * Interpolates a single string value using the provided callback function.
     * This method defaults to not replacing unresolved placeholders.
     *
     * @param val The string to be interpolated.
     * @param callback The function to resolve variable values.
     * @return The interpolated string, or null if the input was null.
     */
    @Nullable
    default String interpolate(@Nullable String val, @Nullable Function<String, String> callback) {
        return interpolate(val, callback, false);
    }

    /**
     * Interpolates a single string value using the provided callback function.
     *
     * @param val The string to be interpolated.
     * @param callback The function to resolve variable values.
     * @param defaultsToEmpty If true, unresolved placeholders are replaced with empty strings.
     * @return The interpolated string, or null if the input was null.
     */
    @Nullable
    default String interpolate(
            @Nullable String val, @Nullable Function<String, String> callback, boolean defaultsToEmpty) {
        return interpolate(val, callback, null, defaultsToEmpty);
    }

    /**
     * Interpolates a single string value using the provided callback function.
     *
     * @param val The string to be interpolated.
     * @param callback The function to resolve variable values.
     * @param defaultsToEmpty If true, unresolved placeholders are replaced with empty strings.
     * @return The interpolated string, or null if the input was null.
     */
    @Nullable
    String interpolate(
            @Nullable String val,
            @Nullable Function<String, String> callback,
            @Nullable BiFunction<String, String, String> postprocessor,
            boolean defaultsToEmpty);

    /**
     * Creates a composite function from a collection of functions.
     *
     * @param functions A collection of functions, each taking a String as input and returning a String.
     * @return A function that applies each function in the collection in order until a non-null result is found.
     *         If all functions return null, the composite function returns null.
     *
     * @throws NullPointerException if the input collection is null or contains null elements.
     */
    static Function<String, String> chain(Collection<? extends Function<String, String>> functions) {
        return s -> {
            for (Function<String, String> function : functions) {
                String v = function.apply(s);
                if (v != null) {
                    return v;
                }
            }
            return null;
        };
    }

    /**
     * Memoizes a given function that takes a String input and produces a String output.
     * This method creates a new function that caches the results of the original function,
     * improving performance for repeated calls with the same input.
     *
     * @param callback The original function to be memoized. It takes a String as input and returns a String.
     * @return A new {@code Function<String, String>} that caches the results of the original function.
     *         If the original function returns null for a given input, null will be cached and returned for subsequent calls with the same input.
     *
     * @see Function
     * @see Optional
     * @see HashMap#computeIfAbsent(Object, Function)
     */
    static Function<String, String> memoize(Function<String, String> callback) {
        Map<String, Optional<String>> cache = new HashMap<>();
        return s -> cache.computeIfAbsent(s, v -> Optional.ofNullable(callback.apply(v)))
                .orElse(null);
    }
}
