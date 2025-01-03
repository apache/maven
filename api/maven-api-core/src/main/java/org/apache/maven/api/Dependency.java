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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * A result of collecting, flattening and resolving {@link DependencyCoordinates}s.
 * Dependency is the output of the <dfn>collection</dfn> process, which builds the graph of dependencies,
 * followed by <dfn>flattening</dfn> and <dfn>resolution</dfn>.
 * The version selection is done for each dependency during the collection phase.
 * The flatten phase will keep only a single version per ({@code groupId}, {@code artifactId}) pair.
 * The resolution will actually download the dependencies (or artifacts) that have been computed.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Dependency extends Artifact {
    /**
     * {@return the type of the dependency}
     * A dependency can be a <abbr>JAR</abbr> file,
     * a modular-<abbr>JAR</abbr> if it is intended to be placed on the module path,
     * a <abbr>JAR</abbr> containing test classes, <i>etc.</i>
     *
     * @see DependencyCoordinates#getType()
     */
    @Nonnull
    Type getType();

    /**
     * {@return the time at which the dependency will be used}
     * If may be, for example, at compile time only, at run time or at test time.
     *
     * @see DependencyCoordinates#getScope()
     */
    @Nonnull
    DependencyScope getScope();

    /**
     * Returns whether the dependency is optional or mandatory.
     * Contrarily to {@link DependencyCoordinates}, the obligation of a {@code Dependency} is always present.
     * The value is computed during the dependencies collection phase.
     *
     * @return {@code true} if the dependency is optional, or {@code false} if mandatory
     * @see DependencyCoordinates#getOptional()
     */
    boolean isOptional();

    /**
     * {@return coordinates with the same identifiers as this dependency}
     */
    @Nonnull
    @Override
    DependencyCoordinates toCoordinates();
}
