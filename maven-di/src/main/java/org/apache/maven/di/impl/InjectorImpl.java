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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

public class InjectorImpl implements Injector {

    private final Map<Key<?>, Set<Binding<?>>> bindings = new HashMap<>();
    private final Map<Class<? extends Annotation>, Scope> scopes = new HashMap<>();

    public InjectorImpl() {
        bindScope(Singleton.class, new SingletonScope());
    }

    public <T> T getInstance(Class<T> key) {
        return getInstance(Key.of(key));
    }

    public <T> T getInstance(Key<T> key) {
        return getCompiledBinding(key).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void injectInstance(T instance) {
        ReflectionUtils.generateInjectingInitializer(Key.of((Class<T>) instance.getClass()))
                .compile(this::getCompiledBinding)
                .accept(instance);
    }

    public Injector bindScope(Class<? extends Annotation> scopeAnnotation, Scope scope) {
        if (scopes.put(scopeAnnotation, scope) != null) {
            throw new DIException(
                    "Cannot rebind scope annotation class to a different implementation: " + scopeAnnotation);
        }
        return this;
    }

    public <U> Injector bindInstance(Class<U> clazz, U instance) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<U> binding = Binding.toInstance(instance);
        return doBind(key, binding);
    }

    @Override
    public Injector bindImplicit(Class<?> clazz) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<?> binding = ReflectionUtils.generateImplicitBinding(key);
        return doBind(key, binding);
    }

    private Injector doBind(Key<?> key, Binding<?> binding) {
        doBindImplicit(key, binding);
        Class<?> cls = key.getRawType().getSuperclass();
        while (cls != Object.class && cls != null) {
            key = Key.of(cls, key.getQualifier());
            doBindImplicit(key, binding);
            cls = cls.getSuperclass();
        }
        return this;
    }

    protected <U> Injector bind(Key<U> key, Binding<U> b) {
        Set<Binding<?>> bindingSet = bindings.computeIfAbsent(key, $ -> new HashSet<>());
        bindingSet.add(b);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Set<Binding<T>> getBindings(Key<T> key) {
        return (Set) bindings.get(key);
    }

    public <Q> Supplier<Q> getCompiledBinding(Key<Q> key) {
        Set<Binding<Q>> res = getBindings(key);
        if (res != null) {
            List<Binding<Q>> bindingList = new ArrayList<>(res);
            Comparator<Binding<Q>> comparing = Comparator.comparing(Binding::getPriority);
            bindingList.sort(comparing.reversed());
            Binding<Q> binding = bindingList.get(0);
            return compile(binding);
        }
        if (key.getRawType() == List.class) {
            Set<Binding<Object>> res2 = getBindings(key.getTypeParameter(0));
            if (res2 != null) {
                List<Supplier<Object>> bindingList =
                        res2.stream().map(this::compile).collect(Collectors.toList());
                //noinspection unchecked
                return () -> (Q) new WrappingList<>(bindingList, Supplier::get);
            }
        }
        if (key.getRawType() == Map.class) {
            Key<?> k = key.getTypeParameter(0);
            Key<Object> v = key.getTypeParameter(1);
            Set<Binding<Object>> res2 = getBindings(v);
            if (k.getRawType() == String.class && res2 != null) {
                Map<String, Supplier<Object>> map = res2.stream()
                        .filter(b -> b.getOriginalKey().getQualifier() == null
                                || b.getOriginalKey().getQualifier() instanceof String)
                        .collect(Collectors.toMap(
                                b -> (String) b.getOriginalKey().getQualifier(), this::compile));
                //noinspection unchecked
                return (() -> (Q) new WrappingMap<>(map, Supplier::get));
            }
        }
        throw new DIException("No binding to construct an instance for key "
                + key.getDisplayString() + ".  Existing bindings:\n"
                + bindings.keySet().stream().map(Key::toString).collect(Collectors.joining("\n - ", " - ", "")));
    }

    @SuppressWarnings("unchecked")
    private <Q> Supplier<Q> compile(Binding<Q> binding) {
        Supplier<Q> compiled = binding.compile(this::getCompiledBinding);
        if (binding.getScope() != null) {
            Scope scope = scopes.entrySet().stream()
                    .filter(e -> e.getKey().isInstance(binding.getScope()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new DIException("Scope not bound for annotation "
                            + binding.getScope().getClass()));
            compiled = scope.scope((Key<Q>) binding.getOriginalKey(), binding.getScope(), compiled);
        }
        return compiled;
    }

    protected void doBindImplicit(Key<?> key, Binding<?> binding) {
        if (binding != null) {
            // For non-explicit bindings, also bind all their base classes and interfaces according to the @Type
            Set<Key<?>> toBind = new HashSet<>();
            Deque<Key<?>> todo = new ArrayDeque<>();
            todo.add(key);

            Set<Class<?>> types;
            Typed typed = key.getRawType().getAnnotation(Typed.class);
            if (typed != null) {
                Class<?>[] typesArray = typed.value();
                if (typesArray == null || typesArray.length == 0) {
                    types = new HashSet<>(Arrays.asList(key.getRawType().getInterfaces()));
                    types.add(Object.class);
                } else {
                    types = new HashSet<>(Arrays.asList(typesArray));
                }
            } else {
                types = null;
            }

            Set<Key<?>> done = new HashSet<>();
            while (!todo.isEmpty()) {
                Key<?> type = todo.remove();
                if (done.add(type)) {
                    Class<?> cls = Types.getRawType(type.getType());
                    Type[] interfaces = cls.getGenericInterfaces();
                    Arrays.stream(interfaces)
                            .map(t -> Key.ofType(t, key.getQualifier()))
                            .forEach(todo::add);
                    Type supercls = cls.getGenericSuperclass();
                    if (supercls != null) {
                        todo.add(Key.ofType(supercls, key.getQualifier()));
                    }
                    if (types == null || types.contains(cls)) {
                        toBind.add(type);
                    }
                }
            }
            // Also bind without the qualifier
            if (key.getQualifier() != null) {
                new HashSet<>(toBind).forEach(k -> toBind.add(Key.ofType(k.getType())));
            }
            toBind.forEach((k -> bind((Key<Object>) k, (Binding<Object>) binding)));
        }
        // Bind inner classes
        for (Class<?> inner : key.getRawType().getDeclaredClasses()) {
            bindImplicit(inner);
        }
        // Bind inner providers
        for (Method method : key.getRawType().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Provides.class)) {
                Object qualifier = ReflectionUtils.qualifierOf(method);
                Annotation scope = ReflectionUtils.scopeOf(method);

                TypeVariable<Method>[] methodTypeParameters = method.getTypeParameters();
                if (methodTypeParameters.length != 0) {
                    throw new DIException("Parameterized method are not supported " + method);
                }
                Map<TypeVariable<?>, Type> mapping = new HashMap<>();
                for (TypeVariable<Method> methodTypeParameter : methodTypeParameters) {
                    mapping.put(methodTypeParameter, methodTypeParameter);
                }
                mapping.putAll(Types.getAllTypeBindings(key.getRawType()));

                Type returnType = Types.bind(method.getGenericReturnType(), mapping);
                Key<Object> rkey = Key.ofType(returnType, qualifier);

                Set<Class<?>> types;
                Typed typed = method.getAnnotation(Typed.class);
                if (typed != null) {
                    Class<?>[] typesArray = typed.value();
                    if (typesArray == null || typesArray.length == 0) {
                        types = new HashSet<>(Arrays.asList(rkey.getRawType().getInterfaces()));
                        types.add(Object.class);
                    } else {
                        types = new HashSet<>(Arrays.asList(typesArray));
                    }
                } else {
                    types = null;
                }

                Set<Key<?>> toBind = new HashSet<>();
                Deque<Key<?>> todo = new ArrayDeque<>();
                todo.add(rkey);

                Set<Key<?>> done = new HashSet<>();
                while (!todo.isEmpty()) {
                    Key<?> type = todo.remove();
                    if (done.add(type)) {
                        Class<?> cls = Types.getRawType(type.getType());
                        Type[] interfaces = cls.getGenericInterfaces();
                        Arrays.stream(interfaces)
                                .map(t -> Key.ofType(t, qualifier))
                                .forEach(todo::add);
                        Type supercls = cls.getGenericSuperclass();
                        if (supercls != null) {
                            todo.add(Key.ofType(supercls, qualifier));
                        }
                        if (types == null || types.contains(cls)) {
                            toBind.add(type);
                        }
                    }
                }
                // Also bind without the qualifier
                if (qualifier != null) {
                    new HashSet<>(toBind).forEach(k -> toBind.add(Key.ofType(k.getType())));
                }

                Binding<Object> bind = ReflectionUtils.bindingFromMethod(method).scope(scope);
                toBind.forEach((k -> bind((Key<Object>) k, bind)));
            }
        }
    }

    private static class WrappingMap<K, V, T> extends AbstractMap<K, V> {

        private final Map<K, T> delegate;
        private final Function<T, V> mapper;

        WrappingMap(Map<K, T> delegate, Function<T, V> mapper) {
            this.delegate = delegate;
            this.mapper = mapper;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<Entry<K, V>>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, T>> it = delegate.entrySet().iterator();
                    return new Iterator<Entry<K, V>>() {
                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<K, V> next() {
                            Entry<K, T> n = it.next();
                            return new SimpleImmutableEntry<>(n.getKey(), mapper.apply(n.getValue()));
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
    }

    private static class WrappingList<Q, T> extends AbstractList<Q> {

        private final List<T> delegate;
        private final Function<T, Q> mapper;

        WrappingList(List<T> delegate, Function<T, Q> mapper) {
            this.delegate = delegate;
            this.mapper = mapper;
        }

        @Override
        public Q get(int index) {
            return mapper.apply(delegate.get(index));
        }

        @Override
        public int size() {
            return delegate.size();
        }
    }

    private static class SingletonScope implements Scope {
        Map<Key<?>, java.util.function.Supplier<?>> cache = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <T> java.util.function.Supplier<T> scope(
                Key<T> key, Annotation scope, java.util.function.Supplier<T> unscoped) {
            return (java.util.function.Supplier<T>)
                    cache.computeIfAbsent(key, k -> new java.util.function.Supplier<T>() {
                        volatile T instance;

                        @Override
                        public T get() {
                            if (instance == null) {
                                synchronized (this) {
                                    if (instance == null) {
                                        instance = unscoped.get();
                                    }
                                }
                            }
                            return instance;
                        }
                    });
        }
    }
}
