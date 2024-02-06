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
package org.apache.maven.repository.internal.scopes;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.*;

import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DependencyScopeManager;

import static java.util.Objects.requireNonNull;

/**
 * Default implementation.
 */
@Singleton
@Named
public final class DefaultDependencyScopeManager implements DependencyScopeManager {
    public static final DependencyScope SYSTEM = new DefaultDependencyScope(DependencyScopeManager.SYSTEM, false);
    public static final DependencyScope NONE = new DefaultDependencyScope(DependencyScopeManager.NONE, false);
    public static final DependencyScope EMPTY = new DefaultDependencyScope(DependencyScopeManager.EMPTY, false);
    public static final DependencyScope COMPILE_ONLY =
            new DefaultDependencyScope(DependencyScopeManager.COMPILE_ONLY, false);
    public static final DependencyScope COMPILE = new DefaultDependencyScope(DependencyScopeManager.COMPILE, true);
    public static final DependencyScope PROVIDED = new DefaultDependencyScope(DependencyScopeManager.PROVIDED, false);
    public static final DependencyScope RUNTIME = new DefaultDependencyScope(DependencyScopeManager.RUNTIME, true);
    public static final DependencyScope TEST_ONLY = new DefaultDependencyScope(DependencyScopeManager.TEST_ONLY, false);
    public static final DependencyScope TEST = new DefaultDependencyScope(DependencyScopeManager.TEST, false);
    public static final DependencyScope TEST_RUNTIME =
            new DefaultDependencyScope(DependencyScopeManager.TEST_RUNTIME, false);

    private static final class DefaultDependencyScope implements DependencyScope {
        private final String id;
        private final boolean transitive;

        private DefaultDependencyScope(String id, boolean transitive) {
            this.id = requireNonNull(id, "id");
            this.transitive = transitive;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isTransitive() {
            return transitive;
        }

        @Override
        public boolean is(String string) {
            return id.equals(string);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultDependencyScope that = (DefaultDependencyScope) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" + "id='" + id + '\'' + ", transitive=" + transitive + '}';
        }
    }

    private final Map<String, DependencyScope> all;

    public DefaultDependencyScopeManager() {
        HashMap<String, DependencyScope> scopes = new HashMap<>();
        scopes.put(SYSTEM.id(), SYSTEM);
        scopes.put(NONE.id(), NONE);
        scopes.put(EMPTY.id(), EMPTY);
        scopes.put(COMPILE_ONLY.id(), COMPILE_ONLY);
        scopes.put(COMPILE.id(), COMPILE);
        scopes.put(PROVIDED.id(), PROVIDED);
        scopes.put(RUNTIME.id(), RUNTIME);
        scopes.put(TEST_ONLY.id(), TEST_ONLY);
        scopes.put(TEST.id(), TEST);
        scopes.put(TEST_RUNTIME.id(), TEST_RUNTIME);
        this.all = Collections.unmodifiableMap(scopes);
    }

    @Override
    public DependencyScope fromString(String string) {
        requireNonNull(string, "string");
        DependencyScope result = all.get(string);
        if (result == null) {
            throw new IllegalArgumentException("unknown scope");
        }
        return result;
    }

    @Override
    public Set<DependencyScope> all() {
        return new HashSet<>(all.values());
    }
}
