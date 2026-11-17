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
package org.apache.maven.di.impl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.di.Key;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Hostile;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.RecordingInjector;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Unconstructed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.MAVEN_DI_DEBUG;
import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.newRealmType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
@Timeout(30)
class InjectorDiagnosticConcurrencyTest {
    private InjectorDiagnosticsFixture environment;

    @BeforeEach
    void enableDiagnostics() {
        environment = new InjectorDiagnosticsFixture();
    }

    @AfterEach
    void restoreEnvironment() {
        environment.close();
    }

    @Test
    void parallelReadersCaptureOneSnapshotPerRevision() throws Exception {
        RecordingInjector injector = new RecordingInjector();
        Unconstructed.CALLS.set(0);
        injector.bindImplicit(Unconstructed.class);
        int readerCount = 8;
        ExecutorService workers = Executors.newFixedThreadPool(readerCount);
        try {
            for (int phase = 0; phase < 2; phase++) {
                CyclicBarrier barrier = new CyclicBarrier(readerCount);
                List<Future<Unconstructed>> results = new ArrayList<>();
                for (int worker = 0; worker < readerCount; worker++) {
                    results.add(workers.submit(() -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        return injector.getInstance(Unconstructed.class);
                    }));
                }
                Unconstructed singleton = results.get(0).get(10, TimeUnit.SECONDS);
                for (Future<Unconstructed> result : results) {
                    assertSame(singleton, result.get(10, TimeUnit.SECONDS));
                }
                assertEquals(phase + 1, injector.dumps.size());
                if (phase == 0) {
                    injector.bindInstance(Integer.class, 42);
                }
            }
            assertEquals(1, Unconstructed.CALLS.get());
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void delayedOutputPreservesCaptureOrderAndAppliesBackpressure() throws Exception {
        CountDownLatch firstOutput = new CountDownLatch(1);
        CountDownLatch releaseOutput = new CountDownLatch(1);
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (dumps.isEmpty()) {
                    firstOutput.countDown();
                    try {
                        assertTrue(releaseOutput.await(10, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
                super.writeDump(output, dump);
            }
        };
        injector.bindInstance(String.class, "value");
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = workers.submit(injector::checkpoint);
            assertTrue(firstOutput.await(10, TimeUnit.SECONDS));
            injector.bindInstance(Integer.class, 1);
            AtomicReference<Thread> producer = new AtomicReference<>();
            Future<Boolean> second = workers.submit(() -> {
                producer.set(Thread.currentThread());
                injector.checkpoint();
                return Thread.currentThread().isInterrupted();
            });
            awaitWaiting(producer);
            producer.get().interrupt();
            assertFalse(second.isDone());
            assertEquals(0, injector.dumps.size());
            releaseOutput.countDown();
            first.get(10, TimeUnit.SECONDS);
            assertTrue(second.get(10, TimeUnit.SECONDS));
            injector.checkpoint();
            assertEquals(2, injector.dumps.size());
            assertFalse(injector.dumps.get(0).contains("candidate: java.lang.Integer"));
            assertTrue(injector.dumps.get(1).contains("+ key: java.lang.Integer"));
            assertFalse(injector.dumps.get(1).contains("candidate: java.lang.String"));
        } finally {
            releaseOutput.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void queuedCapturesShareLoaderLabelsAndFormOneRevisionChain() throws Exception {
        CountDownLatch delayed = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (dump.contains("@Named(\"z\")")) {
                    delayed.countDown();
                    try {
                        assertTrue(release.await(10, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
                super.writeDump(output, dump);
            }
        };
        injector.bindInstance(String.class, "first");
        injector.checkpoint();
        Key<Object> z = Key.ofType(newRealmType(), "z");
        Key<Object> a = Key.ofType(newRealmType(), "a");
        Key<Object> b = Key.ofType(newRealmType(), "b");
        injector.bind(z, Binding.toInstance(new Object()).withKey(z));
        ExecutorService workers = Executors.newFixedThreadPool(3);
        try {
            Future<?> first = workers.submit(injector::checkpoint);
            assertTrue(delayed.await(10, TimeUnit.SECONDS));
            injector.bind(a, Binding.toInstance(new Object()).withKey(a));
            AtomicReference<Thread> secondProducer = new AtomicReference<>();
            Future<?> second = workers.submit(() -> {
                secondProducer.set(Thread.currentThread());
                injector.checkpoint();
            });
            awaitWaiting(secondProducer);
            injector.bind(b, Binding.toInstance(new Object()).withKey(b));
            AtomicReference<Thread> thirdProducer = new AtomicReference<>();
            Future<?> third = workers.submit(() -> {
                thirdProducer.set(Thread.currentThread());
                injector.checkpoint();
            });
            awaitWaiting(thirdProducer);
            assertFalse(second.isDone());
            assertFalse(third.isDone());
            release.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            third.get(10, TimeUnit.SECONDS);
            assertEquals(4, injector.dumps.size());
            String revision = revision(injector.dumps.get(0));
            for (int i = 1; i < injector.dumps.size(); i++) {
                String[] transition = revision(injector.dumps.get(i)).split("->");
                assertEquals(revision, transition[0]);
                revision = transition[1];
            }
            assertTrue(injector.dumps.get(1).contains("@Named(\"z\") " + Hostile.class.getName() + " [loader=L2]"));
            assertTrue(injector.dumps.get(2).contains("@Named(\"a\") " + Hostile.class.getName() + " [loader=L3]"));
            assertTrue(injector.dumps.get(3).contains("@Named(\"b\") " + Hostile.class.getName() + " [loader=L4]"));
            assertFalse(injector.dumps.get(2).contains("@Named(\"z\")"));
            assertFalse(injector.dumps.get(3).contains("@Named(\"a\")"));
        } finally {
            release.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void reentrantOutputKeepsEveryObservedAmbiguousStateInOrder() {
        System.setProperty(MAVEN_DI_DEBUG, "ambiguous");
        AtomicInteger writes = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (writes.getAndIncrement() == 0) {
                    bindInstance(String.class, "second");
                    checkpoint();
                    getBindings().get(Key.of(String.class)).clear();
                    bindInstance(String.class, "third");
                    checkpoint();
                }
                super.writeDump(output, dump);
            }
        };
        injector.bindInstance(String.class, "first");
        injector.checkpoint();
        assertEquals(3, injector.dumps.size());
        assertFalse(injector.dumps.get(0).contains(", delta"));
        assertTrue(injector.dumps.get(1).contains("+ key: java.lang.String"));
        assertTrue(injector.dumps.get(2).contains("- key: java.lang.String"));
    }

    @Test
    void resetWhileWritingKeepsOneWriterAndStartsANewFullHistory() throws Exception {
        PrintStream originalOutput = System.out;
        for (String reset : List.of("output", "mode", "off", "dispose")) {
            CountDownLatch writing = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicInteger active = new AtomicInteger();
            AtomicInteger maximum = new AtomicInteger();
            RecordingInjector injector = new RecordingInjector() {
                @Override
                void writeDump(PrintStream output, String dump) {
                    maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
                    try {
                        if (dumps.isEmpty()) {
                            writing.countDown();
                            try {
                                assertTrue(release.await(10, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(e);
                            }
                        }
                        super.writeDump(output, dump);
                    } finally {
                        active.decrementAndGet();
                    }
                }
            };
            System.setProperty(MAVEN_DI_DEBUG, "true");
            injector.bindInstance(String.class, "first");
            ExecutorService workers = Executors.newFixedThreadPool(2);
            try (PrintStream other = new PrintStream(new ByteArrayOutputStream())) {
                Future<?> first = workers.submit(injector::checkpoint);
                assertTrue(writing.await(10, TimeUnit.SECONDS));
                switch (reset) {
                    case "output" -> System.setOut(other);
                    case "mode" -> System.setProperty(MAVEN_DI_DEBUG, "ambiguous");
                    case "off" -> {
                        System.clearProperty(MAVEN_DI_DEBUG);
                        injector.checkpoint();
                        System.setProperty(MAVEN_DI_DEBUG, "true");
                    }
                    case "dispose" -> injector.dispose();
                    default -> throw new AssertionError(reset);
                }
                injector.bindInstance(Integer.class, 1);
                AtomicReference<Thread> producer = new AtomicReference<>();
                Future<?> second = workers.submit(() -> {
                    producer.set(Thread.currentThread());
                    injector.checkpoint();
                });
                awaitWaiting(producer);
                release.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
                assertEquals(1, maximum.get(), reset);
                assertEquals(2, injector.dumps.size(), reset);
                assertFalse(injector.dumps.get(1).contains(", delta"), reset);
                injector.checkpoint();
                assertEquals(2, injector.dumps.size(), reset);
            } finally {
                release.countDown();
                workers.shutdownNow();
                System.setOut(originalOutput);
            }
        }
    }

    @Test
    void failedWriteRetriesQueuedStateAsFullSnapshot() {
        AtomicInteger writes = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (writes.incrementAndGet() == 2) {
                    bindInstance(Long.class, 2L);
                    checkpoint();
                    throw new IllegalStateException("partially written block");
                }
                super.writeDump(output, dump);
            }
        };
        injector.bindInstance(String.class, "first");
        injector.checkpoint();
        injector.bindInstance(Integer.class, 1);
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        assertFalse(injector.dumps.get(1).contains(", delta"));
        assertTrue(injector.dumps.get(1).contains("candidate: java.lang.Long"));
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
    }

    @Test
    void reentrantStylerIsQueuedAndRenderFailureRetries() {
        AtomicInteger styles = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            protected BindingStyler getBindingStyler() {
                return new BindingStyler() {
                    @Override
                    public String header(String text) {
                        if (styles.getAndIncrement() == 0) {
                            bindInstance(Integer.class, 1);
                            checkpoint();
                            throw new IllegalStateException("styler failed");
                        }
                        return text;
                    }
                };
            }
        };
        injector.bindInstance(String.class, "first");
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        assertFalse(injector.dumps.get(0).contains(", delta"));
        assertTrue(injector.dumps.get(0).contains("candidate: java.lang.Integer"));
    }

    private static String revision(String dump) {
        return dump.lines().findFirst().orElseThrow().split("revision=")[1].split(" ")[0];
    }

    private static void awaitWaiting(AtomicReference<Thread> producer) throws InterruptedException {
        // A producer waits only after its snapshot is captured and queued behind the blocked writer.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Thread thread = producer.get();
            if (thread != null && thread.getState() == Thread.State.WAITING) {
                return;
            }
            Thread.sleep(1);
        }
        throw new AssertionError("checkpoint did not wait for the writer");
    }
}
