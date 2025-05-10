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
package org.apache.maven.api.services;

import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Repository;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Represents the result of a version range resolution request. This interface provides access to
 * information about resolved versions that match a version range constraint, including any exceptions
 * that occurred during resolution, the available versions, and their source repositories.
 *
 * <p>The versions returned by this interface are guaranteed to be in ascending order.</p>
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionRangeResolverResult extends Result<VersionRangeResolverRequest> {

    /**
     * Gets the exceptions that occurred while resolving the version range.
     *
     * @return The list of exceptions that occurred during resolution, never {@code null}
     */
    @Nonnull
    List<Exception> getExceptions();

    /**
     * Gets the versions (in ascending order) that matched the requested range.
     *
     * @return The list of matching versions, never {@code null}. An empty list indicates
     *         no versions matched the requested range.
     */
    @Nonnull
    List<Version> getVersions();

    /**
     * Gets the lowest version matching the requested range.
     *
     * @return An Optional containing the lowest matching version, or empty Optional if no versions
     *         matched the requested range
     */
    @Nonnull
    default Optional<Version> getLowestVersion() {
        return getVersions().isEmpty()
                ? Optional.empty()
                : Optional.of(getVersions().getFirst());
    }

    /**
     * Gets the highest version matching the requested range.
     *
     * @return An Optional containing the highest matching version, or empty Optional if no versions
     *         matched the requested range
     */
    @Nonnull
    default Optional<Version> getHighestVersion() {
        return getVersions().isEmpty()
                ? Optional.empty()
                : Optional.of(getVersions().get(getVersions().size() - 1));
    }

    /**
     * Gets the repository from which the specified version was resolved.
     *
     * @param version The version whose source repository should be retrieved, must not be {@code null}
     * @return An Optional containing the repository from which the version was resolved,
     *         or empty Optional if the repository is unknown
     */
    @Nonnull
    Optional<Repository> getRepository(Version version);
}
