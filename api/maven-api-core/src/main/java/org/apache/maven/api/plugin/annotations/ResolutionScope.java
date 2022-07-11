package org.apache.maven.api.plugin.annotations;

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

import org.apache.maven.api.annotations.Experimental;

/**
 * Dependencies resolution scopes available before
 * <a href="/ref/current/maven-core/apidocs/org/apache/maven/lifecycle/internal/MojoExecutor.html">mojo execution</a>.
 *
 * Important note: The {@code id} values of this enum correspond to constants of
 * {@code org.apache.maven.artifact.Artifact} class and MUST BE KEPT IN SYNC.
 *
 * @since 4.0
 */
@Experimental
public enum ResolutionScope
{
    /**
     * empty resolution scope
     */
    NONE( null ),
    /**
     * <code>compile</code> resolution scope
     * = <code>compile</code> + <code>system</code> + <code>provided</code> dependencies
     */
    COMPILE( "compile" ),
    /**
     * <code>compile+runtime</code> resolution scope (Maven 3 only)
     * = <code>compile</code> + <code>system</code> + <code>provided</code> + <code>runtime</code> dependencies
     */
    COMPILE_PLUS_RUNTIME( "compile+runtime" ),
    /**
     * <code>runtime</code> resolution scope
     * = <code>compile</code> + <code>runtime</code> dependencies
     */
    RUNTIME( "runtime" ),
    /**
     * <code>runtime+system</code> resolution scope (Maven 3 only)
     * = <code>compile</code> + <code>system</code> + <code>runtime</code> dependencies
     */
    RUNTIME_PLUS_SYSTEM( "runtime+system" ),
    /**
     * <code>test</code> resolution scope
     * = <code>compile</code> + <code>system</code> + <code>provided</code> + <code>runtime</code> + <code>test</code>
     * dependencies
     */
    TEST( "test" );

    private final String id;

    ResolutionScope( String id )
    {
        this.id = id;
    }

    public String id()
    {
        return this.id;
    }
}
