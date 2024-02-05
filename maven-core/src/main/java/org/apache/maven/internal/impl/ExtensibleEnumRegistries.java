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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.SessionScoped;
import org.apache.maven.api.*;
import org.apache.maven.api.services.*;
import org.apache.maven.api.spi.*;

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
            super(providers, Language.NONE, Language.JAVA_FAMILY);
        }
    }

    static class DefaultExtensibleEnumRegistry<T extends ExtensibleEnum, P extends ExtensibleEnumProvider<T>>
            implements ExtensibleEnumRegistry<T> {

        private final Map<String, T> values;

        DefaultExtensibleEnumRegistry(List<P> providers, T... builtinValues) {
            values = Stream.<T>concat(
                            Stream.of(builtinValues), providers.stream().flatMap(p -> p.provides().stream()))
                    .collect(Collectors.toMap(t -> t.id(), t -> t));
        }

        @Override
        public Optional<T> lookup(String id) {
            return Optional.ofNullable(values.get(id));
        }
    }
}
