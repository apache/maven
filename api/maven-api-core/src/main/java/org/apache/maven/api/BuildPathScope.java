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

import static org.apache.maven.api.ExtensibleEnums.buildPathScope;

/**
 * Build path scope.
 * <p>
 * Implementation must have {@code equals()} and {@code hashCode()} implemented, so implementations of this interface
 * can be used as keys.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface BuildPathScope extends ExtensibleEnum {

    @Nonnull
    ProjectScope projectScope();

    @Nonnull
    Set<DependencyScope> dependencyScopes();

    BuildPathScope MAIN_COMPILE = buildPathScope(
            "main-compile",
            ProjectScope.MAIN,
            DependencyScope.EMPTY,
            DependencyScope.COMPILE_ONLY,
            DependencyScope.COMPILE,
            DependencyScope.PROVIDED);

    BuildPathScope MAIN_RUNTIME = buildPathScope(
            "main-runtime", ProjectScope.MAIN, DependencyScope.EMPTY, DependencyScope.COMPILE, DependencyScope.RUNTIME);

    BuildPathScope TEST_COMPILE = buildPathScope(
            "test-compile",
            ProjectScope.TEST,
            DependencyScope.EMPTY,
            DependencyScope.COMPILE,
            DependencyScope.PROVIDED,
            DependencyScope.TEST_ONLY,
            DependencyScope.TEST);

    BuildPathScope TEST_RUNTIME = buildPathScope(
            "test-runtime",
            ProjectScope.TEST,
            DependencyScope.EMPTY,
            DependencyScope.COMPILE,
            DependencyScope.RUNTIME,
            DependencyScope.PROVIDED,
            DependencyScope.TEST,
            DependencyScope.TEST_RUNTIME);
}
