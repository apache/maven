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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.Dialect;
import org.apache.maven.api.ExtensibleEnum;
import org.apache.maven.api.Language;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.DialectRegistry;
import org.apache.maven.api.services.ExtensibleEnumRegistry;
import org.apache.maven.api.services.LanguageRegistry;
import org.apache.maven.api.services.PathScopeRegistry;
import org.apache.maven.api.services.ProjectScopeRegistry;
import org.apache.maven.api.spi.DialectProvider;
import org.apache.maven.api.spi.ExtensibleEnumProvider;
import org.apache.maven.api.spi.LanguageProvider;
import org.apache.maven.api.spi.ModelDialectProvider;
import org.apache.maven.api.spi.PathScopeProvider;
import org.apache.maven.api.spi.ProjectScopeProvider;

import static java.util.Objects.requireNonNull;

public class ExtensibleEnumRegistries {

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

    @Named
    @SessionScoped
    public static class DefaultDialectRegistry extends DefaultExtensibleEnumRegistry<Dialect, DialectProvider>
            implements DialectRegistry {

        @Inject
        public DefaultDialectRegistry(
                List<DialectProvider> providers, List<ModelDialectProvider> modelDialectProviders) {
            // Collect all dialect ids already covered by DialectProvider(s) and the built-in XML.
            // Only seed dialects from ModelDialectProvider that are not already registered,
            // so that extensions which implement only ModelDialectProvider (without a separate
            // DialectProvider) still have their dialect appear in the DialectRegistry.
            super(providers, buildBuiltins(providers, modelDialectProviders));
        }

        private static Dialect[] buildBuiltins(
                List<DialectProvider> providers, List<ModelDialectProvider> modelDialectProviders) {
            // Ids already covered by built-in XML and DialectProvider contributions
            Set<String> coveredIds = new HashSet<>();
            coveredIds.add(Dialect.XML.id().toLowerCase(Locale.ROOT));
            providers.stream()
                    .flatMap(p -> p.provides().stream())
                    .map(d -> d.id().toLowerCase(Locale.ROOT))
                    .forEach(coveredIds::add);

            // Collect ModelDialectProvider dialects that fill gaps not covered by DialectProvider
            Dialect[] extra = modelDialectProviders.stream()
                    .map(ModelDialectProvider::getDialect)
                    .filter(d -> coveredIds.add(d.id().toLowerCase(Locale.ROOT))) // add returns true if new
                    .toArray(Dialect[]::new);

            // Prepend XML to the builtins; extra entries are new (not yet in coveredIds before the filter ran)
            return Stream.concat(Stream.of(Dialect.XML), Stream.of(extra)).toArray(Dialect[]::new);
        }
    }

    public abstract static class DefaultExtensibleEnumRegistry<
                    T extends ExtensibleEnum, P extends ExtensibleEnumProvider<T>>
            implements ExtensibleEnumRegistry<T> {

        protected final Map<String, T> values;

        @SafeVarargs
        public DefaultExtensibleEnumRegistry(List<P> providers, T... builtinValues) {
            values = Stream.concat(Stream.of(builtinValues), providers.stream().flatMap(p -> p.provides().stream()))
                    .collect(Collectors.toUnmodifiableMap(t -> t.id().toLowerCase(Locale.ROOT), t -> t));
        }

        @Override
        public Optional<T> lookup(String id) {
            return Optional.ofNullable(values.get(requireNonNull(id, "id").toLowerCase(Locale.ROOT)));
        }
    }
}
