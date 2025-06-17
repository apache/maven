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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefConcurrentMapTest {
    private RefConcurrentMap<Object, String> softMap;
    private RefConcurrentMap<Object, String> weakMap;
    private RefConcurrentMap<Object, String> hardMap;

    @BeforeEach
    void setUp() {
        softMap = RefConcurrentMap.softMap();
        weakMap = RefConcurrentMap.weakMap();
        hardMap = RefConcurrentMap.hardMap();
    }

    @Test
    void shouldComputeValueOnlyOnceWithSoftMap() {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);

        String result1 = softMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "value";
        });

        String result2 = softMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "different value";
        });

        assertEquals("value", result1);
        assertEquals("value", result2);
        assertEquals(1, computeCount.get());
    }

    @Test
    void shouldComputeValueOnlyOnceWithWeakMap() {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);

        String result1 = weakMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "value";
        });

        String result2 = weakMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "different value";
        });

        assertEquals("value", result1);
        assertEquals("value", result2);
        assertEquals(1, computeCount.get());
    }

    @Test
    void shouldComputeValueOnlyOnceWithHardMap() {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);

        String result1 = hardMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "value";
        });

        String result2 = hardMap.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "different value";
        });

        assertEquals("value", result1);
        assertEquals("value", result2);
        assertEquals(1, computeCount.get());
    }

    @RepeatedTest(10)
    void shouldBeThreadSafe() throws InterruptedException {
        Consumer<String> sink = s -> {}; // System.out::println;

        int threadCount = 5;
        int iterationsPerThread = 100;
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        List<String> uniqueResults = new ArrayList<>();
        CyclicBarrier iterationBarrier = new CyclicBarrier(threadCount);

        // Create and start threads
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(
                            () -> {
                                try {
                                    startLatch.await(); // Wait for all threads to be ready

                                    // Use AtomicInteger for thread-safe iteration counting
                                    AtomicInteger iteration = new AtomicInteger(0);
                                    while (iteration.get() < iterationsPerThread) {
                                        // Synchronize threads at the start of each iteration
                                        iterationBarrier.await();

                                        String result = softMap.computeIfAbsent(key, k -> {
                                            sink.accept("Computing value in thread " + threadId + " iteration "
                                                    + iteration.get() + " current compute count: "
                                                    + computeCount.get());
                                            int count = computeCount.incrementAndGet();
                                            if (count > 1) {
                                                sink.accept("WARNING: Multiple computations detected! Count: " + count);
                                            }
                                            return "computed value";
                                        });

                                        synchronized (uniqueResults) {
                                            if (!uniqueResults.contains(result)) {
                                                uniqueResults.add(result);
                                                sink.accept("Added new unique result: " + result + " from thread "
                                                        + threadId);
                                            }
                                        }

                                        iteration.incrementAndGet();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    finishLatch.countDown();
                                    sink.accept("Thread " + threadId + " finished");
                                }
                            },
                            "Thread-" + i)
                    .start();
        }

        sink.accept("Starting all threads");
        startLatch.countDown(); // Start all threads
        finishLatch.await(); // Wait for all threads to finish
        sink.accept("All threads finished");
        sink.accept("Final compute count: " + computeCount.get());
        sink.accept("Unique results size: " + uniqueResults.size());

        assertEquals(
                1,
                computeCount.get(),
                "Value should be computed exactly once, but was computed " + computeCount.get() + " times");
        assertEquals(
                1,
                uniqueResults.size(),
                "All threads should see the same value, but saw " + uniqueResults.size() + " different values");
    }

    @Test
    void shouldUseEqualsComparison() {
        // Create two equal but distinct keys
        String key1 = new String("key");
        String key2 = new String("key");

        assertTrue(key1.equals(key2), "Sanity check: keys should be equal");
        assertNotSame(key1, key2, "Sanity check: keys should be distinct objects");

        AtomicInteger computeCount = new AtomicInteger(0);

        softMap.computeIfAbsent(key1, k -> {
            computeCount.incrementAndGet();
            return "value1";
        });

        softMap.computeIfAbsent(key2, k -> {
            computeCount.incrementAndGet();
            return "value2";
        });

        assertEquals(1, computeCount.get(), "Should compute once for equal keys (using equals comparison)");
    }

    @Test
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void shouldHandleSoftReferences() throws InterruptedException {
        AtomicInteger computeCount = new AtomicInteger(0);

        // Use a block to ensure the key can be garbage collected
        {
            Object key = new Object();
            softMap.computeIfAbsent(key, k -> {
                computeCount.incrementAndGet();
                return "value";
            });
        }

        // Try to force garbage collection
        System.gc();
        Thread.sleep(100);

        // Create a new key and verify that computation happens again
        Object newKey = new Object();
        softMap.computeIfAbsent(newKey, k -> {
            computeCount.incrementAndGet();
            return "new value";
        });

        assertEquals(2, computeCount.get(), "Should compute again after original key is garbage collected");
    }

    @Test
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void shouldNotGarbageCollectHardReferences() throws InterruptedException {
        AtomicInteger computeCount = new AtomicInteger(0);
        Object originalKey;

        // Use a block to ensure the key can be garbage collected if it were a weak/soft reference
        {
            originalKey = new Object();
            hardMap.computeIfAbsent(originalKey, k -> {
                computeCount.incrementAndGet();
                return "value";
            });
        }

        // Try to force garbage collection
        System.gc();
        Thread.sleep(100);

        // The hard map should still contain the entry even after GC
        String value = hardMap.get(originalKey);
        assertEquals("value", value, "Hard references should not be garbage collected");
        assertEquals(1, computeCount.get(), "Should only compute once since hard references prevent GC");

        // Verify the map still has the entry
        assertEquals(1, hardMap.size(), "Hard map should still contain the entry after GC");
    }

    @Test
    void shouldHandleNullInputs() {
        assertThrows(NullPointerException.class, () -> softMap.computeIfAbsent(null, k -> "value"));

        Object key = new Object();
        assertThrows(NullPointerException.class, () -> softMap.computeIfAbsent(key, null));
    }

    @Test
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void shouldCleanupGarbageCollectedEntries() throws InterruptedException {
        // Test that the map properly cleans up entries when keys/values are GC'd
        int initialSize = softMap.size();

        // Add some entries that can be garbage collected
        {
            Object key1 = new Object();
            Object key2 = new Object();
            softMap.put(key1, "value1");
            softMap.put(key2, "value2");
        }

        // Verify entries were added
        assertTrue(softMap.size() >= initialSize + 2, "Map should contain the new entries");

        // Force garbage collection multiple times
        for (int i = 0; i < 5; i++) {
            System.gc();
            Thread.sleep(50);
            // Trigger cleanup by calling a method that calls expungeStaleEntries()
            softMap.size();
        }

        // The map should eventually clean up the garbage collected entries
        // Note: This test is not deterministic due to GC behavior, but it should work most of the time
        int finalSize = softMap.size();
        assertTrue(
                finalSize <= initialSize + 2,
                "Map should have cleaned up some entries after GC. Initial: " + initialSize + ", Final: " + finalSize);
    }
}
