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

import java.util.Set;

import org.apache.maven.api.annotations.Experimental;

/**
 * Dependency scope.
 * This represents at which time the dependency will be used, for example, at compile time only,
 * at run time or at test time.  For a given dependency, the scope is directly derived from the
 * {@link org.apache.maven.api.model.Dependency#getScope()} and will be used when using {@link PathScope}
 * and the {@link org.apache.maven.api.services.DependencyResolver}.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.model.Dependency#getScope()
 * @see org.apache.maven.api.services.DependencyResolver
 */
@Experimental
public interface DependencyScopeManager extends Service {
    String SYSTEM = "system";

    String NONE = "none";

    String EMPTY = "";

    String COMPILE_ONLY = "compile-only";

    String COMPILE = "compile";

    String PROVIDED = "provided";

    String RUNTIME = "runtime";

    String TEST_ONLY = "test-only";

    String TEST = "test";

    String TEST_RUNTIME = "test-runtime";

    DependencyScope fromString(String string);

    Set<DependencyScope> all();
}
