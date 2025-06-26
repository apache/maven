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
package org.apache.maven.api.cache;

import java.util.Map;

import org.apache.maven.api.annotations.Experimental;

/**
 * Interface providing detailed statistics about cache performance and usage.
 * <p>
 * This interface allows monitoring of cache behavior including hit/miss ratios,
 * request type distribution, and cache retention policy effectiveness.
 * </p>
 *
 * @since 4.0.0
 */
@Experimental
public interface CacheStatistics {

    /**
     * Returns the total number of cache requests made.
     *
     * @return Total number of requests
     */
    long getTotalRequests();

    /**
     * Returns the number of cache hits (requests served from cache).
     *
     * @return Number of cache hits
     */
    long getCacheHits();

    /**
     * Returns the number of cache misses (requests that required computation).
     *
     * @return Number of cache misses
     */
    long getCacheMisses();

    /**
     * Returns the cache hit ratio as a percentage (0.0 to 100.0).
     *
     * @return Cache hit ratio percentage
     */
    double getHitRatio();

    /**
     * Returns the cache miss ratio as a percentage (0.0 to 100.0).
     *
     * @return Cache miss ratio percentage
     */
    double getMissRatio();

    /**
     * Returns statistics broken down by request type.
     * The map key is the request class name, and the value contains
     * hit/miss statistics for that specific request type.
     *
     * @return Map of request type to statistics
     */
    Map<String, RequestTypeStatistics> getRequestTypeStatistics();

    /**
     * Returns statistics broken down by cache retention policy.
     * The map key is the cache retention policy, and the value contains
     * hit/miss statistics for that retention level.
     *
     * @return Map of cache retention to statistics
     */
    Map<CacheRetention, RetentionStatistics> getRetentionStatistics();

    /**
     * Returns the current number of entries in each cache level.
     *
     * @return Map of cache retention to current entry count
     */
    Map<CacheRetention, Long> getCacheSizes();

    /**
     * Returns the total number of exceptions that were cached and re-thrown.
     *
     * @return Number of cached exceptions
     */
    long getCachedExceptions();

    /**
     * Resets all statistics counters to zero.
     * This does not affect the actual cache contents, only the statistics.
     */
    void reset();

    /**
     * Statistics for a specific request type.
     */
    interface RequestTypeStatistics {
        /**
         * Returns the request type class name.
         *
         * @return Request type class name
         */
        String getRequestType();

        /**
         * Returns the number of hits for this request type.
         *
         * @return Number of hits
         */
        long getHits();

        /**
         * Returns the number of misses for this request type.
         *
         * @return Number of misses
         */
        long getMisses();

        /**
         * Returns the total requests for this request type.
         *
         * @return Total requests
         */
        long getTotal();

        /**
         * Returns the hit ratio for this request type.
         *
         * @return Hit ratio percentage
         */
        double getHitRatio();
    }

    /**
     * Statistics for a specific cache retention policy.
     */
    interface RetentionStatistics {
        /**
         * Returns the cache retention policy.
         *
         * @return Cache retention policy
         */
        CacheRetention getRetention();

        /**
         * Returns the number of hits for this retention policy.
         *
         * @return Number of hits
         */
        long getHits();

        /**
         * Returns the number of misses for this retention policy.
         *
         * @return Number of misses
         */
        long getMisses();

        /**
         * Returns the total requests for this retention policy.
         *
         * @return Total requests
         */
        long getTotal();

        /**
         * Returns the hit ratio for this retention policy.
         *
         * @return Hit ratio percentage
         */
        double getHitRatio();

        /**
         * Returns the current number of entries in this cache level.
         *
         * @return Current cache size
         */
        long getCurrentSize();
    }
}
