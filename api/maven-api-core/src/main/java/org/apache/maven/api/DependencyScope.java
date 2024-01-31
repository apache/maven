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
 * Dependency scope.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public enum DependencyScope {

    /**
     * None. Allows you to declare dependencies (for example to alter reactor build order) but in reality dependencies
     * in this scope are not part of any build path scope.
     */
    NONE("none", false),

    /**
     * Empty scope.
     */
    EMPTY("", false),

    /**
     * Compile only.
     */
    COMPILE_ONLY("compile-only", false),

    /**
     * Compile.
     */
    COMPILE("compile", true),

    /**
     * Runtime.
     */
    RUNTIME("runtime", true),

    /**
     * Provided.
     */
    PROVIDED("provided", false),

    /**
     * Test compile only.
     */
    TEST_ONLY("test-only", false),

    /**
     * Test.
     */
    TEST("test", false),

    /**
     * Test runtime.
     */
    TEST_RUNTIME("test-runtime", true),

    /**
     * System scope.
     * <p>
     * Important: this scope {@code id} MUST BE KEPT in sync with label in
     * {@code org.eclipse.aether.util.artifact.Scopes#SYSTEM}.
     */
    SYSTEM("system", false);

    private static final Map<String, DependencyScope> IDS = Collections.unmodifiableMap(
            Stream.of(DependencyScope.values()).collect(Collectors.toMap(s -> s.id, s -> s)));

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
}
