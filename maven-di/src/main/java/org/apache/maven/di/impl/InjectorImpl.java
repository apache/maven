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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

public class InjectorImpl implements Injector {

    private final Map<Key<?>, Set<Binding<?>>> bindings = new HashMap<>();
    private final Map<Class<? extends Annotation>, Supplier<Scope>> scopes = new HashMap<>();

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

    @Override
    public Injector discover(ClassLoader classLoader) {
        try {
            Enumeration<URL> enumeration = classLoader.getResources("META-INF/maven/org.apache.maven.api.di.Inject");
            while (enumeration.hasMoreElements()) {
                try (InputStream is = enumeration.nextElement().openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
                    for (String line :
                            reader.lines().filter(l -> !l.startsWith("#")).collect(Collectors.toList())) {
                        Class<?> clazz = classLoader.loadClass(line);
                        bindImplicit(clazz);
                    }
                }
            }
        } catch (Exception e) {
            throw new DIException("Error while discovering DI classes from classLoader", e);
        }
        return this;
    }

    public Injector bindScope(Class<? extends Annotation> scopeAnnotation, Scope scope) {
        return bindScope(scopeAnnotation, () -> scope);
    }

    public Injector bindScope(Class<? extends Annotation> scopeAnnotation, Supplier<Scope> scope) {
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
        if (clazz.isInterface()) {
            bindings.computeIfAbsent(key, $ -> new HashSet<>());
        } else if (!Modifier.isAbstract(clazz.getModifiers())) {
            Binding<?> binding = ReflectionUtils.generateImplicitBinding(key);
            doBind(key, binding);
        }
        return this;
    }

    private LinkedHashSet<Key<?>> current = new LinkedHashSet<>();

    private Injector doBind(Key<?> key, Binding<?> binding) {
        if (!current.add(key)) {
            current.add(key);
            throw new DIException("Circular references: " + current);
        }
        doBindImplicit(key, binding);
        Class<?> cls = key.getRawType().getSuperclass();
        while (cls != Object.class && cls != null) {
            key = Key.of(cls, key.getQualifier());
            doBindImplicit(key, binding);
            cls = cls.getSuperclass();
        }
        current.remove(key);
        return this;
    }

