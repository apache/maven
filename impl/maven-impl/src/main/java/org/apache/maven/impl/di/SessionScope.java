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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;
import org.apache.maven.di.impl.Types;

public class SessionScope implements Scope {

    /**
     * ScopeState
     */
    protected static final class ScopeState {
        private final Map<Key<?>, CachingProvider<?>> provided = new ConcurrentHashMap<>();

        public <T> void seed(Class<T> clazz, Supplier<T> value) {
            provided.put(Key.of(clazz), new CachingProvider<>(value));
        }

        @SuppressWarnings("unchecked")
        public <T> Supplier<T> scope(Key<T> key, Supplier<T> unscoped) {
            Supplier<?> provider = provided.computeIfAbsent(key, k -> new CachingProvider<>(unscoped));
            return (Supplier<T>) provider;
        }

        public Collection<CachingProvider<?>> providers() {
            return provided.values();
        }
    }

    protected final List<ScopeState> values = new CopyOnWriteArrayList<>();

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

    public <T> void seed(Class<T> clazz, Supplier<T> value) {
        getScopeState().seed(clazz, value);
    }

    public <T> void seed(Class<T> clazz, T value) {
        seed(clazz, (Supplier<T>) () -> value);
    }

    @Nonnull
    @Override
    public <T> Supplier<T> scope(@Nonnull Key<T> key, @Nonnull Supplier<T> unscoped) {
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
    protected <T> T createProxy(Key<T> key, Supplier<T> unscoped) {
        InvocationHandler dispatcher = (proxy, method, args) -> dispatch(key, unscoped, method, args);
        Class<T> superType = (Class<T>) Types.getRawType(key.getType());
        Class<?>[] interfaces = getInterfaces(superType);
        return (T) java.lang.reflect.Proxy.newProxyInstance(superType.getClassLoader(), interfaces, dispatcher);
    }

    protected <T> Object dispatch(Key<T> key, Supplier<T> unscoped, Method method, Object[] args) throws Throwable {
        method.setAccessible(true);
        try {
            return method.invoke(getScopeState().scope(key, unscoped).get(), args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    protected Class<?>[] getInterfaces(Class<?> superType) {
        if (superType.isInterface()) {
            return new Class<?>[] {superType};
        } else {
            for (Annotation a : superType.getAnnotations()) {
                Class<? extends Annotation> annotationType = a.annotationType();
                if (isTypeAnnotation(annotationType)) {
                    try {
                        Class<?>[] value =
                                (Class<?>[]) annotationType.getMethod("value").invoke(a);
                        if (value.length == 0) {
                            value = superType.getInterfaces();
                        }
                        List<Class<?>> nonInterfaces =
                                Stream.of(value).filter(c -> !c.isInterface()).toList();
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

    protected boolean isTypeAnnotation(Class<? extends Annotation> annotationType) {
        return "org.apache.maven.api.di.Typed".equals(annotationType.getName());
    }

    /**
     * A provider wrapping an existing provider with a cache
     * @param <T> the provided type
     */
    protected static class CachingProvider<T> implements Supplier<T> {
        private final Supplier<T> provider;
        private volatile T value;

        CachingProvider(Supplier<T> provider) {
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

    public static <T> Supplier<T> seededKeySupplier(Class<? extends T> clazz) {
        return () -> {
            throw new IllegalStateException("No instance of " + clazz.getName() + " is bound to the session scope.");
        };
    }
}
