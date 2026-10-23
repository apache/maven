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
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Member;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
@Timeout(30)
class InjectorDiagnosticsTest {
    private static final String MAVEN_DI_DEBUG = "maven.di.debug";
    private static final String SISU_DEBUG = "sisu.debug";

    private String previousProperty;
    private PrintStream previousOutput;

    @BeforeEach
    void enableDiagnostics() {
        previousProperty = System.getProperty(MAVEN_DI_DEBUG);
        previousOutput = System.out;
        System.setProperty(MAVEN_DI_DEBUG, "true");
    }

    @AfterEach
    void restoreEnvironment() {
        if (previousProperty == null) {
            System.clearProperty(MAVEN_DI_DEBUG);
        } else {
            System.setProperty(MAVEN_DI_DEBUG, previousProperty);
        }
        System.setOut(previousOutput);
    }

    @Test
    void snapshotDoesNotProvisionOrInvokeUserToString() {
        RecordingInjector injector = new RecordingInjector();
        injector.bindInstance(Hostile.class, new Hostile());
        injector.bindInstance(CharSequence.class, "secret-value");
        Key<Long> initialized = Key.of(Long.class);
        injector.bind(
                initialized,
                new Binding<Long>(initialized, Set.of()) {
                    @Override
                    public Supplier<Long> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                        throw new AssertionError("binding compiled for diagnostics");
                    }

                    @Override
                    public String toString() {
                        throw new AssertionError("binding toString");
                    }
                }.initializeWith(new BindingInitializer<>(Set.of()) {
                    @Override
                    public Consumer<Long> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                        throw new AssertionError("initializer compiled for diagnostics");
                    }
                }));
        injector.bindSupplier(String.class, new Supplier<String>() {
            @Override
            public String get() {
                throw new AssertionError("supplier called for diagnostics");
            }

            @Override
            public String toString() {
                throw new AssertionError("supplier toString");
            }
        });
        injector.bindScope(SessionScoped.class, () -> {
            throw new AssertionError("scope provisioned");
        });
        Unconstructed.CALLS.set(0);
        injector.bindImplicit(Unconstructed.class);
        injector.bindImplicit(Provider.class);
        String snapshot = injector.dumpBindings();
        injector.checkpoint();
        assertEquals(0, Unconstructed.CALLS.get());
        assertTrue(snapshot.contains("initialized by"));
        assertTrue(snapshot.contains("Provider.provide"));
        assertTrue(snapshot.contains("  scopes:\n") && snapshot.contains(SessionScoped.class.getName()));
        assertTrue(snapshot.contains("supplier "));
        assertFalse(snapshot.contains("secret-value"));
        assertEquals(body(snapshot), body(injector.dumps.get(0)));
    }

    @Test
    void snapshotGroupsCandidatesAndListsDependenciesAndClassLoaders() {
        RecordingInjector injector = new RecordingInjector();
        Key<String> key = Key.of(String.class);
        Binding<String> factory = Binding.to(key, args -> "unused", new Dependency<?>[] {
                    new Dependency<>(Key.of(Integer.class), false), new Dependency<>(Key.of(Long.class), true)
                })
                .prioritize(10);
        injector.bind(key, factory);
        injector.bind(key, Binding.toInstance("other").withKey(key));
        injector.bind(Key.of(String.class, "alias"), factory);

        String snapshot = injector.dumpBindings();
        assertEquals(
                1,
                snapshot.lines()
                        .filter(line -> line.equals("  key: java.lang.String [loader=bootstrap]"))
                        .count());
        assertEquals(3, bindingRows(snapshot).size());
        assertTrue(snapshot.contains("  key: java.lang.String [loader=bootstrap]\n"
                + "    candidate: java.lang.String [loader=bootstrap]\n      priority: 10\n"));
        assertTrue(snapshot.contains("      dependencies:\n"
                + "        - ?java.lang.Long [loader=bootstrap]\n"
                + "        - java.lang.Integer [loader=bootstrap]"));
        assertTrue(snapshot.contains(
                "\n  class loaders:\n    L1: " + Binding.identityString(Singleton.class.getClassLoader())));
        assertTrue(snapshot.contains("    " + Singleton.class.getName() + " [loader=L1]\n      supplier: "));
    }

    @Test
    void snapshotSeparatesSourceParametersAndExceptions() {
        RecordingInjector injector = new RecordingInjector();
        injector.bindImplicit(DetailedSource.class);
        injector.bindImplicit(Provider.class);

        String snapshot = injector.dumpBindings();
        assertTrue(
                snapshot.contains("      source:\n        " + DetailedSource.class.getName() + "(\n"
                        + "          java.lang.String [loader=bootstrap],\n"
                        + "          int [loader=bootstrap]\n"
                        + "        ) [loader=L1]\n"
                        + "        throws java.io.IOException [loader=bootstrap]"),
                snapshot);
        assertTrue(
                snapshot.contains("      source:\n"
                        + "        java.lang.String [loader=bootstrap]\n"
                        + "        " + Provider.class.getName() + ".provide() [loader=L1]\n"),
                snapshot);
    }

    @Test
    void snapshotPreservesDefaultMethodModifier() throws Exception {
        RecordingInjector injector = new RecordingInjector();
        injector.bind(
                Key.of(String.class),
                ReflectionUtils.bindingFromMethod(DefaultProvider.class.getDeclaredMethod("provideDefault")));

        String snapshot = injector.dumpBindings();
        assertTrue(
                snapshot.contains("        public default java.lang.String [loader=bootstrap]\n" + "        "
                        + DefaultProvider.class.getName() + ".provideDefault() [loader=L1]"),
                snapshot);
    }

    @Test
    void snapshotsRenderWildcardsInOwnerTypes() {
        class Outer<T> {
            class Inner<U> {}
        }

        // Key simplifies its type arguments, but preserves wildcards in parameterized owners.
        String bound = Number.class.getName() + " [loader=bootstrap]";
        Map<Key<?>, String> cases = Map.of(
                new Key<Outer<?>.Inner<String>>() {}, "<?>",
                new Key<Outer<? extends Number>.Inner<String>>() {}, "<? extends " + bound + ">",
                new Key<Outer<? super Number>.Inner<String>>() {}, "<? super " + bound + ">");
        cases.forEach((key, expected) -> {
            InjectorImpl injector = new InjectorImpl();
            injector.getBindings().put(key, Set.of());
            String dump = injector.dumpBindings();
            assertTrue(dump.contains(expected + ".Inner [loader="), dump);
        });
    }

    @Test
    void lateRegistrationsPreserveAliasesAndEqualDescriptions() {
        RecordingInjector injector = new RecordingInjector();
        Key<String> key = Key.of(String.class);
        Binding.TupleConstructorN<String> factory = args -> "created";
        Binding<String> first = Binding.to(key, factory, new Class<?>[0]);
        Binding<String> second = Binding.to(key, factory, new Class<?>[0]);
        injector.bind(key, first);
        injector.bind(key, second);
        injector.bind(Key.of(String.class, "a\nb\r\tc"), first);
        injector.checkpoint();
        assertEquals(3, bindingRows(injector.dumps.get(0)).size());
        assertTrue(injector.dumps.get(0).contains("a\\u000ab\\u000d\\u0009c"));
        injector.bind(key, second);
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        injector.bind(key, Binding.to(key, factory, new Class<?>[0]));
        injector.bind(Key.of(Integer.class), Binding.toInstance(5).withKey(Key.of(Integer.class)));
        assertThrows(DIException.class, () -> injector.getInstance(String.class));
        assertEquals(2, bindingRows(injector.dumps.get(1)).size());
        assertTrue(injector.dumps.get(1).contains(", delta"));
        assertFalse(injector.dumps.get(1).contains("@Named(\"a\\u000ab\\u000d\\u0009c\")"));
        List<String> keys = body(injector.dumpBindings())
                .lines()
                .filter(line -> line.startsWith("  key: "))
                .toList();
        assertEquals(keys.stream().sorted().toList(), keys);
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        first.prioritize(42);
        assertTrue(injector.dumpBindings().contains("priority: 42"));
        assertFalse(injector.dumps.get(1).contains("priority: 42"));
    }

    @Test
    void snapshotsDoNotInvokeAnnotationMembers() {
        AtomicInteger memberCalls = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector();
        // Equal hashes force qualifier equality during registry lookup.
        assertEquals("Aa".hashCode(), "BB".hashCode());
        for (String value : List.of("Aa", "BB")) {
            Key<Object> key = Key.of(Object.class, new ObservedFlavor(value, memberCalls));
            injector.bind(key, Binding.toInstance(new Object()).withKey(key));
        }
        assertTrue(memberCalls.get() > 0);
        memberCalls.set(0);
        injector.dumpBindings();
        injector.checkpoint();
        assertEquals(0, memberCalls.get());

        Key<Object> first = Key.of(Object.class, new ObservedFlavor("same", memberCalls));
        injector.bind(first, Binding.toInstance(new Object()).withKey(first));
        injector.checkpoint();
        Key<Object> second = Key.of(Object.class, new ObservedFlavor("same", memberCalls));
        injector.bind(second, Binding.toInstance(new Object()).withKey(second));
        assertTrue(memberCalls.get() > 0);
        memberCalls.set(0);
        String explicit = injector.dumpBindings();
        injector.checkpoint();
        assertEquals(0, memberCalls.get());
        assertTrue(explicit.contains("[qualifier="));
        assertTrue(injector.dumps.get(2).contains("+ candidate:"));
    }

    @Test
    void snapshotsReflectRegistryChanges() {
        RecordingInjector injector = new RecordingInjector();
        injector.bindImplicit(Runnable.class);
        injector.checkpoint();
        assertTrue(injector.dumps.get(0).contains("<no candidates>"));
        injector.bindInstance(Runnable.class, () -> {});
        injector.checkpoint();
        assertFalse(injector.dumps.get(1).contains("<no candidates>"));
        assertTrue(injector.dumps.get(1).contains("+ candidate: java.lang.Runnable"));
        assertTrue(injector.dumps.get(1).contains(", delta"));
        injector.bindScope(SessionScoped.class, new Scope() {
            @Override
            public <T> Supplier<T> scope(Key<T> key, Supplier<T> supplier) {
                throw new AssertionError("scope called");
            }
        });
        injector.checkpoint();
        assertTrue(injector.dumps.get(2).contains("+ scopes:"));
        assertFalse(injector.dumps.get(2).contains("candidate:"));
        injector.dispose();
        injector.checkpoint();
        assertEquals("", body(injector.dumps.get(3)));
        injector.bindInstance(Integer.class, 1);
        injector.checkpoint();
        assertTrue(injector.dumps.get(4).contains("+ key: java.lang.Integer"));
        assertTrue(injector.dumps.get(4).contains(", delta"));
    }

    @Test
    void sisuFlagDoesNotEnableNativeDiagnostics() {
        String previous = System.getProperty(SISU_DEBUG);
        try {
            System.setProperty(SISU_DEBUG, "true");
            RecordingInjector injector = new RecordingInjector();
            injector.bindInstance(String.class, "value");
            System.clearProperty(MAVEN_DI_DEBUG);
            assertEquals("value", injector.getInstance(String.class));
            assertTrue(injector.dumps.isEmpty());
            System.setProperty(MAVEN_DI_DEBUG, "false");
            injector.checkpoint();
            assertTrue(injector.dumps.isEmpty());
        } finally {
            if (previous == null) {
                System.clearProperty(SISU_DEBUG);
            } else {
                System.setProperty(SISU_DEBUG, previous);
            }
        }
    }

    @Test
    void flagIsReadAgainForReusedInjector() {
        RecordingInjector injector = new RecordingInjector();
        AtomicInteger descriptions = new AtomicInteger();
        Binding<String> binding = new Binding.BindingToInstance<>("value") {
            @Override
            List<String> getDiagnosticSourceLines(Function<Member, List<String>> memberSource) {
                descriptions.incrementAndGet();
                return super.getDiagnosticSourceLines(memberSource);
            }
        }.withKey(Key.of(String.class));
        injector.bind(Key.of(String.class), binding);
        injector.bind(Key.of(String.class, "alias"), binding);
        for (String value : List.of("false", "yes", "1")) {
            System.setProperty(MAVEN_DI_DEBUG, value);
            assertEquals("value", injector.getInstance(String.class));
        }
        System.clearProperty(MAVEN_DI_DEBUG);
        injector.checkpoint();
        assertTrue(injector.dumps.isEmpty());
        assertEquals(0, descriptions.get());
        for (String value : List.of("", "TRUE", "true")) {
            System.setProperty(MAVEN_DI_DEBUG, value);
            injector.checkpoint();
            injector.checkpoint();
            System.setProperty(MAVEN_DI_DEBUG, "false");
            injector.checkpoint();
        }
        assertEquals(3, injector.dumps.size());
        assertEquals(3, descriptions.get());
    }

    @Test
    void discoveryPrintsOnceAndPreservesPartialRegistryOnFailure(@TempDir Path directory) throws Exception {
        RecordingInjector injector = new RecordingInjector();
        try (URLClassLoader loader = indexLoader(directory, Unconstructed.class.getName())) {
            injector.discover(loader);
            assertEquals(1, injector.dumps.size());
            assertTrue(injector.dumps.get(0).contains("discovery"));
            injector.discover(loader);
            injector.checkpoint();
            assertEquals(1, injector.dumps.size());
        }
        try (URLClassLoader loader =
                indexLoader(directory.resolve("broken"), Hostile.class.getName() + "\nmissing.Type")) {
            DIException failure = assertThrows(DIException.class, () -> injector.discover(loader));
            assertTrue(failure.getCause() instanceof ClassNotFoundException);
            assertEquals(2, injector.dumps.size());
            assertTrue(injector.dumps.get(1).contains("partial"));
            assertTrue(injector.dumps.get(1).contains(Hostile.class.getName()));
        }
    }

    @Test
    void registrationAndOutputFailuresPreserveThePrimaryError() {
        RecordingInjector injector = new RecordingInjector();
        DIException failure = assertThrows(DIException.class, () -> injector.bindImplicit(BrokenProvider.class));
        assertTrue(failure.getMessage().contains("Parameterized method"));
        assertTrue(injector.dumps.get(0).contains("partial"));
        assertTrue(injector.dumps.get(0).contains(BrokenProvider.class.getName()));
        AtomicInteger attempts = new AtomicInteger();
        RecordingInjector brokenSink = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (attempts.getAndIncrement() == 0) {
                    throw new IllegalStateException("output failed");
                }
                super.writeDump(output, dump);
            }
        };
        DIException original = assertThrows(DIException.class, () -> brokenSink.bindImplicit(BrokenProvider.class));
        assertEquals(failure.getMessage(), original.getMessage());
        assertTrue(brokenSink.dumps.isEmpty());
        assertSame(
                BrokenProvider.class,
                brokenSink.getInstance(BrokenProvider.class).getClass());
        assertEquals(2, attempts.get());
        assertEquals(1, brokenSink.dumps.size());
        assertTrue(brokenSink.dumps.get(0).contains(BrokenProvider.class.getName()));
    }

    @Test
    void separateRealmsRemainDistinctRegardlessOfDependencyOrder() throws Exception {
        String name = Hostile.class.getName();
        byte[] bytes;
        try (var input = Hostile.class.getResourceAsStream("InjectorDiagnosticsTest$Hostile.class")) {
            bytes = input.readAllBytes();
        }
        class Realm extends ClassLoader {
            Class<?> define() {
                return defineClass(name, bytes, 0, bytes.length);
            }
        }
        Class<?> first = new Realm().define();
        Class<?> second = new Realm().define();
        RecordingInjector injector = new RecordingInjector();
        for (Class<?> type : List.of(first, second)) {
            Key<Object> key = Key.ofType(type);
            injector.bind(key, Binding.toInstance(new Object()).withKey(key));
        }
        List<String> rows = bindingRows(injector.dumpBindings());
        assertEquals(2, rows.size());
        assertNotEquals(rows.get(0), rows.get(1));

        Dependency<?> firstDependency = new Dependency<>(Key.ofType(first), false);
        Dependency<?> secondDependency = new Dependency<>(Key.ofType(second), false);
        Set<Dependency<?>> dependencies = new LinkedHashSet<>(List.of(firstDependency, secondDependency));
        RecordingInjector dependent = new RecordingInjector();
        Key<String> key = Key.of(String.class);
        dependent.bind(key, new Binding<String>(key, dependencies) {
            @Override
            public Supplier<String> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                throw new AssertionError("binding compiled for diagnostics");
            }
        });
        String before = dependent.dumpBindings();
        dependencies.clear();
        dependencies.add(secondDependency);
        dependencies.add(firstDependency);
        assertEquals(before, dependent.dumpBindings());
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
            awaitQueued(injector, 1);
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
    void initializerSourcesSurviveCombinationWithoutExecutingUserCode() {
        RecordingInjector injector = new RecordingInjector();
        injector.bindImplicit(InjectedMembers.class);
        String dump = injector.dumpBindings();
        assertTrue(dump.contains("initialized by field injector:"), dump);
        assertTrue(dump.contains(InjectedMembers.class.getName() + ".field"), dump);
        assertTrue(dump.contains("initialized by method injector:"), dump);
        assertTrue(dump.contains(InjectedMembers.class.getName() + ".initialize("), dump);
        long candidates = dump.lines()
                .filter(line -> line.contains("candidate: " + InjectedMembers.class.getName()))
                .count();
        assertEquals(
                candidates,
                dump.lines()
                        .filter(line -> line.contains("initialized by field injector:"))
                        .count());
        assertEquals(
                candidates,
                dump.lines()
                        .filter(line -> line.contains("initialized by method injector:"))
                        .count());
        assertFalse(dump.contains("initialized by method injector: )"), dump);
        assertTrue(dump.contains("\n            java.lang.Integer [loader=bootstrap]\n"), dump);
    }

    @Test
    void outputChangesAndFailedDeltasRestartWithFullSnapshots() {
        AtomicInteger attempts = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector() {
            @Override
            void writeDump(PrintStream output, String dump) {
                if (attempts.incrementAndGet() == 2) {
                    throw new IllegalStateException("delta output failed");
                }
                super.writeDump(output, dump);
            }
        };
        injector.bindInstance(String.class, "value");
        injector.checkpoint();
        injector.bindInstance(Integer.class, 1);
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        assertFalse(injector.dumps.get(1).contains(", delta"));
        assertTrue(injector.dumps.get(1).contains("candidate: java.lang.String"));
        try (PrintStream other = new PrintStream(new ByteArrayOutputStream())) {
            System.setOut(other);
            injector.checkpoint();
            assertEquals(3, injector.dumps.size());
            assertFalse(injector.dumps.get(2).contains(", delta"));
            assertEquals(body(injector.dumps.get(1)), body(injector.dumps.get(2)));
        }
    }

    @Test
    void printStreamErrorsDoNotCommitAnUnwrittenSnapshot() {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        AtomicInteger writes = new AtomicInteger();
        try (PrintStream sink =
                new PrintStream(
                        new OutputStream() {
                            @Override
                            public void write(int value) throws IOException {
                                if (writes.getAndIncrement() == 0) {
                                    throw new IOException("output unavailable");
                                }
                                captured.write(value);
                            }
                        },
                        true,
                        StandardCharsets.UTF_8) {
                    @Override
                    public boolean checkError() {
                        boolean failed = super.checkError();
                        clearError();
                        return failed;
                    }
                }) {
            System.setOut(sink);
            InjectorImpl injector = new InjectorImpl();
            injector.bindInstance(String.class, "value");
            assertEquals("value", injector.getInstance(String.class));
            captured.reset();
            assertEquals("value", injector.getInstance(String.class));
            String retry = captured.toString(StandardCharsets.UTF_8);
            assertTrue(retry.contains("candidate: java.lang.String"), retry);
            assertFalse(retry.contains(", delta"), retry);
        }
    }

    @Test
    void stickyPrintStreamErrorKeepsRetryingFullSnapshotsUntilOutputChanges() {
        ByteArrayOutputStream failed = new ByteArrayOutputStream();
        ByteArrayOutputStream recovered = new ByteArrayOutputStream();
        try (PrintStream sink = new PrintStream(failed, true, StandardCharsets.UTF_8) {
                    {
                        setError();
                    }
                };
                PrintStream recovery = new PrintStream(recovered, true, StandardCharsets.UTF_8)) {
            InjectorImpl injector = new InjectorImpl();
            injector.bindInstance(String.class, "fixture");
            System.setOut(sink);
            injector.debugBindings();
            injector.debugBindings();
            String attempts = failed.toString(StandardCharsets.UTF_8);
            assertEquals(
                    2,
                    attempts.lines()
                            .filter(line -> line.startsWith("Maven DI native registry "))
                            .count());
            assertFalse(attempts.contains(", delta"), attempts);
            System.setOut(recovery);
            injector.debugBindings();
            String success = recovered.toString(StandardCharsets.UTF_8);
            assertTrue(success.contains("candidate: java.lang.String"), success);
            assertFalse(success.contains(", delta"), success);
        }
    }

    @Test
    void queuedCapturesShareLoaderLabelsAndFormOneRevisionChain() throws Exception {
        byte[] bytes;
        try (var input = Hostile.class.getResourceAsStream("InjectorDiagnosticsTest$Hostile.class")) {
            bytes = input.readAllBytes();
        }
        class Realm extends ClassLoader {
            Class<?> define() {
                return defineClass(Hostile.class.getName(), bytes, 0, bytes.length);
            }
        }
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
        Key<Object> z = Key.ofType(new Realm().define(), "z");
        Key<Object> a = Key.ofType(new Realm().define(), "a");
        Key<Object> b = Key.ofType(new Realm().define(), "b");
        injector.bind(z, Binding.toInstance(new Object()).withKey(z));
        ExecutorService workers = Executors.newFixedThreadPool(3);
        try {
            Future<?> first = workers.submit(injector::checkpoint);
            assertTrue(delayed.await(10, TimeUnit.SECONDS));
            injector.bind(a, Binding.toInstance(new Object()).withKey(a));
            Future<?> second = workers.submit(injector::checkpoint);
            awaitQueued(injector, 1);
            injector.bind(b, Binding.toInstance(new Object()).withKey(b));
            Future<?> third = workers.submit(injector::checkpoint);
            awaitQueued(injector, 2);
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

    private static String revision(String dump) {
        return dump.lines().findFirst().orElseThrow().split("revision=")[1].split(" ")[0];
    }

    private static void awaitQueued(InjectorImpl injector, int count) throws Exception {
        // Wait for capture to finish before mutating the registry, which does not support concurrent registration.
        var lockField = InjectorImpl.class.getDeclaredField("diagnosticLock");
        var queueField = InjectorImpl.class.getDeclaredField("diagnosticQueue");
        lockField.setAccessible(true);
        queueField.setAccessible(true);
        Object lock = lockField.get(injector);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            synchronized (lock) {
                if (((Collection<?>) queueField.get(injector)).size() == count) {
                    return;
                }
            }
            Thread.sleep(1);
        }
        throw new AssertionError("checkpoint did not finish capture");
    }

    @Test
    void resetWhileWritingKeepsOneWriterAndStartsANewFullHistory() throws Exception {
        for (String reset : List.of("output", "off", "dispose")) {
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
                    case "off" -> {
                        System.clearProperty(MAVEN_DI_DEBUG);
                        injector.checkpoint();
                        System.setProperty(MAVEN_DI_DEBUG, "true");
                    }
                    case "dispose" -> injector.dispose();
                    default -> throw new AssertionError(reset);
                }
                injector.bindInstance(Integer.class, 1);
                Future<?> second = workers.submit(injector::checkpoint);
                awaitQueued(injector, 1);
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
                System.setOut(previousOutput);
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
    void captureFailureRetainsTheLastSuccessfulBaseline() {
        AtomicInteger captures = new AtomicInteger();
        RecordingInjector injector = new RecordingInjector();
        injector.bindInstance(String.class, "first");
        injector.checkpoint();
        Key<Integer> key = Key.of(Integer.class);
        injector.bind(key, new Binding<Integer>(key, Set.of()) {
            @Override
            public Supplier<Integer> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                throw new AssertionError("compiled for diagnostics");
            }

            @Override
            List<String> getDiagnosticSourceLines(Function<Member, List<String>> source) {
                if (captures.getAndIncrement() == 0) {
                    throw new IllegalStateException("capture unavailable");
                }
                return List.of("test source");
            }
        });
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        assertTrue(injector.dumps.get(1).contains(", delta"));
        assertTrue(injector.dumps.get(1).contains("+ key: java.lang.Integer"));
    }

    private static URLClassLoader indexLoader(Path directory, String content) throws Exception {
        Path index = directory.resolve("META-INF/maven/org.apache.maven.api.di.Inject");
        Files.createDirectories(index.getParent());
        Files.writeString(index, content, StandardCharsets.UTF_8);
        return new URLClassLoader(
                new URL[] {directory.toUri().toURL()}, InjectorDiagnosticsTest.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return findResources(name);
            }
        };
    }

    private static String body(String dump) {
        return dump.substring(dump.indexOf("{\n") + 2, dump.lastIndexOf("\n}"));
    }

    private static List<String> bindingRows(String dump) {
        return body(dump)
                .lines()
                .filter(line -> line.matches("    (?:[+~-] )?candidate: .*"))
                .toList();
    }

    static class RecordingInjector extends InjectorImpl {
        final List<String> dumps = new CopyOnWriteArrayList<>();

        void checkpoint() {
            debugBindings();
        }

        @Override
        void writeDump(PrintStream output, String dump) {
            dumps.add(dump);
        }
    }

    public static class Hostile {
        @Override
        public String toString() {
            throw new AssertionError("component toString");
        }
    }

    @Qualifier
    @Retention(RUNTIME)
    @interface Flavor {
        String value();
    }

    static final class ObservedFlavor implements Flavor {
        private final String value;
        private final AtomicInteger memberCalls;

        ObservedFlavor(String value, AtomicInteger memberCalls) {
            this.value = value;
            this.memberCalls = memberCalls;
        }

        @Override
        public String value() {
            memberCalls.incrementAndGet();
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Flavor.class;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Flavor flavor && value.equals(flavor.value());
        }

        @Override
        public int hashCode() {
            return (127 * "value".hashCode()) ^ value.hashCode();
        }
    }

    @Singleton
    static class Unconstructed {
        static final AtomicInteger CALLS = new AtomicInteger();

        Unconstructed() {
            CALLS.incrementAndGet();
        }
    }

    static class Provider {
        @Provides
        @Named("provided")
        String provide() {
            throw new AssertionError("provider called for diagnostics");
        }
    }

    static class InjectedMembers {
        @Inject
        String field;

        @Inject
        void initialize(Integer count) {
            throw new AssertionError("initializer executed for diagnostics");
        }
    }

    static class DetailedSource {
        DetailedSource(String text, int count) throws IOException {
            throw new AssertionError("constructor called for diagnostics");
        }
    }

    interface DefaultProvider {
        default String provideDefault() {
            throw new AssertionError("default method called for diagnostics");
        }
    }

    static class BrokenProvider {
        @Provides
        <T> T provide() {
            return null;
        }
    }
}
