package org.apache.maven.artifact.repository;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.Proxy;

/**
 * Abstraction of an artifact repository. Artifact repositories can be remote, local, or even build reactor or
 * IDE workspace.
 */
public interface ArtifactRepository
{
    String pathOf( Artifact artifact );

    String pathOfRemoteRepositoryMetadata( ArtifactMetadata artifactMetadata );

    String pathOfLocalRepositoryMetadata( ArtifactMetadata metadata, ArtifactRepository repository );

    String getUrl();

    void setUrl( String url );

    String getBasedir();

    String getProtocol();

    String getId();

    void setId( String id );

    ArtifactRepositoryPolicy getSnapshots();

    void setSnapshotUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryPolicy getReleases();

    void setReleaseUpdatePolicy( ArtifactRepositoryPolicy policy );

    ArtifactRepositoryLayout getLayout();

    void setLayout( ArtifactRepositoryLayout layout );

    String getKey();

    @Deprecated
    boolean isUniqueVersion();

    @Deprecated
    boolean isBlacklisted();

    @Deprecated
    void setBlacklisted( boolean blackListed );

    /** @since 3.8.1 **/
    boolean isBlocked();

    /** @since 3.8.1 **/
    void setBlocked( boolean blocked );

    //
    // New interface methods for the repository system.
    //
    /**
     *
     * @param artifact
     * @since 3.0-alpha-3
     */
    Artifact find( Artifact artifact );

    /**
     * Finds the versions of the specified artifact that are available in this repository.
     *
     * @param artifact The artifact whose available versions should be determined, must not be {@code null}.
     * @return The available versions of the artifact or an empty list if none, never {@code null}.
     * @since 3.0-alpha-3
     */
    List<String> findVersions( Artifact artifact );

    /**
     * Indicates whether this repository is backed by actual projects. For instance, the build reactor or IDE workspace
     * are examples of such repositories.
     *
     * @return {@code true} if the repository is backed by actual projects, {@code false} otherwise.
     * @since 3.0-beta-1
     */
    boolean isProjectAware();

    /**
     * @since 3.0-alpha-3
     */
    void setAuthentication( Authentication authentication );
    /**
     * @since 3.0-alpha-3
     */
    Authentication getAuthentication();

    /**
     * @since 3.0-alpha-3
     */
    void setProxy( Proxy proxy );
    /**
     * @since 3.0-alpha-3
     */
    Proxy getProxy();

    /**
     * @since 3.0.3
     * @return the repositories mirrored by the actual one
     */
    List<ArtifactRepository> getMirroredRepositories();

    /**
     * @since 3.0.3
     * @param mirroredRepositories the repositories that the actual one mirrors
     */
    void setMirroredRepositories( List<ArtifactRepository> mirroredRepositories );

}
