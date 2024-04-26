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
 * Version constraint for dependency. Constraint is either a range ("[1,2)") or recommended version ("1.0").
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionConstraint {
    /**
     * Returns the range of this constraint, or {@code null} if none.
     * <p>
     * Note: only one, this method or {@link #getRecommendedVersion()} method must return non-{@code null} value.
     */
    @Nullable
    VersionRange getVersionRange();

    /**
     * Returns the recommended version of this constraint, or {@code null} if none.
     * <p>
     * Note: only one, this method or {@link #getVersionRange()} method must return non-{@code null} value.
     */
    @Nullable
    Version getRecommendedVersion();

    /**
     * Determines whether the specified version is contained within this constraint.
     *
     * @param version the version to test, must not be {@code null}
     * @return {@code true} if this range contains the specified version, {@code false} otherwise
     */
    boolean contains(@Nonnull Version version);

    /**
     * Returns a string representation of this version constraint
     * @return the string representation of this version constraint
     */
    @Nonnull
    String asString();
}
