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

import java.util.Map;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Dependency properties supported by Maven Core.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface DependencyProperties {
    /**
     * Boolean flag telling that dependency contains all of its dependencies. Value of this key should be parsed with
     * {@link Boolean#parseBoolean(String)} to obtain value.
     * <p>
     * <em>Important: this flag must be kept in sync with resolver! (as is used during collection)</em>
     */
    String FLAG_INCLUDES_DEPENDENCIES = "includesDependencies";

    /**
     * Boolean flag telling that dependency is meant to be part of build path. Value of this key should be parsed with
     * {@link Boolean#parseBoolean(String)} to obtain value.
     * <p>
     * <em>Important: this flag must be kept in sync with resolver! (as is used during collection)</em>
     */
    String FLAG_BUILD_PATH_CONSTITUENT = "constitutesBuildPath";

    /**
     * Returns immutable "map view" of all the properties.
     */
    @Nonnull
    Map<String, String> asMap();

    /**
     * Returns {@code true} if given flag is {@code true}.
     */
    boolean checkFlag(@Nonnull String flag);
}
