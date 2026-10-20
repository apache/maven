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
package org.apache.maven.impl;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.ExtensibleEnum;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ExtensibleEnumRegistry;
import org.apache.maven.api.services.LanguageRegistry;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.PathScopeRegistry;
import org.apache.maven.api.services.ProjectScopeRegistry;
import org.apache.maven.api.spi.ExtensibleEnumProvider;
import org.apache.maven.api.spi.LanguageProvider;
import org.apache.maven.api.spi.PathScopeProvider;
import org.apache.maven.api.spi.ProjectScopeProvider;

import static java.util.Objects.requireNonNull;

public class ExtensibleEnumRegistries {

    private static final ClassValue<Optional<Method>> GET_IMPORT_REALMS = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("getImportRealms"));
            } catch (NoSuchMethodException | SecurityException e) {
                return Optional.empty();
            }
        }
    };

    /**
     * Checks whether a provider is visible from the given classloader.
     * Providers from project extensions are loaded by extension-specific classloaders
     * (realms) that are only imported into the project realm that declared the extension.
     * This prevents extension-scoped providers from leaking across reactor modules
     * during parallel builds.
     *
     * @param provider the SPI provider to check
     * @param classLoader the classloader to check visibility from (typically the TCCL)
     * @return {@code true} if the provider is visible, {@code false} otherwise
     */
    public static boolean isVisibleFrom(Object provider, ClassLoader classLoader) {
        if (classLoader == null) {
            return true;
        }
        Class<?> providerClass = provider.getClass();
        ClassLoader providerCl = providerClass.getClassLoader();
        if (providerCl == null) {
            return true;
        }
        try {
            if (classLoader.loadClass(providerClass.getName()) == providerClass) {
                return true;
            }
        } catch (ClassNotFoundException | LinkageError e) {
            // Not loadable by name (hidden class, realm-ID import, or broken link) — check further
        }
        for (ClassLoader cl = classLoader; cl != null; cl = cl.getParent()) {
            if (cl == providerCl) {
                return true;
            }
        }
        return isImportedRealm(classLoader, providerCl);
    }

    private static boolean isImportedRealm(ClassLoader classLoader, ClassLoader providerCl) {
        return GET_IMPORT_REALMS
                .get(classLoader.getClass())
                .map(method -> {
                    try {
                        Collection<?> importedRealms = (Collection<?>) method.invoke(classLoader);
                        return importedRealms.contains(providerCl);
                    } catch (ReflectiveOperationException | ClassCastException e) {
                        return false;
                    }
                })
                .orElse(false);
    }

    @Named
    @SessionScoped
    public static class DefaultPathScopeRegistry extends DefaultExtensibleEnumRegistry<PathScope, PathScopeProvider>
            implements PathScopeRegistry {

        @Inject
        public DefaultPathScopeRegistry(List<PathScopeProvider> providers) {
            super(
                    providers,
                    PathScope.MAIN_COMPILE,
                    PathScope.MAIN_RUNTIME,
                    PathScope.TEST_COMPILE,
                    PathScope.TEST_RUNTIME);
        }
    }

    @Named
    @SessionScoped
    public static class DefaultProjectScopeRegistry
            extends DefaultExtensibleEnumRegistry<ProjectScope, ProjectScopeProvider> implements ProjectScopeRegistry {

        @Inject
        public DefaultProjectScopeRegistry(List<ProjectScopeProvider> providers) {
            super(providers, ProjectScope.MAIN, ProjectScope.TEST);
        }
    }

    @Named
    @Singleton
    public static class DefaultLanguageRegistry extends DefaultExtensibleEnumRegistry<Language, LanguageProvider>
            implements LanguageRegistry {

        @Inject
        public DefaultLanguageRegistry(List<LanguageProvider> providers) {
            super(providers, Language.NONE, Language.RESOURCES, Language.SCRIPT, Language.JAVA_FAMILY);
        }
    }

    public abstract static class DefaultExtensibleEnumRegistry<
                    T extends ExtensibleEnum, P extends ExtensibleEnumProvider<T>>
            implements ExtensibleEnumRegistry<T> {

        protected final Map<String, T> values;
        private final Lookup lookup;
        private final Class<P> providerType;

        @SafeVarargs
        public DefaultExtensibleEnumRegistry(List<P> providers, T... builtinValues) {
            this(null, null, providers, builtinValues);
        }

        @SafeVarargs
        public DefaultExtensibleEnumRegistry(
                Lookup lookup, Class<P> providerType, List<P> providers, T... builtinValues) {
            this.lookup = lookup;
            this.providerType = providerType;
            values = Stream.concat(Stream.of(builtinValues), providers.stream().flatMap(p -> p.provides().stream()))
                    .collect(Collectors.toUnmodifiableMap(t -> t.id().toLowerCase(Locale.ROOT), t -> t));
        }

        /**
         * Looks up an enum value by id, checking dynamic providers before the static registry.
         *
         * <p><b>Cardinality assumption:</b> when dynamic lookup is enabled, this method iterates
         * all registered providers and all their values on every call — O(n·m) where {@code n} is
         * the number of providers and {@code m} is the average number of values per provider.
         * Both are expected to be O(tens) in practice (a handful of SPI providers, each contributing
         * a small set of values). This method is <em>not</em> designed for cardinalities in the
         * thousands; callers that need to look up the same value repeatedly in a tight loop should
         * cache the result themselves.</p>
         *
         * <p><b>Classloader visibility:</b> providers contributed by project extensions are only
         * visible when the current thread's context classloader can load the provider's class.
         * This prevents extension-scoped providers from leaking across reactor modules during
         * parallel builds.</p>
         *
         * @param id the enum value id, must not be {@code null}
         * @return the matching value, or {@code Optional.empty()} if not found
         */
        @Override
        public Optional<T> lookup(String id) {
            String key = requireNonNull(id, "id").toLowerCase(Locale.ROOT);
            if (lookup != null && providerType != null) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                Optional<T> result = lookup.lookupList(providerType).stream()
                        .filter(p -> isVisibleFrom(p, tccl))
                        .flatMap(p -> p.provides().stream())
                        .filter(t -> key.equals(t.id().toLowerCase(Locale.ROOT)))
                        .findFirst();
                if (result.isPresent()) {
                    return result;
                }
            }
            return Optional.ofNullable(values.get(key));
        }
    }
}
