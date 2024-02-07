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

import java.util.*;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

import static org.apache.maven.api.ExtensibleEnums.pathScope;

/**
 * Path scope.
 * A path scope is used to determine the kind of build or class path that will be built when resolving
 * dependencies using the {@link org.apache.maven.api.services.DependencyResolver} service.
 * <p>
 * This extensible enum has four defined values, {@link #MAIN_COMPILE}, {@link #MAIN_RUNTIME},
 * {@link #TEST_COMPILE} and {@link #TEST_RUNTIME}, but can be extended by registering a
 * {@code org.apache.maven.api.spi.PathScopeProvider}.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.DependencyResolver
 * @see DependencyScope
 */
@Experimental
@Immutable
public interface PathScope extends ExtensibleEnum {

    @Nonnull
    ProjectScope projectScope();

    @Nonnull
    Set<DependencyScope> dependencyScopes();

    PathScope MAIN_COMPILE = pathScope(
            "main-compile",
            ProjectScope.MAIN,
            DependencyScope.COMPILE_ONLY,
            DependencyScope.COMPILE,
            DependencyScope.PROVIDED);

    PathScope MAIN_RUNTIME =
            pathScope("main-runtime", ProjectScope.MAIN, DependencyScope.COMPILE, DependencyScope.RUNTIME);

    PathScope TEST_COMPILE = pathScope(
            "test-compile",
            ProjectScope.TEST,
            DependencyScope.COMPILE,
            DependencyScope.PROVIDED,
            DependencyScope.TEST_ONLY,
            DependencyScope.TEST);

    PathScope TEST_RUNTIME = pathScope(
            "test-runtime",
            ProjectScope.TEST,
            DependencyScope.COMPILE,
            DependencyScope.RUNTIME,
            DependencyScope.PROVIDED,
            DependencyScope.TEST,
            DependencyScope.TEST_RUNTIME);
}
