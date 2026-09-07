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
package org.apache.maven.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.graph.DependencyFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultProjectRealmCache}.
 */
class DefaultProjectRealmCacheTest {

    @Test
    void testPutReturnsSameRecordForDuplicateKey() throws Exception {
        DefaultProjectRealmCache cache = new DefaultProjectRealmCache();
        ClassWorld world = new ClassWorld();
        ClassRealm realm1 = world.newRealm("test-realm-1");
        ClassRealm realm2 = world.newRealm("test-realm-2");
        DependencyFilter filter = mock(DependencyFilter.class);

        ProjectRealmCache.Key key = cache.createKey(Collections.singletonList(realm1));

        // First put should succeed and return a record with our realm
        ProjectRealmCache.CacheRecord record1 = cache.put(key, realm1, filter);
        assertNotNull(record1);
        assertSame(realm1, record1.getRealm());

        // Second put with the same key should return the existing record
        ProjectRealmCache.Key sameKey = cache.createKey(Collections.singletonList(realm1));
        ProjectRealmCache.CacheRecord record2 = cache.put(sameKey, realm2, filter);
        assertNotNull(record2);
        assertSame(record1, record2, "Duplicate put should return the existing cached record");
        assertSame(realm1, record2.getRealm(), "Duplicate put should return the first realm, not the second");
    }

    @Test
    void testConcurrentPutWithSameKeyDoesNotThrow() throws Exception {
        DefaultProjectRealmCache cache = new DefaultProjectRealmCache();
        ClassWorld world = new ClassWorld();

        // Create a shared extension realm for the cache key
        ClassRealm sharedExtensionRealm = world.newRealm("shared-extension");
        DependencyFilter filter = mock(DependencyFilter.class);

        int threadCount = 8;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            List<Future<ProjectRealmCache.CacheRecord>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    ClassRealm projectRealm = world.newRealm("project-realm-" + index);
                    ProjectRealmCache.Key key = cache.createKey(Collections.singletonList(sharedExtensionRealm));
                    barrier.await(); // synchronize all threads to maximize contention
                    return cache.put(key, projectRealm, filter);
                }));
            }

            // All threads should complete without throwing IllegalStateException
            ProjectRealmCache.CacheRecord firstRecord = null;
            for (Future<ProjectRealmCache.CacheRecord> future : futures) {
                ProjectRealmCache.CacheRecord record = future.get();
                assertNotNull(record);
                if (firstRecord == null) {
                    firstRecord = record;
                }
                // All threads should get back the same cached record
                assertSame(firstRecord, record, "All concurrent puts should return the same cached record");
            }

            // Cache should contain exactly one entry
            assertEquals(1, cache.cache.size(), "Cache should contain exactly one entry");
        } finally {
            executor.shutdown();
        }
    }
}
