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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.di.Key;

import static java.util.stream.Collectors.joining;

public abstract class Binding<T> {
    private final Set<Key<?>> dependencies;
    private Annotation scope;
    private int priority;
    private Key<?> originalKey;

    protected Binding(Key<? extends T> originalKey, Set<Key<?>> dependencies) {
        this(originalKey, dependencies, null, 0);
    }

    protected Binding(Key<?> originalKey, Set<Key<?>> dependencies, Annotation scope, int priority) {
        this.originalKey = originalKey;
        this.dependencies = dependencies;
        this.scope = scope;
        this.priority = priority;
    }

    public static <T> Binding<T> toInstance(T instance) {
        return new BindingToInstance<>(instance);
    }

    public static <R> Binding<R> to(TupleConstructorN<R> constructor, Class<?>[] types) {
        return Binding.to(constructor, Stream.of(types).map(Key::of).toArray(Key<?>[]::new));
    }

    public static <R> Binding<R> to(TupleConstructorN<R> constructor, Key<?>[] dependencies) {
        return to(constructor, dependencies, 0);
    }

    public static <R> Binding<R> to(TupleConstructorN<R> constructor, Key<?>[] dependencies, int priority) {
        return new BindingToConstructor<>(null, constructor, dependencies, priority);
    }

    // endregion

    public Binding<T> scope(Annotation scope) {
        this.scope = scope;
        return this;
    }

    public Binding<T> prioritize(int priority) {
        this.priority = priority;
        return this;
    }

    public Binding<T> withKey(Key<?> key) {
        this.originalKey = key;
        return this;
    }

    public Binding<T> initializeWith(BindingInitializer<T> bindingInitializer) {
        return new Binding<T>(
                this.originalKey,
                Stream.of(this.dependencies, bindingInitializer.getDependencies())
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet()),
                this.scope,
                this.priority) {
            @Override
            public Supplier<T> compile(Function<Key<?>, Supplier<?>> compiler) {
                final Supplier<T> compiledBinding = Binding.this.compile(compiler);
                final Consumer<T> consumer = bindingInitializer.compile(compiler);
                return () -> {
                    T instance = compiledBinding.get();
                    consumer.accept(instance);
                    return instance;
                };
            }

            @Override
            public String toString() {
                return Binding.this.toString();
            }
        };
    }

    public abstract Supplier<T> compile(Function<Key<?>, Supplier<?>> compiler);

    public Set<Key<?>> getDependencies() {
        return dependencies;
    }

    public Annotation getScope() {
        return scope;
    }

    public String getDisplayString() {
        return dependencies.stream().map(Key::getDisplayString).collect(joining(", ", "[", "]"));
    }

    public Key<?> getOriginalKey() {
        return originalKey;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "Binding" + dependencies.toString();
    }

    @FunctionalInterface
    public interface TupleConstructorN<R> {
        R create(Object... args);
    }

    public static class BindingToInstance<T> extends Binding<T> {
        final T instance;

        BindingToInstance(T instance) {
            super(null, Collections.emptySet());
            this.instance = instance;
        }

        @Override
        public Supplier<T> compile(Function<Key<?>, Supplier<?>> compiler) {
            return () -> instance;
        }

        @Override
        public String toString() {
            return "BindingToInstance[" + instance + "]" + getDependencies();
        }
    }

    public static class BindingToConstructor<T> extends Binding<T> {
        final TupleConstructorN<T> constructor;

        BindingToConstructor(
                Key<? extends T> key, TupleConstructorN<T> constructor, Key<?>[] dependencies, int priority) {
            super(key, new HashSet<>(Arrays.asList(dependencies)), null, priority);
            this.constructor = constructor;
        }

        @Override
        public Supplier<T> compile(Function<Key<?>, Supplier<?>> compiler) {
            return () -> {
                Object[] args = getDependencies().stream()
                        .map(compiler)
                        .map(Supplier::get)
                        .toArray();
                return constructor.create(args);
            };
        }

        @Override
        public String toString() {
            return "BindingToConstructor[" + constructor + "]" + getDependencies();
        }
    }
}
