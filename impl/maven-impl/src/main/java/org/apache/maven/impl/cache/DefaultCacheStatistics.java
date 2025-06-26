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
import org.apache.maven.api.cache.CacheStatistics;

/**
 * Default implementation of cache statistics that tracks detailed metrics
 * about cache performance and usage patterns.
 */
public class DefaultCacheStatistics implements CacheStatistics {

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong cachedExceptions = new AtomicLong();

    private final Map<String, DefaultRequestTypeStatistics> requestTypeStats = new ConcurrentHashMap<>();
    private final Map<CacheRetention, DefaultRetentionStatistics> retentionStats = new ConcurrentHashMap<>();
    private final Map<CacheRetention, Supplier<Long>> cacheSizeSuppliers = new ConcurrentHashMap<>();

    @Override
    public long getTotalRequests() {
        return totalRequests.get();
    }

    @Override
    public long getCacheHits() {
        return cacheHits.get();
    }

    @Override
    public long getCacheMisses() {
        return cacheMisses.get();
    }

    @Override
    public double getHitRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (getCacheHits() * 100.0) / total;
    }

    @Override
    public double getMissRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (getCacheMisses() * 100.0) / total;
    }

    @Override
    public Map<String, RequestTypeStatistics> getRequestTypeStatistics() {
        return Map.copyOf(requestTypeStats);
    }

    @Override
    public Map<CacheRetention, RetentionStatistics> getRetentionStatistics() {
        return Map.copyOf(retentionStats);
    }

    @Override
    public Map<CacheRetention, Long> getCacheSizes() {
        Map<CacheRetention, Long> sizes = new ConcurrentHashMap<>();
        cacheSizeSuppliers.forEach((retention, supplier) -> sizes.put(retention, supplier.get()));
        return sizes;
    }

    @Override
    public long getCachedExceptions() {
        return cachedExceptions.get();
    }

    @Override
    public void reset() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        cachedExceptions.set(0);
        requestTypeStats.clear();
        retentionStats.clear();
    }

    /**
     * Records a cache hit for the given request type and retention policy.
     */
    public void recordHit(String requestType, CacheRetention retention) {
        totalRequests.incrementAndGet();
        cacheHits.incrementAndGet();

        requestTypeStats
                .computeIfAbsent(requestType, DefaultRequestTypeStatistics::new)
                .recordHit();
        retentionStats
                .computeIfAbsent(retention, DefaultRetentionStatistics::new)
                .recordHit();
    }

    /**
     * Records a cache miss for the given request type and retention policy.
     */
    public void recordMiss(String requestType, CacheRetention retention) {
        totalRequests.incrementAndGet();
        cacheMisses.incrementAndGet();

        requestTypeStats
                .computeIfAbsent(requestType, DefaultRequestTypeStatistics::new)
                .recordMiss();
        retentionStats
                .computeIfAbsent(retention, DefaultRetentionStatistics::new)
                .recordMiss();
    }

    /**
     * Records a cached exception.
     */
    public void recordCachedException() {
        cachedExceptions.incrementAndGet();
    }

    /**
     * Registers a cache size supplier for the given retention policy.
     */
    public void registerCacheSizeSupplier(CacheRetention retention, Supplier<Long> sizeSupplier) {
        cacheSizeSuppliers.put(retention, sizeSupplier);
        retentionStats
                .computeIfAbsent(retention, DefaultRetentionStatistics::new)
                .setSizeSupplier(sizeSupplier);
    }

    /**
     * Default implementation of request type statistics.
     */
    private static class DefaultRequestTypeStatistics implements RequestTypeStatistics {
        private final String requestType;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();

        DefaultRequestTypeStatistics(String requestType) {
            this.requestType = requestType;
        }

        @Override
        public String getRequestType() {
            return requestType;
        }

        @Override
        public long getHits() {
            return hits.get();
        }

        @Override
        public long getMisses() {
            return misses.get();
        }

        @Override
        public long getTotal() {
            return getHits() + getMisses();
        }

        @Override
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
    private static class DefaultRetentionStatistics implements RetentionStatistics {
        private final CacheRetention retention;
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private volatile Supplier<Long> sizeSupplier = () -> 0L;

        DefaultRetentionStatistics(CacheRetention retention) {
            this.retention = retention;
        }

        @Override
        public CacheRetention getRetention() {
            return retention;
        }

        @Override
        public long getHits() {
            return hits.get();
        }

        @Override
        public long getMisses() {
            return misses.get();
        }

        @Override
        public long getTotal() {
            return getHits() + getMisses();
        }

        @Override
        public double getHitRatio() {
            long total = getTotal();
            return total == 0 ? 0.0 : (getHits() * 100.0) / total;
        }

        @Override
        public long getCurrentSize() {
            return sizeSupplier.get();
        }

        void recordHit() {
            hits.incrementAndGet();
        }

        void recordMiss() {
            misses.incrementAndGet();
        }

        void setSizeSupplier(Supplier<Long> sizeSupplier) {
            this.sizeSupplier = sizeSupplier;
        }
    }
}
