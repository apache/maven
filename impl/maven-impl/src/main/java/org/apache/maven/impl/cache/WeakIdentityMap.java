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
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A Map implementation that uses weak references for both keys and values,
 * and compares keys using identity (==) rather than equals().
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class WeakIdentityMap<K, V> implements Map<K, V> {

    private final ReferenceQueue<K> keyQueue = new ReferenceQueue<>();
    private final ReferenceQueue<V> valueQueue = new ReferenceQueue<>();
    private final ConcurrentHashMap<WeakIdentityReference<K>, ComputeReference<V>> map = new ConcurrentHashMap<>();

    private static class WeakIdentityReference<T> extends WeakReference<T> {
        private final int hash;

        WeakIdentityReference(T referent, ReferenceQueue<T> queue) {
            super(referent, queue);
            this.hash = referent.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof WeakIdentityReference<?> other)) {
                return false;
            }
            T thisRef = this.get();
            Object otherRef = other.get();
            return thisRef != null && thisRef.equals(otherRef);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static class ComputeReference<V> extends WeakReference<V> {
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

            WeakIdentityReference<K> weakKey = new WeakIdentityReference<>(key, keyQueue);

            // Try to get existing value
            ComputeReference<V> valueRef = map.get(weakKey);
            if (valueRef != null && !valueRef.computing) {
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd, remove it
                map.remove(weakKey, valueRef);
            }

            // Try to claim computation
            ComputeReference<V> computingRef = ComputeReference.computing(valueQueue);
            valueRef = map.putIfAbsent(weakKey, computingRef);

            if (valueRef == null) {
                // We claimed the computation
                try {
                    V newValue = mappingFunction.apply(key);
                    if (newValue == null) {
                        map.remove(weakKey, computingRef);
                        return null;
                    }

                    ComputeReference<V> newValueRef = new ComputeReference<>(newValue, valueQueue);
                    map.replace(weakKey, computingRef, newValueRef);
                    return newValue;
                } catch (Throwable t) {
                    map.remove(weakKey, computingRef);
                    throw t;
                }
            } else if (!valueRef.computing) {
                // Another thread has a value
                V value = valueRef.get();
                if (value != null) {
                    return value;
                }
                // Value was GC'd
                if (map.remove(weakKey, valueRef)) {
                    continue;
                }
            }
            // Another thread is computing or the reference changed, try again
        }
    }

    private void expungeStaleEntries() {
        Reference<?> ref;
        while ((ref = keyQueue.poll()) != null) {
            map.remove(ref);
        }
        while ((ref = valueQueue.poll()) != null) {
            map.values().remove(ref);
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
        return map.containsKey(new WeakIdentityReference<>((K) key, null));
    }

    @Override
    public boolean containsValue(Object value) {
        expungeStaleEntries();
        for (WeakReference<V> ref : map.values()) {
            V v = ref.get();
            if (v != null && v == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        expungeStaleEntries();
        WeakReference<V> ref = map.get(new WeakIdentityReference<>((K) key, null));
        return ref != null ? ref.get() : null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        expungeStaleEntries();

        WeakReference<V> oldValueRef =
                map.put(new WeakIdentityReference<>(key, keyQueue), new ComputeReference<>(value, valueQueue));

        return oldValueRef != null ? oldValueRef.get() : null;
    }

    @Override
    public V remove(Object key) {
        expungeStaleEntries();
        WeakReference<V> valueRef = map.remove(new WeakIdentityReference<>((K) key, null));
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
}
