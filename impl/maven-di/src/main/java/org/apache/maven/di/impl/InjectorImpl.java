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
import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Qualifier;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.di.Typed;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;

import static org.apache.maven.di.impl.Binding.getPriorityComparator;

public class InjectorImpl implements Injector {

    private static final String DEBUG_PROPERTY = "maven.di.debug";
    private static final long NO_CAPTURED_REVISION = -1;
    private static final String AMBIGUOUS = "ambiguous";

    private final Map<Key<?>, Set<Binding<?>>> bindings = new HashMap<>();
    private final Map<Class<? extends Annotation>, Supplier<Scope>> scopes = new HashMap<>();
    private final Set<String> loadedUrls = new HashSet<>();
    private final ThreadLocal<Set<Key<?>>> resolutionStack = new ThreadLocal<>();

    // Registration, as with the binding map itself, must not race with resolution.
    private volatile long registryRevision;
    private final Object diagnosticLock = new Object();
    private volatile DiagnosticState diagnosticState;
    private Deque<DiagnosticWork> diagnosticQueue;
    private Thread diagnosticWriter;
    private int registrationDepth;

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
        debugBindings();
        ReflectionUtils.generateInjectingInitializer(Key.of((Class<T>) instance.getClass()))
                .compile(this::getCompiledBinding)
                .accept(instance);
    }

    @Nonnull
    @Override
    public Injector discover(@Nonnull ClassLoader classLoader) {
        registrationDepth++;
        boolean complete = false;
        try {
            Enumeration<URL> enumeration = classLoader.getResources("META-INF/maven/org.apache.maven.api.di.Inject");
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (loadedUrls.add(url.toExternalForm())) {
                    try (InputStream is = url.openStream();
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
                        for (String line : reader.lines()
                                .map(String::trim)
                                .filter(l -> !l.isEmpty())
                                .filter(l -> !l.startsWith("#"))
                                .toList()) {
                            Class<?> clazz = classLoader.loadClass(line);
                            bindImplicit(clazz);
                        }
                    }
                }
            }
            complete = true;
        } catch (Exception e) {
            throw new DIException("Error while discovering DI classes from classLoader", e);
        } finally {
            if (--registrationDepth == 0) {
                debugBindings("discovery", !complete);
            }
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
        Supplier<Scope> previous = scopes.put(scopeAnnotation, scope);
        registryRevision++;
        if (previous != null) {
            debugBindings("registration", true);
            throw new DIException(
                    "Cannot rebind scope annotation class to a different implementation: " + scopeAnnotation);
        }
        return this;
    }

    @Nonnull
    @Override
    public <U> Injector bindInstance(@Nonnull Class<U> clazz, @Nonnull U instance) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<U> binding = Binding.toInstance(instance).withKey(key);
        return doBind(key, binding);
    }

    @Override
    public <U> Injector bindSupplier(@Nonnull Class<U> clazz, @Nonnull Supplier<U> supplier) {
        Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
        Binding<U> binding = Binding.toSupplier(supplier).withKey(key);
        return doBind(key, binding);
    }

    @Nonnull
    @Override
    public Injector bindImplicit(@Nonnull Class<?> clazz) {
        registrationDepth++;
        boolean complete = false;
        try {
            Key<?> key = Key.of(clazz, ReflectionUtils.qualifierOf(clazz));
            if (clazz.isInterface()) {
                bindEmptyKey(key);
                if (key.getQualifier() != null) {
                    bindEmptyKey(Key.ofType(clazz));
                }
            } else if (!Modifier.isAbstract(clazz.getModifiers())) {
                Binding<?> binding = ReflectionUtils.generateImplicitBinding(key);
                doBind(key, binding);
            }
            complete = true;
            return this;
        } finally {
            if (--registrationDepth == 0 && !complete) {
                debugBindings("registration", true);
            }
        }
    }

    private void bindEmptyKey(Key<?> key) {
        if (!bindings.containsKey(key)) {
            bindings.put(key, new HashSet<>());
            registryRevision++;
        }
    }

    private final LinkedHashSet<Key<?>> current = new LinkedHashSet<>();

    private Injector doBind(Key<?> key, Binding<?> binding) {
        registrationDepth++;
        boolean complete = false;
        try {
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
                complete = true;
                return this;
            } finally {
                current.remove(key);
            }
        } finally {
            if (--registrationDepth == 0 && !complete) {
                debugBindings("registration", true);
            }
        }
    }

    protected <U> Injector bind(Key<U> key, Binding<U> b) {
        Set<Binding<?>> bindingSet = bindings.computeIfAbsent(key, $ -> new HashSet<>());
        if (bindingSet.stream().noneMatch(existing -> isSameBinding(existing, b))) {
            bindingSet.add(b);
            registryRevision++;
        }
        return this;
    }

    private static boolean isSameBinding(Binding<?> first, Binding<?> second) {
        if (!Objects.equals(first.getOriginalKey(), second.getOriginalKey())
                || first.getPriority() != second.getPriority()
                || !Objects.equals(first.getScope(), second.getScope())) {
            return false;
        }
        Object firstIdentity = first.getRegistrationIdentity();
        Object secondIdentity = second.getRegistrationIdentity();
        // Both sides have explicit identities: use equals-based comparison (e.g. two initialized
        // bindings wrapping the same source class). If only one side has an explicit identity,
        // fall through to instanceof/reference checks — they represent distinct providers.
        if (firstIdentity != first && secondIdentity != second) {
            return firstIdentity.equals(secondIdentity);
        }
        if (first instanceof Binding.BindingToInstance<?> a && second instanceof Binding.BindingToInstance<?> b) {
            return a.instance == b.instance;
        }
        if (first instanceof Binding.BindingToSupplier<?> a && second instanceof Binding.BindingToSupplier<?> b) {
            return a.supplier == b.supplier;
        }
        return first == second;
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
        debugBindings();
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
            List<Binding<Q>> bindingList = new ArrayList<>(res);
            Comparator<Binding<?>> comparator = key.getQualifier() == null
                    ? getPriorityComparator().thenComparing(b -> !isDefaultBinding(b))
                    : getPriorityComparator();
            bindingList.sort(comparator);
            Binding<Q> binding = bindingList.get(0);
            if (bindingList.size() > 1 && comparator.compare(binding, bindingList.get(1)) == 0) {
                throw new DIException("Ambiguous bindings for key " + key.getDisplayString() + ":\n"
                        + bindingList.stream()
                                .filter(b -> comparator.compare(binding, b) == 0)
                                .map(b -> b + " (priority=" + b.getPriority() + ")")
                                .sorted()
                                .collect(Collectors.joining("\n - ", " - ", ""))
                        + "\nUse a qualifier or a distinct @Priority to select a single binding.");
            }
            return compile(binding);
        }
        if (key.getRawType() == List.class) {
            Set<Binding<Object>> res2 = getBindings(key.getTypeParameter(0));
            if (res2 != null) {
                // Sort bindings by priority (highest first) for deterministic ordering
                List<Binding<Object>> sortedBindings = new ArrayList<>(res2);
                sortedBindings.sort(getPriorityComparator());

                List<Supplier<Object>> list =
                        sortedBindings.stream().map(this::compile).collect(Collectors.toList());
                //noinspection unchecked
                return () -> (Q) list(list, Supplier::get);
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
                return () -> (Q) map(map, Supplier::get);
            }
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

    private static boolean isDefaultBinding(Binding<?> binding) {
        Key<?> key = binding.getOriginalKey();
        Object qualifier = key != null ? key.getQualifier() : null;
        return qualifier == null || "".equals(qualifier);
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
        Map<Key<?>, Supplier<?>> cache = new ConcurrentHashMap<>();

        @Nonnull
        @SuppressWarnings("unchecked")
        @Override
        public <T> Supplier<T> scope(@Nonnull Key<T> key, @Nonnull Supplier<T> unscoped) {
            return (Supplier<T>) cache.computeIfAbsent(key, k -> new Supplier<T>() {
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
     * Captures a human-readable snapshot of the current native registry for Maven internals,
     * including direct binding map and metadata changes, without resolving components.
     * Registration must not run concurrently with this method.
     */
    public String dumpBindings() {
        return BindingSnapshot.capture(bindings, scopes, new DiagnosticIdentity(), false, registryRevision)
                .render(this, "explicit", false, null, getBindingStyler());
    }

    /** Diagnostic checkpoint for native and bridge resolution entry points. */
    protected final void debugBindings() {
        debugBindings("lookup", false);
    }

    private void debugBindings(String reason, boolean partial) {
        DiagnosticMode mode = diagnosticMode();
        if (mode == DiagnosticMode.OFF) {
            if (diagnosticState != null) {
                synchronized (diagnosticLock) {
                    diagnosticState = null;
                }
            }
            return;
        }
        PrintStream output = System.out;
        DiagnosticState state = diagnosticState;
        if (!partial
                && state != null
                && state.mode == mode
                && state.capturedRevision == registryRevision
                && state.output.get() == output) {
            return;
        }
        writeBindings(output, reason, partial, mode);
    }

    private static DiagnosticMode diagnosticMode() {
        try {
            String property = System.getProperty(DEBUG_PROPERTY);
            if (property == null) {
                return DiagnosticMode.OFF;
            }
            if (property.isEmpty() || Boolean.parseBoolean(property)) {
                return DiagnosticMode.ALL;
            }
            return AMBIGUOUS.equalsIgnoreCase(property) ? DiagnosticMode.AMBIGUOUS : DiagnosticMode.OFF;
        } catch (RuntimeException e) {
            return DiagnosticMode.OFF;
        }
    }

    private void writeBindings(PrintStream output, String reason, boolean partial, DiagnosticMode mode) {
        DiagnosticWork work;
        synchronized (diagnosticLock) {
            DiagnosticState state = diagnosticState;
            if (state == null || state.mode != mode || state.output.get() != output) {
                state = new DiagnosticState(mode, output);
                diagnosticState = state;
            }
            long revision = registryRevision;
            if (!partial && state.capturedRevision == revision) {
                return;
            }
            try {
                BindingSnapshot snapshot = BindingSnapshot.capture(
                        bindings, scopes, state.identities, mode == DiagnosticMode.AMBIGUOUS, revision);
                work = new DiagnosticWork(state, snapshot, output, reason, partial);
                state.capturedRevision = revision;
                if (diagnosticQueue == null) {
                    diagnosticQueue = new ArrayDeque<>();
                }
                diagnosticQueue.addLast(work);
            } catch (RuntimeException e) {
                // Diagnostics must not replace a registration or resolution failure. Retry on the next checkpoint.
                return;
            }
            if (diagnosticWriter != null) {
                // A callback on the writer cannot wait for itself; its captured state is drained after this block.
                if (diagnosticWriter == Thread.currentThread()) {
                    return;
                }
                boolean interrupted = false;
                while (!work.done) {
                    try {
                        diagnosticLock.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            diagnosticWriter = Thread.currentThread();
        }
        drainDiagnostics();
    }

    private void drainDiagnostics() {
        try {
            while (true) {
                DiagnosticWork work;
                BindingSnapshot previous;
                synchronized (diagnosticLock) {
                    work = diagnosticQueue.pollFirst();
                    if (work == null) {
                        diagnosticWriter = null;
                        return;
                    }
                    if (diagnosticState != work.state) {
                        work.done = true;
                        diagnosticLock.notifyAll();
                        continue;
                    }
                    previous = work.state.emittedSnapshot;
                }
                try {
                    String dump = work.snapshot.render(this, work.reason, work.partial, previous, getBindingStyler());
                    if (dump != null) {
                        writeDump(work.output, dump);
                        synchronized (diagnosticLock) {
                            if (diagnosticState == work.state) {
                                work.state.emittedSnapshot = work.snapshot;
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    synchronized (diagnosticLock) {
                        work.state.emittedSnapshot = null;
                        work.state.capturedRevision = NO_CAPTURED_REVISION;
                    }
                } finally {
                    synchronized (diagnosticLock) {
                        work.done = true;
                        diagnosticLock.notifyAll();
                    }
                }
            }
        } finally {
            synchronized (diagnosticLock) {
                if (diagnosticWriter == Thread.currentThread()) {
                    diagnosticWriter = null;
                    diagnosticState = null;
                    diagnosticQueue.forEach(work -> work.done = true);
                    diagnosticQueue.clear();
                    diagnosticLock.notifyAll();
                }
            }
        }
    }

    /** Supplies diagnostic styling without resolving components from this injector. */
    protected BindingStyler getBindingStyler() {
        return BindingStyler.NONE;
    }

    /** Writes a complete block; a separate hook lets tests exercise slow and failing output. */
    void writeDump(PrintStream output, String dump) {
        output.println(dump);
        if (output.checkError()) {
            throw new IllegalStateException("Unable to write Maven DI diagnostics");
        }
    }

    private enum DiagnosticMode {
        OFF,
        ALL,
        AMBIGUOUS
    }

    private static final class DiagnosticWork {
        final DiagnosticState state;
        final BindingSnapshot snapshot;
        final PrintStream output;
        final String reason;
        final boolean partial;
        boolean done;

        DiagnosticWork(
                DiagnosticState state, BindingSnapshot snapshot, PrintStream output, String reason, boolean partial) {
            this.state = state;
            this.snapshot = snapshot;
            this.output = output;
            this.reason = reason;
            this.partial = partial;
        }
    }

    private static final class DiagnosticState {
        final DiagnosticIdentity identities = new DiagnosticIdentity();
        final DiagnosticMode mode;
        volatile long capturedRevision = NO_CAPTURED_REVISION;
        // A reused injector must not retain a previous embedded invocation's output stream.
        final WeakReference<PrintStream> output;
        BindingSnapshot emittedSnapshot;

        DiagnosticState(DiagnosticMode mode, PrintStream output) {
            this.mode = mode;
            this.output = new WeakReference<>(output);
        }
    }

    /**
     * Release all internal state so this Injector can be GC’d
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
        synchronized (diagnosticLock) {
            diagnosticState = null;
        }
        registryRevision++;
    }
}
