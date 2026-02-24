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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Aggregate;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

import static org.apache.maven.di.impl.Binding.getPriorityComparator;

public class InjectorImpl implements Injector {

    private final Map<Key<?>, Set<Binding<?>>> bindings = new HashMap<>();
    private final Map<Class<? extends Annotation>, Supplier<Scope>> scopes = new HashMap<>();
    private final Set<String> loadedUrls = new HashSet<>();
    private final ThreadLocal<Set<Key<?>>> resolutionStack = new ThreadLocal<>();

    public InjectorImpl() {
        bindScope(Singleton.class, new SingletonScope());
    }

    @Nonnull
    @Override
    public <T> T getInstance(@Nonnull Class<T> key) {
        return getInstance(Key.of(key));
    }

    @Nonnull
    @Override
    public <T> T getInstance(@Nonnull Key<T> key) {
        return getCompiledBinding(new Dependency<>(key, false)).get();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void injectInstance(@Nonnull T instance) {
        ReflectionUtils.generateInjectingInitializer(Key.of((Class<T>) instance.getClass()))
                .compile(this::getCompiledBinding)
                .accept(instance);
    }

    @Nonnull
    @Override
    public Injector discover(@Nonnull ClassLoader classLoader) {
        try {
            Enumeration<URL> enumeration = classLoader.getResources("META-INF/maven/org.apache.maven.api.di.Inject");
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (loadedUrls.add(url.toExternalForm())) {
                    try (InputStream is = url.openStream();
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
                        for (String line :
                                reader.lines().filter(l -> !l.startsWith("#")).toList()) {
                            Class<?> clazz = classLoader.loadClass(line);
                            bindImplicit(clazz);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DIException("Error while discovering DI classes from classLoader", e);
        }
        return this;
    }

    @Nonnull
    @Override
    public Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Scope scope) {
        return bindScope(scopeAnnotation, () -> scope);
    }

    @Nonnull
    @Override
    public Injector bindScope(@Nonnull Class<? extends Annotation> scopeAnnotation, @Nonnull Supplier<Scope> scope) {
        if (scopes.put(scopeAnnotation, scope) != null) {
            throw new DIException(
                    "Cannot rebind scope annotation class to a different implementation: " + scopeAnnotation);
        }
        return this;
    }

    @Nonnull
    @Override
    public <U> Injector bindInstance(@Nonnull Class<U> clazz, @Nonnull U instance) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<U> binding = Binding.toInstance(instance);
        return doBind(key, binding);
    }

    @Override
    public <U> Injector bindSupplier(@Nonnull Class<U> clazz, @Nonnull Supplier<U> supplier) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<U> binding = Binding.toSupplier(supplier);
        return doBind(key, binding);
    }

    @Nonnull
    @Override
    public Injector bindImplicit(@Nonnull Class<?> clazz) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        if (clazz.isInterface()) {
            bindings.computeIfAbsent(key, $ -> new HashSet<>());
            if (key.getQualifier() != null) {
                bindings.computeIfAbsent(Key.ofType(clazz), $ -> new HashSet<>());
            }
        } else if (!Modifier.isAbstract(clazz.getModifiers())) {
            Binding<?> binding = ReflectionUtils.generateImplicitBinding(key);
            doBind(key, binding);
        }
        return this;
    }

    private final LinkedHashSet<Key<?>> current = new LinkedHashSet<>();

    private Injector doBind(Key<?> key, Binding<?> binding) {
        if (!current.add(key)) {
            current.add(key);
            throw new DIException("Circular references: " + current);
        }
        try {
            doBindImplicit(key, binding);
            Class<?> cls = key.getRawType().getSuperclass();
            while (cls != Object.class && cls != null) {
                doBindImplicit(Key.of(cls, key.getQualifier()), binding);
                if (key.getQualifier() != null) {
                    bind(Key.ofType(cls), binding);
                }
                cls = cls.getSuperclass();
            }
            return this;
        } finally {
            current.remove(key);
        }
    }

