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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;

/**
 * Dependencies resolution scopes available before
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/lifecycle/internal/MojoExecutor.html">mojo execution</a>.
 *
 * Important note: The {@code id} values of this enum correspond to constants of
 * {@code org.apache.maven.artifact.Artifact} class and MUST BE KEPT IN SYNC.
 *
 * @since 4.0.0
 */
@Experimental
public enum ResolutionScope {
    /**
     * <code>compile</code> resolution scope
     * = <code>compile-only</code> + <code>compile</code> + <code>provided</code> dependencies
     */
    PROJECT_COMPILE("project-compile", Scope.EMPTY, Scope.COMPILE_ONLY, Scope.COMPILE, Scope.PROVIDED),
    /**
     * <code>runtime</code> resolution scope
     * = <code>compile</code> + <code>runtime</code> dependencies
     */
    PROJECT_RUNTIME("project-runtime", Scope.EMPTY, Scope.COMPILE, Scope.RUNTIME),
    /**
     * <code>test-compile</code> resolution scope
     * = <code>compile-only</code> + <code>compile</code> + <code>provided</code> + <code>test-compile-only</code> + <code>test</code>
     * dependencies
     */
    TEST_COMPILE(
            "test-compile",
            Scope.EMPTY,
            Scope.COMPILE_ONLY,
            Scope.COMPILE,
            Scope.PROVIDED,
            Scope.TEST_COMPILE_ONLY,
            Scope.TEST),
    /**
     * <code>test</code> resolution scope
     * = <code>compile</code> + <code>runtime</code> + <code>provided</code> + <code>test</code> + <code>test-runtime</code>
     * dependencies
     */
    TEST_RUNTIME(
            "test-runtime", Scope.EMPTY, Scope.COMPILE, Scope.RUNTIME, Scope.PROVIDED, Scope.TEST, Scope.TEST_RUNTIME);

    private static final Map<String, ResolutionScope> VALUES =
            Stream.of(ResolutionScope.values()).collect(Collectors.toMap(ResolutionScope::id, s -> s));

    public static ResolutionScope fromString(String id) {
        return Optional.ofNullable(VALUES.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown resolution scope " + id));
    }

    private final String id;
    private final Set<Scope> scopes;

    ResolutionScope(String id, Scope... scopes) {
        this.id = id;
        this.scopes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(scopes)));
    }

    public String id() {
        return this.id;
    }

    public Set<Scope> scopes() {
        return scopes;
    }
}
