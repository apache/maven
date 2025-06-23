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
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * A cache interface that provides configurable reference types for both keys and values,
 * and supports automatic cleanup of garbage-collected entries.
 * <p>
 * This cache is designed for scenarios where:
 * <ul>
 *   <li>Values should be eligible for garbage collection when memory is low</li>
 *   <li>Concurrent access is required</li>
 *   <li>Automatic cleanup of stale entries is desired</li>
 * </ul>
 * <p>
 * The cache can use different reference types (none, soft, weak, hard) for both keys and values,
 * depending on the factory method used to create the cache instance.
 * <p>
 * Note: All implementations are thread-safe and optimized for concurrent read access.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 */
public interface Cache<K, V> {

    /**
     * Enumeration of reference types that can be used for individual entries.
     */
    enum ReferenceType {
        /** No caching - always compute the value */
        NONE,
        /** Soft references - cleared before OutOfMemoryError */
        SOFT,
        /** Weak references - cleared more aggressively */
        WEAK,
        /** Hard references - never cleared by GC */
        HARD
    }

    /**
     * Computes a value for the given key if it's not already present, using the specified reference type.
     * <p>
     * This method allows fine-grained control over the reference type used for individual entries,
     * overriding the cache's default reference type for this specific key-value pair.
     *
     * @param key the key whose associated value is to be returned or computed
     * @param mappingFunction the function to compute a value
     * @param referenceType the reference type to use for this entry (null uses cache default)
     * @return the current (existing or computed) value associated with the specified key
     */
    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, ReferenceType referenceType);

    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return computeIfAbsent(key, mappingFunction, null);
    }

    void removeIf(BiPredicate<K, V> o);

    V get(K key);

    int size();

    void clear();

    default boolean containsKey(K key) {
        return get(key) != null;
    }

    static <K, V> Cache<K, V> newCache(ReferenceType referenceType) {
        return RefConcurrentMap.newCache(referenceType);
    }

    /**
     * A concurrent map implementation that uses configurable reference types for both keys and values,
     * and supports automatic cleanup of garbage-collected entries.
     * <p>
     * This implementation is package-private and accessed through the {@link Cache} interface.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    class RefConcurrentMap<K, V> implements Map<K, V>, Cache<K, V> {

        private final ReferenceQueue<K> keyQueue = new ReferenceQueue<>();
        private final ReferenceQueue<V> valueQueue = new ReferenceQueue<>();
        private final ConcurrentHashMap<RefConcurrentReference<K>, ComputeReference<V>> map = new ConcurrentHashMap<>();

        // Default reference type for this map
        private final ReferenceType defaultReferenceType;

        // Eviction statistics
        private final AtomicLong keyEvictions = new AtomicLong(0);
        private final AtomicLong valueEvictions = new AtomicLong(0);

        /**
         * Private constructor - use factory methods to create instances.
         */
        private RefConcurrentMap(ReferenceType defaultReferenceType) {
            this.defaultReferenceType = defaultReferenceType;
        }

        static <K, V> RefConcurrentMap<K, V> newCache(ReferenceType referenceType) {
            return new RefConcurrentMap<>(referenceType);
        }

        // Base class for reference implementations
        private abstract static class RefConcurrentReference<T> {
            protected final int hash;

            RefConcurrentReference(T referent) {
                this.hash = referent.hashCode();
            }

            public abstract Reference<T> getReference();

            public abstract T get();

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof RefConcurrentMap.RefConcurrentReference<?> other)) {
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

        // Soft reference implementation
        private static class SoftRefConcurrentReference<T> extends RefConcurrentReference<T> {
            final SoftReference<T> softRef;

            SoftRefConcurrentReference(T referent, ReferenceQueue<T> queue) {
                super(referent);
                this.softRef = new SoftReference<>(referent, queue);
            }

            @Override
            public SoftReference<T> getReference() {
                return softRef;
            }

            @Override
            public T get() {
                return softRef.get();
            }
        }

        // Weak reference implementation
        private static class WeakRefConcurrentReference<T> extends RefConcurrentReference<T> {
            final WeakReference<T> weakRef;

            WeakRefConcurrentReference(T referent, ReferenceQueue<T> queue) {
                super(referent);
                this.weakRef = new WeakReference<>(referent, queue);
            }

            @Override
            public Reference<T> getReference() {
                return weakRef;
            }

            @Override
            public T get() {
                return weakRef.get();
            }
        }

        // Hard reference implementation (strong references)
        private static class HardRefConcurrentReference<T> extends RefConcurrentReference<T> {
            private final T referent;

            HardRefConcurrentReference(T referent, ReferenceQueue<T> queue) {
                super(referent);
                this.referent = referent;
                // Note: queue is ignored for hard references since they're never GC'd
            }

            @Override
            public Reference<T> getReference() {
                // Return null since hard references don't use Reference objects
                return null;
            }

            @Override
            public T get() {
                return referent;
            }
        }

        // Base class for compute references
        private abstract static class ComputeReference<V> {
            protected final boolean computing;

            ComputeReference(boolean computing) {
                this.computing = computing;
            }

            public abstract V get();

            public abstract Reference<V> getReference();
        }

        // Soft compute reference implementation
        private static class SoftComputeReference<V> extends ComputeReference<V> {
            final SoftReference<V> softRef;

            SoftComputeReference(V value, ReferenceQueue<V> queue) {
                super(false);
                this.softRef = new SoftReference<>(value, queue);
            }

            private SoftComputeReference(ReferenceQueue<V> queue) {
                super(true);
                this.softRef = new SoftReference<>(null, queue);
            }

            static <V> SoftComputeReference<V> computing(ReferenceQueue<V> queue) {
                return new SoftComputeReference<>(queue);
            }

            @Override
            public V get() {
                return softRef.get();
            }

            @Override
            public Reference<V> getReference() {
                return softRef;
            }
        }

        // Weak compute reference implementation
        private static class WeakComputeReference<V> extends ComputeReference<V> {
            final WeakReference<V> weakRef;

            WeakComputeReference(V value, ReferenceQueue<V> queue) {
                super(false);
                this.weakRef = new WeakReference<>(value, queue);
            }

            private WeakComputeReference(ReferenceQueue<V> queue) {
                super(true);
                this.weakRef = new WeakReference<>(null, queue);
            }

            static <V> WeakComputeReference<V> computing(ReferenceQueue<V> queue) {
                return new WeakComputeReference<>(queue);
            }

            @Override
            public V get() {
                return weakRef.get();
            }

            @Override
            public Reference<V> getReference() {
                return weakRef;
            }
        }

        // Hard compute reference implementation (strong references)
        private static class HardComputeReference<V> extends ComputeReference<V> {
            private final V value;

            HardComputeReference(V value, ReferenceQueue<V> queue) {
                super(false);
                this.value = value;
                // Note: queue is ignored for hard references since they're never GC'd
            }

            private HardComputeReference(ReferenceQueue<V> queue) {
                super(true);
                this.value = null;
            }

            static <V> HardComputeReference<V> computing(ReferenceQueue<V> queue) {
                return new HardComputeReference<>(queue);
            }

            @Override
            public V get() {
                return value;
            }

            @Override
            public Reference<V> getReference() {
                // Return null since hard references don't use Reference objects
                return null;
            }
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            return computeIfAbsent(key, mappingFunction, null);
        }

        /**
         * Computes a value for the given key if it's not already present, using the specified reference type.
         * <p>
         * This method allows fine-grained control over the reference type used for individual entries,
         * overriding the map's default reference type for this specific key-value pair.
         *
         * @param key the key whose associated value is to be returned or computed
         * @param mappingFunction the function to compute a value
         * @param referenceType the reference type to use for this entry (null uses map default)
         * @return the current (existing or computed) value associated with the specified key
         */
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction, ReferenceType referenceType) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(mappingFunction);

            if (referenceType == null) {
                referenceType = defaultReferenceType;
            }

            // Handle NONE reference type - always compute, never cache
            if (referenceType == ReferenceType.NONE) {
                return mappingFunction.apply(key);
            }

            while (true) {
                expungeStaleEntries();

                RefConcurrentReference<K> keyRef = getKeyReference(key, referenceType);

                // Try to get existing value
                ComputeReference<V> valueRef = map.get(keyRef);
                if (valueRef != null && !valueRef.computing) {
                    V value = valueRef.get();
                    if (value != null) {
                        return value;
                    }
                    // Value was GC'd, remove it
                    map.remove(keyRef, valueRef);
                }

                // Try to claim computation
                ComputeReference<V> computingRef = getComputingReference(referenceType);
                valueRef = map.putIfAbsent(keyRef, computingRef);

                if (valueRef == null) {
                    // We claimed the computation
                    try {
                        V newValue = mappingFunction.apply(key);
                        if (newValue == null) {
                            map.remove(keyRef, computingRef);
                            return null;
                        }

                        ComputeReference<V> newValueRef = getValueReference(newValue, referenceType);
                        map.replace(keyRef, computingRef, newValueRef);
                        return newValue;
                    } catch (Throwable t) {
                        map.remove(keyRef, computingRef);
                        throw t;
                    }
                } else if (!valueRef.computing) {
                    // Another thread has a value
                    V value = valueRef.get();
                    if (value != null) {
                        return value;
                    }
                    // Value was GC'd
                    if (map.remove(keyRef, valueRef)) {
                        continue;
                    }
                }
                // Another thread is computing or the reference changed, try again
            }
        }

        /**
         * Creates a computing reference using the specified reference type.
         * If referenceType is null, uses the map's default reference type.
         */
        private ComputeReference<V> getComputingReference(ReferenceType referenceType) {
            return switch (referenceType) {
                case SOFT -> SoftComputeReference.computing(valueQueue);
                case WEAK -> WeakComputeReference.computing(valueQueue);
                case HARD -> HardComputeReference.computing(valueQueue);
                case NONE -> throw new IllegalArgumentException(
                        "NONE reference type should be handled before calling this method");
            };
        }

        /**
         * Creates a value reference using the specified reference type.
         * If referenceType is null, uses the map's default reference type.
         */
        private ComputeReference<V> getValueReference(V value, ReferenceType referenceType) {
            return switch (referenceType) {
                case SOFT -> new SoftComputeReference<>(value, valueQueue);
                case WEAK -> new WeakComputeReference<>(value, valueQueue);
                case HARD -> new HardComputeReference<>(value, valueQueue);
                case NONE -> throw new IllegalArgumentException(
                        "NONE reference type should be handled before calling this method");
            };
        }

        /**
         * Creates a key reference using the specified reference type.
         * If referenceType is null, uses the map's default reference type.
         */
        private RefConcurrentReference<K> getKeyReference(K key, ReferenceType referenceType) {
            return switch (referenceType) {
                case SOFT -> new SoftRefConcurrentReference<>(key, keyQueue);
                case WEAK -> new WeakRefConcurrentReference<>(key, keyQueue);
                case HARD -> new HardRefConcurrentReference<>(key, keyQueue);
                case NONE -> throw new IllegalArgumentException(
                        "NONE reference type should be handled before calling this method");
            };
        }

        private void expungeStaleEntries() {
            // Remove entries where the key has been garbage collected
            Reference<?> ref;
            while ((ref = keyQueue.poll()) != null) {
                final Reference<?> finalRef = ref;
                // Find and remove map entries where the key reference matches
                // Hard references return null from getReference(), so they won't match
                boolean removed = map.entrySet().removeIf(entry -> {
                    Reference<?> keyRef = entry.getKey().getReference();
                    return keyRef != null && keyRef == finalRef;
                });
                if (removed) {
                    keyEvictions.incrementAndGet();
                }
            }
            // Remove entries where the value has been garbage collected
            while ((ref = valueQueue.poll()) != null) {
                final Reference<?> finalRef = ref;
                // Find and remove map entries where the value reference matches
                // Hard references return null from getReference(), so they won't match
                boolean removed = map.entrySet().removeIf(entry -> {
                    Reference<?> valueRef = entry.getValue().getReference();
                    return valueRef != null && valueRef == finalRef;
                });
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

            // Handle NONE reference type - always compute, never cache
            if (defaultReferenceType == ReferenceType.NONE) {
                return false;
            }

            return map.containsKey(getKeyReference((K) key, defaultReferenceType));
        }

        @Override
        public boolean containsValue(Object value) {
            expungeStaleEntries();

            // Handle NONE reference type - always compute, never cache
            if (defaultReferenceType == ReferenceType.NONE) {
                return false;
            }

            for (ComputeReference<V> ref : map.values()) {
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

            // Handle NONE reference type - always compute, never cache
            if (defaultReferenceType == ReferenceType.NONE) {
                return null;
            }

            ComputeReference<V> ref = map.get(getKeyReference((K) key, defaultReferenceType));
            return ref != null ? ref.get() : null;
        }

        @Override
        public V put(K key, V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            expungeStaleEntries();

            // Handle NONE reference type - always compute, never cache
            if (defaultReferenceType == ReferenceType.NONE) {
                return null;
            }

            ComputeReference<V> oldValueRef =
                    map.put(getKeyReference(key, defaultReferenceType), getValueReference(value, null));

            return oldValueRef != null ? oldValueRef.get() : null;
        }

        @Override
        public V remove(Object key) {
            expungeStaleEntries();
            ComputeReference<V> valueRef = map.remove(getKeyReference((K) key, defaultReferenceType));
            return valueRef != null ? valueRef.get() : null;
        }

        public void removeIf(BiPredicate<K, V> filter) {
            expungeStaleEntries();
            map.entrySet()
                    .removeIf(e -> filter.test(e.getKey().get(), e.getValue().get()));
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
}
