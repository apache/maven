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
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Hostile;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.RecordingInjector;
import org.apache.maven.di.impl.InjectorDiagnosticsFixture.Unconstructed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.bindingRows;
import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.body;
import static org.apache.maven.di.impl.InjectorDiagnosticsFixture.newRealmType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_OUT;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

@ResourceLock(SYSTEM_PROPERTIES)
@ResourceLock(SYSTEM_OUT)
@Timeout(30)
class InjectorSnapshotTest {
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
    void separateRealmsRemainDistinctRegardlessOfDependencyOrder() throws Exception {
        Class<?> first = newRealmType();
        Class<?> second = newRealmType();
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
}
