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

import java.nio.file.Path;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface LocalRepositoryManager extends Service {

    /**
     * Gets the relative path for a locally installed artifact.
     * Note that the artifact need not actually exist yet at
     * the returned location, the path merely indicates where
     * the artifact would eventually be stored.
     *
     * @param session The session to use, must not be {@code null}.
     * @param artifact The artifact for which to determine the path, must not be {@code null}.
     * @return The path, resolved against the local repository's base directory.
     */
    @Nonnull
    Path getPathForLocalArtifact(@Nonnull Session session, @Nonnull LocalRepository local, @Nonnull Artifact artifact);

    /**
     * Gets the relative path for an artifact cached from a remote repository.
     * Note that the artifact need not actually exist yet at the returned location,
     * the path merely indicates where the artifact would eventually be stored.
     *
     * @param session The session to use, must not be {@code null}.
     * @param local The local repository, must not be {@code null}.
     * @param artifact The artifact for which to determine the path, must not be {@code null}.
     * @param remote – The source repository of the artifact, must not be {@code null}.
     * @return The path, relative to the local repository's base directory.
     */
    @Nonnull
    Path getPathForRemoteArtifact(
            @Nonnull Session session,
            @Nonnull LocalRepository local,
            @Nonnull RemoteRepository remote,
            @Nonnull Artifact artifact);
}
