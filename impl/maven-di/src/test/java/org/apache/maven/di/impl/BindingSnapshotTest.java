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

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.api.di.Singleton;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BindingSnapshotTest {
    private final DiagnosticIdentity identities = new DiagnosticIdentity();
    private final Object injector = new Object();

    @Test
    void deltaAddsCandidateToExistingKeyWithoutRepeatingUnchangedCandidates() {
        Key<String> key = Key.of(String.class);
        Binding<String> first = Binding.toInstance("first").withKey(key);
        Binding<String> second = Binding.toInstance("second").withKey(key);
        Map<Key<?>, Set<Binding<?>>> registry = new HashMap<>();
        registry.put(key, new HashSet<>(Set.of(first)));
        BindingSnapshot before = capture(registry, 1);
        registry.get(key).add(second);
        BindingSnapshot after = capture(registry, 2);
        String delta = render(after, before);
        assertTrue(delta.contains("revision=1->2"), delta);
        assertTrue(delta.contains("~ key: java.lang.String"), delta);
        assertTrue(delta.contains("+ candidate: java.lang.String"), delta);
        assertFalse(delta.contains(Binding.identityString(((Binding.BindingToInstance<?>) first).instance)), delta);
        assertEquals(
                1, delta.lines().filter(line -> line.contains("candidate:")).count());
        assertNull(render(capture(registry, 3), after));
    }

    @Test
    void metadataChangesAndRemovalsRemainVisible() {
        Key<String> key = Key.of(String.class);
        Binding<String> first = Binding.toInstance("first").withKey(key);
        Binding<String> second = Binding.toInstance("second").withKey(key);
        Binding<Integer> other = Binding.toInstance(1).withKey(Key.of(Integer.class));
        Map<Key<?>, Set<Binding<?>>> registry = new HashMap<>();
        registry.put(key, new HashSet<>(Set.of(first, second)));
        registry.put(Key.of(Integer.class), Set.of(other));
        BindingSnapshot before = capture(registry, 1);
        first.prioritize(42);
        registry.get(key).remove(second);
        registry.remove(Key.of(Integer.class));
        BindingSnapshot after = capture(registry, 2);
        String delta = render(after, before);
        assertTrue(delta.contains("~ candidate: java.lang.String"), delta);
        assertTrue(delta.contains("priority: 42"), delta);
        assertTrue(delta.contains("- candidate: java.lang.String"), delta);
        assertTrue(delta.contains("- key: java.lang.Integer"), delta);
        assertFalse(render(before, null).contains("priority: 42"));
        registry.get(key).clear();
        String empty = render(capture(registry, 3), after);
        assertTrue(empty.contains("- candidate: java.lang.String"), empty);
        assertTrue(empty.contains("<no candidates>"), empty);
    }

    @Test
    void earlierKeysFromNewRealmsDoNotRenumberExistingLoaderReferences() throws Exception {
        byte[] bytes;
        try (var input = Sample.class.getResourceAsStream("BindingSnapshotTest$Sample.class")) {
            bytes = input.readAllBytes();
        }
        class Realm extends ClassLoader {
            Class<?> define() {
                return defineClass(Sample.class.getName(), bytes, 0, bytes.length);
            }
        }
        Key<Object> oldKey = Key.ofType(new Realm().define(), "z");
        Key<Object> newKey = Key.ofType(new Realm().define(), "a");
        Map<Key<?>, Set<Binding<?>>> registry = new HashMap<>();
        registry.put(oldKey, Set.of(Binding.toInstance(new Object()).withKey(oldKey)));
        BindingSnapshot before = capture(registry, 1);
        registry.put(newKey, Set.of(Binding.toInstance(new Object()).withKey(newKey)));
        BindingSnapshot after = capture(registry, 2);
        String delta = render(after, before);
        assertTrue(delta.contains("+ key: @Named(\"a\") " + Sample.class.getName() + " [loader=L2]"), delta);
        assertFalse(delta.contains("@Named(\"z\")"), delta);
        assertTrue(delta.contains("L1:"), delta);
        assertTrue(delta.contains("L2:"), delta);
    }

    @Test
    void stylingDoesNotChangeSnapshotComparisonOrEscapedContent() {
        Key<String> key = Key.of(String.class, "quote\"\n");
        Binding<String> binding = Binding.toInstance("unused").withKey(key);
        Map<Key<?>, Set<Binding<?>>> registry = Map.of(key, Set.of(binding));
        BindingSnapshot snapshot = capture(registry, 1);
        BindingStyler styled = new BindingStyler() {
            @Override
            public String key(String text) {
                return "<K>" + text + "</K>";
            }

            @Override
            public String qualifier(String text) {
                return "<Q>" + text + "</Q>";
            }

            @Override
            public String header(String text) {
                return "<H>" + text + "</H>";
            }
        };
        String plain = render(snapshot, null);
        String colored = snapshot.render(injector, "test", false, null, styled);
        assertTrue(colored.contains("<K>java.lang.String [loader=bootstrap]</K>"), colored);
        assertTrue(colored.contains("<Q>@Named("), colored);
        assertTrue(colored.contains("<H>key:</H>"), colored);
        assertEquals(plain, colored.replaceAll("</?[KQH]>", ""));
        assertTrue(plain.contains("quote\\\"\\u000a"), plain);
        assertNull(capture(registry, 2).render(injector, "test", false, snapshot, styled));
    }

    @Test
    void scopeChangesAreComparedIndependentlyOfBindings() {
        var singleton = Singleton.class;
        Supplier<Scope> first = () -> {
            throw new AssertionError("scope provisioned");
        };
        Supplier<Scope> second = () -> {
            throw new AssertionError("scope provisioned");
        };
        BindingSnapshot before = BindingSnapshot.capture(Map.of(), Map.of(singleton, first), identities, false, 1);
        BindingSnapshot after = BindingSnapshot.capture(Map.of(), Map.of(singleton, second), identities, false, 2);
        String delta = render(after, before);
        assertTrue(delta.contains("- scopes:"), delta);
        assertTrue(delta.contains("+ scopes:"), delta);
        assertTrue(delta.contains(Binding.identityString(first)), delta);
        assertTrue(delta.contains(Binding.identityString(second)), delta);
    }

    @Test
    void collidingHandlesPreserveCandidatesWithEqualDescriptions() {
        DiagnosticIdentity colliding = new DiagnosticIdentity(object -> 1);
        Key<String> key = Key.of(String.class);
        Binding.TupleConstructorN<String> factory = args -> "unused";
        Binding<String> first = Binding.to(key, factory, new Class<?>[0]);
        Binding<String> second = Binding.to(key, factory, new Class<?>[0]);
        Binding<String> third = Binding.to(key, factory, new Class<?>[0]);
        BindingSnapshot initial = BindingSnapshot.capture(Map.of(key, Set.of(first)), Map.of(), colliding, false, 1);
        BindingSnapshot before =
                BindingSnapshot.capture(Map.of(key, Set.of(first, second)), Map.of(), colliding, false, 2);
        String added = render(before, initial);
        assertEquals(
                1, added.lines().filter(line -> line.contains("+ candidate:")).count());
        assertFalse(added.contains("- candidate:"));
        BindingSnapshot after = BindingSnapshot.capture(Map.of(key, Set.of(second)), Map.of(), colliding, false, 3);
        String removed = render(after, before);
        assertEquals(
                1, removed.lines().filter(line -> line.contains("- candidate:")).count());
        BindingSnapshot replacement =
                BindingSnapshot.capture(Map.of(key, Set.of(third)), Map.of(), colliding, false, 4);
        String replaced = render(replacement, after);
        assertEquals(
                1,
                replaced.lines().filter(line -> line.contains("- candidate:")).count());
        assertEquals(
                1,
                replaced.lines().filter(line -> line.contains("+ candidate:")).count());
    }

    @Test
    void repeatedMethodAndConstructorDeclarationsHaveTheSameTypeIdentity() throws Exception {
        for (int kind = 0; kind < 2; kind++) {
            GenericDeclaration first =
                    kind == 0 ? Variables.class.getDeclaredMethod("method") : Variables.class.getDeclaredConstructor();
            GenericDeclaration second =
                    kind == 0 ? Variables.class.getDeclaredMethod("method") : Variables.class.getDeclaredConstructor();
            Type firstType = ownerWith(first.getTypeParameters()[0]);
            Type secondType = ownerWith(second.getTypeParameters()[0]);
            Binding<Object> binding = Binding.toInstance(new Object());
            BindingSnapshot before = capture(Map.of(Key.ofType(firstType), Set.of(binding)), 1);
            BindingSnapshot after = capture(Map.of(Key.ofType(secondType), Set.of(binding)), 2);
            assertNull(render(after, before));
        }
    }

    private static Type ownerWith(Type variable) {
        Type owner = Types.parameterizedType(Outer.class, variable);
        return Types.parameterizedType(owner, Outer.Inner.class, new Type[] {String.class});
    }

    static class Variables {
        <T> Variables() {}

        <T> void method() {}
    }

    @Test
    void dependencyComparisonUsesSemanticKeysAndIgnoresSetIterationOrder() throws Exception {
        Key<Object> first = Key.ofType(First.class.getDeclaredField("field").getGenericType());
        Key<Object> second = Key.ofType(Second.class.getDeclaredField("field").getGenericType());
        Dependency<?> firstDependency = new Dependency<>(first, false);
        Dependency<?> secondDependency = new Dependency<>(second, false);
        Set<Dependency<?>> dependencies = new LinkedHashSet<>(List.of(firstDependency, secondDependency));
        Key<String> key = Key.of(String.class);
        Binding<String> binding = new Binding<>(key, dependencies) {
            @Override
            public Supplier<String> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                throw new AssertionError("compiled for diagnostics");
            }
        };
        BindingSnapshot before = capture(Map.of(key, Set.of(binding)), 1);
        dependencies.clear();
        dependencies.add(secondDependency);
        dependencies.add(firstDependency);
        BindingSnapshot reordered = capture(Map.of(key, Set.of(binding)), 2);
        assertNull(render(reordered, before));
        dependencies.remove(firstDependency);
        BindingSnapshot changed = capture(Map.of(key, Set.of(binding)), 3);
        String delta = render(changed, reordered);
        assertTrue(delta != null && delta.contains("~ candidate:"), String.valueOf(delta));
    }

    @Test
    void opaqueQualifiersAreComparedWithoutCallingTheirMethods() {
        class Qualifier {
            boolean armed;

            @Override
            public int hashCode() {
                if (armed) {
                    throw new AssertionError("qualifier hashCode");
                }
                return 1;
            }

            @Override
            public boolean equals(Object object) {
                if (armed) {
                    throw new AssertionError("qualifier equals");
                }
                return this == object;
            }

            @Override
            public String toString() {
                throw new AssertionError("qualifier toString");
            }
        }
        Qualifier first = new Qualifier();
        Qualifier second = new Qualifier();
        Key<String> firstKey = Key.of(String.class, first);
        Key<String> secondKey = Key.of(String.class, second);
        Binding<String> binding = Binding.toInstance("unused");
        Map<Key<?>, Set<Binding<?>>> registry = Map.of(firstKey, Set.of(binding), secondKey, Set.of(binding));
        first.armed = true;
        second.armed = true;
        BindingSnapshot before = capture(registry, 1);
        BindingSnapshot after = capture(registry, 2);
        assertNull(render(after, before));
        assertEquals(
                2,
                render(before, null)
                        .lines()
                        .filter(line -> line.startsWith("  key:"))
                        .count());
    }

    @Test
    void ownerTypeVariablesFromDifferentDeclarationsRemainDistinct() throws Exception {
        Key<Object> first = Key.ofType(First.class.getDeclaredField("field").getGenericType());
        Key<Object> second = Key.ofType(Second.class.getDeclaredField("field").getGenericType());
        Binding<Object> binding = Binding.toInstance(new Object());
        BindingSnapshot before = capture(Map.of(first, Set.of(binding)), 1);
        BindingSnapshot after = capture(Map.of(second, Set.of(binding)), 2);
        String delta = render(after, before);
        assertTrue(delta != null && delta.contains("- key:") && delta.contains("+ key:"), String.valueOf(delta));
    }

    static class Outer<T> {
        class Inner<U> {}
    }

    static class First<T> {
        Outer<T>.Inner<String> field;
    }

    static class Second<T> {
        Outer<T>.Inner<String> field;
    }

    private BindingSnapshot capture(Map<Key<?>, Set<Binding<?>>> registry, long revision) {
        return BindingSnapshot.capture(registry, Map.of(), identities, false, revision);
    }

    private String render(BindingSnapshot snapshot, BindingSnapshot previous) {
        return snapshot.render(injector, "test", false, previous, BindingStyler.NONE);
    }

    static class Sample {}
}
