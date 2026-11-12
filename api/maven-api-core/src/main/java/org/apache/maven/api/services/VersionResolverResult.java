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
 * Result of resolving a meta-version, providing the concrete resolved version
 * and the repository it was resolved from.
 *
 * @since 4.0.0
 */
@Experimental
public interface VersionResolverResult extends Result<VersionResolverRequest> {

    /**
     * Gets the exceptions that occurred during version resolution.
     *
     * @return the list of exceptions, never {@code null}
     */
    @Nonnull
    List<Exception> getExceptions();

    /**
     * Gets the resolved version.
     *
     * @return the resolved version
     */
    @Nonnull
    Version getVersion();

    /**
     * Gets the repository from which the version was resolved.
     *
     * @return an optional containing the repository, or empty if unknown
     */
    @Nonnull
    Optional<Repository> getRepository();
}
