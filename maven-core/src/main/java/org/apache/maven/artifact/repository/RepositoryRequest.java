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
package org.apache.maven.artifact.repository;

import java.util.List;

/**
 * Collects basic settings to access the repository system.
 *
 * @author Benjamin Bentmann
 */
public interface RepositoryRequest {

    /**
     * Indicates whether network access to remote repositories has been disabled.
     *
     * @return {@code true} if remote access has been disabled, {@code false} otherwise.
     */
    boolean isOffline();

    /**
     * Enables/disables network access to remote repositories.
     *
     * @param offline {@code true} to disable remote access, {@code false} to allow network access.
     * @return This request, never {@code null}.
     */
    RepositoryRequest setOffline(boolean offline);

    /**
     * Indicates whether remote repositories should be re-checked for updated artifacts/metadata regardless of their
     * configured update policy.
     *
     * @return {@code true} if remote repositories should be re-checked for updated artifacts/metadata, {@code false}
     *         otherwise.
     */
    boolean isForceUpdate();

    /**
     * Enables/disabled forced checks for updated artifacts/metadata on remote repositories.
     *
     * @param forceUpdate {@code true} to forcibly check the remote repositories for updated artifacts/metadata, {@code
     *            false} to use the update policy configured on each repository.
     * @return This request, never {@code null}.
     */
    RepositoryRequest setForceUpdate(boolean forceUpdate);

    /**
     * Gets the local repository to use.
     *
     * @return The local repository to use or {@code null} if not set.
     */
    ArtifactRepository getLocalRepository();

    /**
     * Sets the local repository to use.
     *
     * @param localRepository The local repository to use.
     * @return This request, never {@code null}.
     */
    RepositoryRequest setLocalRepository(ArtifactRepository localRepository);

    /**
     * Gets the remote repositories to use.
     *
     * @return The remote repositories to use, never {@code null}.
     */
    List<ArtifactRepository> getRemoteRepositories();

    /**
     * Sets the remote repositories to use.
     *
     * @param remoteRepositories The remote repositories to use.
     * @return This request, never {@code null}.
     */
    RepositoryRequest setRemoteRepositories(List<ArtifactRepository> remoteRepositories);
}
