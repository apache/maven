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
package org.apache.maven.impl.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Priority;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ModelBuilder;
import org.apache.maven.api.services.ModelBuilderException;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelBuilderResult;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.api.spi.ModelTransformer;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.standalone.ApiRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BomRelocationConcurrencyTest {
    private static final int TIMEOUT_SECONDS = 20;

    @TempDir
    Path tempDir;

    private ModelResolver resolver;
    private CountingModelTransformer transformer;

    @Test
    void detectsConcurrentRelocationCycle() throws Exception {
        buildConcurrently(Map.of("a", relocation("b"), "b", relocation("a")), true);
    }

    @Test
    void detectsConcurrentMixedImportRelocationCycle() throws Exception {
        // Without relocation, these are two acyclic import graphs, not an existing import-only cycle.
        buildConcurrently(
                Map.of(
                        "a", management(bom("x")),
                        "x", relocation("b"),
                        "b", management(bom("y")),
                        "y", relocation("a")),
                true);
    }

    @Test
    void concurrentAliasesImportSharedTarget() throws Exception {
        buildConcurrently(
                Map.of(
                        "a", relocation("target"),
                        "b", relocation("target"),
                        "target",
                                management("<dependency><groupId>test</groupId><artifactId>library</artifactId>"
                                        + "<version>2</version></dependency>")),
                false);
    }

    @Test
    void concurrentOrdinaryImportsBuildSharedBomOnce() throws Exception {
        buildConcurrently(
                Map.of(
                        "a", management(bom("target")),
                        "b", management(bom("target")),
                        "target",
                                management("<dependency><groupId>test</groupId><artifactId>library</artifactId>"
                                        + "<version>2</version></dependency>")),
                false,
                true);
    }

    private void buildConcurrently(Map<String, String> contents, boolean expectCycle) throws Exception {
        buildConcurrently(contents, expectCycle, false);
    }

    private void buildConcurrently(Map<String, String> contents, boolean expectCycle, boolean coldOrdinary)
            throws Exception {
        Map<String, ModelSource> sources = new HashMap<>();
        for (var entry : contents.entrySet()) {
            Path path = tempDir.resolve(entry.getKey() + ".pom");
            Files.writeString(path, pom(entry.getKey(), entry.getValue()));
            sources.put("test:" + entry.getKey() + ":1", Sources.resolvedSource(path, "test:" + entry.getKey() + ":1"));
        }
        BarrierModelResolver barrierResolver = new BarrierModelResolver(sources, !expectCycle && !coldOrdinary);
        resolver = barrierResolver;
        transformer = new CountingModelTransformer(coldOrdinary);
        Session session = ApiRunner.createSession(
                injector -> injector.bindInstance(BomRelocationConcurrencyTest.class, this),
                tempDir.resolve("local-repo"));
        ModelBuilder builder = session.getService(ModelBuilder.class);

        // Parallel module builds share the outer request, hence the same request-scoped import cache.
        Path root = tempDir.resolve("root.pom");
        Files.writeString(root, pom("root", ""));
        ModelBuilderRequest outerRequest = ModelBuilderRequest.builder()
                .session(session)
                .requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT)
                .source(Sources.buildSource(root))
                .build();
        RequestTrace outerTrace = new RequestTrace(null, outerRequest);
        List<FutureTask<ModelBuilderResult>> tasks = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (String artifactId : List.of("a", "b")) {
            Path consumer = tempDir.resolve("consumer-" + artifactId + ".pom");
            Files.writeString(consumer, pom("consumer-" + artifactId, management(bom(artifactId))));
            ModelBuilderRequest request = ModelBuilderRequest.builder()
                    .session(session)
                    .trace(new RequestTrace(outerTrace, consumer))
                    // Avoid another executor: only these daemon threads may block on a regression.
                    .requestType(ModelBuilderRequest.RequestType.BUILD_EFFECTIVE)
                    .source(Sources.buildSource(consumer))
                    .build();
            FutureTask<ModelBuilderResult> task = new FutureTask<>(() -> {
                try {
                    return builder.newSession().build(request);
                } finally {
                    InternalSession.from(session).setCurrentTrace(null);
                    if ("a".equals(artifactId)) {
                        barrierResolver.firstBuildFinished.countDown();
                    }
                }
            });
            Thread thread = new Thread(task, "bom-relocation-" + artifactId);
            // Monitor acquisition cannot be interrupted; a deadlock must not keep Surefire alive.
            thread.setDaemon(true);
            tasks.add(task);
            threads.add(thread);
        }

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        try {
            threads.forEach(Thread::start);
            if (coldOrdinary) {
                transformer.awaitContendingBuild(threads, deadline);
                transformer.releaseTarget.countDown();
            }
            for (FutureTask<ModelBuilderResult> task : tasks) {
                try {
                    ModelBuilderResult result =
                            task.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    assertFalse(expectCycle, "Build completed without detecting the relocation cycle");
                    assertEquals(
                            List.of("library:2"),
                            result.getEffectiveModel().getDependencyManagement().getDependencies().stream()
                                    .map(d -> d.getArtifactId() + ":" + d.getVersion())
                                    .toList());
                } catch (ExecutionException e) {
                    if (!expectCycle) {
                        throw e;
                    }
                    ModelBuilderException failure = assertInstanceOf(ModelBuilderException.class, e.getCause());
                    assertTrue(failure.getMessage().contains("form a cycle"), failure.getMessage());
                    assertTrue(failure.getMessage().contains("test:a:1"), failure.getMessage());
                    assertTrue(failure.getMessage().contains("test:b:1"), failure.getMessage());
                } catch (TimeoutException e) {
                    fail(
                            "Concurrent BOM resolution did not terminate; initial resolver barrier remaining: "
                                    + barrierResolver.firstLoads.getCount() + threadStacks(threads),
                            e);
                }
            }
            assertEquals(0, barrierResolver.firstLoads.getCount(), "Both source loads must overlap");
            if (!expectCycle) {
                assertEquals(
                        Map.of("test:a:1", 1, "test:b:1", 1, "test:target:1", 1),
                        barrierResolver.resolveCounts,
                        "The second model build must reuse the target loaded by the first build");
                assertEquals(1, transformer.targetBuilds.get(), "The target effective model must be built once");
            }
        } finally {
            transformer.releaseTarget.countDown();
            tasks.forEach(task -> task.cancel(true));
            threads.forEach(Thread::interrupt);
            for (Thread thread : threads) {
                thread.join(100);
            }
        }
    }

    private static String threadStacks(List<Thread> threads) {
        StringBuilder message = new StringBuilder();
        for (Thread thread : threads) {
            message.append('\n').append(thread.getName()).append(": ").append(thread.getState());
            for (StackTraceElement frame : thread.getStackTrace()) {
                message.append("\n    at ").append(frame);
            }
        }
        return message.toString();
    }

    @Provides
    @Priority(100)
    ModelResolver modelResolver() {
        return resolver;
    }

    @Provides
    ModelTransformer modelTransformer() {
        return transformer;
    }

    private static String pom(String artifactId, String content) {
        return "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>" + artifactId
                + "</artifactId><version>1</version><packaging>pom</packaging>" + content + "</project>";
    }

    private static String relocation(String artifactId) {
        return "<distributionManagement><relocation><artifactId>" + artifactId
                + "</artifactId></relocation></distributionManagement>";
    }

    private static String management(String dependencies) {
        return "<dependencyManagement><dependencies>" + dependencies + "</dependencies></dependencyManagement>";
    }

    private static String bom(String artifactId) {
        return "<dependency><groupId>test</groupId><artifactId>" + artifactId
                + "</artifactId><version>1</version><type>pom</type><scope>import</scope></dependency>";
    }

    private static final class CountingModelTransformer implements ModelTransformer {
        private final boolean blockTarget;
        private final AtomicInteger targetBuilds = new AtomicInteger();
        private final AtomicReference<Thread> firstBuilder = new AtomicReference<>();
        private final CountDownLatch targetStarted = new CountDownLatch(1);
        private final CountDownLatch releaseTarget = new CountDownLatch(1);

        private CountingModelTransformer(boolean blockTarget) {
            this.blockTarget = blockTarget;
        }

        @Override
        public Model transformEffectiveModel(Model model) {
            if ("test".equals(model.getGroupId()) && "target".equals(model.getArtifactId())) {
                targetBuilds.incrementAndGet();
                if (blockTarget && firstBuilder.compareAndSet(null, Thread.currentThread())) {
                    targetStarted.countDown();
                    try {
                        if (!releaseTarget.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                            throw new AssertionError("The shared target build was not released");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("The shared target build was interrupted", e);
                    }
                }
            }
            return model;
        }

        private void awaitContendingBuild(List<Thread> threads, long deadline) throws InterruptedException {
            assertTrue(
                    targetStarted.await(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS),
                    "The first target effective-model build did not start");
            // Keep the first build cold until another caller waits for it or incorrectly duplicates it.
            while (System.nanoTime() < deadline) {
                if (targetBuilds.get() > 1) {
                    return;
                }
                for (Thread thread : threads) {
                    if (thread != firstBuilder.get() && waitsForImport(thread)) {
                        return;
                    }
                }
                Thread.sleep(10);
            }
            fail("The second caller neither waited nor built the shared BOM" + threadStacks(threads));
        }

        private static boolean waitsForImport(Thread thread) {
            if (thread.getState() != Thread.State.WAITING && thread.getState() != Thread.State.BLOCKED) {
                return false;
            }
            boolean importFrame = false;
            boolean lockFrame = false;
            for (StackTraceElement frame : thread.getStackTrace()) {
                importFrame |= frame.getClassName().startsWith(DefaultModelBuilder.class.getName())
                        && ("loadImportModel".equals(frame.getMethodName())
                                || "loadDependencyManagement".equals(frame.getMethodName()));
                lockFrame |= frame.getClassName().startsWith("java.util.concurrent.locks.ReentrantLock")
                        || frame.getClassName().equals("org.apache.maven.impl.cache.CachingSupplier");
            }
            return importFrame && lockFrame;
        }
    }

    private static final class BarrierModelResolver implements ModelResolver {
        private final Map<String, ModelSource> sources;
        private final boolean orderTargetLoads;
        private final Set<String> firstCoordinates = ConcurrentHashMap.newKeySet();
        private final Map<String, Integer> resolveCounts = new ConcurrentHashMap<>();
        private final CountDownLatch firstLoads = new CountDownLatch(2);
        private final CountDownLatch firstBuildFinished = new CountDownLatch(1);

        private BarrierModelResolver(Map<String, ModelSource> sources, boolean orderTargetLoads) {
            this.sources = Map.copyOf(sources);
            this.orderTargetLoads = orderTargetLoads;
        }

        @Override
        public ModelSource resolveModel(
                Session session, List<RemoteRepository> repositories, Parent parent, AtomicReference<Parent> modified) {
            return resolve(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }

        @Override
        public ModelSource resolveModel(
                Session session,
                List<RemoteRepository> repositories,
                Dependency dependency,
                AtomicReference<Dependency> modified) {
            return resolve(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
        }

        @Override
        public ModelResolverResult resolveModel(ModelResolverRequest request) {
            return new ModelResolverResult(
                    request, resolve(request.groupId(), request.artifactId(), request.version()), null);
        }

        private ModelSource resolve(String groupId, String artifactId, String version) {
            String coordinates = groupId + ':' + artifactId + ':' + version;
            ModelSource source = sources.get(coordinates);
            if (source == null) {
                throw new ModelResolverException(
                        "Unexpected model lookup: " + coordinates, groupId, artifactId, version);
            }
            resolveCounts.merge(coordinates, 1, Integer::sum);
            if (("test:a:1".equals(coordinates) || "test:b:1".equals(coordinates))
                    && firstCoordinates.add(coordinates)) {
                firstLoads.countDown();
                try {
                    if (!firstLoads.await(TIMEOUT_SECONDS / 2, TimeUnit.SECONDS)) {
                        throw new AssertionError("The two initial BOM loads did not overlap");
                    }
                    // Prove cross-request cache reuse without requiring single-flight concurrent loading.
                    // Cycle fixtures do not use this ordering: both paths must continue together.
                    if (orderTargetLoads
                            && "test:b:1".equals(coordinates)
                            && !firstBuildFinished.await(TIMEOUT_SECONDS / 2, TimeUnit.SECONDS)) {
                        throw new AssertionError("The first alias build did not complete");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ModelResolverException(e, groupId, artifactId, version);
                }
            }
            return source;
        }
    }
}
