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

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Member;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.api.di.Provides;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Hostile;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.RecordingInjector;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Unconstructed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.MAVEN_DI_DEBUG;
import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.bindingRows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
@Timeout(30)
class InjectorDiagnosticsTest {
    private static final String SISU_DEBUG = "sisu.debug";

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
    void ambiguousModeFiltersByCandidateCountAndResetsOnModeChanges() {
        RecordingInjector injector = new RecordingInjector();
        injector.bindInstance(String.class, "single");
        injector.bindImplicit(Runnable.class);
        System.setProperty(MAVEN_DI_DEBUG, "ambiguous");
        injector.checkpoint();
        assertTrue(injector.dumps.get(0).contains(", ambiguous"));
        assertTrue(injector.dumps.get(0).contains("key: java.lang.Runnable"));
        assertFalse(injector.dumps.get(0).contains("java.lang.String"));
        injector.bind(Key.of(String.class), Binding.toInstance("second").withKey(Key.of(String.class)));
        injector.checkpoint();
        assertTrue(injector.dumps.get(1).contains("+ key: java.lang.String"));
        assertEquals(2, bindingRows(injector.dumps.get(1)).size());
        injector.bindInstance(Runnable.class, () -> {});
        injector.checkpoint();
        assertTrue(injector.dumps.get(2).contains("- key: java.lang.Runnable"));
        System.setProperty(MAVEN_DI_DEBUG, "true");
        injector.checkpoint();
        assertFalse(injector.dumps.get(3).contains(", delta"));
        assertTrue(injector.dumps.get(3).contains("key: java.lang.Runnable"));
        System.setProperty(MAVEN_DI_DEBUG, "AMBIGUOUS");
        injector.checkpoint();
        assertFalse(injector.dumps.get(4).contains(", delta"));
        assertFalse(injector.dumps.get(4).contains("java.lang.Runnable"));
    }

    @Test
    void changesHiddenByAmbiguousFilterDoNotPrintEmptyDeltas() {
        System.setProperty(MAVEN_DI_DEBUG, "ambiguous");
        RecordingInjector injector = new RecordingInjector();
        injector.checkpoint();
        injector.bindInstance(String.class, "single");
        injector.checkpoint();
        injector.checkpoint();
        assertEquals(1, injector.dumps.size());
        injector.bindImplicit(Runnable.class);
        injector.checkpoint();
        assertEquals(2, injector.dumps.size());
        assertTrue(injector.dumps.get(1).contains("+ key: java.lang.Runnable"));
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

    static class BrokenProvider {
        @Provides
        <T> T provide() {
            return null;
        }
    }
}
