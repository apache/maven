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
package org.apache.maven.internal.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.apache.maven.api.spi.CacheRecord;
import org.apache.maven.api.spi.MavenCache;

/**
 * A {@link MavenCache} backed by a {@link ConcurrentHashMap}.
 *
 * <p>This is the default implementation used by {@link DefaultCacheFactory}. It does
 * <em>not</em> perform any automatic invalidation; entries are only removed when
 * explicitly evicted via {@link #remove}, {@link #removeIf}, or {@link #clear}.
 * Consequently it is suitable for single-shot CLI builds where cache entries are
 * never stale within a single JVM lifetime.
 *
 * <p>All operations are thread-safe.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 *
 * @since 4.1.0
 */
public class DefaultMavenCache<K, V extends CacheRecord> implements MavenCache<K, V> {

    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public void remove(K key) {
        V removed = map.remove(key);
        if (removed != null) {
            removed.invalidate();
        }
    }

    @Override
    public void removeIf(BiPredicate<K, V> predicate) {
        map.entrySet().removeIf(e -> {
            if (predicate.test(e.getKey(), e.getValue())) {
                e.getValue().invalidate();
                return true;
            }
            return false;
        });
    }

    @Override
    public void clear() {
        // Snapshot the values before clearing so invalidate() is called on all of them
        List<V> snapshot = new ArrayList<>(map.values());
        map.clear();
        snapshot.forEach(CacheRecord::invalidate);
    }

    @Override
    public void clearSilently() {
        // Clear without calling invalidate() — for Plexus dispose() lifecycle where
        // the realm (or other resource) must not be closed here
        map.clear();
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(map.values());
    }
}
