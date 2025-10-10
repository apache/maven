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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nullable;

/**
 * Base interface for service requests that involve remote repository operations.
 * This interface provides common functionality for requests that need to specify
 * and validate remote repositories for artifact resolution, dependency collection,
 * model building, and other Maven operations.
 *
 * <p>Implementations of this interface can specify a list of remote repositories
 * to be used during the operation. If no repositories are specified (null),
 * the session's default remote repositories will be used. The repositories
 * are validated to ensure they don't contain duplicates or null entries.
 *
 * <p>Remote repositories are used for:
 * <ul>
 *   <li>Resolving artifacts and their metadata</li>
 *   <li>Downloading parent POMs and dependency POMs</li>
 *   <li>Retrieving version information and ranges</li>
 *   <li>Accessing plugin artifacts and their dependencies</li>
 * </ul>
 *
 * <p>Repository validation ensures data integrity by:
 * <ul>
 *   <li>Preventing duplicate repositories that could cause confusion</li>
 *   <li>Rejecting null repository entries that would cause failures</li>
 *   <li>Maintaining consistent repository ordering for reproducible builds</li>
 * </ul>
 *
 * @since 4.0.0
 * @see RemoteRepository
 * @see Session#getRemoteRepositories()
 */
@Experimental
@Immutable
public interface RepositoryAwareRequest extends Request<Session> {

    /**
     * Returns the list of remote repositories to be used for this request.
     *
     * <p>If this method returns {@code null}, the session's default remote repositories
     * will be used. If a non-null list is returned, it will be used instead of the
     * session's repositories, allowing for request-specific repository configuration.
     *
     * <p>The returned list should not contain duplicate repositories (based on their
     * equality) or null entries, as these will cause validation failures when the
     * request is processed.
     *
     * @return the list of remote repositories to use, or {@code null} to use session defaults
     * @see Session#getRemoteRepositories()
     */
    @Nullable
    List<RemoteRepository> getRepositories();

    /**
     * Validates a list of remote repositories to ensure data integrity.
     *
     * <p>This method performs the following validations:
     * <ul>
     *   <li>Allows null input (returns null)</li>
     *   <li>Ensures no duplicate repositories exist in the list</li>
     *   <li>Ensures no null repository entries exist in the list</li>
     * </ul>
     *
     * <p>Duplicate detection is based on the {@code RemoteRepository#equals(Object)}
     * method, which typically compares repository IDs and URLs.
     *
     * @param repositories the list of repositories to validate, may be {@code null}
     * @return the same list if validation passes, or {@code null} if input was {@code null}
     * @throws IllegalArgumentException if the list contains duplicate repositories
     * @throws IllegalArgumentException if the list contains null repository entries
     */
    default List<RemoteRepository> validate(List<RemoteRepository> repositories) {
        if (repositories == null) {
            return null;
        }
        HashSet<RemoteRepository> set = new HashSet<>(repositories);
        if (repositories.size() != set.size()) {
            throw new IllegalArgumentException(
                    "Repository list contains duplicate entries. Each repository must be unique based on its ID and URL. "
                            + "Found " + repositories.size() + " repositories but only " + set.size()
                            + " unique entries.");
        }
        if (repositories.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Repository list contains null entries. All repository entries must be non-null RemoteRepository instances.");
        }
        return repositories;
    }
}
