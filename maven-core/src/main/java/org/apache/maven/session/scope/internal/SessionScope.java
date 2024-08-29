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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.name.Names;

/**
 * SessionScope
 */
public class SessionScope implements Scope, org.apache.maven.di.Scope {

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
        return () -> {
            if (values.isEmpty()) {
                return createProxy(key, unscoped);
            } else {
                return getScopeState().scope(key, unscoped).get();
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Supplier<T> scope(org.apache.maven.di.Key<T> key, Annotation scope, Supplier<T> unscoped) {
        Object qualifier = key.getQualifier();
        Key<?> k = qualifier != null
                ? Key.get(key.getType(), qualifier instanceof String s ? Names.named(s) : (Annotation) qualifier)
                : Key.get(key.getType());
        Provider<T> up = unscoped::get;
        Provider<T> p = scope((Key<T>) k, up);
        return p::get;
    }

    @SuppressWarnings("unchecked")
    private <T> T createProxy(Key<T> key, Provider<T> unscoped) {
        InvocationHandler dispatcher = (proxy, method, args) -> {
            method.setAccessible(true);
            try {
                return method.invoke(getScopeState().scope(key, unscoped).get(), args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        Class<T> superType = (Class<T>) key.getTypeLiteral().getRawType();
        Class<?>[] interfaces = getInterfaces(superType);
        return (T) java.lang.reflect.Proxy.newProxyInstance(superType.getClassLoader(), interfaces, dispatcher);
    }

    private Class<?>[] getInterfaces(Class<?> superType) {
        if (superType.isInterface()) {
            return new Class<?>[] {superType};
        } else {
            for (Annotation a : superType.getAnnotations()) {
                Class<? extends Annotation> annotationType = a.annotationType();
                if ("org.apache.maven.api.di.Typed".equals(annotationType.getName())
                        || "org.eclipse.sisu.Typed".equals(annotationType.getName())
                        || "javax.enterprise.inject.Typed".equals(annotationType.getName())
                        || "jakarta.enterprise.inject.Typed".equals(annotationType.getName())) {
                    try {
                        Class<?>[] value =
                                (Class<?>[]) annotationType.getMethod("value").invoke(a);
                        if (value.length == 0) {
                            value = superType.getInterfaces();
                        }
                        List<Class<?>> nonInterfaces =
                                Stream.of(value).filter(c -> !c.isInterface()).collect(Collectors.toList());
                        if (!nonInterfaces.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "The Typed annotation must contain only interfaces but the following types are not: "
                                            + nonInterfaces);
                        }
                        return value;
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            throw new IllegalArgumentException("The use of session scoped proxies require "
                    + "a org.eclipse.sisu.Typed or javax.enterprise.inject.Typed annotation");
        }
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

    public static <T> Provider<T> seededKeyProvider(Class<? extends T> clazz) {
        return () -> {
            throw new IllegalStateException("No instance of " + clazz.getName() + " is bound to the session scope.");
        };
    }
}
