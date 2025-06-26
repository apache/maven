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

import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.cache.CacheRetention;
import org.apache.maven.api.cache.CacheStatistics;
import org.apache.maven.api.services.Request;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test for cache statistics functionality.
 */
class CacheStatisticsTest {

    private DefaultRequestCache cache;
    private CacheStatistics statistics;

    @BeforeEach
    void setUp() {
        cache = new DefaultRequestCache();
        statistics = cache.getStatistics();
        assertNotNull(statistics, "Statistics should be available");
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
        // Test the statistics implementation directly
        DefaultCacheStatistics stats = new DefaultCacheStatistics();

        // Record some hits and misses
        stats.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(1, stats.getTotalRequests());
        assertEquals(0, stats.getCacheHits());
        assertEquals(1, stats.getCacheMisses());
        assertEquals(0.0, stats.getHitRatio(), 0.01);
        assertEquals(100.0, stats.getMissRatio(), 0.01);

        // Record a hit
        stats.recordHit("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(2, stats.getTotalRequests());
        assertEquals(1, stats.getCacheHits());
        assertEquals(1, stats.getCacheMisses());
        assertEquals(50.0, stats.getHitRatio(), 0.01);
        assertEquals(50.0, stats.getMissRatio(), 0.01);

        // Record another miss
        stats.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        assertEquals(3, stats.getTotalRequests());
        assertEquals(1, stats.getCacheHits());
        assertEquals(2, stats.getCacheMisses());
        assertEquals(33.33, stats.getHitRatio(), 0.01);
        assertEquals(66.67, stats.getMissRatio(), 0.01);
    }

    @Test
    void testRequestTypeStatistics() {
        DefaultCacheStatistics stats = new DefaultCacheStatistics();

        // Record statistics for different request types
        stats.recordMiss("TestRequestImpl", CacheRetention.SESSION_SCOPED);
        stats.recordHit("TestRequestImpl", CacheRetention.SESSION_SCOPED);

        Map<String, CacheStatistics.RequestTypeStatistics> requestStats = stats.getRequestTypeStatistics();
        assertNotNull(requestStats);
        assertTrue(requestStats.containsKey("TestRequestImpl"));

        CacheStatistics.RequestTypeStatistics testRequestStats = requestStats.get("TestRequestImpl");
        assertEquals("TestRequestImpl", testRequestStats.getRequestType());
        assertEquals(1, testRequestStats.getHits());
        assertEquals(1, testRequestStats.getMisses());
        assertEquals(2, testRequestStats.getTotal());
        assertEquals(50.0, testRequestStats.getHitRatio(), 0.01);
    }

    @Test
    void testRetentionStatistics() {
        DefaultCacheStatistics stats = new DefaultCacheStatistics();

        // Record statistics for different retention policies
        stats.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        stats.recordHit("TestRequest", CacheRetention.PERSISTENT);

        Map<CacheRetention, CacheStatistics.RetentionStatistics> retentionStats = stats.getRetentionStatistics();
        assertNotNull(retentionStats);
        assertTrue(retentionStats.containsKey(CacheRetention.SESSION_SCOPED));
        assertTrue(retentionStats.containsKey(CacheRetention.PERSISTENT));

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
    }

    @Test
    void testCacheSizes() {
        Map<CacheRetention, Long> cacheSizes = statistics.getCacheSizes();
        assertNotNull(cacheSizes);
        assertTrue(cacheSizes.containsKey(CacheRetention.PERSISTENT));
        assertTrue(cacheSizes.containsKey(CacheRetention.SESSION_SCOPED));
        assertTrue(cacheSizes.containsKey(CacheRetention.REQUEST_SCOPED));

        // Initially all caches should be empty
        assertEquals(0L, cacheSizes.get(CacheRetention.PERSISTENT));
        assertEquals(0L, cacheSizes.get(CacheRetention.SESSION_SCOPED));
        assertEquals(0L, cacheSizes.get(CacheRetention.REQUEST_SCOPED));
    }

    @Test
    void testStatisticsReset() {
        DefaultCacheStatistics stats = new DefaultCacheStatistics();

        // Record some statistics
        stats.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);
        stats.recordHit("TestRequest", CacheRetention.SESSION_SCOPED);

        assertTrue(stats.getTotalRequests() > 0);
        assertTrue(stats.getCacheHits() > 0);

        // Reset statistics
        stats.reset();

        assertEquals(0, stats.getTotalRequests());
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
        assertEquals(0.0, stats.getHitRatio(), 0.01);
        assertEquals(0.0, stats.getMissRatio(), 0.01);
        assertTrue(stats.getRequestTypeStatistics().isEmpty());
        assertTrue(stats.getRetentionStatistics().isEmpty());
    }

    // Helper methods and test classes

    private TestRequest createTestRequest(String id) {
        ProtoSession session = mock(ProtoSession.class);
        return new TestRequestImpl(id, session);
    }

    // Test implementations

    interface TestRequest extends Request<ProtoSession> {
        String getId();
    }

    static class TestRequestImpl implements TestRequest {
        private final String id;
        private final ProtoSession session;

        TestRequestImpl(String id, ProtoSession session) {
            this.id = id;
            this.session = session;
        }

        @Override
        @Nonnull
        public ProtoSession getSession() {
            return session;
        }

        @Override
        public RequestTrace getTrace() {
            return null;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestRequestImpl that = (TestRequestImpl) obj;
            return java.util.Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id);
        }

        @Override
        @Nonnull
        public String toString() {
            return "TestRequest[" + id + "]";
        }
    }

    static class TestResult implements Result<TestRequest> {
        private final TestRequest request;

        TestResult(TestRequest request) {
            this.request = request;
        }

        @Override
        @Nonnull
        public TestRequest getRequest() {
            return request;
        }
    }
}
