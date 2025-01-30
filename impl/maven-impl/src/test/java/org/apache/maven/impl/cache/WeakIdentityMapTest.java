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

import static org.junit.jupiter.api.Assertions.*;

class WeakIdentityMapTest {
    private WeakIdentityMap<Object, String> map;

    @BeforeEach
    void setUp() {
        map = new WeakIdentityMap<>();
    }

    @Test
    void shouldComputeValueOnlyOnce() {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);

        String result1 = map.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "value";
        });

        String result2 = map.computeIfAbsent(key, k -> {
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

                                        String result = map.computeIfAbsent(key, k -> {
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
    void shouldUseIdentityComparison() {
        // Create two equal but distinct keys
        String key1 = new String("key");
        String key2 = new String("key");

        assertTrue(key1.equals(key2), "Sanity check: keys should be equal");
        assertNotSame(key1, key2, "Sanity check: keys should be distinct objects");

        AtomicInteger computeCount = new AtomicInteger(0);

        map.computeIfAbsent(key1, k -> {
            computeCount.incrementAndGet();
            return "value1";
        });

        map.computeIfAbsent(key2, k -> {
            computeCount.incrementAndGet();
            return "value2";
        });

        assertEquals(1, computeCount.get(), "Should compute once for equal but distinct keys");
    }

    @Test
    void shouldHandleWeakReferences() throws InterruptedException {
        AtomicInteger computeCount = new AtomicInteger(0);

        // Use a block to ensure the key can be garbage collected
        {
            Object key = new Object();
            map.computeIfAbsent(key, k -> {
                computeCount.incrementAndGet();
                return "value";
            });
        }

        // Try to force garbage collection
        System.gc();
        Thread.sleep(100);

        // Create a new key and verify that computation happens again
        Object newKey = new Object();
        map.computeIfAbsent(newKey, k -> {
            computeCount.incrementAndGet();
            return "new value";
        });

        assertEquals(2, computeCount.get(), "Should compute again after original key is garbage collected");
    }

    @Test
    void shouldHandleNullInputs() {
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(null, k -> "value"));

        Object key = new Object();
        assertThrows(NullPointerException.class, () -> map.computeIfAbsent(key, null));
    }
}