    protected <U> Injector bind(Key<U> key, Binding<U> b) {
        Set<Binding<?>> bindingSet = bindings.computeIfAbsent(key, $ -> new HashSet<>());
        bindingSet.add(b);
        return this;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> Set<Binding<T>> getBindings(Key<T> key) {
        return (Set) bindings.get(key);
    }

    protected Set<Key<?>> getBoundKeys() {
        return bindings.keySet();
    }

    public Map<Key<?>, Set<Binding<?>>> getBindings() {
        return bindings;
    }

    public <Q> Supplier<Q> getCompiledBinding(Key<Q> key) {
        Set<Binding<Q>> res = getBindings(key);
        if (res != null && !res.isEmpty()) {
            List<Binding<Q>> bindingList = new ArrayList<>(res);
            Comparator<Binding<Q>> comparing = Comparator.comparing(Binding::getPriority);
            bindingList.sort(comparing.reversed());
            Binding<Q> binding = bindingList.get(0);
            return compile(binding);
        }
        if (key.getRawType() == List.class) {
            Set<Binding<Object>> res2 = getBindings(key.getTypeParameter(0));
            if (res2 != null) {
                List<Supplier<Object>> list = res2.stream().map(this::compile).collect(Collectors.toList());
                //noinspection unchecked
                return () -> (Q) list(list);
            }
        }
        if (key.getRawType() == Map.class) {
            Key<?> k = key.getTypeParameter(0);
            Key<Object> v = key.getTypeParameter(1);
            Set<Binding<Object>> res2 = getBindings(v);
            if (k.getRawType() == String.class && res2 != null) {
                Map<String, Supplier<Object>> map = res2.stream()
                        .filter(b -> b.getOriginalKey() == null
                                || b.getOriginalKey().getQualifier() == null
                                || b.getOriginalKey().getQualifier() instanceof String)
                        .collect(Collectors.toMap(
                                b -> (String)
                                        (b.getOriginalKey() != null
                                                ? b.getOriginalKey().getQualifier()
                                                : null),
                                this::compile));
                //noinspection unchecked
                return () -> (Q) map(map);
            }
        }
        throw new DIException("No binding to construct an instance for key "
                + key.getDisplayString() + ".  Existing bindings:\n"
                + getBoundKeys().stream()
                        .map(Key::toString)
                        .map(String::trim)
                        .sorted()
                        .distinct()
                        .collect(Collectors.joining("\n - ", " - ", "")));
    }

    @SuppressWarnings("unchecked")
    protected <Q> Supplier<Q> compile(Binding<Q> binding) {
        Supplier<Q> compiled = binding.compile(this::getCompiledBinding);
        if (binding.getScope() != null) {
            Scope scope = scopes.entrySet().stream()
                    .filter(e -> e.getKey().isInstance(binding.getScope()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new DIException("Scope not bound for annotation "
                            + binding.getScope().annotationType()))
                    .get();
            compiled = scope.scope((Key<Q>) binding.getOriginalKey(), binding.getScope(), compiled);
        }
        return compiled;
    }

    protected void doBindImplicit(Key<?> key, Binding<?> binding) {
        if (binding != null) {
            // For non-explicit bindings, also bind all their base classes and interfaces according to the @Type
            Object qualifier = key.getQualifier();
            Class<?> type = key.getRawType();
            Set<Class<?>> types = getBoundTypes(type.getAnnotation(Typed.class), type);
            for (Type t : Types.getAllSuperTypes(type)) {
                if (types == null || types.contains(Types.getRawType(t))) {
                    bind(Key.ofType(t, qualifier), binding);
                    if (qualifier != null) {
                        bind(Key.ofType(t), binding);
                    }
                }
            }
        }
        // Bind inner classes
        for (Class<?> inner : key.getRawType().getDeclaredClasses()) {
            boolean hasQualifier = Stream.of(inner.getAnnotations())
                    .anyMatch(ann -> ann.annotationType().isAnnotationPresent(Qualifier.class));
            if (hasQualifier) {
                bindImplicit(inner);
            }
        }
        // Bind inner providers
        for (Method method : key.getRawType().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Provides.class)) {
                if (method.getTypeParameters().length != 0) {
                    throw new DIException("Parameterized method are not supported " + method);
                }
                Object qualifier = ReflectionUtils.qualifierOf(method);
                Annotation scope = ReflectionUtils.scopeOf(method);
                Type returnType = method.getGenericReturnType();
                Set<Class<?>> types = getBoundTypes(method.getAnnotation(Typed.class), Types.getRawType(returnType));
                Binding<Object> bind = ReflectionUtils.bindingFromMethod(method).scope(scope);
                for (Type t : Types.getAllSuperTypes(returnType)) {
                    if (types == null || types.contains(Types.getRawType(t))) {
                        bind(Key.ofType(t, qualifier), bind);
                        if (qualifier != null) {
                            bind(Key.ofType(t), bind);
                        }
                    }
                }
            }
        }
    }

    private static Set<Class<?>> getBoundTypes(Typed typed, Class<?> clazz) {
        if (typed != null) {
            Class<?>[] typesArray = typed.value();
            if (typesArray == null || typesArray.length == 0) {
                Set<Class<?>> types = new HashSet<>(Arrays.asList(clazz.getInterfaces()));
                types.add(Object.class);
                return types;
            } else {
                return new HashSet<>(Arrays.asList(typesArray));
            }
        } else {
            return null;
        }
    }

    protected <K, V> Map<K, V> map(Map<K, Supplier<V>> map) {
        return new WrappingMap<>(map, Supplier::get);
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

    protected <T> List<T> list(List<Supplier<T>> bindingList) {
        return new WrappingList<>(bindingList, Supplier::get);
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
        Map<Key<?>, java.util.function.Supplier<?>> cache = new ConcurrentHashMap<>();

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
