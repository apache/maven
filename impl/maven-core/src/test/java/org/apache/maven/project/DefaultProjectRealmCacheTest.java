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
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.eclipse.aether.graph.DependencyFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class DefaultProjectRealmCacheTest {

    @Test
    void testConcurrentPutWithSameKey() throws Exception {
        DefaultProjectRealmCache cache = new DefaultProjectRealmCache();
        ClassRealm realm = mock(ClassRealm.class);
        DependencyFilter filter = mock(DependencyFilter.class);
        ProjectRealmCache.Key key = cache.createKey(List.of(realm));

        int threadCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                barrier.await();
                try {
                    cache.put(key, mock(ClassRealm.class), mock(DependencyFilter.class));
                    return true;
                } catch (IllegalStateException e) {
                    return false;
                }
            }));
        }

        int successCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) {
                successCount++;
            }
        }
        executor.shutdown();

        assertEquals(1, successCount, "Only one put should succeed");
        assertNotNull(cache.get(key));
    }
}