    protected <U> Injector bind(Key<U> key, Binding<U> b) {
        Set<Binding<?>> bindingSet = bindings.computeIfAbsent(key, $ -> new LinkedHashSet<>());
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

    public <T> Set<Binding<T>> getAllBindings(Class<T> clazz) {
        return getBindings(Key.of(clazz));
    }

    public <Q> Supplier<Q> getCompiledBinding(Dependency<Q> dep) {
        Key<Q> key = dep.key();
        Supplier<Q> originalSupplier = doGetCompiledBinding(dep);
        return () -> {
            checkCyclicDependency(key);
            try {
                return originalSupplier.get();
            } finally {
                removeFromResolutionStack(key);
            }
        };
    }

    public <Q> Supplier<Q> doGetCompiledBinding(Dependency<Q> dep) {
        Key<Q> key = dep.key();
        Set<Binding<Q>> res = getBindings(key);

        if (res != null && !res.isEmpty()) {
            // Check if this is a List/Map and all bindings are @Aggregate
            // If so, use aggregation logic instead of explicit binding
            if (key.getRawType() == List.class && areAllAggregateListProviders(res)) {
                return handleListAggregation(key);
            }
            if (key.getRawType() == Map.class && areAllAggregateMapProviders(res)) {
                return handleMapAggregation(key);
            }

            // Use explicit binding (highest priority wins)
            List<Binding<Q>> bindingList = new ArrayList<>(res);
            bindingList.sort(getPriorityComparator());
            Binding<Q> binding = bindingList.get(0);
            return compile(binding);
        }
        if (key.getRawType() == List.class) {
            return handleListAggregation(key);
        }
        if (key.getRawType() == Map.class) {
            return handleMapAggregation(key);
        }
        if (dep.optional()) {
            return () -> null;
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

    /**
     * Handle aggregation for List<T> injection.
     * Rules:
     * 1. If there's an explicit (non-@Aggregate) provider for List<T>, use only that
     * 2. Otherwise, aggregate all beans of type T (including @Aggregate contributions)
     * 3. Sort by priority (highest first)
     */
    @SuppressWarnings("unchecked")
    private <Q> Supplier<Q> handleListAggregation(Key<Q> key) {
        Key<Object> elementKey = key.getTypeParameter(0);
        Set<Binding<Object>> elementBindings = getBindings(elementKey);
        Set<Binding<Q>> listBindings = getBindings(key);

        if ((elementBindings == null || elementBindings.isEmpty())
                && (listBindings == null || listBindings.isEmpty())) {
            // No elements found, return empty list
            return () -> (Q) new ArrayList<>();
        }

        if (elementBindings == null) {
            elementBindings = new HashSet<>();
        }

        // Check if there's an explicit (non-@Aggregate) List<T> provider in both element and list bindings
        Binding<Object> explicitListProvider = findExplicitListProvider(elementBindings);
        if (explicitListProvider == null && listBindings != null) {
            explicitListProvider = (Binding<Object>) listBindings.stream()
                    .filter(binding -> isListProvider(binding) && !isAggregate(binding))
                    .findFirst()
                    .orElse(null);
        }
        if (explicitListProvider != null) {
            // Use explicit provider, ignore aggregation
            Supplier<Object> compiled = compile(explicitListProvider);
            return () -> (Q) compiled.get();
        }

        // Aggregate all beans including @Aggregate contributions
        List<Binding<Object>> allBindings = new ArrayList<>();

        // Process element bindings (individual beans)
        for (Binding<Object> binding : elementBindings) {

            if (isAggregateListProvider(binding)) {
                // This is an @Aggregate List<T> provider, expand its elements
                allBindings.addAll(expandAggregateList(binding));
            } else if (!isListProvider(binding)) {
                // Regular bean (not a List provider), add it directly
                allBindings.add(binding);
            }
        }

        // Process list bindings (@Aggregate List providers)
        if (listBindings != null) {
            for (Binding<Q> binding : listBindings) {
                Binding<Object> objBinding = (Binding<Object>) binding;

                if (isAggregateListProvider(objBinding)) {
                    // This is an @Aggregate List<T> provider, expand its elements
                    allBindings.addAll(expandAggregateList(objBinding));
                }
            }
        }

        // Sort by priority (highest first)
        allBindings.sort(getPriorityComparator());

        List<Supplier<Object>> suppliers =
                allBindings.stream().map(this::compile).collect(Collectors.toList());

        return () -> (Q) list(suppliers, Supplier::get);
    }

    /**
     * Handle aggregation for Map<String, T> injection.
     * Rules:
     * 1. If there's an explicit (non-@Aggregate) provider for Map<String, T>, use only that
     * 2. Otherwise, aggregate all @Named beans of type T (including @Aggregate contributions)
     * 3. Use @Named qualifier value as the key
     */
    @SuppressWarnings("unchecked")
    private <Q> Supplier<Q> handleMapAggregation(Key<Q> key) {
        Key<?> keyType = key.getTypeParameter(0);
        Key<Object> valueType = key.getTypeParameter(1);
        Set<Binding<Object>> valueBindings = getBindings(valueType);
        Set<Binding<Q>> mapBindings = getBindings(key);

        if (keyType.getRawType() != String.class) {
            // Only support Map<String, T>
            return null;
        }

        if ((valueBindings == null || valueBindings.isEmpty()) && (mapBindings == null || mapBindings.isEmpty())) {
            // No elements found, return empty map
            return () -> (Q) new LinkedHashMap<>();
        }

        // Ensure valueBindings is not null for the loop below
        if (valueBindings == null) {
            valueBindings = new HashSet<>();
        }

        // Check if there's an explicit (non-@Aggregate) Map<String, T> provider
        // Look in both value bindings and map bindings
        Binding<Object> explicitMapProvider = findExplicitMapProvider(valueBindings);
        if (explicitMapProvider == null && mapBindings != null) {
            explicitMapProvider = (Binding<Object>) mapBindings.stream()
                    .filter(binding -> isMapProvider(binding) && !isAggregate(binding))
                    .findFirst()
                    .orElse(null);
        }
        if (explicitMapProvider != null) {
            // Use explicit provider, ignore aggregation
            Supplier<Object> compiled = compile(explicitMapProvider);
            return () -> (Q) compiled.get();
        }

        // Aggregate all @Named beans including @Aggregate contributions
        Map<String, Binding<Object>> aggregatedBindings = new LinkedHashMap<>();

        // Process value bindings (individual @Named beans)
        for (Binding<Object> binding : valueBindings) {
            if (isAggregateMapProvider(binding)) {
                // This is an @Aggregate Map<String, T> provider, expand its entries
                Map<String, Binding<Object>> expanded = expandAggregateMap(binding);
                aggregatedBindings.putAll(expanded);
            } else if (!isMapProvider(binding)) {
                // Regular bean with @Named qualifier
                String name = extractNamedQualifier(binding);
                if (name != null) {
                    aggregatedBindings.put(name, binding);
                }
            }
        }

        // Process map bindings (@Aggregate Map providers)
        if (mapBindings != null) {
            for (Binding<Q> binding : mapBindings) {
                Binding<Object> objBinding = (Binding<Object>) binding;
                if (isAggregateMapProvider(objBinding)) {
                    // This is an @Aggregate Map<String, T> provider, expand its entries
                    Map<String, Binding<Object>> expanded = expandAggregateMap(objBinding);
                    aggregatedBindings.putAll(expanded);
                }
            }
        }

        Map<String, Supplier<Object>> supplierMap = aggregatedBindings.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> compile(e.getValue()),
                        (a, b) -> b, // Last write wins on duplicate keys
                        LinkedHashMap::new));

        return () -> (Q) map(supplierMap, Supplier::get);
    }

    /**
     * Find an explicit (non-@Aggregate) List<T> provider
     */
    private Binding<Object> findExplicitListProvider(Set<Binding<Object>> bindings) {
        for (Binding<Object> binding : bindings) {
            if (isListProvider(binding) && !isAggregate(binding)) {
                return binding;
            }
        }
        return null;
    }

    /**
     * Find an explicit (non-@Aggregate) Map<String, T> provider
     */
    private Binding<Object> findExplicitMapProvider(Set<Binding<Object>> bindings) {
        for (Binding<Object> binding : bindings) {
            if (isMapProvider(binding) && !isAggregate(binding)) {
                return binding;
            }
        }
        return null;
    }

    /**
     * Check if a binding is for a List type
     */
    private boolean isListProvider(Binding<?> binding) {
        Key<?> originalKey = binding.getOriginalKey();
        if (originalKey == null) {
            return false;
        }
        Type type = originalKey.getType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return pt.getRawType() == List.class;
        }
        return false;
    }

