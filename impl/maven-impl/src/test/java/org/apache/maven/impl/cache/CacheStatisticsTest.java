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

import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.cache.CacheStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for cache statistics functionality with the improved cache architecture.
 */
class CacheStatisticsTest {

    private DefaultCacheStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new DefaultCacheStatistics();
    }

    @Test
    void testInitialStatistics() {
        assertEquals(0, statistics.getTotalRequests());
        assertEquals(0, statistics.getCacheHits());
        assertEquals(0, statistics.getCacheMisses());
        assertEquals(0.0, statistics.getHitRatio(), 0.01);
        assertEquals(0.0, statistics.getMissRatio(), 0.01);
        assertEquals(0, statistics.getCachedExceptions());
    }

    @Test
    void testBasicStatisticsTracking() {
        // Record some hits and misses
        statistics.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(1, statistics.getTotalRequests());
        assertEquals(0, statistics.getCacheHits());
        assertEquals(1, statistics.getCacheMisses());
        assertEquals(0.0, statistics.getHitRatio(), 0.01);
        assertEquals(100.0, statistics.getMissRatio(), 0.01);

        // Record a hit
        statistics.recordHit("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(2, statistics.getTotalRequests());
        assertEquals(1, statistics.getCacheHits());
        assertEquals(1, statistics.getCacheMisses());
        assertEquals(50.0, statistics.getHitRatio(), 0.01);
        assertEquals(50.0, statistics.getMissRatio(), 0.01);

        // Record another miss
        statistics.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(3, statistics.getTotalRequests());
        assertEquals(1, statistics.getCacheHits());
        assertEquals(2, statistics.getCacheMisses());
        assertEquals(33.33, statistics.getHitRatio(), 0.01);
        assertEquals(66.67, statistics.getMissRatio(), 0.01);
    }

    @Test
    void testRequestTypeStatistics() {
        // Record statistics for different request types
        statistics.recordMiss("TestRequestImpl", CacheRetention.SESSION_SCOPED);
        statistics.recordHit("TestRequestImpl", CacheRetention.SESSION_SCOPED);
        statistics.recordMiss("AnotherRequest", CacheRetention.PERSISTENT);

        Map<String, CacheStatistics.RequestTypeStatistics> requestStats = statistics.getRequestTypeStatistics();
        assertNotNull(requestStats);
        assertTrue(requestStats.containsKey("TestRequestImpl"));
        assertTrue(requestStats.containsKey("AnotherRequest"));

        CacheStatistics.RequestTypeStatistics testRequestStats = requestStats.get("TestRequestImpl");
        assertEquals("TestRequestImpl", testRequestStats.getRequestType());
        assertEquals(1, testRequestStats.getHits());
        assertEquals(1, testRequestStats.getMisses());
        assertEquals(2, testRequestStats.getTotal());
        assertEquals(50.0, testRequestStats.getHitRatio(), 0.01);

        CacheStatistics.RequestTypeStatistics anotherRequestStats = requestStats.get("AnotherRequest");
        assertEquals("AnotherRequest", anotherRequestStats.getRequestType());
        assertEquals(0, anotherRequestStats.getHits());
        assertEquals(1, anotherRequestStats.getMisses());
        assertEquals(1, anotherRequestStats.getTotal());
        assertEquals(0.0, anotherRequestStats.getHitRatio(), 0.01);
    }

    @Test
    void testRetentionStatistics() {
        // Record statistics for different retention policies
        statistics.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        statistics.recordHit("TestRequest", CacheRetention.PERSISTENT);
        statistics.recordMiss("TestRequest", CacheRetention.REQUEST_SCOPED);

        Map<CacheRetention, CacheStatistics.RetentionStatistics> retentionStats = statistics.getRetentionStatistics();
        assertNotNull(retentionStats);
        assertTrue(retentionStats.containsKey(CacheRetention.SESSION_SCOPED));
        assertTrue(retentionStats.containsKey(CacheRetention.PERSISTENT));
        assertTrue(retentionStats.containsKey(CacheRetention.REQUEST_SCOPED));

        CacheStatistics.RetentionStatistics sessionStats = retentionStats.get(CacheRetention.SESSION_SCOPED);
        assertEquals(CacheRetention.SESSION_SCOPED, sessionStats.getRetention());
        assertEquals(0, sessionStats.getHits());
        assertEquals(1, sessionStats.getMisses());
        assertEquals(1, sessionStats.getTotal());
        assertEquals(0.0, sessionStats.getHitRatio(), 0.01);

        CacheStatistics.RetentionStatistics persistentStats = retentionStats.get(CacheRetention.PERSISTENT);
        assertEquals(CacheRetention.PERSISTENT, persistentStats.getRetention());
        assertEquals(1, persistentStats.getHits());
        assertEquals(0, persistentStats.getMisses());
        assertEquals(1, persistentStats.getTotal());
        assertEquals(100.0, persistentStats.getHitRatio(), 0.01);

        CacheStatistics.RetentionStatistics requestStats = retentionStats.get(CacheRetention.REQUEST_SCOPED);
        assertEquals(CacheRetention.REQUEST_SCOPED, requestStats.getRetention());
        assertEquals(0, requestStats.getHits());
        assertEquals(1, requestStats.getMisses());
        assertEquals(1, requestStats.getTotal());
        assertEquals(0.0, requestStats.getHitRatio(), 0.01);
    }

    @Test
    void testCacheSizes() {
        // Register some cache size suppliers
        statistics.registerCacheSizeSupplier(CacheRetention.PERSISTENT, () -> 42L);
        statistics.registerCacheSizeSupplier(CacheRetention.SESSION_SCOPED, () -> 17L);
        statistics.registerCacheSizeSupplier(CacheRetention.REQUEST_SCOPED, () -> 3L);

        Map<CacheRetention, Long> cacheSizes = statistics.getCacheSizes();
        assertNotNull(cacheSizes);
        assertTrue(cacheSizes.containsKey(CacheRetention.PERSISTENT));
        assertTrue(cacheSizes.containsKey(CacheRetention.SESSION_SCOPED));
        assertTrue(cacheSizes.containsKey(CacheRetention.REQUEST_SCOPED));

        assertEquals(42L, cacheSizes.get(CacheRetention.PERSISTENT));
        assertEquals(17L, cacheSizes.get(CacheRetention.SESSION_SCOPED));
        assertEquals(3L, cacheSizes.get(CacheRetention.REQUEST_SCOPED));
    }

    @Test
    void testCachedExceptions() {
        assertEquals(0, statistics.getCachedExceptions());

        statistics.recordCachedException();
        assertEquals(1, statistics.getCachedExceptions());

        statistics.recordCachedException();
        statistics.recordCachedException();
        assertEquals(3, statistics.getCachedExceptions());
    }

    @Test
    void testStatisticsReset() {
        // Record some statistics
        statistics.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        statistics.recordHit("TestRequest", CacheRetention.SESSION_SCOPED);
        statistics.recordCachedException();

        assertTrue(statistics.getTotalRequests() > 0);
        assertTrue(statistics.getCacheHits() > 0);
        assertTrue(statistics.getCachedExceptions() > 0);

        // Reset statistics
        statistics.reset();

        assertEquals(0, statistics.getTotalRequests());
        assertEquals(0, statistics.getCacheHits());
        assertEquals(0, statistics.getCacheMisses());
        assertEquals(0.0, statistics.getHitRatio(), 0.01);
        assertEquals(0.0, statistics.getMissRatio(), 0.01);
        assertEquals(0, statistics.getCachedExceptions());
        assertTrue(statistics.getRequestTypeStatistics().isEmpty());
        assertTrue(statistics.getRetentionStatistics().isEmpty());
    }

    @Test
    void testDefaultRequestCacheIntegration() {
        DefaultRequestCache cache = new DefaultRequestCache();
        CacheStatistics stats = cache.getStatistics();

        assertNotNull(stats);
        assertEquals(0, stats.getTotalRequests());
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());

        // Verify cache size suppliers are registered
        Map<CacheRetention, Long> sizes = stats.getCacheSizes();
        assertNotNull(sizes);
        assertTrue(sizes.containsKey(CacheRetention.PERSISTENT));
        assertTrue(sizes.containsKey(CacheRetention.SESSION_SCOPED));
        assertTrue(sizes.containsKey(CacheRetention.REQUEST_SCOPED));
    }
}
