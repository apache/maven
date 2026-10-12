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
import java.util.List;

import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Repository;

/**
 * Factory service to create {@link LocalRepository} or {@link RemoteRepository} objects.
 *
 * @since 4.0.0
 */
@Experimental
public interface RepositoryFactory extends Service {

    /**
     * Creates a {@link LocalRepository} backed by the given filesystem path.
     * The path will be converted to an absolute path.
     *
     * @param path the filesystem path to the local repository, must not be {@code null}
     * @return a new local repository instance, never {@code null}
     */
    @Nonnull
    LocalRepository createLocal(@Nonnull Path path);

    /**
     * Creates a {@link RemoteRepository} with the given identifier and URL.
     * The repository will use the standard default layout and default release
     * and snapshot policies.
     *
     * @param id the identifier of the remote repository, must not be {@code null}
     * @param url the URL of the remote repository, must not be {@code null}
     * @return a new remote repository instance, never {@code null}
     */
    @Nonnull
    RemoteRepository createRemote(@Nonnull String id, @Nonnull String url);

    /**
     * Creates a {@link RemoteRepository} from the given POM model repository definition.
     * <p>
     * The returned repository preserves the identifier, layout, and URL from the model,
     * and translates its release and snapshot policies (including enabled state, update
     * policy, and checksum policy).
     *
     * @param repository the model repository to convert, must not be {@code null}
     * @return a new remote repository instance, never {@code null}
     * @throws IllegalArgumentException if the model repository is invalid
     */
    @Nonnull
    RemoteRepository createRemote(@Nonnull Repository repository);

    /**
     * Aggregates two lists of remote repositories into a single list by merging
     * definitions and resolving duplicates.
     * <p>
     * Repositories in the {@code dominant} list take precedence over repositories
     * in the {@code recessive} list with matching identifiers. The aggregated list
     * retains the order of dominant repositories, followed by non-duplicate recessive
     * repositories.
     * <p>
     * When {@code processRecessive} is {@code true}, recessive repositories are treated
     * as raw definitions and are subjected to mirror selection, proxy selection, and
     * authentication configuration from the {@link Session}. When {@code false},
     * recessive repositories are assumed to be already processed and are merged without
     * reapplying session settings.
     *
     * @param session the current build session, must not be {@code null}
     * @param dominant the list of dominant remote repositories taking precedence, must not be {@code null}
     * @param recessive the list of recessive remote repositories to merge into the dominant list, must not be {@code null}
     * @param processRecessive {@code true} if recessive repositories should be configured using mirrors,
     *                         proxies, and authentication from the session; {@code false} if they are already configured
     * @return the aggregated list of remote repositories, never {@code null}
     */
    @Nonnull
    List<RemoteRepository> aggregate(
            @Nonnull Session session,
            @Nonnull List<RemoteRepository> dominant,
            @Nonnull List<RemoteRepository> recessive,
            boolean processRecessive);
}
