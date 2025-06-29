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
package org.apache.maven.impl.cache;

import java.util.function.Function;

/**
 * A simple cache interface for storing key-value pairs with automatic cleanup
 * of garbage-collected entries.
 *
 * <p>This cache uses soft references to allow garbage collection under memory
 * pressure while providing memory benefits for typical usage patterns.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 * @since 4.0.0
 */
public interface Cache<K, V> {

    /**
     * Returns the value to which the specified key is mapped, or computes and
     * caches the value using the provided mapping function if no mapping exists.
     *
     * <p>This method is thread-safe and ensures that the mapping function is
     * called at most once per key, even under concurrent access.
     *
     * @param key the key whose associated value is to be returned
     * @param mappingFunction the function to compute a value if none exists
     * @return the current (existing or computed) value associated with the key,
     *         or null if the computed value is null
     * @throws NullPointerException if the key or mappingFunction is null
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

    /**
     * Returns the value to which the specified key is mapped, or null if no
     * mapping exists or the value has been garbage collected.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the key is mapped, or null
     */
    V get(K key);

    /**
     * Associates the specified value with the specified key in this cache.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the key, or null if there was
     *         no mapping for the key or the previous value was garbage collected
     * @throws NullPointerException if the key or value is null
     */
    V put(K key, V value);

    /**
     * Removes the mapping for the specified key from this cache if present.
     *
     * @param key the key whose mapping is to be removed from the cache
     * @return the previous value associated with the key, or null if there was
     *         no mapping for the key
     */
    V remove(K key);

    /**
     * Removes all mappings from this cache.
     */
    void clear();

    /**
     * Returns the approximate number of key-value mappings in this cache.
     * This count may include mappings whose values have been garbage collected
     * but not yet cleaned up.
     *
     * @return the approximate number of key-value mappings in this cache
     */
    int size();

    /**
     * Returns true if this cache contains no key-value mappings.
     *
     * @return true if this cache contains no key-value mappings
     */
    boolean isEmpty();

    /**
     * Creates a new cache instance with default configuration.
     * The cache uses soft references and identity-based key comparison.
     *
     * @param <K> the type of keys maintained by the cache
     * @param <V> the type of cached values
     * @return a new cache instance
     */
    static <K, V> Cache<K, V> newCache() {
        return new DefaultCache<>();
    }
}
