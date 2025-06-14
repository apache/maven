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
package org.apache.maven.api.model;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * A concurrent map implementation that uses soft references for both keys and values,
 * and supports automatic cleanup of garbage-collected entries.
 * <p>
 * This map is designed for caching scenarios where:
 * <ul>
 *   <li>Values should be eligible for garbage collection when memory is low</li>
 *   <li>Concurrent access is required</li>
 *   <li>Automatic cleanup of stale entries is desired</li>
 * </ul>
 * <p>
 * The map uses soft references for both keys and values, which means they will be garbage collected
 * before an OutOfMemoryError is thrown, making this suitable for memory-sensitive caches.
 * <p>
 * Note: This implementation is thread-safe and optimized for concurrent read access.
 * Write operations may block briefly during cleanup operations.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
class SoftConcurrentMap<K, V> implements Map<K, V> {

    private final ReferenceQueue<K> keyQueue = new ReferenceQueue<>();
    private final ReferenceQueue<V> valueQueue = new ReferenceQueue<>();
    private final ConcurrentHashMap<SoftConcurrentReference<K>, ComputeReference<V>> map = new ConcurrentHashMap<>();

    // Eviction statistics
    private final AtomicLong keyEvictions = new AtomicLong(0);
    private final AtomicLong valueEvictions = new AtomicLong(0);

    private static class SoftConcurrentReference<T> extends SoftReference<T> {
        private final int hash;

        SoftConcurrentReference(T referent, ReferenceQueue<T> queue) {
            super(referent, queue);
            this.hash = referent.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SoftConcurrentReference<?> other)) {
                return false;
            }
            T thisRef = this.get();
            Object otherRef = other.get();
            // Use equals() for proper object comparison
            return thisRef != null && thisRef.equals(otherRef);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class ComputeReference<V> extends SoftReference<V> {
        private final boolean computing;

        ComputeReference(V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.computing = false;
        }

        private ComputeReference(ReferenceQueue<V> queue) {
            super(null, queue);
            this.computing = true;
        }

        static <V> ComputeReference<V> computing(ReferenceQueue<V> queue) {
            return new ComputeReference<>(queue);
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(mappingFunction);

        while (true) {
            expungeStaleEntries();

            SoftConcurrentReference<K> softKey = new SoftConcurrentReference<>(key, keyQueue);

            // Try to get existing value
            ComputeReference<V> valueRef = map.get(softKey);
            if (valueRef != null && !valueRef.computing) {
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd, remove it
                map.remove(softKey, valueRef);
            }

            // Try to claim computation
            ComputeReference<V> computingRef = ComputeReference.computing(valueQueue);
            valueRef = map.putIfAbsent(softKey, computingRef);

            if (valueRef == null) {
                // We claimed the computation
                try {
                    V newValue = mappingFunction.apply(key);
                    if (newValue == null) {
                        map.remove(softKey, computingRef);
                        return null;
                    }

                    ComputeReference<V> newValueRef = new ComputeReference<>(newValue, valueQueue);
                    map.replace(softKey, computingRef, newValueRef);
                    return newValue;
                } catch (Throwable t) {
                    map.remove(softKey, computingRef);
                    throw t;
                }
            } else if (!valueRef.computing) {
                // Another thread has a value
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd
                if (map.remove(softKey, valueRef)) {
                    continue;
                }
            }
            // Another thread is computing or the reference changed, try again
        }
    }

    private void expungeStaleEntries() {
        // Remove entries where the key has been garbage collected
        Reference<?> ref;
        while ((ref = keyQueue.poll()) != null) {
            // The ref is a SoftConcurrentReference that was used as a key
            if (map.remove(ref) != null) {
                keyEvictions.incrementAndGet();
            }
        }
        // Remove entries where the value has been garbage collected
        while ((ref = valueQueue.poll()) != null) {
            // The ref is a ComputeReference that was used as a value
            // We need to find and remove the map entry that has this value
            final Reference<?> valueRef = ref;
            boolean removed = map.entrySet().removeIf(entry -> entry.getValue() == valueRef);
            if (removed) {
                valueEvictions.incrementAndGet();
            }
        }
    }

    @Override
    public int size() {
        expungeStaleEntries();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        expungeStaleEntries();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        expungeStaleEntries();
        return map.containsKey(new SoftConcurrentReference<>((K) key, null));
    }

    @Override
    public boolean containsValue(Object value) {
        expungeStaleEntries();
        for (Reference<V> ref : map.values()) {
            V v = ref.get();
            if (v != null && Objects.equals(v, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        expungeStaleEntries();
        Reference<V> ref = map.get(new SoftConcurrentReference<>((K) key, null));
        return ref != null ? ref.get() : null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        expungeStaleEntries();

        Reference<V> oldValueRef =
                map.put(new SoftConcurrentReference<>(key, keyQueue), new ComputeReference<>(value, valueQueue));

        return oldValueRef != null ? oldValueRef.get() : null;
    }

    @Override
    public V remove(Object key) {
        expungeStaleEntries();
        Reference<V> valueRef = map.remove(new SoftConcurrentReference<>((K) key, null));
        return valueRef != null ? valueRef.get() : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        Objects.requireNonNull(m);
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
        expungeStaleEntries();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("keySet not supported");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("values not supported");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("entrySet not supported");
    }

    /**
     * Returns the number of entries evicted due to key garbage collection.
     */
    long getKeyEvictions() {
        return keyEvictions.get();
    }

    /**
     * Returns the number of entries evicted due to value garbage collection.
     */
    long getValueEvictions() {
        return valueEvictions.get();
    }

    /**
     * Returns the total number of evictions (keys + values).
     */
    long getTotalEvictions() {
        return keyEvictions.get() + valueEvictions.get();
    }
}
