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
import org.apache.maven.impl.cache.CacheStatistics.ReferenceTypeStatistics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTypeStatisticsTest {

    private final CacheStatistics statistics = new CacheStatistics();

    @Test
    void shouldTrackReferenceTypeStatistics() {
        // Record cache creation with different reference types
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.SESSION_SCOPED);
        statistics.recordCacheCreation("HARD", "SOFT", CacheRetention.REQUEST_SCOPED);
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.SESSION_SCOPED);

        // Record cache accesses
        statistics.recordCacheAccess("SOFT", "WEAK", true); // hit
        statistics.recordCacheAccess("SOFT", "WEAK", false); // miss
        statistics.recordCacheAccess("HARD", "SOFT", true); // hit

        Map<String, ReferenceTypeStatistics> refTypeStats = statistics.getReferenceTypeStatistics();

        // Should have two reference type combinations
        assertEquals(2, refTypeStats.size());

        // Check SOFT/WEAK statistics
        ReferenceTypeStatistics softWeakStats = refTypeStats.get("SOFT/WEAK");
        assertNotNull(softWeakStats);
        assertEquals(2, softWeakStats.getCacheCreations());
        assertEquals(1, softWeakStats.getHits());
        assertEquals(1, softWeakStats.getMisses());
        assertEquals(2, softWeakStats.getTotal());
        assertEquals(50.0, softWeakStats.getHitRatio(), 0.1);

        // Check HARD/SOFT statistics
        ReferenceTypeStatistics hardSoftStats = refTypeStats.get("HARD/SOFT");
        assertNotNull(hardSoftStats);
        assertEquals(1, hardSoftStats.getCacheCreations());
        assertEquals(1, hardSoftStats.getHits());
        assertEquals(0, hardSoftStats.getMisses());
        assertEquals(1, hardSoftStats.getTotal());
        assertEquals(100.0, hardSoftStats.getHitRatio(), 0.1);
    }

    @Test
    void shouldTrackCreationsByRetention() {
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.SESSION_SCOPED);
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.REQUEST_SCOPED);
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.SESSION_SCOPED);

        var refTypeStats = statistics.getReferenceTypeStatistics();
        var softWeakStats = refTypeStats.get("SOFT/WEAK");

        assertNotNull(softWeakStats);
        assertEquals(3, softWeakStats.getCacheCreations());

        var creationsByRetention = softWeakStats.getCreationsByRetention();
        assertEquals(2, creationsByRetention.get(CacheRetention.SESSION_SCOPED).longValue());
        assertEquals(1, creationsByRetention.get(CacheRetention.REQUEST_SCOPED).longValue());
    }

    @Test
    void shouldHandleEmptyStatistics() {
        var refTypeStats = statistics.getReferenceTypeStatistics();
        assertTrue(refTypeStats.isEmpty());
    }

    @Test
    void shouldDisplayReferenceTypeStatisticsInOutput() {
        CacheStatistics statistics = new CacheStatistics();

        // Simulate cache usage with different reference types
        statistics.recordCacheCreation("HARD", "HARD", CacheRetention.SESSION_SCOPED);
        statistics.recordCacheCreation("SOFT", "WEAK", CacheRetention.REQUEST_SCOPED);
        statistics.recordCacheCreation("WEAK", "SOFT", CacheRetention.PERSISTENT);

        // Simulate cache accesses
        statistics.recordCacheAccess("HARD", "HARD", true);
        statistics.recordCacheAccess("HARD", "HARD", true);
        statistics.recordCacheAccess("HARD", "HARD", false);

        statistics.recordCacheAccess("SOFT", "WEAK", true);
        statistics.recordCacheAccess("SOFT", "WEAK", false);
        statistics.recordCacheAccess("SOFT", "WEAK", false);

        statistics.recordCacheAccess("WEAK", "SOFT", false);

        // Simulate some regular cache statistics
        statistics.recordHit("TestRequest", CacheRetention.SESSION_SCOPED);
        statistics.recordMiss("TestRequest", CacheRetention.SESSION_SCOPED);

        // Capture the formatted output (not used in this test, but could be useful for future enhancements)

        String output = DefaultRequestCache.formatCacheStatistics(statistics);

        // Verify that reference type information is included
        assertTrue(output.contains("Reference type usage:"), "Should contain reference type section\n" + output);
        assertTrue(output.contains("HARD/HARD:"), "Should show HARD/HARD reference type\n" + output);
        assertTrue(output.contains("SOFT/WEAK:"), "Should show SOFT/WEAK reference type\n" + output);
        assertTrue(output.contains("WEAK/SOFT:"), "Should show WEAK/SOFT reference type\n" + output);
        assertTrue(output.contains("caches"), "Should show cache creation count\n" + output);
        assertTrue(output.contains("accesses"), "Should show access count\n" + output);
        assertTrue(output.contains("hit ratio"), "Should show hit ratio\n" + output);

        // Verify that different hit ratios are shown correctly
        assertTrue(
                output.contains("66.7%") || output.contains("66.6%"),
                "Should show HARD/HARD hit ratio (~66.7%):\n" + output);
        assertTrue(output.contains("33.3%"), "Should show SOFT/WEAK hit ratio (33.3%):\n" + output);
        assertTrue(output.contains("0.0%"), "Should show WEAK/SOFT hit ratio (0.0%):\n" + output);
    }

    @Test
    void shouldShowMemoryPressureIndicators() {
        CacheStatistics statistics = new CacheStatistics();

        // Create scenario that might indicate memory pressure
        statistics.recordCacheCreation("HARD", "HARD", CacheRetention.SESSION_SCOPED);
        statistics.recordCacheCreation("SOFT", "SOFT", CacheRetention.SESSION_SCOPED);

        // Simulate many cache accesses with hard references (potential OOM risk)
        for (int i = 0; i < 1000; i++) {
            statistics.recordCacheAccess("HARD", "HARD", true);
        }

        // Simulate some soft reference usage
        for (int i = 0; i < 100; i++) {
            statistics.recordCacheAccess("SOFT", "SOFT", i % 2 == 0);
        }

        String output = DefaultRequestCache.formatCacheStatistics(statistics);

        // Should show high usage of hard references
        assertTrue(output.contains("HARD/HARD:"), "Should show hard reference usage: \n" + output);
        assertTrue(output.contains("1000 accesses"), "Should show high access count for hard references: \n" + output);
    }
}
