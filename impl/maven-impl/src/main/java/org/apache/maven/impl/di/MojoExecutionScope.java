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
package org.apache.maven.impl.di;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;
import org.apache.maven.di.impl.DIException;

/**
 * MojoExecutionScope
 */
public class MojoExecutionScope implements Scope {

    protected static final class ScopeState {
        private final Map<Key<?>, Supplier<?>> seeded = new HashMap<>();

        private final Map<Key<?>, Object> provided = new HashMap<>();

        public <T> void seed(Class<T> clazz, Supplier<T> value) {
            seeded.put(Key.of(clazz), value);
        }

        public Collection<Object> provided() {
            return provided.values();
        }
    }

    private final ThreadLocal<LinkedList<ScopeState>> values = new ThreadLocal<>();

    public MojoExecutionScope() {}

    public static <T> Supplier<T> seededKeySupplier(Class<? extends T> clazz) {
        return () -> {
            throw new IllegalStateException(
                    "No instance of " + clazz.getName() + " is bound to the mojo execution scope.");
        };
    }

    public void enter() {
        LinkedList<ScopeState> stack = values.get();
        if (stack == null) {
            stack = new LinkedList<>();
            values.set(stack);
        }
        stack.addFirst(new ScopeState());
    }

    protected ScopeState getScopeState() {
        LinkedList<ScopeState> stack = values.get();
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException();
        }
        return stack.getFirst();
    }

    public void exit() {
        final LinkedList<ScopeState> stack = values.get();
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException();
        }
        stack.removeFirst();
        if (stack.isEmpty()) {
            values.remove();
        }
    }

    public <T> void seed(Class<T> clazz, Supplier<T> value) {
        getScopeState().seed(clazz, value);
    }

    public <T> void seed(Class<T> clazz, final T value) {
        seed(clazz, (Supplier<T>) () -> value);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public <T> Supplier<T> scope(@Nonnull Key<T> key, @Nonnull Supplier<T> unscoped) {
        return () -> {
            LinkedList<ScopeState> stack = values.get();
            if (stack == null || stack.isEmpty()) {
                throw new DIException("Cannot access " + key + " outside of a scoping block");
            }

            ScopeState state = stack.getFirst();

            Supplier<?> seeded = state.seeded.get(key);

            if (seeded != null) {
                return (T) seeded.get();
            }

            T provided = (T) state.provided.get(key);
            if (provided == null && unscoped != null) {
                provided = unscoped.get();
                state.provided.put(key, provided);
            }

            return provided;
        };
    }
}
