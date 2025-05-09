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
package org.apache.maven.internal.impl;

import javax.inject.Named;
import javax.inject.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.name.Names;
import com.google.inject.spi.ProviderInstanceBinding;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.Scope;
import org.apache.maven.di.impl.Binding;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.di.impl.Dependency;
import org.apache.maven.di.impl.InjectorImpl;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;

@Named
public class SisuDiBridgeModule extends AbstractModule {

    protected final boolean discover;
    protected InjectorImpl injector;

    public SisuDiBridgeModule() {
        this(true);
    }

    public SisuDiBridgeModule(boolean discover) {
        this.discover = discover;
    }

    @Override
    protected void configure() {
        Provider<PlexusContainer> containerProvider = getProvider(PlexusContainer.class);
        Provider<BeanLocator> beanLocatorProvider = getProvider(BeanLocator.class);
        injector = new BridgeInjectorImpl(beanLocatorProvider, binder());
        bindScope(injector, containerProvider, SessionScoped.class, SessionScope.class);
        bindScope(injector, containerProvider, MojoExecutionScoped.class, MojoExecutionScope.class);
        injector.bindInstance(Injector.class, injector);
        bind(Injector.class).toInstance(injector);
        bind(SisuDiBridgeModule.class).toInstance(this);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        if (discover) {
            injector.discover(classLoader);
        }
    }

    private void bindScope(
            InjectorImpl injector,
            Provider<PlexusContainer> containerProvider,
            Class<? extends Annotation> sa,
            Class<? extends Scope> ss) {
        injector.bindScope(sa, () -> {
            try {
                return containerProvider.get().lookup(ss);
            } catch (ComponentLookupException e) {
                throw new RuntimeException(e);
            }
        });
    }

    static class BridgeInjectorImpl extends InjectorImpl {
        final Provider<BeanLocator> locator;
        final Binder binder;

        BridgeInjectorImpl(Provider<BeanLocator> locator, Binder binder) {
            this.locator = locator;
            this.binder = binder;
        }

        @Override
        protected <U> Injector bind(Key<U> key, Binding<U> binding) {
            super.bind(key, binding);
            if (key.getQualifier() != null) {
                com.google.inject.Key<U> k = toGuiceKey(key);
                this.binder.bind(k).toProvider(new BridgeProvider<>(binding));
            }
            return this;
        }

        @SuppressWarnings("unchecked")
        private static <U> com.google.inject.Key<U> toGuiceKey(Key<U> key) {
            if (key.getQualifier() instanceof String s) {
                return (com.google.inject.Key<U>) com.google.inject.Key.get(key.getType(), Names.named(s));
            } else if (key.getQualifier() instanceof Annotation a) {
                return (com.google.inject.Key<U>) com.google.inject.Key.get(key.getType(), a);
            } else {
                return (com.google.inject.Key<U>) com.google.inject.Key.get(key.getType());
            }
        }

        static class BindingToBeanEntry<T> extends Binding<T> {
            private BeanEntry<Annotation, T> beanEntry;

            BindingToBeanEntry(Key<T> elementType) {
                super(elementType, Set.of());
            }

            public BindingToBeanEntry<T> toBeanEntry(BeanEntry<Annotation, T> beanEntry) {
                this.beanEntry = beanEntry;
                return this;
            }

            @Override
            public Supplier<T> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                return beanEntry.getProvider()::get;
            }
        }

        class BridgeProvider<T> implements Provider<T> {
            final Binding<T> binding;

            BridgeProvider(Binding<T> binding) {
                this.binding = binding;
            }

            @Override
            public T get() {
                return compile(binding).get();
            }
        }

        @Override
        public <Q> Supplier<Q> getCompiledBinding(Dependency<Q> dep) {
            Key<Q> key = dep.key();
            Class<Q> rawType = key.getRawType();
            if (rawType == List.class) {
                return getListSupplier(key);
            } else if (rawType == Map.class) {
                return getMapSupplier(key);
            } else {
                return getBeanSupplier(dep, key);
            }
        }

