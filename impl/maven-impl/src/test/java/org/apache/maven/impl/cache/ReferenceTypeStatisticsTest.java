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

import org.apache.maven.api.cache.CacheRetention;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTypeStatisticsTest {

    private CacheStatistics statistics;

    @BeforeEach
    void setUp() {
        statistics = new CacheStatistics();
    }

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

        var refTypeStats = statistics.getReferenceTypeStatistics();

        // Should have two reference type combinations
        assertEquals(2, refTypeStats.size());

        // Check SOFT/WEAK statistics
        var softWeakStats = refTypeStats.get("SOFT/WEAK");
        assertNotNull(softWeakStats);
        assertEquals(2, softWeakStats.getCacheCreations());
        assertEquals(1, softWeakStats.getHits());
        assertEquals(1, softWeakStats.getMisses());
        assertEquals(2, softWeakStats.getTotal());
        assertEquals(50.0, softWeakStats.getHitRatio(), 0.1);

        // Check HARD/SOFT statistics
        var hardSoftStats = refTypeStats.get("HARD/SOFT");
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
}
