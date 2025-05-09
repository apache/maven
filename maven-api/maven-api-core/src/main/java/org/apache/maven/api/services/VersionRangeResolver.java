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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Parses and evaluates version ranges encountered in dependency declarations.
 *
 * @since 4.0.0
 */
@Experimental
@Consumer
public interface VersionRangeResolver extends Service {

    /**
     * Expands a version range to a list of matching versions, in ascending order.
     * For example, resolves "[3.8,4.0)" to "3.8", "3.8.1", "3.8.2".
     * The returned list of versions is only dependent on the configured repositories and their contents.
     * The supplied request may also refer to a single concrete version rather than a version range.
     * In this case though, the result contains simply the (parsed) input version, regardless of the
     * repositories and their contents.
     *
     * @param session the session to use
     * @param artifactCoordinates t
     * @return the version range resolution result
     * @throws VersionResolverException if an errors occurs
     */
    @Nonnull
    default VersionRangeResolverResult resolve(
            @Nonnull Session session, @Nonnull ArtifactCoordinates artifactCoordinates)
            throws VersionResolverException {
        return resolve(VersionRangeResolverRequest.build(session, artifactCoordinates));
    }

    /**
     * Expands a version range to a list of matching versions, in ascending order.
     * For example, resolves "[3.8,4.0)" to "3.8", "3.8.1", "3.8.2".
     * The returned list of versions is only dependent on the configured repositories and their contents.
     * The supplied request may also refer to a single concrete version rather than a version range.
     * In this case though, the result contains simply the (parsed) input version, regardless of the
     * repositories and their contents.
     *
     * @param session the session to use
     * @param artifactCoordinates t
     * @param repositories the repositories to use (if {@code null}, the session repositories are used)
     * @return the version range resolution result
     * @throws VersionResolverException if an errors occurs
     */
    @Nonnull
    default VersionRangeResolverResult resolve(
            @Nonnull Session session,
            @Nonnull ArtifactCoordinates artifactCoordinates,
            @Nullable List<RemoteRepository> repositories)
            throws VersionResolverException {
        return resolve(VersionRangeResolverRequest.build(session, artifactCoordinates, repositories));
    }

    @Nonnull
    VersionRangeResolverResult resolve(@Nonnull VersionRangeResolverRequest request)
            throws VersionRangeResolverException;
}
