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

import java.util.Collection;
import java.util.Collections;

import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Installs {@link ProducedArtifact}s to the local repository.
 *
 * @since 4.0.0
 * @see Session#withLocalRepository(org.apache.maven.api.LocalRepository)
 */
@Experimental
public interface ArtifactInstaller extends Service {
    /**
     * @param request {@link ArtifactInstallerRequest}
     * @throws ArtifactInstallerException in case of an error
     * @throws IllegalArgumentException in case {@code request} is {@code null}
     */
    void install(@Nonnull ArtifactInstallerRequest request);

    /**
     * @param session the repository session
     * @param artifact the {@link ProducedArtifact} to install
     * @throws ArtifactInstallerException In case of an error which can be the a given artifact cannot be found or the
     *             installation has failed.
     * @throws IllegalArgumentException in case of parameter {@code session} is {@code null} or
     *          {@code artifact} is {@code null}.
     */
    default void install(Session session, ProducedArtifact artifact) {
        install(session, Collections.singletonList(artifact));
    }

    /**
     * @param session the repository session
     * @param artifacts Collection of {@link ProducedArtifact MavenArtifacts}
     * @throws ArtifactInstallerException In case of an error which can be the a given artifact cannot be found or the
     *             installation has failed.
     * @throws IllegalArgumentException in case of parameter {@code request} is {@code null} or parameter
     *             {@code localRepository} is {@code null} or {@code localRepository} is not a directory
     *             or parameter {@code mavenArtifacts} is {@code null} or
     *             {@code mavenArtifacts.isEmpty()} is {@code true}.
     */
    default void install(Session session, Collection<ProducedArtifact> artifacts) {
        install(ArtifactInstallerRequest.build(session, artifacts));
    }
}
