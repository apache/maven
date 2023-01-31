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
package org.apache.maven.execution.scope.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.util.Providers;
import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.execution.scope.WeakMojoExecutionListener;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * MojoExecutionScope
 */
public class MojoExecutionScope implements Scope, MojoExecutionListener {
    private static final Provider<Object> SEEDED_KEY_PROVIDER = new Provider<Object>() {
        public Object get() {
            throw new IllegalStateException();
        }
    };

    private static final class ScopeState {
        private final Map<Key<?>, Provider<?>> seeded = new HashMap<>();

        private final Map<Key<?>, Object> provided = new HashMap<>();
    }

    private final ThreadLocal<LinkedList<ScopeState>> values = new ThreadLocal<>();

    public MojoExecutionScope() {}

    public void enter() {
        LinkedList<ScopeState> stack = values.get();
        if (stack == null) {
            stack = new LinkedList<>();
            values.set(stack);
        }
        stack.addFirst(new ScopeState());
    }

    private ScopeState getScopeState() {
        LinkedList<ScopeState> stack = values.get();
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException();
        }
        return stack.getFirst();
    }

    public void exit() throws MojoExecutionException {
        final LinkedList<ScopeState> stack = values.get();
        if (stack == null || stack.isEmpty()) {
            throw new IllegalStateException();
        }
        stack.removeFirst();
        if (stack.isEmpty()) {
            values.remove();
        }
    }

    public <T> void seed(Class<T> clazz, Provider<T> value) {
        getScopeState().seeded.put(Key.get(clazz), value);
    }

    public <T> void seed(Class<T> clazz, final T value) {
        getScopeState().seeded.put(Key.get(clazz), Providers.of(value));
    }

    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {
            @SuppressWarnings("unchecked")
            public T get() {
                LinkedList<ScopeState> stack = values.get();
                if (stack == null || stack.isEmpty()) {
                    throw new OutOfScopeException("Cannot access " + key + " outside of a scoping block");
                }

                ScopeState state = stack.getFirst();

                Provider<?> seeded = state.seeded.get(key);

                if (seeded != null) {
                    return (T) seeded.get();
                }

                T provided = (T) state.provided.get(key);
                if (provided == null && unscoped != null) {
                    provided = unscoped.get();
                    state.provided.put(key, provided);
                }

                return provided;
            }
        };
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Provider<T> seededKeyProvider() {
        return (Provider<T>) SEEDED_KEY_PROVIDER;
    }

    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.beforeMojoExecution(event);
        }
    }

    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.afterMojoExecutionSuccess(event);
        }
    }

    public void afterExecutionFailure(MojoExecutionEvent event) {
        for (WeakMojoExecutionListener provided : getProvidedListeners()) {
            provided.afterExecutionFailure(event);
        }
    }

    private Collection<WeakMojoExecutionListener> getProvidedListeners() {
        // the same instance can be provided multiple times under different Key's
        // deduplicate instances to avoid redundant beforeXXX/afterXXX callbacks
        IdentityHashMap<WeakMojoExecutionListener, Object> listeners = new IdentityHashMap<>();
        for (Object provided : getScopeState().provided.values()) {
            if (provided instanceof WeakMojoExecutionListener) {
                listeners.put((WeakMojoExecutionListener) provided, null);
            }
        }
        return listeners.keySet();
    }
}
