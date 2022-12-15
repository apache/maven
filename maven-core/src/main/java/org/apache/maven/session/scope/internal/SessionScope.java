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
package org.apache.maven.session.scope.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * SessionScope
 */
public class SessionScope implements Scope {

    private static final Provider<Object> SEEDED_KEY_PROVIDER = () -> {
        throw new IllegalStateException();
    };

    /**
     * ScopeState
     */
    protected static final class ScopeState {
        private final Map<Key<?>, CachingProvider<?>> provided = new ConcurrentHashMap<>();

        public <T> void seed(Class<T> clazz, Provider<T> value) {
            provided.put(Key.get(clazz), new CachingProvider<>(value));
        }

        @SuppressWarnings("unchecked")
        public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
            Provider<?> provider = provided.computeIfAbsent(key, k -> new CachingProvider<>(unscoped));
            return (Provider<T>) provider;
        }

        public Collection<CachingProvider<?>> providers() {
            return provided.values();
        }
    }

    private final List<ScopeState> values = new CopyOnWriteArrayList<>();

    public void enter() {
        values.add(0, new ScopeState());
    }

    protected ScopeState getScopeState() {
        if (values.isEmpty()) {
            throw new OutOfScopeException("Cannot access session scope outside of a scoping block");
        }
        return values.get(0);
    }

    public void exit() {
        if (values.isEmpty()) {
            throw new IllegalStateException();
        }
        values.remove(0);
    }

    public <T> void seed(Class<T> clazz, Provider<T> value) {
        getScopeState().seed(clazz, value);
    }

    public <T> void seed(Class<T> clazz, final T value) {
        seed(clazz, (Provider<T>) () -> value);
    }

    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        // Lazy evaluating provider
        return () -> getScopeState().scope(key, unscoped).get();
    }

    /**
     * A provider wrapping an existing provider with a cache
     * @param <T> the provided type
     */
    protected static class CachingProvider<T> implements Provider<T> {
        private final Provider<T> provider;
        private volatile T value;

        CachingProvider(Provider<T> provider) {
            this.provider = provider;
        }

        public T value() {
            return value;
        }

        @Override
        public T get() {
            if (value == null) {
                synchronized (this) {
                    if (value == null) {
                        value = provider.get();
                    }
                }
            }
            return value;
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Provider<T> seededKeyProvider() {
        return (Provider<T>) SEEDED_KEY_PROVIDER;
    }
}
