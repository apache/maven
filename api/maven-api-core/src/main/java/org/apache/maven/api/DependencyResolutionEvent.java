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

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Describes the resolution of the dependencies of a project during a build.
 * The result is reported once the resolution completes, whether it succeeded or failed.
 *
 * @see DependencyResolutionListener
 * @see DependencyResolutionEventType
 * @since 4.1.0
 */
@Experimental
@Immutable
public interface DependencyResolutionEvent extends Event {

    /** {@return the kind of dependency resolution operation represented by this event} */
    @Nonnull
    DependencyResolutionEventType type();

    /** {@return the project whose dependencies are being resolved} */
    @Nonnull
    Project project();

    /** {@return the dependencies which were resolved, empty when the resolution has not completed} */
    @Nonnull
    List<Dependency> resolvedDependencies();

    /** {@return the dependencies which could not be resolved, empty when the resolution has not completed} */
    @Nonnull
    List<Dependency> unresolvedDependencies();

    /**
     * {@return the primary failure of the resolution, if any}
     * This is the first dependency collection error or, when there is none, the first resolution error.
     * It may be empty even when {@link #type()} is {@link DependencyResolutionEventType#FAILED}, if some
     * dependencies could not be resolved without any error being reported.
     */
    @Nonnull
    Optional<Exception> exception();
}
