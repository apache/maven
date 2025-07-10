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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTest {
    private Cache<Object, String> cache;

    @BeforeEach
    void setUp() {
        cache = Cache.newCache();
    }

    @Test
    void shouldComputeValueOnlyOnce() {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);

        String result1 = cache.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "value";
        });

        String result2 = cache.computeIfAbsent(key, k -> {
            computeCount.incrementAndGet();
            return "different value";
        });

        assertEquals("value", result1);
        assertEquals("value", result2);
        assertEquals(1, computeCount.get());
    }

    @Test
    void shouldHandleConcurrentComputation() throws InterruptedException {
        Object key = new Object();
        AtomicInteger computeCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    cache.computeIfAbsent(key, k -> {
                        computeCount.incrementAndGet();
                        try {
                            Thread.sleep(10); // Simulate computation time
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return "value";
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, computeCount.get(), "Should compute only once even with concurrent access");
        executor.shutdown();
    }

    @Test
    void shouldUseIdentityBasedComparison() {
        String key1 = new String("test");
        String key2 = new String("test");

        cache.put(key1, "value1");
        cache.put(key2, "value2");

        assertEquals("value1", cache.get(key1));
        assertEquals("value2", cache.get(key2));
        assertEquals(2, cache.size());
    }

    @Test
    void shouldHandleBasicOperations() {
        Object key = new Object();

        // Test put and get
        assertNull(cache.put(key, "value"));
        assertEquals("value", cache.get(key));
        assertEquals(1, cache.size());
        assertFalse(cache.isEmpty());

        // Test replace
        assertEquals("value", cache.put(key, "new value"));
        assertEquals("new value", cache.get(key));

        // Test remove
        assertEquals("new value", cache.remove(key));
        assertNull(cache.get(key));
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @Test
    void shouldHandleNullValues() {
        Object key = new Object();

        // computeIfAbsent with null result
        assertNull(cache.computeIfAbsent(key, k -> null));
        assertNull(cache.get(key));
        assertEquals(0, cache.size());

        // get with null key
        assertNull(cache.get(null));

        // remove with null key
        assertNull(cache.remove(null));
    }

    @Test
    void shouldRejectNullInputs() {
        Object key = new Object();

        assertThrows(NullPointerException.class, () -> cache.computeIfAbsent(null, k -> "value"));
        assertThrows(NullPointerException.class, () -> cache.computeIfAbsent(key, null));
        assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
        assertThrows(NullPointerException.class, () -> cache.put(key, null));
    }

    @Test
    void shouldHandleClear() {
        Object key1 = new Object();
        Object key2 = new Object();

        cache.put(key1, "value1");
        cache.put(key2, "value2");
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        assertNull(cache.get(key1));
        assertNull(cache.get(key2));
    }

    @Test
    void shouldHandleComputationExceptions() {
        Object key = new Object();
        RuntimeException exception = new RuntimeException("computation failed");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> cache.computeIfAbsent(key, k -> {
                    throw exception;
                }));

        assertEquals(exception, thrown);
        assertNull(cache.get(key));
        assertEquals(0, cache.size());
    }

    @Test
    @SuppressWarnings("checkstyle:AvoidNestedBlocks")
    void shouldHandleSoftReferences() throws InterruptedException {
        AtomicInteger computeCount = new AtomicInteger(0);

        // Use a block to ensure the key can be garbage collected
        {
            Object key = new Object();
            cache.computeIfAbsent(key, k -> {
                computeCount.incrementAndGet();
                return "value";
            });
        }

        // Try to force garbage collection
        System.gc();
        Thread.sleep(100);

        // Create a new key and verify that computation happens again
        Object newKey = new Object();
        cache.computeIfAbsent(newKey, k -> {
            computeCount.incrementAndGet();
            return "new value";
        });

        assertEquals(2, computeCount.get(), "Should compute again after original key is garbage collected");
    }
}
