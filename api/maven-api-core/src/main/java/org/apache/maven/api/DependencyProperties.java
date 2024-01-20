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
package org.apache.maven.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Dependency properties supported by Maven Core.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyProperties {
    /**
     * Keys in the dependency properties map.
     * Each key can be associated to values of a specific class.
     *
     * @param  <V>  type of value associated to the key
     */
    class Key<V> {
        /**
         * The keys that are defined in this {@code DependencyProperties} map.
         * Accesses to this map shall be synchronized on the map.
         *
         * @see #intern()
         */
        private static final ConcurrentMap<String, Key<?>> INTERNS = new ConcurrentHashMap<>();

        /**
         * Value returned by {@link #name()}.
         */
        @Nonnull
        private final String name;

        /**
         * Value returned by {@link #valueType()}.
         */
        @Nonnull
        private final Class<V> valueType;

        /**
         * Creates a new key.
         *
         * @param name name of the key
         * @param valueType type of value associated to the key
         */
        public Key(@Nonnull String name, @Nonnull Class<V> valueType) {
            this.name = Objects.requireNonNull(name);
            this.valueType = Objects.requireNonNull(valueType);
        }

        /**
         * If a key exists in the {@linkplain #intern() intern pool} for the given name, returns that key.
         *
         * @param name name of the key to search
         * @return key for the given name
         */
        public static Optional<Key<?>> forName(@Nonnull String name) {
            return Optional.ofNullable(INTERNS.get(name));
        }

        /**
         * Returns the name of the key.
         *
         * @return the name of the key
         */
        @Nonnull
        public String name() {
            return name;
        }

        /**
         * Returns the type of value associated to the key.
         *
         * @return the type of value associated to the key
         */
        @Nonnull
        public Class<V> valueType() {
            return valueType;
        }

        /**
         * Returns a canonical representation of this key. A pool of keys, initially empty, is maintained privately.
         * When the {@code intern()} method is invoked, if the pool already contains a key equal to this {@code Key}
         * as determined by the {@link #equals(Object)} method, then the key from the pool is returned. Otherwise,
         * if no key exist in the pool for this key {@linkplain #name() name}, then this {@code Key} object is added
         * to the pool and {@code this} is returned. Otherwise an {@link IllegalStateException} is thrown.
         *
         * @return a canonical representation of this key
         * @throws IllegalStateException if a key exists in the pool for the same name but a different class of values.
         *
         * @see String#intern()
         */
        @SuppressWarnings("unchecked")
        public Key<V> intern() {
            Key<?> previous = INTERNS.putIfAbsent(name, this);
            if (previous == null) {
                return this;
            }
            if (equals(previous)) {
                return (Key<V>) previous;
            }
            throw new IllegalStateException("Key " + name + " already exists for a different class of values.");
        }

        /**
         * Returns a string representation of this key.
         * By default, this is the name of this key.
         *
         * @return a string representation of this key
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns an hash code value for this key}.
         *
         * @return an hash code value for this key
         */
        @Override
        public int hashCode() {
            return 7 + name.hashCode() + 31 * valueType.hashCode();
        }

        /**
         * Compares this key with the given object for equality.
         * Two keys are considered equal if they have the same name
         * and are associated to values of the same class.
         *
         * @param obj the object to compare with this key
         * @return whether the given object is equal to this key
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj != null && getClass() == obj.getClass()) {
                final Key<?> other = (Key<?>) obj;
                return name.equals(other.name) && valueType.equals(other.valueType);
            }
            return false;
        }
    }

    /**
     * The dependency type. The {@linkplain Key#name() name} of this property
     * is equal to the {@code ArtifactProperties.TYPE} value.
     */
    Key<String> TYPE = new Key<>("type", String.class).intern();

    /**
     * The dependency language. The {@linkplain Key#name() name} of this property
     * is equal to the {@code ArtifactProperties.LANGUAGE} value.
     */
    Key<String> LANGUAGE = new Key<>("language", String.class).intern();

    /**
     * Types of path (class-path, module-path, …) where the dependency can be placed.
     * For most deterministic builds, the array length should be 1. In such case,
     * the dependency will be unconditionally placed on the specified type of path
     * and no heuristic rule will be involved.
     *
     * <p>It is nevertheless common to specify two or more types of path. For example,
     * a Java library may be compatible with either the class-path or the module-path,
     * and the user may have provided no instruction about which type to use. In such
     * case, the plugin may apply rules for choosing a path. See for example
     * {@link JavaPathType#CLASSES} and {@link JavaPathType#MODULES}.</p>
     */
    Key<PathType[]> PATH_TYPES = new Key<>("pathTypes", PathType[].class).intern();

    /**
     * Boolean flag telling that dependency contains all of its dependencies.
     * <p>
     * <em>Important: this flag must be kept in sync with resolver! (as is used during collection)</em>
     */
    Key<Boolean> FLAG_INCLUDES_DEPENDENCIES = new Key<>("includesDependencies", Boolean.class).intern();

    /**
     * Returns the keys of all properties in this map.
     *
     * @return the keys of all properties in this map
     */
    Set<Key<?>> keys();

    /**
     * Returns the value associated to the given key.
     *
     * @param <V> type of value to get
     * @param key key of the value to get
     * @return value associated to the given key
     */
    <V> Optional<V> get(@Nonnull Key<V> key);

    /**
     * Returns the value associated to the given key, or the given default value if none.
     *
     * @param <V> type of value to get
     * @param key key of the value to get
     * @param defaultValue the value to return is none is associated to the given key, or {@code null}
     * @return value associated to the given key, or {@code null} if none and the default is null
     */
    @Nullable
    <V> V getOrDefault(@Nonnull Key<V> key, @Nullable V defaultValue);

    /**
     * Returns {@code true} if given flag is {@code true}.
     * An absence of value is interpreted as {@code false}.
     *
     * @param flag the property to check
     * @return whether the value associated to the given key is non-null and true
     */
    default boolean checkFlag(@Nonnull Key<Boolean> flag) {
        return getOrDefault(flag, Boolean.FALSE);
    }

    /**
     * Returns an immutable "map view" of all the properties.
     * This method is provided for compatibility with API working with {@link String}.
     * The type-safe method expecting {@link Key} arguments should be preferred.
     *
     * @return an immutable "map view" of all the properties
     */
    @Nonnull
    Map<String, String> asMap();
}
