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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

/**
 * A thread-safe object pool for reusing objects to reduce memory consumption.
 * <p>
 * This pool is particularly useful for immutable objects that are created frequently
 * and have a high likelihood of duplication, such as InputLocation objects in Maven builds.
 * <p>
 * The pool uses a custom equality predicate to determine if two objects are equivalent,
 * allowing for flexible comparison strategies beyond the standard equals() method.
 * <p>
 * This class is package-protected and intended for internal use by generated model classes.
 *
 * @param <T> the type of objects to pool
 */
class ObjectPool<T> {

    private final SoftConcurrentMap<PoolKey<T>, T> pool = new SoftConcurrentMap<>();

    /**
     * Statistics tracking for monitoring pool effectiveness.
     */
    private final AtomicLong totalInternCalls = new AtomicLong(0);

    private final AtomicLong cacheHits = new AtomicLong(0);

    /**
     * Creates a new empty object pool.
     */
    ObjectPool() {}

    /**
     * Interns an object in the pool using a custom equality predicate.
     * <p>
     * If an equivalent object already exists in the pool, that object is returned.
     * Otherwise, the provided object is added to the pool and returned.
     *
     * @param object the object to intern
     * @param equalityPredicate predicate to test if two objects are equivalent
     * @return the pooled object (either existing or newly added)
     * @throws NullPointerException if object or equalityPredicate is null
     */
    T intern(T object, BiPredicate<T, T> equalityPredicate) {
        Objects.requireNonNull(object, "object cannot be null");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate cannot be null");

        totalInternCalls.incrementAndGet();
        PoolKey<T> key = new PoolKey<>(object, equalityPredicate);
        T existing = pool.get(key);
        if (existing != null) {
            cacheHits.incrementAndGet();
            return existing;
        }
        return pool.computeIfAbsent(key, k -> object);
    }

    /**
     * Retrieves an object from the pool if it exists.
     *
     * @param object the object to look for
     * @param equalityPredicate predicate to test if two objects are equivalent
     * @return the pooled object if it exists, null otherwise
     */
    T getIfPresent(T object, BiPredicate<T, T> equalityPredicate) {
        if (object == null || equalityPredicate == null) {
            return null;
        }

        PoolKey<T> key = new PoolKey<>(object, equalityPredicate);
        return pool.get(key);
    }

    /**
     * Returns the approximate number of objects currently in the pool.
     *
     * @return the approximate size of the pool
     */
    int size() {
        return pool.size();
    }

    /**
     * Removes all objects from the pool and resets statistics.
     */
    void clear() {
        pool.clear();
        totalInternCalls.set(0);
        cacheHits.set(0);
    }

    /**
     * Returns true if the pool contains no objects.
     *
     * @return true if the pool is empty, false otherwise
     */
    boolean isEmpty() {
        return pool.isEmpty();
    }

    /**
     * Returns the total number of intern calls made to this pool.
     * This method is primarily for monitoring and debugging purposes.
     *
     * @return the total number of intern calls
     */
    long getTotalInternCalls() {
        return totalInternCalls.get();
    }

    /**
     * Returns the number of cache hits (objects found in pool).
     * This method is primarily for monitoring and debugging purposes.
     *
     * @return the number of cache hits
     */
    long getCacheHits() {
        return cacheHits.get();
    }

    /**
     * Returns the cache hit ratio as a percentage.
     * This method is primarily for monitoring and debugging purposes.
     *
     * @return the cache hit ratio (0.0 to 1.0)
     */
    double getCacheHitRatio() {
        long total = totalInternCalls.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    /**
     * Returns statistics about this pool.
     * This method is primarily for monitoring and debugging purposes.
     *
     * @return a string containing pool statistics
     */
    String getStatistics() {
        long total = totalInternCalls.get();
        long hits = cacheHits.get();
        double hitRatio = total > 0 ? (double) hits / total : 0.0;
        return String.format(
                "ObjectPool[size=%d, totalCalls=%d, hits=%d, hitRatio=%.2f%%]",
                pool.size(), total, hits, hitRatio * 100);
    }

    /**
     * Returns a string representation of this pool, including its size and statistics.
     *
     * @return a string representation of this pool
     */
    @Override
    public String toString() {
        return getStatistics();
    }

    /**
     * A wrapper class that combines an object with its equality predicate.
     * This allows us to use custom equality comparison for pooling.
     */
    private static class PoolKey<T> {
        private final T object;
        private final BiPredicate<T, T> equalityPredicate;
        private final int hashCode;

        PoolKey(T object, BiPredicate<T, T> equalityPredicate) {
            this.object = Objects.requireNonNull(object, "object cannot be null");
            this.equalityPredicate = Objects.requireNonNull(equalityPredicate, "equalityPredicate cannot be null");
            this.hashCode = object.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PoolKey<?> other)) {
                return false;
            }
            // Use the equality predicate to compare objects
            @SuppressWarnings("unchecked")
            PoolKey<T> otherKey = (PoolKey<T>) other;
            return equalityPredicate.test(this.object, otherKey.object);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "PoolKey{object=" + object + "}";
        }
    }
}