        private <Q> Supplier<Q> getBeanSupplier(Dependency<Q> dep, Key<Q> key) {
            List<Binding<?>> list = new ArrayList<>();
            // Add DI bindings
            list.addAll(getBindings().getOrDefault(key, Set.of()));
            // Add Plexus bindings
            for (var bean : locator.get().locate(toGuiceKey(key))) {
                if (isPlexusBean(bean)) {
                    list.add(new BindingToBeanEntry<>(key).toBeanEntry(bean).prioritize(bean.getRank()));
                }
            }
            if (!list.isEmpty()) {
                list.sort(getBindingComparator());
                //noinspection unchecked
                return () -> (Q) getInstance(list.iterator().next());
            } else if (dep.optional()) {
                return () -> null;
            } else {
                throw new DIException("No binding to construct an instance for key "
                        + key.getDisplayString() + ".  Existing bindings:\n"
                        + getBoundKeys().stream()
                                .map(Key::toString)
                                .map(String::trim)
                                .sorted()
                                .distinct()
                                .collect(Collectors.joining("\n - ", " - ", "")));
            }
        }

        private <Q> Supplier<Q> getListSupplier(Key<Q> key) {
            Key<Object> elementType = key.getTypeParameter(0);
            return () -> {
                List<Binding<?>> list = new ArrayList<>();
                // Add DI bindings
                list.addAll(getBindings().getOrDefault(elementType, Set.of()));
                // Add Plexus bindings
                for (var bean : locator.get().locate(toGuiceKey(elementType))) {
                    if (isPlexusBean(bean)) {
                        list.add(new BindingToBeanEntry<>(elementType).toBeanEntry(bean));
                    }
                }
                //noinspection unchecked
                return (Q) list(list.stream().sorted(getBindingComparator()).toList(), this::getInstance);
            };
        }

        private <Q> Supplier<Q> getMapSupplier(Key<Q> key) {
            Key<?> keyType = key.getTypeParameter(0);
            Key<Object> valueType = key.getTypeParameter(1);
            if (keyType.getRawType() != String.class) {
                throw new DIException("Only String keys are supported for maps: " + key);
            }
            return () -> {
                var comparator = getBindingComparator();
                Map<String, Binding<?>> map = new HashMap<>();
                for (Binding<?> b : getBindings().getOrDefault(valueType, Set.of())) {
                    String name =
                            b.getOriginalKey() != null && b.getOriginalKey().getQualifier() instanceof String s
                                    ? s
                                    : "";
                    map.compute(name, (n, ob) -> ob == null || comparator.compare(ob, b) < 0 ? b : ob);
                }
                for (var bean : locator.get().locate(toGuiceKey(valueType))) {
                    if (isPlexusBean(bean)) {
                        Binding<?> b = new BindingToBeanEntry<>(valueType)
                                .toBeanEntry(bean)
                                .prioritize(bean.getRank());
                        String name = bean.getKey() instanceof com.google.inject.name.Named n ? n.value() : "";
                        map.compute(name, (n, ob) -> ob == null || ob.getPriority() < b.getPriority() ? b : ob);
                    }
                }
                //noinspection unchecked
                return (Q) map(map, this::getInstance);
            };
        }

        private <Q> Q getInstance(Binding<Q> binding) {
            return compile(binding).get();
        }

        private static Comparator<Binding<?>> getBindingComparator() {
            Comparator<Binding<?>> comparing = Comparator.comparing(Binding::getPriority);
            return comparing.reversed();
        }

        private <T> boolean isPlexusBean(BeanEntry<Annotation, T> entry) {
            try {
                if ("org.eclipse.sisu.inject.LazyBeanEntry"
                        .equals(entry.getClass().getName())) {
                    Field f = entry.getClass().getDeclaredField("binding");
                    f.setAccessible(true);
                    Object b = f.get(entry);
                    return !(b instanceof ProviderInstanceBinding<?> pib)
                            || !(pib.getUserSuppliedProvider() instanceof BridgeProvider<?>);
                }
            } catch (Exception e) {
                // ignore
            }
            return true;
        }
    }
}
