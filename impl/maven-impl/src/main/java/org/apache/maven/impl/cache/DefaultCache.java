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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Default implementation of Cache that uses soft references for both keys and values,
 * with identity-based key comparison for better performance.
 *
 * <p>This implementation is thread-safe and automatically cleans up garbage-collected
 * entries. It uses soft references to allow garbage collection under memory pressure
 * while providing memory benefits for typical usage patterns.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
class DefaultCache<K, V> implements Cache<K, V> {

    private final ReferenceQueue<K> keyQueue = new ReferenceQueue<>();
    private final ReferenceQueue<V> valueQueue = new ReferenceQueue<>();
    private final ConcurrentMap<KeyReference<K>, ValueReference<V>> map = new ConcurrentHashMap<>();

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(mappingFunction, "mappingFunction cannot be null");

        while (true) {
            expungeStaleEntries();

            KeyReference<K> keyRef = new KeyReference<>(key, keyQueue);

            // Try to get existing value
            ValueReference<V> valueRef = map.get(keyRef);
            if (valueRef != null && !valueRef.isComputing()) {
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd, remove it
                map.remove(keyRef, valueRef);
            }

            // Try to claim computation
            ValueReference<V> computingRef = ValueReference.computing(valueQueue);
            valueRef = map.putIfAbsent(keyRef, computingRef);

            if (valueRef == null) {
                // We claimed the computation
                try {
                    V newValue = mappingFunction.apply(key);
                    if (newValue == null) {
                        map.remove(keyRef, computingRef);
                        return null;
                    }

                    ValueReference<V> newValueRef = new ValueReference<>(newValue, valueQueue);
                    map.replace(keyRef, computingRef, newValueRef);
                    return newValue;
                } catch (Throwable t) {
                    map.remove(keyRef, computingRef);
                    throw t;
                }
            } else if (!valueRef.isComputing()) {
                // Another thread has a value
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd, try again
                if (map.remove(keyRef, valueRef)) {
                    continue;
                }
            }
            // Another thread is computing or the reference changed, try again
        }
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        expungeStaleEntries();
        ValueReference<V> valueRef = map.get(new KeyReference<>(key, null));
        return valueRef != null ? valueRef.get() : null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        expungeStaleEntries();

        ValueReference<V> oldValueRef =
                map.put(new KeyReference<>(key, keyQueue), new ValueReference<>(value, valueQueue));

        return oldValueRef != null ? oldValueRef.get() : null;
    }

    @Override
    public V remove(K key) {
        if (key == null) {
            return null;
        }

        expungeStaleEntries();
        ValueReference<V> valueRef = map.remove(new KeyReference<>(key, null));
        return valueRef != null ? valueRef.get() : null;
    }

    @Override
    public void clear() {
        map.clear();
        expungeStaleEntries();
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

    /**
     * Removes stale entries whose keys or values have been garbage collected.
     */
    private void expungeStaleEntries() {
        Reference<?> ref;
        while ((ref = keyQueue.poll()) != null) {
            map.remove(ref);
        }
        while ((ref = valueQueue.poll()) != null) {
            map.values().remove(ref);
        }
    }

    /**
     * Soft reference wrapper for keys that uses identity-based equality.
     */
    private static class KeyReference<K> extends SoftReference<K> {
        private final int hashCode;

        KeyReference(K key, ReferenceQueue<K> queue) {
            super(key, queue);
            this.hashCode = System.identityHashCode(key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyReference)) {
                return false;
            }
            KeyReference<?> other = (KeyReference<?>) obj;
            K thisKey = this.get();
            Object otherKey = other.get();
            return thisKey != null && thisKey == otherKey;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Soft reference wrapper for values with computation state tracking.
     */
    private static class ValueReference<V> extends SoftReference<V> {
        private final boolean computing;

        ValueReference(V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.computing = false;
        }

        private ValueReference(ReferenceQueue<V> queue) {
            super(null, queue);
            this.computing = true;
        }

        boolean isComputing() {
            return computing;
        }

        static <V> ValueReference<V> computing(ReferenceQueue<V> queue) {
            return new ValueReference<>(queue);
        }
    }
}
