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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.apache.maven.api.di.MojoExecutionScoped;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.Binding;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.di.impl.Dependency;
import org.apache.maven.di.impl.InjectorImpl;
import org.apache.maven.execution.scope.internal.MojoExecutionScope;
import org.apache.maven.session.scope.internal.SessionScope;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

@Named
public class SisuDiBridgeModule extends AbstractModule {

    InjectorImpl injector;
    final Set<String> loaded = new HashSet<>();

    @Override
    protected void configure() {
        Provider<PlexusContainer> containerProvider = getProvider(PlexusContainer.class);

        injector = new InjectorImpl() {
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
                    Set<Binding<Object>> res2 = getBindings(key.getTypeParameter(0));
                    Set<Binding<Object>> res3 = res2 != null ? new HashSet<>(res2) : new HashSet<>();
                    try {
                        List<Object> l = containerProvider
                                .get()
                                .lookupList(key.getTypeParameter(0).getRawType());
                        l.forEach(o -> res3.add(new Binding.BindingToInstance<>(o)));
                    } catch (Throwable e) {
                        // ignore
                        e.printStackTrace();
                    }
                    List<Supplier<Object>> list =
                            res3.stream().map(this::compile).collect(Collectors.toList());
                    //noinspection unchecked
                    return () -> (Q) list(list);
                }
                if (key.getRawType() == Map.class) {
                    Key<?> k = key.getTypeParameter(0);
                    Key<Object> v = key.getTypeParameter(1);
                    if (k.getRawType() == String.class) {
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
                        return () -> (Q) map(map);
                    }
                }
                try {
                    Q t = containerProvider.get().lookup(key.getRawType());
                    return compile(new Binding.BindingToInstance<>(t));
                } catch (Throwable e) {
                    // ignore
                    e.printStackTrace();
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
        };
        injector.bindScope(SessionScoped.class, () -> {
            try {
                return containerProvider.get().lookup(SessionScope.class);
            } catch (ComponentLookupException e) {
                throw new RuntimeException(e);
            }
        });
        injector.bindScope(MojoExecutionScoped.class, () -> {
            try {
                return containerProvider.get().lookup(MojoExecutionScope.class);
            } catch (ComponentLookupException e) {
                throw new RuntimeException(e);
            }
        });
        injector.bindInstance(Injector.class, injector);
        bind(Injector.class).toInstance(injector);
        bind(SisuDiBridgeModule.class).toInstance(this);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        loadFromClassLoader(classLoader);
        injector.getBindings().keySet().stream()
                .filter(k -> k.getQualifier() != null)
                .sorted(Comparator.comparing(k -> k.getRawType().getName()))
                .distinct()
                .forEach(key -> {
                    var mappedKey = (com.google.inject.Key<Object>) com.google.inject.Key.get(key.getType());
                    if (key.getQualifier() instanceof String s && !s.isEmpty()) {
                        mappedKey = mappedKey.withAnnotation(Names.named(s));
                    } else if (key.getQualifier() instanceof Annotation a) {
                        mappedKey = mappedKey.withAnnotation(a);
                    }
                    bind(mappedKey).toProvider(() -> injector.getInstance(key));
                });
    }

    public void loadFromClassLoader(ClassLoader classLoader) {
        try {
            for (Iterator<URL> it = classLoader
                            .getResources("META-INF/maven/org.apache.maven.api.di.Inject")
                            .asIterator();
                    it.hasNext(); ) {
                URL url = it.next();
                if (loaded.add(url.toExternalForm())) {
                    List<String> lines;
                    try (InputStream is = url.openStream();
                            BufferedReader reader =
                                    new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        lines = reader.lines()
                                .map(String::trim)
                                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                                .toList();
                    }
                    for (String className : lines) {
                        try {
                            Class<?> clazz = classLoader.loadClass(className);
                            injector.bindImplicit(clazz);
                        } catch (ClassNotFoundException e) {
                            // ignore
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MavenException(e);
        }
    }
}
