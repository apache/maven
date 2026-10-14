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
package org.apache.maven.api.spi;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A generic, invalidation-aware cache used by Maven's internal component caches.
 *
 * <p>Instances are created by a {@link CacheFactory} so that embedders can plug in
 * custom invalidation strategies (e.g. file-watching in mvnd) without subclassing
 * every individual cache component.
 *
 * <p>All implementations must be <strong>thread-safe</strong>.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cache values; must implement {@link CacheRecord} so the
 *            cache can call {@link CacheRecord#invalidate()} on removal
 *
 * @since 4.1.0
 * @see CacheFactory
 * @see CacheRecord
 */
@Experimental
public interface MavenCache<K, V extends CacheRecord> {

    /**
     * Returns the value associated with {@code key}, or {@code null} if absent.
     *
     * @param key the lookup key, never {@code null}
     * @return the cached value, or {@code null}
     */
    @Nullable
    V get(@Nonnull K key);

    /**
     * Associates {@code value} with {@code key}, replacing any existing mapping.
     *
     * @param key   the key, never {@code null}
     * @param value the value to store, never {@code null}
     * @return the previously cached value for {@code key}, or {@code null}
     */
    @Nullable
    V put(@Nonnull K key, @Nonnull V value);

    /**
     * Returns the cached value for {@code key}, computing and storing it via
     * {@code mappingFunction} if absent.
     *
     * @param key             the key, never {@code null}
     * @param mappingFunction called when the key is absent; must not return {@code null}
     * @return the existing or newly computed value, never {@code null}
     */
    @Nonnull
    V computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> mappingFunction);

    /**
     * Removes the mapping for {@code key}, calling {@link CacheRecord#invalidate()} on
     * the removed entry if present.
     *
     * @param key the key to remove, never {@code null}
     */
    void remove(@Nonnull K key);

    /**
     * Removes all entries matching the predicate, calling {@link CacheRecord#invalidate()}
     * on each removed entry.
     *
     * @param predicate the filter; entries for which it returns {@code true} are removed
     */
    void removeIf(@Nonnull BiPredicate<K, V> predicate);

    /**
     * Removes all entries, calling {@link CacheRecord#invalidate()} on each one.
     */
    void clear();

    /**
     * Returns a snapshot of the values currently held in this cache.
     *
     * @return an unmodifiable collection of current values
     */
    @Nonnull
    Collection<V> values();

    /**
     * Removes all entries <em>without</em> calling {@link CacheRecord#invalidate()} on them.
     *
     * <p>Use this for lifecycle disposal (e.g. Plexus {@code Disposable.dispose()}) where
     * the entries themselves must not be invalidated because other components may still be
     * using them; their own lifecycle will clean them up independently.
     *
     * <p>The default implementation delegates to {@link #clear()} — override when the
     * invalidation side-effect (e.g. realm disposal) must be suppressed.
     */
    default void clearSilently() {
        clear();
    }

    /**
     * Returns the number of entries currently held in this cache.
     *
     * @return the entry count
     */
    default int size() {
        return values().size();
    }

    /**
     * Returns {@code true} if this cache contains no entries.
     *
     * @return {@code true} if empty
     */
    default boolean isEmpty() {
        return values().isEmpty();
    }
}
