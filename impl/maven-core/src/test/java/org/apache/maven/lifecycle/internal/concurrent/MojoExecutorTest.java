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

import java.util.List;
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
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The concurrent builder runs independent steps of one project in parallel. Every mojo execution stores the
 * dependencies of its step and its scope filter on the shared {@link MavenProject}, so executions on the same
 * project must not overlap, while executions on different projects keep running in parallel.
 */
class MojoExecutorTest {

    private final MojoExecutor executor = new MojoExecutor(null, null, null, null, null, null);

    private final ExecutorService threads = Executors.newCachedThreadPool();

    private final MavenSession session = newSession();

    @AfterEach
    void shutdown() {
        threads.shutdownNow();
    }

    @Test
    void mojosOfTheSameProjectDoNotOverlap() throws Exception {
        MavenProject project = project("a");
        CountDownLatch release = holdLock(project, resolving("compile"));

        Future<?> other = acquire(project, collecting("runtime"));

        assertThrows(TimeoutException.class, () -> other.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        other.get(10, TimeUnit.SECONDS);
    }

    @Test
    void mojosWithoutOwnDependencyRequirementsAlsoWait() throws Exception {
        MavenProject project = project("a");
        CountDownLatch release = holdLock(project, resolving("compile"));

        Future<?> other = acquire(project, new MojoDescriptor());

        assertThrows(TimeoutException.class, () -> other.get(200, TimeUnit.MILLISECONDS));
        release.countDown();
        other.get(10, TimeUnit.SECONDS);
    }

    @Test
    void differentProjectsKeepRunningInParallel() throws Exception {
        CountDownLatch release = holdLock(project("a"), resolving("compile"));

        acquire(project("b"), resolving("compile")).get(10, TimeUnit.SECONDS);

        release.countDown();
    }

    private CountDownLatch holdLock(MavenProject project, MojoDescriptor mojo) throws Exception {
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        threads.submit(() -> {
            session.setCurrentProject(project);
            try (AutoCloseable lock = executor.getProjectLock(session, mojo)) {
                acquired.countDown();
                release.await();
            }
            return null;
        });
        assertTrue(acquired.await(10, TimeUnit.SECONDS));
        return release;
    }

    private Future<?> acquire(MavenProject project, MojoDescriptor mojo) {
        return threads.submit(() -> {
            session.setCurrentProject(project);
            try (AutoCloseable lock = executor.getProjectLock(session, mojo)) {
                return null;
            }
        });
    }

    private static MavenProject project(String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId(artifactId);
        project.setVersion("1.0");
        return project;
    }

    private static MojoDescriptor resolving(String scope) {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setDependencyResolutionRequired(scope);
        return mojo;
    }

    private static MojoDescriptor collecting(String scope) {
        MojoDescriptor mojo = new MojoDescriptor();
        mojo.setDependencyCollectionRequired(scope);
        return mojo;
    }

    private static MavenSession newSession() {
        MavenSession session = new MavenSession(
                null,
                new DefaultRepositorySystemSession(h -> false),
                new DefaultMavenExecutionRequest(),
                new DefaultMavenExecutionResult());
        session.setProjects(List.of());
        Session apiSession = mock(Session.class);
        when(apiSession.getData()).thenReturn(new MapSessionData());
        session.setSession(apiSession);
        return session;
    }

    private static final class MapSessionData implements SessionData {
        private final Map<Key<?>, Object> map = new ConcurrentHashMap<>();

        @Override
        public <T> void set(Key<T> key, T value) {
            map.put(key, value);
        }

        @Override
        public <T> boolean replace(Key<T> key, T oldValue, T newValue) {
            return map.replace(key, oldValue, newValue);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Key<T> key) {
            return (T) map.get(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T computeIfAbsent(Key<T> key, Supplier<T> supplier) {
            return (T) map.computeIfAbsent(key, k -> supplier.get());
        }
    }
}
