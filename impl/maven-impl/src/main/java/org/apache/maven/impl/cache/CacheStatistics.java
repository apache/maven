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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.maven.api.cache.CacheRetention;

/**
 * Cache statistics that tracks detailed metrics
 * about cache performance and usage patterns.
 * <p>
 * This implementation integrates with the improved cache architecture and
 * provides thread-safe statistics tracking with minimal performance overhead.
 * </p>
 */
public class CacheStatistics {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong cachedExceptions = new AtomicLong();

    // Enhanced eviction tracking
    private final AtomicLong keyEvictions = new AtomicLong();
    private final AtomicLong valueEvictions = new AtomicLong();
    private final AtomicLong totalEvictions = new AtomicLong();

    private final Map<String, RequestTypeStatistics> requestTypeStats = new ConcurrentHashMap<>();
    private final Map<CacheRetention, RetentionStatistics> retentionStats = new ConcurrentHashMap<>();
    private final Map<CacheRetention, Supplier<Long>> cacheSizeSuppliers = new ConcurrentHashMap<>();

    // Reference type statistics
    private final Map<String, ReferenceTypeStatistics> referenceTypeStats = new ConcurrentHashMap<>();

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public double getHitRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (getCacheHits() * 100.0) / total;
    }

    public double getMissRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (getCacheMisses() * 100.0) / total;
    }

    public Map<String, RequestTypeStatistics> getRequestTypeStatistics() {
        return Map.copyOf(requestTypeStats);
    }

    public Map<CacheRetention, RetentionStatistics> getRetentionStatistics() {
        return Map.copyOf(retentionStats);
    }

    public Map<String, ReferenceTypeStatistics> getReferenceTypeStatistics() {
        return Map.copyOf(referenceTypeStats);
    }

    public Map<CacheRetention, Long> getCacheSizes() {
        Map<CacheRetention, Long> sizes = new ConcurrentHashMap<>();
        cacheSizeSuppliers.forEach((retention, supplier) -> sizes.put(retention, supplier.get()));
        return sizes;
    }

    public long getCachedExceptions() {
        return cachedExceptions.get();
    }

    /**
     * Returns the total number of key evictions across all caches.
     */
    public long getKeyEvictions() {
        return keyEvictions.get();
    }

    /**
     * Returns the total number of value evictions across all caches.
     */
    public long getValueEvictions() {
        return valueEvictions.get();
    }

    /**
     * Returns the total number of evictions (keys + values).
     */
    public long getTotalEvictions() {
        return totalEvictions.get();
    }

    /**
     * Returns the ratio of key evictions to total evictions.
     */
    public double getKeyEvictionRatio() {
        long total = getTotalEvictions();
        return total == 0 ? 0.0 : (getKeyEvictions() * 100.0) / total;
    }

    /**
     * Returns the ratio of value evictions to total evictions.
     */
    public double getValueEvictionRatio() {
        long total = getTotalEvictions();
        return total == 0 ? 0.0 : (getValueEvictions() * 100.0) / total;
    }

    /**
     * Records a cache hit for the given request type and retention policy.
     */
    public void recordHit(String requestType, CacheRetention retention) {
        totalRequests.incrementAndGet();
        cacheHits.incrementAndGet();

        requestTypeStats
                .computeIfAbsent(requestType, RequestTypeStatistics::new)
                .recordHit();
        retentionStats.computeIfAbsent(retention, RetentionStatistics::new).recordHit();
    }

    /**
     * Records a cache miss for the given request type and retention policy.
     */
    public void recordMiss(String requestType, CacheRetention retention) {
        totalRequests.incrementAndGet();
        cacheMisses.incrementAndGet();

        requestTypeStats
                .computeIfAbsent(requestType, RequestTypeStatistics::new)
                .recordMiss();
        retentionStats.computeIfAbsent(retention, RetentionStatistics::new).recordMiss();
    }

    /**
     * Records a cached exception.
     */
    public void recordCachedException() {
        cachedExceptions.incrementAndGet();
    }

    /**
     * Records a key eviction for the specified retention policy.
     */
    public void recordKeyEviction(CacheRetention retention) {
        keyEvictions.incrementAndGet();
        totalEvictions.incrementAndGet();
        retentionStats.computeIfAbsent(retention, RetentionStatistics::new).recordKeyEviction();
    }

    /**
     * Records a value eviction for the specified retention policy.
     */
    public void recordValueEviction(CacheRetention retention) {
        valueEvictions.incrementAndGet();
        totalEvictions.incrementAndGet();
        retentionStats.computeIfAbsent(retention, RetentionStatistics::new).recordValueEviction();
    }

    /**
     * Registers a cache size supplier for the given retention policy.
     */
    public void registerCacheSizeSupplier(CacheRetention retention, Supplier<Long> sizeSupplier) {
        cacheSizeSuppliers.put(retention, sizeSupplier);
        retentionStats.computeIfAbsent(retention, RetentionStatistics::new).setSizeSupplier(sizeSupplier);
    }

    /**
     * Returns eviction statistics by retention policy.
     */
    public Map<CacheRetention, Long> getKeyEvictionsByRetention() {
        Map<CacheRetention, Long> evictions = new ConcurrentHashMap<>();
        retentionStats.forEach((retention, stats) -> evictions.put(retention, stats.getKeyEvictions()));
        return evictions;
    }

    /**
     * Returns value eviction statistics by retention policy.
     */
    public Map<CacheRetention, Long> getValueEvictionsByRetention() {
        Map<CacheRetention, Long> evictions = new ConcurrentHashMap<>();
        retentionStats.forEach((retention, stats) -> evictions.put(retention, stats.getValueEvictions()));
        return evictions;
    }

    /**
     * Records cache creation with specific reference types.
     */
    public void recordCacheCreation(String keyRefType, String valueRefType, CacheRetention retention) {
        String refTypeKey = keyRefType + "/" + valueRefType;
        referenceTypeStats
                .computeIfAbsent(refTypeKey, ReferenceTypeStatistics::new)
                .recordCacheCreation(retention);
    }

    /**
     * Records cache access for specific reference types.
     */
    public void recordCacheAccess(String keyRefType, String valueRefType, boolean hit) {
        String refTypeKey = keyRefType + "/" + valueRefType;
        ReferenceTypeStatistics stats = referenceTypeStats.computeIfAbsent(refTypeKey, ReferenceTypeStatistics::new);
        if (hit) {
            stats.recordHit();
        } else {
            stats.recordMiss();
        }
    }

    /**
     * Default implementation of request type statistics.
     */
    public static class RequestTypeStatistics {
        private final String requestType;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();

        RequestTypeStatistics(String requestType) {
            this.requestType = requestType;
        }

        public String getRequestType() {
            return requestType;
        }

        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        public long getTotal() {
            return getHits() + getMisses();
        }

        public double getHitRatio() {
            long total = getTotal();
            return total == 0 ? 0.0 : (getHits() * 100.0) / total;
        }

        void recordHit() {
            hits.incrementAndGet();
        }

        void recordMiss() {
            misses.incrementAndGet();
        }
    }

    /**
     * Default implementation of retention statistics.
     */
    public static class RetentionStatistics {
        private final CacheRetention retention;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong keyEvictions = new AtomicLong();
        private final AtomicLong valueEvictions = new AtomicLong();
        private volatile Supplier<Long> sizeSupplier = () -> 0L;

        RetentionStatistics(CacheRetention retention) {
            this.retention = retention;
        }

        public CacheRetention getRetention() {
            return retention;
        }

        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        public long getTotal() {
            return getHits() + getMisses();
        }

        public double getHitRatio() {
            long total = getTotal();
            return total == 0 ? 0.0 : (getHits() * 100.0) / total;
        }

        public long getCurrentSize() {
            return sizeSupplier.get();
        }

        public long getKeyEvictions() {
            return keyEvictions.get();
        }

        public long getValueEvictions() {
            return valueEvictions.get();
        }

        public long getTotalEvictions() {
            return getKeyEvictions() + getValueEvictions();
        }

        public double getKeyEvictionRatio() {
            long total = getTotalEvictions();
            return total == 0 ? 0.0 : (getKeyEvictions() * 100.0) / total;
        }

        void recordHit() {
            hits.incrementAndGet();
        }

        void recordMiss() {
            misses.incrementAndGet();
        }

        void recordKeyEviction() {
            keyEvictions.incrementAndGet();
        }

        void recordValueEviction() {
            valueEvictions.incrementAndGet();
        }

        void setSizeSupplier(Supplier<Long> sizeSupplier) {
            this.sizeSupplier = sizeSupplier;
        }
    }

    /**
     * Statistics for specific reference type combinations.
     */
    public static class ReferenceTypeStatistics {
        private final String referenceTypeKey;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong cacheCreations = new AtomicLong();
        private final Map<CacheRetention, AtomicLong> creationsByRetention = new ConcurrentHashMap<>();

        ReferenceTypeStatistics(String referenceTypeKey) {
            this.referenceTypeKey = referenceTypeKey;
        }

        public String getReferenceTypeKey() {
            return referenceTypeKey;
        }

        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        public long getTotal() {
            return getHits() + getMisses();
        }

        public double getHitRatio() {
            long total = getTotal();
            return total == 0 ? 0.0 : (getHits() * 100.0) / total;
        }

        public long getCacheCreations() {
            return cacheCreations.get();
        }

        public Map<CacheRetention, Long> getCreationsByRetention() {
            Map<CacheRetention, Long> result = new ConcurrentHashMap<>();
            creationsByRetention.forEach((retention, count) -> result.put(retention, count.get()));
            return result;
        }

        void recordHit() {
            hits.incrementAndGet();
        }

        void recordMiss() {
            misses.incrementAndGet();
        }

        void recordCacheCreation(CacheRetention retention) {
            cacheCreations.incrementAndGet();
            creationsByRetention
                    .computeIfAbsent(retention, k -> new AtomicLong())
                    .incrementAndGet();
        }
    }
}