    /**
     * Check if a binding is for a Map type
     */
    private boolean isMapProvider(Binding<?> binding) {
        Key<?> originalKey = binding.getOriginalKey();
        if (originalKey == null) {
            return false;
        }
        Type type = originalKey.getType();
        return isMapType(type);
    }

    /**
     * Check if a Type is a Map type
     */
    private boolean isMapType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return pt.getRawType() == Map.class;
        }
        return false;
    }

    /**
     * Check if a Type is a List type
     */
    private boolean isListType(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            return pt.getRawType() == List.class;
        }
        return false;
    }

    /**
     * Check if a binding has @Aggregate annotation
     */
    private boolean isAggregate(Binding<?> binding) {
        // Check if the binding's source method has @Aggregate
        if (binding instanceof Binding.BindingToMethod) {
            Method method = ((Binding.BindingToMethod<?>) binding).getMethod();
            return method.isAnnotationPresent(Aggregate.class);
        }
        return false;
    }

    /**
     * Check if all bindings are @Aggregate List providers
     */
    private <Q> boolean areAllAggregateListProviders(Set<Binding<Q>> bindings) {
        return bindings.stream().allMatch(binding -> isAggregateListProvider((Binding<Object>) binding));
    }

    /**
     * Check if all bindings are @Aggregate Map providers
     */
    private <Q> boolean areAllAggregateMapProviders(Set<Binding<Q>> bindings) {
        return bindings.stream().allMatch(binding -> isAggregateMapProvider((Binding<Object>) binding));
    }

    /**
     * Check if this is an @Aggregate List<T> provider
     */
    private boolean isAggregateListProvider(Binding<?> binding) {
        return isListProvider(binding) && isAggregate(binding);
    }

    /**
     * Check if this is an @Aggregate Map<String, T> provider
     */
    private boolean isAggregateMapProvider(Binding<?> binding) {
        return isMapProvider(binding) && isAggregate(binding);
    }

    /**
     * Expand an @Aggregate List<T> provider into individual bindings
     */
    @SuppressWarnings("unchecked")
    private List<Binding<Object>> expandAggregateList(Binding<Object> listBinding) {
        List<Binding<Object>> result = new ArrayList<>();
        Supplier<Object> compiled = compile(listBinding);
        List<Object> elements = (List<Object>) compiled.get();

        if (elements != null) {
            for (Object element : elements) {
                // Create instance binding for each element, inheriting priority from the original binding
                Binding<Object> elementBinding = (Binding<Object>) Binding.toInstance(element);
                elementBinding = elementBinding.prioritize(listBinding.getPriority());
                result.add(elementBinding);
            }
        }

        return result;
    }

    /**
     * Expand an @Aggregate Map<String, T> provider into individual bindings
     */
    @SuppressWarnings("unchecked")
    private Map<String, Binding<Object>> expandAggregateMap(Binding<Object> mapBinding) {
        Map<String, Binding<Object>> result = new LinkedHashMap<>();
        Supplier<Object> compiled = compile(mapBinding);
        Map<String, Object> entries = (Map<String, Object>) compiled.get();

        if (entries != null) {
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                // Create instance binding for each entry
                result.put(entry.getKey(), (Binding<Object>) Binding.toInstance(entry.getValue()));
            }
        }

        return result;
    }

    /**
     * Extract @Named qualifier value from a binding
     */
    private String extractNamedQualifier(Binding<?> binding) {
        Key<?> originalKey = binding.getOriginalKey();
        if (originalKey == null) {
            return null;
        }
        Object qualifier = originalKey.getQualifier();
        if (qualifier instanceof String str && !str.isEmpty()) {
            return str;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected <Q> Supplier<Q> compile(Binding<Q> binding) {
        Supplier<Q> compiled = binding.compile(this::getCompiledBinding);
        if (binding.getScope() != null) {
            Scope scope = scopes.entrySet().stream()
                    .filter(e -> e.getKey().isInstance(binding.getScope()))
                    .findFirst()
                    .map(Map.Entry::getValue)
                    .orElseThrow(() -> new DIException("Scope not bound for annotation "
                            + binding.getScope().annotationType()))
                    .get();
            compiled = scope.scope((Key<Q>) binding.getOriginalKey(), compiled);
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

                // Special handling for @Aggregate providers
                if (method.isAnnotationPresent(Aggregate.class)) {
                    if (isMapType(returnType)) {
                        // Register individual named bindings for each map entry
                        registerAggregateMapEntries(method, bind);
                    } else if (isListType(returnType)) {
                        // Register individual bindings for each list element
                        registerAggregateListEntries(method, bind);
                    }
                }
            }
        }
    }

    /**
     * Register individual named bindings for entries in an @Aggregate Map<String, T> provider
     */
    @SuppressWarnings("unchecked")
    private void registerAggregateMapEntries(Method method, Binding<Object> mapBinding) {
        try {
            // Get the value type from Map<String, T>
            Type returnType = method.getGenericReturnType();
            if (!(returnType instanceof ParameterizedType)) {
                return;
            }
            ParameterizedType pt = (ParameterizedType) returnType;
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length != 2 || typeArgs[0] != String.class) {
                return; // Only support Map<String, T>
            }
            Type valueType = typeArgs[1];

            // Execute the map provider to get the actual map
            Supplier<Object> compiled = compile(mapBinding);
            Map<String, Object> map = (Map<String, Object>) compiled.get();

            if (map != null) {
                // Register each map entry as a named binding
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    String name = entry.getKey();
                    Object value = entry.getValue();

                    if (name != null && !name.isEmpty() && value != null) {
                        // Create a binding for this individual service
                        Binding<Object> valueBinding = (Binding<Object>) Binding.toInstance(value);

                        // Bind it with the @Named qualifier
                        Key<Object> namedKey = (Key<Object>) Key.ofType(valueType, name);
                        bind(namedKey, valueBinding);
                    }
                }
            }
        } catch (Exception e) {
            // If we can't execute the provider at binding time, that's okay
            // The individual services will be resolved through map aggregation at runtime
        }
    }

    /**
     * Register individual bindings for elements in an @Aggregate List<T> provider
     * Only registers individual bindings if there are no existing individual bindings for that type
     */
    @SuppressWarnings("unchecked")
    private void registerAggregateListEntries(Method method, Binding<Object> listBinding) {
        try {
            // Get the element type from List<T>
            Type returnType = method.getGenericReturnType();
            if (!(returnType instanceof ParameterizedType)) {
                return;
            }
            ParameterizedType pt = (ParameterizedType) returnType;
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length != 1) {
                return; // Only support List<T>
            }
            Type elementType = typeArgs[0];
            Key<Object> elementKey = (Key<Object>) Key.ofType(elementType);

            // Only register individual bindings if there are no existing individual bindings for this type
            Set<Binding<Object>> existingBindings = getBindings(elementKey);
            if (existingBindings != null && !existingBindings.isEmpty()) {
                // There are already individual bindings for this type, don't add more
                return;
            }

            // Execute the list provider to get the actual list
            Supplier<Object> compiled = compile(listBinding);
            List<Object> list = (List<Object>) compiled.get();

            if (list != null) {
                // Register each list element as an individual binding
                for (Object element : list) {
                    if (element != null) {
                        // Create a binding for this individual service
                        Binding<Object> elementBinding = (Binding<Object>) Binding.toInstance(element);

                        // Bind it without any qualifier (so it can be injected as @Inject T)
                        bind(elementKey, elementBinding);
                    }
                }
            }
        } catch (Exception e) {
            // If we can't execute the provider at binding time, that's okay
            // The individual services will be resolved through list aggregation at runtime
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

    protected <K, V, T> Map<K, V> map(Map<K, T> map, Function<T, V> mapper) {
        return new WrappingMap<>(map, mapper);
    }

    private static class WrappingMap<K, V, T> extends AbstractMap<K, V> {

        private final Map<K, T> delegate;
        private final Function<T, V> mapper;

        WrappingMap(Map<K, T> delegate, Function<T, V> mapper) {
            this.delegate = delegate;
            this.mapper = mapper;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    Iterator<Entry<K, T>> it = delegate.entrySet().iterator();
                    return new Iterator<>() {
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

    protected <Q, T> List<Q> list(List<T> bindingList, Function<T, Q> mapper) {
        return new WrappingList<>(bindingList, mapper);
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

    private void checkCyclicDependency(Key<?> key) {
        Set<Key<?>> stack = resolutionStack.get();
        if (stack == null) {
            stack = new LinkedHashSet<>();
            resolutionStack.set(stack);
        }
        if (!stack.add(key)) {
            throw new DIException("Cyclic dependency detected: "
                    + stack.stream().map(Key::getDisplayString).collect(Collectors.joining(" -> "))
                    + " -> "
                    + key.getDisplayString());
        }
    }

    private void removeFromResolutionStack(Key<?> key) {
        Set<Key<?>> stack = resolutionStack.get();
        if (stack != null) {
            stack.remove(key);
            if (stack.isEmpty()) {
                resolutionStack.remove();
            }
        }
    }

    private static class SingletonScope implements Scope {
        Map<Key<?>, java.util.function.Supplier<?>> cache = new ConcurrentHashMap<>();

        @Nonnull
        @SuppressWarnings("unchecked")
        @Override
        public <T> java.util.function.Supplier<T> scope(
                @Nonnull Key<T> key, @Nonnull java.util.function.Supplier<T> unscoped) {
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

    /**
     * Release all internal state so this Injector can be GC'd
     * (and so that subsequent tests start from a clean slate).
     * @since 4.1
     */
    public void dispose() {
        // First, clear any singleton‐scope caches
        scopes.values().stream()
                .map(Supplier::get)
                .filter(scope -> scope instanceof SingletonScope)
                .map(scope -> (SingletonScope) scope)
                .forEach(singleton -> singleton.cache.clear());

        // Now clear everything else
        bindings.clear();
        scopes.clear();
        loadedUrls.clear();
        resolutionStack.remove();
    }
}
