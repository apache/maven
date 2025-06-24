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

/**
 * A pluggable service for pooling and interning model objects to reduce memory usage
 * and improve performance through object reuse.
 *
 * <p>This service allows implementations to:</p>
 * <ul>
 *   <li>Pool identical objects to reduce memory footprint</li>
 *   <li>Intern objects for faster equality comparisons</li>
 *   <li>Apply custom optimization strategies</li>
 *   <li>Process objects during model building</li>
 * </ul>
 *
 * <p>Implementations are discovered via the Java ServiceLoader mechanism and should
 * be registered in {@code META-INF/services/org.apache.maven.api.model.ModelObjectPool}.</p>
 *
 * <p>The service is called during model building for all model objects, allowing
 * implementations to decide which objects to pool and how to optimize them.</p>
 *
 * @since 4.0.0
 */
public interface ModelObjectPool {

    /**
     * Process a model object, potentially returning a pooled or optimized version.
     *
     * <p>This method is called during model building for various model objects.
     * Implementations can:</p>
     * <ul>
     *   <li>Return the same object if no pooling is desired</li>
     *   <li>Return a pooled equivalent object to reduce memory usage</li>
     *   <li>Return a modified or optimized version of the object</li>
     * </ul>
     *
     * <p>The implementation must ensure that the returned object is functionally
     * equivalent to the input object from the perspective of the Maven model.</p>
     *
     * @param <T> the type of the model object
     * @param object the model object to process
     * @return the processed object (may be the same instance, a pooled instance, or a modified instance)
     * @throws IllegalArgumentException if the object cannot be processed
     */
    <T> T intern(T object);

    /**
     * Get the priority of this pool implementation.
     *
     * <p>When multiple implementations are available, the one with the highest
     * priority will be used. Higher numbers indicate higher priority.</p>
     *
     * @return the priority of this implementation (higher numbers = higher priority)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this pool implementation supports the given object type.
     *
     * <p>This method allows implementations to declare which types of objects
     * they can handle, enabling more efficient service selection.</p>
     *
     * @param objectType the class of the object to check
     * @return true if this pool can handle objects of the given type
     */
    default boolean supports(Class<?> objectType) {
        return true; // Default: support all types
    }

    /**
     * Get statistics about the pool usage (optional).
     *
     * <p>This method can be used for monitoring and debugging purposes.
     * Implementations may return null if statistics are not available.</p>
     *
     * @return pool statistics or null if not available
     */
    default PoolStatistics getStatistics() {
        return null;
    }

    /**
     * Statistics about pool usage.
     */
    interface PoolStatistics {
        /**
         * Get the number of objects currently in the pool.
         * @return the pool size
         */
        long getPoolSize();

        /**
         * Get the number of cache hits (objects returned from pool).
         * @return the hit count
         */
        long getHitCount();

        /**
         * Get the number of cache misses (new objects created).
         * @return the miss count
         */
        long getMissCount();

        /**
         * Get the hit ratio (hits / (hits + misses)).
         * @return the hit ratio between 0.0 and 1.0
         */
        default double getHitRatio() {
            long hits = getHitCount();
            long misses = getMissCount();
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }
    }
}
