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

/**
 * The dependency scopes used for Java dependencies in Maven. This class defines labels only, that are doing pass-thru
 * over Resolver.
 *
 * @since 4.0.0
 */
public final class MavenDependencyScopes {

    /**
     * Important: keep this label in sync with Resolver.
     *
     * TODO: once Resolver 2.0.0-alpha-7 is out, use org.eclipse.aether.util.artifact.DependencyScopes#SYSTEM
     */
    public static final String SYSTEM = "system";

    public static final String COMPILE_ONLY = "compile-only";

    public static final String COMPILE = "compile";

    public static final String PROVIDED = "provided";

    public static final String RUNTIME = "runtime";

    public static final String TEST_ONLY = "test-only";

    public static final String TEST = "test";

    public static final String TEST_RUNTIME = "test-runtime";

    private MavenDependencyScopes() {
        // hide constructor
    }
}
