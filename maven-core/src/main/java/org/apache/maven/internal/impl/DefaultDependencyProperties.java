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
package org.apache.maven.internal.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.api.DependencyProperties;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.internal.impl.Utils.nonNull;

/**
 * Default implementation of artifact properties.
 */
public class DefaultDependencyProperties implements DependencyProperties {
    /**
     * The property values. The type of values is determined by the key.
     */
    @Nonnull
    private final Map<Key<?>, Object> properties;

    /**
     * A builder of dependency properties.
     * Each builder can be used only once.
     */
    public static class Builder {
        /**
         * The property values to be assigned to {@link DefaultDependencyProperties} on completion.
         * This is set to {@code null} after the dependency properties have been built, for preventing changes.
         */
        private Map<Key<?>, Object> properties;

        /**
         * Creates an initially empty builder.
         */
        public Builder() {
            properties = new HashMap<>();
        }

        /**
         * Associates the given value to the given key.
         * If a value was already defined for the given key, the new value replaces it.
         *
         * <h4>Implementation note</h4>
         * All setter methods in {@code Builder} ultimately delegate to this method.
         * This architecture provides a single point that subclasses can override.
         *
         * @param <V> type of value to add
         * @param key key of the value to add
         * @param value the value to add
         * @return {@code this} for method call chaining
         * @throws IllegalStateException if the dependency properties have already been built
         */
        @Nonnull
        public <V> Builder set(@Nonnull Key<V> key, @Nonnull V value) {
            if (properties == null) {
                throw new IllegalStateException("Already built.");
            }
            properties.put(nonNull(key, "key"), nonNull(value, "value"));
            return this;
        }

        /**
         * Checks the type of the given value, then associates to the given key.
         *
         * @param <V> type of value to add
         * @param key key of the value to add
         * @param value the value to add
         * @return {@code this} for method call chaining
         * @throws ClassCastException if the given value is not of type {@code <V>}
         * @throws IllegalStateException if the dependency properties have already been built
         */
        public <V> Builder checkAndSet(@Nonnull Key<V> key, @Nonnull Object value) {
            return set(key, key.valueType().cast(value));
        }

        /**
         * Associates the value {@code Boolean.TRUE} to the given key.
         *
         * @param key key of the flag to set
         * @return {@code this} for method call chaining
         * @throws IllegalStateException if the dependency properties have already been built
         */
        @Nonnull
        public Builder setFlag(@Nonnull Key<Boolean> key) {
            return set(key, Boolean.TRUE);
        }

        /**
         * Copies all values from the given dependency properties.
         *
         * @param source the dependency properties to copy
         * @return {@code this} for method call chaining
         * @throws IllegalStateException if the dependency properties have already been built
         */
        public Builder setAll(@Nonnull DependencyProperties source) {
            source.keys().forEach((key) -> checkAndSet(key, source.get(key)));
            return this;
        }

        /**
         * {@return the dependency properties with the values previously set}.
         * This method  can be invoked only once.
         */
        @Nonnull
        public DefaultDependencyProperties build() {
            Map<Key<?>, Object> map = properties;
            if (map != null) {
                properties = null;
                return new DefaultDependencyProperties(map);
            }
            throw new IllegalStateException("Already built.");
        }
    }

    /**
     * Creates dependency properties with the given map. The caller is responsible to ensure
     * that all keys are associated to a value of the type specified by {@link Key#valueType()}.
     * For safe construction, use the {@link Builder} instead.
     *
     * @param properties the map which will be backing the dependency properties
     */
    protected DefaultDependencyProperties(@Nonnull final Map<Key<?>, Object> properties) {
        this.properties = nonNull(properties, "properties");
    }

    /**
     * Creates new dependency properties with the given flag sets to {@code true}.
     *
     * @param flags the flags to set to {@code true}
     */
    @SafeVarargs
    public DefaultDependencyProperties(@Nonnull Key<Boolean>... flags) {
        int count = nonNull(flags, "flags").length;
        properties = new HashMap<>(count + count / 3);
        for (Key<Boolean> flag : flags) {
            properties.put(flag, Boolean.TRUE);
        }
    }

    /**
     * {@return the keys of all properties in this map}.
     */
    @Override
    public Set<Key<?>> keys() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    /**
     * Returns the value associated to the given key.
     *
     * @param <V> type of value to get
     * @param key key of the value to get
     * @return value associated to the given key, or {@code null} if none
     */
    @Override
    public <V> V get(Key<V> key) {
        return key.valueType().cast(properties.get(key));
    }

    /**
     * Returns the value associated to the given key, or the given default value if none.
     *
     * @param <V> type of value to get
     * @param key key of the value to get
     * @param defaultValue the value to return is none is associated to the given key, or {@code null}
     * @return value associated to the given key, or {@code null} if none and the default is null
     */
    @Override
    public <V> V getOrDefault(Key<V> key, V defaultValue) {
        return key.valueType().cast(properties.getOrDefault(key, defaultValue));
    }

    private transient Map<String, String> mapView;

    /**
     * {@return an immutable "map view" of all the properties}.
     * This is computed when first needed
     */
    @Nonnull
    @Override
    public synchronized Map<String, String> asMap() {
        if (mapView == null) {
            int count = properties.size();
            Map<String, String> m = new HashMap<>(count + count / 3);
            for (Map.Entry<Key<?>, Object> entry : properties.entrySet()) {
                m.put(entry.getKey().name(), entry.getValue().toString());
            }
            mapView = Collections.unmodifiableMap(m);
        }
        return mapView;
    }

    /**
     * {@return a string representation of the key-value mapping}.
     */
    @Override
    public String toString() {
        return properties.toString();
    }
}
