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

import org.apache.maven.api.DependencyScope;

/**
 * The dependency scopes used for Java dependencies in Maven. This class defines labels only, that are doing pass-thru
 * over Resolver. The labels are defined in {@link DependencyScope} class, these are here used only for "easier
 * reachability" in internal classes.
 *
 * @since 4.0.0
 */
public final class MavenDependencyScopes {

    public static final String SYSTEM = DependencyScope.SYSTEM.id();

    public static final String NONE = DependencyScope.NONE.id();

    public static final String COMPILE_ONLY = DependencyScope.COMPILE_ONLY.id();

    public static final String COMPILE = DependencyScope.COMPILE.id();

    public static final String PROVIDED = DependencyScope.PROVIDED.id();

    public static final String RUNTIME = DependencyScope.RUNTIME.id();

    public static final String TEST_ONLY = DependencyScope.TEST_ONLY.id();

    public static final String TEST = DependencyScope.TEST.id();

    public static final String TEST_RUNTIME = DependencyScope.TEST_RUNTIME.id();

    private MavenDependencyScopes() {
        // hide constructor
    }
}
