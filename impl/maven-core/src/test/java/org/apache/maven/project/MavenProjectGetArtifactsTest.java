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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MavenProjectGetArtifactsTest {

    @Test
    void concurrentGetArtifactsDoesNotExposeAHalfBuiltSet() throws Exception {
        MavenProject project = new MavenProject();
        Set<Artifact> resolved = new LinkedHashSet<>();
        for (int i = 0; i < 200; i++) {
            resolved.add(new DefaultArtifact("g", "a" + i, "1", "compile", "jar", "", null));
        }
        project.setResolvedArtifacts(resolved);
        project.setArtifactFilter(artifact -> {
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(50));
            return true;
        });

        int readers = 4;
        ExecutorService pool = Executors.newFixedThreadPool(readers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < readers; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    Set<Artifact> artifacts = project.getArtifacts();
                    assertEquals(resolved.size(), artifacts.size());
                    AtomicInteger seen = new AtomicInteger();
                    artifacts.forEach(artifact -> seen.incrementAndGet());
                    assertEquals(resolved.size(), seen.get());
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
                }
            }
        } finally {
            pool.shutdownNow();
        }

        assertFalse(project.getArtifacts().isEmpty());
        assertEquals(resolved.size(), project.getArtifacts().size());
    }
}
