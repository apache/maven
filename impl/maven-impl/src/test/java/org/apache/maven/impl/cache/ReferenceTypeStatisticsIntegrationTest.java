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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceTypeStatisticsIntegrationTest {

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

        // Use reflection to call the private formatCacheStatistics method
        try {
            var method = DefaultRequestCache.class.getDeclaredMethod("formatCacheStatistics", CacheStatistics.class);
            method.setAccessible(true);
            String output = (String) method.invoke(null, statistics);

            System.out.println("=== Enhanced Cache Statistics Output ===");
            System.out.println(output);

            // Verify that reference type information is included
            assertTrue(output.contains("Reference type usage:"), "Should contain reference type section");
            assertTrue(output.contains("HARD/HARD:"), "Should show HARD/HARD reference type");
            assertTrue(output.contains("SOFT/WEAK:"), "Should show SOFT/WEAK reference type");
            assertTrue(output.contains("WEAK/SOFT:"), "Should show WEAK/SOFT reference type");
            assertTrue(output.contains("caches"), "Should show cache creation count");
            assertTrue(output.contains("accesses"), "Should show access count");
            assertTrue(output.contains("hit ratio"), "Should show hit ratio");

            // Verify that different hit ratios are shown correctly
            assertTrue(
                    output.contains("66.7%") || output.contains("66.6%"), "Should show HARD/HARD hit ratio (~66.7%)");
            assertTrue(output.contains("33.3%"), "Should show SOFT/WEAK hit ratio (33.3%)");
            assertTrue(output.contains("0.0%"), "Should show WEAK/SOFT hit ratio (0.0%)");

        } catch (Exception e) {
            throw new RuntimeException("Failed to test statistics output", e);
        }
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

        try {
            var method = DefaultRequestCache.class.getDeclaredMethod("formatCacheStatistics", CacheStatistics.class);
            method.setAccessible(true);
            String output = (String) method.invoke(null, statistics);

            System.out.println("=== Memory Pressure Analysis ===");
            System.out.println(output);

            // Should show high usage of hard references
            assertTrue(output.contains("HARD/HARD:"), "Should show hard reference usage");
            assertTrue(output.contains("1000 accesses"), "Should show high access count for hard references");

        } catch (Exception e) {
            throw new RuntimeException("Failed to test memory pressure indicators", e);
        }
    }
}
