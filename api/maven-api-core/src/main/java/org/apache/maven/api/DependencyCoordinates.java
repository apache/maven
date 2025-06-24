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

import java.util.Collection;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * {@code ArtifactCoordinates} completed with information about how the artifact will be used.
 * This information includes the dependency type (main classes, test classes, <i>etc.</i>),
 * a scope (compile, runtime <i>etc.</i>), an obligation (whether the dependency
 * is optional or mandatory), and possible exclusions for transitive dependencies.
 * The {@linkplain #getVersionConstraint() version} and the {@linkplain #getOptional() obligation}
 * may not be defined precisely.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyCoordinates extends ArtifactCoordinates {
    /**
     * {@return the type of the dependency}
     * A dependency can be a <abbr>JAR</abbr> file,
     * a modular-<abbr>JAR</abbr> if it is intended to be placed on the module path,
     * a <abbr>JAR</abbr> containing test classes, a POM file, <i>etc.</i>
     */
    @Nonnull
    Type getType();

    /**
     * {@return the time at which the dependency will be used}
     * It may be, for example, at compile time only, at run time, or at test time.
     */
    @Nonnull
    DependencyScope getScope();

    /**
     * Returns whether the dependency is optional, mandatory, or of unspecified obligation.
     *
     * @return {@code Boolean.TRUE} and {@code Boolean.FALSE} if optional, or {@code null} if unspecified
     */
    @Nullable
    Boolean getOptional();

    /**
     * {@return transitive dependencies to exclude}
     */
    @Nonnull
    Collection<Exclusion> getExclusions();
}
