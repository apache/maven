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
package org.apache.maven.api;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Indicates when the dependency will be used.
 * For example, it may be at compile time only, at runtime, or at test time.
 * For a given dependency, the scope is directly derived from the
 * {@link org.apache.maven.api.model.Dependency#getScope()} and will be used when using {@link PathScope}
 * and the {@link org.apache.maven.api.services.DependencyResolver}.
 * <p>
 * Each scope controls which {@link PathScope} classpaths the dependency appears on
 * (see {@link PathScope#MAIN_COMPILE}, {@link PathScope#MAIN_RUNTIME},
 * {@link PathScope#TEST_COMPILE}, {@link PathScope#TEST_RUNTIME}),
 * and whether the dependency is propagated transitively to downstream projects
 * (see {@link #isTransitive()}).
 * <p>
 * When resolving the transitive graph of a dependency, the effective scope of its transitive
 * dependencies may be narrowed. In particular, transitive {@code compile} and {@code runtime}
 * dependencies of a {@link #PROVIDED} dependency are pulled in with effective {@code provided}
 * scope, while transitive {@code provided} and {@code test} dependencies of a {@code provided}
 * dependency are omitted entirely.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.model.Dependency#getScope()
 * @see org.apache.maven.api.services.DependencyResolver
 */
@Experimental
@Immutable
public enum DependencyScope {

    /**
     * None. Allows you to declare dependencies (for example to alter reactor build order) but in reality dependencies
     * in this scope are not part of any path scope.
     */
    NONE("none", false),

    /**
     * Undefined. When no scope is explicitly given, UNDEFINED will be used, but its meaning will depend on
     * whether the DependencyCoordinates is used in dependency management, in which case it means the scope is not
     * explicitly managed by this managed dependency, or as a real dependency, in which case, the scope
     * will default to {@link #COMPILE}.
     */
    UNDEFINED("", false),

    /**
     * Compile only.
     * <p>
     * A dependency required for compiling the main project code, but available only on the main
     * compile classpath. It is not available on the main runtime, test compile, or test runtime
     * classpaths.
     */
    COMPILE_ONLY("compile-only", false),

    /**
     * Compile.
     * <p>
     * A dependency required for compiling, testing, and running the project.
     * Dependencies in this scope are available on all classpaths and are transitive.
     */
    COMPILE("compile", true),

    /**
     * Runtime.
     * <p>
     * A dependency not required for compiling the project source code, but required for executing
     * the project and running tests. Dependencies in this scope are available on the main runtime
     * and test runtime classpaths, but not on the main compile or test compile classpaths.
     * Dependencies in this scope are transitive.
     */
    RUNTIME("runtime", true),

    /**
     * Provided.
     * <p>
     * A dependency required for compilation and testing that is expected to be provided at runtime
     * by the JDK or the runtime environment/container. Dependencies in this scope are available on
     * the main compile, test compile, and test runtime classpaths, but not on the main runtime
     * classpath. Dependencies in this scope are not transitive to downstream projects.
     */
    PROVIDED("provided", false),

    /**
     * Test compile only.
     * <p>
     * A dependency required only for compiling the test sources, but not needed for running tests.
     */
    TEST_ONLY("test-only", false),

    /**
     * Test.
     * <p>
     * A dependency required for compiling and running tests, but not required for compiling or
     * running the main project code. Dependencies in this scope are not transitive.
     */
    TEST("test", false),

    /**
     * Test runtime.
     * <p>
     * A dependency not required for compiling test sources, but required for executing tests.
     */
    TEST_RUNTIME("test-runtime", false),

    /**
     * System.
     * <p>
     * Similar to {@link #PROVIDED}, but the artifact is bound to an explicit path on the local file system
     * rather than resolved from a repository.
     */
    SYSTEM("system", false);

    private static final Map<String, DependencyScope> IDS = Collections.unmodifiableMap(
            Stream.of(DependencyScope.values()).collect(Collectors.toMap(s -> s.id, s -> s)));

    /**
     * {@return the dependency scope for the given identifier, or {@code null} if none}.
     * The identifiers are usually in lower cases with {@code '-'} instead of {@code '_'}
     * as word separator.
     *
     * @param id the identifier of the scope (case-sensitive)
     */
    public static DependencyScope forId(String id) {
        return IDS.get(id);
    }

    private final String id;
    private final boolean transitive;

    DependencyScope(String id, boolean transitive) {
        this.id = id;
        this.transitive = transitive;
    }

    /**
     * The {@code id} uniquely represents a value for this extensible enum.
     * This id should be used to compute the equality and hash code for the instance.
     *
     * @return the id
     */
    @Nonnull
    public String id() {
        return id;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public boolean is(String id) {
        return id().equals(id);
    }
}
