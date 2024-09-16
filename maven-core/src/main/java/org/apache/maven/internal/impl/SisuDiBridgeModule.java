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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
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
import org.eclipse.sisu.plexus.PlexusBean;
import org.eclipse.sisu.plexus.PlexusBeanLocator;

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
        injector = new BridgeInjectorImpl(containerProvider, binder());
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
        final Provider<PlexusContainer> containerProvider;
        final Binder binder;

        BridgeInjectorImpl(Provider<PlexusContainer> containerProvider, Binder binder) {
            this.containerProvider = containerProvider;
            this.binder = binder;
        }

        @Override
        protected <U> Injector bind(Key<U> key, Binding<U> binding) {
            super.bind(key, binding);
            if (key.getQualifier() != null) {
                com.google.inject.Key<U> k = toGuiceKey(key);
                // System.out.println("Bind di -> guice: " + k);
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
            Set<Binding<Q>> res = getBindings(key);
            if (res != null && !res.isEmpty()) {
                List<Binding<Q>> bindingList = new ArrayList<>(res);
                Comparator<Binding<Q>> comparing = Comparator.comparing(Binding::getPriority);
                bindingList.sort(comparing.reversed());
                Binding<Q> binding = bindingList.get(0);
                return compile(binding);
            }
            if (key.getRawType() == List.class) {
                return () -> {
                    Key<Object> elementType = key.getTypeParameter(0);
                    Set<Binding<Object>> res2 = getBindings(elementType);
                    Set<Binding<Object>> res3 = res2 != null ? new HashSet<>(res2) : new HashSet<>();
                    try {
                        PlexusContainer container = containerProvider.get();
                        PlexusBeanLocator locator = container.lookup(PlexusBeanLocator.class);
                        for (PlexusBean<Object> bean : locator.locate(TypeLiteral.get(elementType.getRawType()))) {
                            if (!isDiBean(bean)) {
                                res3.add(new Binding<>(elementType, Set.of()) {
                                    @Override
                                    public Supplier<Object> compile(Function<Dependency<?>, Supplier<?>> compiler) {
                                        return bean::getValue;
                                    }
                                });
                            }
                        }
                    } catch (Throwable e) {
                        // ignore
                    }
                    List<Supplier<Object>> list =
                            res3.stream().map(this::compile).collect(Collectors.toList());
                    //noinspection unchecked
                    return (Q) list(list);
                };
            }
            if (key.getRawType() == Map.class) {
                Key<?> k = key.getTypeParameter(0);
                Key<Object> v = key.getTypeParameter(1);
                if (k.getRawType() == String.class) {
                    return () -> {
                        Set<Binding<Object>> res2 = getBindings(v);
                        Set<Binding<Object>> res3 = res2 != null ? new HashSet<>(res2) : new HashSet<>();
                        Map<String, Supplier<Object>> map = res3.stream()
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
                        return (Q) map(map);
                    };
                }
            }
            try {
                Q t = containerProvider.get().lookup(key.getRawType());
                return compile(new Binding.BindingToInstance<>(t));
            } catch (Throwable e) {
                // ignore
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

        private boolean isDiBean(PlexusBean<Object> bean) throws IllegalAccessException {
            try {
                if ("org.eclipse.sisu.plexus.LazyPlexusBean"
                        .equals(bean.getClass().getName())) {
                    Field f = bean.getClass().getDeclaredField("bean");
                    f.setAccessible(true);
                    Object entry = f.get(bean);
                    if ("org.eclipse.sisu.inject.LazyBeanEntry"
                            .equals(entry.getClass().getName())) {
                        f = entry.getClass().getDeclaredField("binding");
                        f.setAccessible(true);
                        Object b = f.get(entry);
                        if (b instanceof ProviderInstanceBinding<?> pib
                                && pib.getUserSuppliedProvider() instanceof BridgeProvider<?>) {
                            return true;
                        }
                    }
                }
            } catch (NoSuchFieldException e) {
                // ignore
            }
            return false;
        }
    }
}
