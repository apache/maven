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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.api.PluginRealm;
import org.apache.maven.api.services.Lookup;

import static java.util.Objects.requireNonNull;

final class DefaultPluginRealm implements PluginRealm {

    private final ClassLoader classLoader;
    private final Optional<ModuleLayer> moduleLayer;
    private final Lookup lookup;

    DefaultPluginRealm(ClassLoader classLoader, Lookup lookup) {
        this(classLoader, null, lookup);
    }

    DefaultPluginRealm(ClassLoader classLoader, ModuleLayer moduleLayer, Lookup lookup) {
        this.classLoader = requireNonNull(classLoader, "classLoader");
        this.moduleLayer = Optional.ofNullable(moduleLayer);
        this.lookup = new RealmLookup(classLoader, requireNonNull(lookup, "lookup"));
    }

    @Override
    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public Optional<ModuleLayer> moduleLayer() {
        return moduleLayer;
    }

    @Override
    public Lookup lookup() {
        return lookup;
    }

    private static final class RealmLookup implements Lookup {

        private final ClassLoader classLoader;
        private final Lookup delegate;

        private RealmLookup(ClassLoader classLoader, Lookup delegate) {
            this.classLoader = classLoader;
            this.delegate = delegate;
        }

        @Override
        public <T> T lookup(Class<T> type) {
            return scoped(() -> delegate.lookup(type));
        }

        @Override
        public <T> T lookup(Class<T> type, String name) {
            return scoped(() -> delegate.lookup(type, name));
        }

        @Override
        public <T> Optional<T> lookupOptional(Class<T> type) {
            return scoped(() -> delegate.lookupOptional(type));
        }

        @Override
        public <T> Optional<T> lookupOptional(Class<T> type, String name) {
            return scoped(() -> delegate.lookupOptional(type, name));
        }

        @Override
        public <T> List<T> lookupList(Class<T> type) {
            return scoped(() -> delegate.lookupList(type));
        }

        @Override
        public <T> Map<String, T> lookupMap(Class<T> type) {
            return scoped(() -> delegate.lookupMap(type));
        }

        private <T> T scoped(Supplier<T> action) {
            Thread thread = Thread.currentThread();
            ClassLoader originalClassLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(classLoader);
                return action.get();
            } finally {
                thread.setContextClassLoader(originalClassLoader);
            }
        }
    }
}
