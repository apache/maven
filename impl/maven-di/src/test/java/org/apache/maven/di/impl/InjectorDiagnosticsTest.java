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
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

@ResourceLock("java.lang.System.properties")
@ResourceLock("java.lang.System.out")
@Timeout(30)
class InjectorDiagnosticsTest {
    private String previousProperty;
    private PrintStream previousOutput;

    @BeforeEach
    void enableDiagnostics() {
        previousProperty = System.getProperty("sisu.debug");
        previousOutput = System.out;
        System.setProperty("sisu.debug", "true");
    }

    @AfterEach
    void restoreEnvironment() {
        if (previousProperty == null) {
            System.clearProperty("sisu.debug");
        } else {
            System.setProperty("sisu.debug", previousProperty);
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
        assertTrue(snapshot.contains("scope " + SessionScoped.class.getName()));
        assertTrue(snapshot.contains("supplier "));
        assertFalse(snapshot.contains("secret-value"));
        assertEquals(body(snapshot), body(injector.dumps.get(0)));
    }

    @Test
    void snapshotsRenderWildcardsInOwnerTypes() {
        class Outer<T> {
            class Inner<U> {}
        }

        // Key simplifies its type arguments, but preserves wildcards in parameterized owners.
        String bound = Number.class.getName() + " [loader=null]";
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
        assertEquals(5, bindingRows(injector.dumps.get(1)).size());
        assertEquals(body(injector.dumpBindings()), body(injector.dumps.get(1)));
        List<String> rows = bindingRows(injector.dumps.get(1));
        assertEquals(rows.stream().sorted().toList(), rows);
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        first.prioritize(42);
        assertTrue(injector.dumpBindings().contains("priority=42"));
        assertFalse(injector.dumps.get(1).contains("priority=42"));
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
        assertEquals(body(explicit), body(injector.dumps.get(2)));
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
        assertEquals(body(injector.dumpBindings()), body(injector.dumps.get(1)));
        injector.bindScope(SessionScoped.class, new Scope() {
            @Override
            public <T> Supplier<T> scope(Key<T> key, Supplier<T> supplier) {
                throw new AssertionError("scope called");
            }
        });
        injector.checkpoint();
        assertEquals(body(injector.dumpBindings()), body(injector.dumps.get(2)));
        injector.dispose();
        injector.checkpoint();
        assertEquals("", body(injector.dumps.get(3)));
        injector.bindInstance(Integer.class, 1);
        injector.checkpoint();
        assertEquals(body(injector.dumpBindings()), body(injector.dumps.get(4)));
    }

    @Test
    void flagIsReadAgainForReusedInjector() {
        RecordingInjector injector = new RecordingInjector();
        AtomicInteger descriptions = new AtomicInteger();
        Binding<String> binding = new Binding.BindingToInstance<>("value") {
            @Override
            String getDiagnosticSource() {
                descriptions.incrementAndGet();
                return super.getDiagnosticSource();
            }
        }.withKey(Key.of(String.class));
        injector.bind(Key.of(String.class), binding);
        injector.bind(Key.of(String.class, "alias"), binding);
        for (String value : List.of("false", "yes", "1")) {
            System.setProperty("sisu.debug", value);
            assertEquals("value", injector.getInstance(String.class));
        }
        System.clearProperty("sisu.debug");
        injector.checkpoint();
        assertTrue(injector.dumps.isEmpty());
        assertEquals(0, descriptions.get());
        for (String value : List.of("", "TRUE", "true")) {
            System.setProperty("sisu.debug", value);
            injector.checkpoint();
            injector.checkpoint();
            System.setProperty("sisu.debug", "false");
            injector.checkpoint();
        }
        assertEquals(3, injector.dumps.size());
        assertEquals(3, descriptions.get());
        System.setProperty("sisu.debug", "true");
        injector.checkpoint();
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        injector.checkpoint();
        assertEquals(5, injector.dumps.size());
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
            protected void writeDump(PrintStream output, String dump) {
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
    void separateRealmsWithTheSameBinaryNameRemainVisible() throws Exception {
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
    void oldOutputCannotSuppressNewRevisionOrHoldTheStateLock() throws Exception {
        CountDownLatch firstOutput = new CountDownLatch(1);
        CountDownLatch releaseOutput = new CountDownLatch(1);
        RecordingInjector injector = new RecordingInjector() {
            @Override
            protected void writeDump(PrintStream output, String dump) {
                if (dump.contains("original=java.lang.String") && !dump.contains("original=java.lang.Integer")) {
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
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> first = worker.submit(injector::checkpoint);
            assertTrue(firstOutput.await(10, TimeUnit.SECONDS));
            // The old snapshot is detached: registration no longer races with its capture.
            injector.bindInstance(Integer.class, 1);
            injector.checkpoint();
            assertEquals(1, injector.dumps.size());
            releaseOutput.countDown();
            first.get(10, TimeUnit.SECONDS);
            injector.checkpoint();
            assertEquals(2, injector.dumps.size());
            assertEquals(body(injector.dumpBindings()), body(injector.dumps.get(0)));
        } finally {
            releaseOutput.countDown();
            worker.shutdownNow();
        }
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
        return body(dump).lines().filter(line -> !line.startsWith("  scope ")).toList();
    }

    static class RecordingInjector extends InjectorImpl {
        final List<String> dumps = new CopyOnWriteArrayList<>();

        void checkpoint() {
            debugBindings();
        }

        @Override
        protected void writeDump(PrintStream output, String dump) {
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

    static class BrokenProvider {
        @Provides
        <T> T provide() {
            return null;
        }
    }
}
