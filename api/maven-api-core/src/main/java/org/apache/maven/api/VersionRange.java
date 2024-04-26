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
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * A range of versions.
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionRange {
    /**
     * Determines whether the specified version is contained within this range.
     *
     * @param version the version to test, must not be {@code null}
     * @return {@code true} if this range contains the specified version, {@code false} otherwise
     */
    boolean contains(@Nonnull Version version);

    /**
     * Returns the upper boundary of this range, or {@code null} if none.
     */
    @Nullable
    Boundary getUpperBoundary();

    /**
     * Returns the lower boundary of this range, or {@code null} if none.
     */
    @Nullable
    Boundary getLowerBoundary();

    /**
     * Returns a string representation of this version range
     * @return the string representation of this version range
     */
    @Nonnull
    String asString();

    /**
     * Represents range boundary.
     */
    interface Boundary {
        /**
         * The bounding version.
         */
        Version getVersion();

        /**
         * Returns {@code true} if version is included of the range.
         */
        boolean isInclusive();
    }
}
