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
package org.apache.maven.lifecycle.internal.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The executions of a project must not overlap, as each of them replaces the artifacts of the shared project (#13590).
 */
class MojoExecutorTest {

    private final MojoExecutor executor = new MojoExecutor(null, null, null, null, null, null);

    private final Map<Object, Object> data = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private MavenSession session(MavenProject project) {
        SessionData sessionData = mock(SessionData.class);
        when(sessionData.computeIfAbsent(any(), any())).thenAnswer(invocation -> {
            Supplier<Object> supplier = invocation.getArgument(1);
            return data.computeIfAbsent(invocation.getArgument(0), k -> supplier.get());
        });
        Session apiSession = mock(Session.class);
        when(apiSession.getData()).thenReturn(sessionData);
        MavenSession session = mock(MavenSession.class);
        when(session.getSession()).thenReturn(apiSession);
        when(session.getCurrentProject()).thenReturn(project);
        return session;
    }

    private static MavenProject project(String artifactId) {
        org.apache.maven.model.Model model = new org.apache.maven.model.Model();
        model.setGroupId("g");
        model.setArtifactId(artifactId);
        model.setVersion("1");
        return new MavenProject(model);
    }

    private static MojoDescriptor mojoWithoutDependencies() {
        return new MojoDescriptor();
    }

    private static MojoDescriptor v4Mojo() {
        return new MojoDescriptor(
                null,
                org.apache.maven.api.plugin.descriptor.MojoDescriptor.newBuilder()
                        .goal("test")
                        .dependencyResolution("compile")
                        .build());
    }

    @Test
    void testExecutionsOfTheSameProjectDoNotOverlap() throws Exception {
        MavenSession session = session(project("a"));
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch holding = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Future<?> first = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(session, mojoWithoutDependencies())) {
                    holding.countDown();
                    release.await();
                }
                return null;
            });
            assertTrue(holding.await(5, TimeUnit.SECONDS));

            Future<?> second = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(session, mojoWithoutDependencies())) {
                    return null;
                }
            });
            assertThrows(TimeoutException.class, () -> second.get(300, TimeUnit.MILLISECONDS));

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    void testV4MojosOfTheSameProjectRunInParallel() throws Exception {
        MavenSession session = session(project("a"));
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch holding = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Future<?> first = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(session, v4Mojo())) {
                    holding.countDown();
                    release.await();
                }
                return null;
            });
            assertTrue(holding.await(5, TimeUnit.SECONDS));

            Future<Boolean> second = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(session, v4Mojo())) {
                    return true;
                }
            });
            assertTrue(second.get(5, TimeUnit.SECONDS));
            assertFalse(first.isDone());

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            threads.shutdownNow();
        }
    }

    @Test
    void testExecutionsOfDifferentProjectsRunInParallel() throws Exception {
        MavenSession sessionA = session(project("a"));
        MavenSession sessionB = session(project("b"));
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch holding = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Future<?> first = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(sessionA, mojoWithoutDependencies())) {
                    holding.countDown();
                    release.await();
                }
                return null;
            });
            assertTrue(holding.await(5, TimeUnit.SECONDS));

            Future<Boolean> second = threads.submit(() -> {
                try (AutoCloseable lock = executor.getProjectLock(sessionB, mojoWithoutDependencies())) {
                    return true;
                }
            });
            assertTrue(second.get(5, TimeUnit.SECONDS));
            assertFalse(first.isDone());

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
        } finally {
            threads.shutdownNow();
        }
    }
}
