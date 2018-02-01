package org.apache.maven.repository;

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

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.eclipse.aether.RepositorySystemSession;

/**
 * @author Jason van Zyl
 * @since 3.0-alpha
 */
public interface RepositorySystem
{
    String DEFAULT_LOCAL_REPO_ID = "local";

    @SuppressWarnings( "checkstyle:constantname" )
    String userHome = System.getProperty( "user.home" );

    @SuppressWarnings( "checkstyle:constantname" )
    File userMavenConfigurationHome = new File( userHome, ".m2" );

    @SuppressWarnings( "checkstyle:constantname" )
    File defaultUserLocalRepository = new File( userMavenConfigurationHome, "repository" );

    String DEFAULT_REMOTE_REPO_ID = "central";

    String DEFAULT_REMOTE_REPO_URL = "https://repo.maven.apache.org/maven2";

    Artifact createArtifact( String groupId, String artifactId, String version, String packaging );

    Artifact createArtifact( String groupId, String artifactId, String version, String scope, String type );

    Artifact createProjectArtifact( String groupId, String artifactId, String version );

    Artifact createArtifactWithClassifier( String groupId, String artifactId, String version, String type,
                                           String classifier );

    Artifact createPluginArtifact( Plugin plugin );

    Artifact createDependencyArtifact( Dependency dependency );

    ArtifactRepository buildArtifactRepository( Repository repository )
        throws InvalidRepositoryException;

    ArtifactRepository createDefaultRemoteRepository()
        throws InvalidRepositoryException;

    ArtifactRepository createDefaultLocalRepository()
        throws InvalidRepositoryException;

    ArtifactRepository createLocalRepository( File localRepository )
        throws InvalidRepositoryException;

    ArtifactRepository createArtifactRepository( String id, String url, ArtifactRepositoryLayout repositoryLayout,
                                                 ArtifactRepositoryPolicy snapshots,
                                                 ArtifactRepositoryPolicy releases );

    /**
     * Calculates the effective repositories for the given input repositories which are assumed to be already mirrored
     * (if applicable). This process will essentially remove duplicate repositories by merging them into one equivalent
     * repository. It is worth to point out that merging does not simply choose one of the input repositories and
     * discards the others but actually combines their possibly different policies.
     *
     * @param repositories The original repositories, may be {@code null}.
     * @return The effective repositories or {@code null} if the input was {@code null}.
     */
    List<ArtifactRepository> getEffectiveRepositories( List<ArtifactRepository> repositories );

    /**
     * Determines the mirror for the specified repository.
     *
     * @param repository The repository to determine the mirror for, must not be {@code null}.
     * @param mirrors The available mirrors, may be {@code null}.
     * @return The mirror specification for the repository or {@code null} if no mirror matched.
     */
    Mirror getMirror( ArtifactRepository repository, List<Mirror> mirrors );

    /**
     * Injects the mirroring information into the specified repositories. For each repository that is matched by a
     * mirror, its URL and ID will be updated to match the values from the mirror specification. Repositories without a
     * matching mirror will pass through unchanged. <em>Note:</em> This method must be called before
     * {@link #injectAuthentication(List, List)} or the repositories will end up with the wrong credentials.
     *
     * @param repositories The repositories into which to inject the mirror information, may be {@code null}.
     * @param mirrors The available mirrors, may be {@code null}.
     */
    void injectMirror( List<ArtifactRepository> repositories, List<Mirror> mirrors );

    /**
     * Injects the proxy information into the specified repositories. For each repository that is matched by a proxy,
     * its proxy data will be set accordingly. Repositories without a matching proxy will have their proxy cleared.
     * <em>Note:</em> This method must be called after {@link #injectMirror(List, List)} or the repositories will end up
     * with the wrong proxies.
     *
     * @param repositories The repositories into which to inject the proxy information, may be {@code null}.
     * @param proxies The available proxies, may be {@code null}.
     */
    void injectProxy( List<ArtifactRepository> repositories, List<org.apache.maven.settings.Proxy> proxies );

    /**
     * Injects the authentication information into the specified repositories. For each repository that is matched by a
     * server, its credentials will be updated to match the values from the server specification. Repositories without a
     * matching server will have their credentials cleared. <em>Note:</em> This method must be called after
     * {@link #injectMirror(List, List)} or the repositories will end up with the wrong credentials.
     *
     * @param repositories The repositories into which to inject the authentication information, may be {@code null}.
     * @param servers The available servers, may be {@code null}.
     */
    void injectAuthentication( List<ArtifactRepository> repositories, List<Server> servers );

    void injectMirror( RepositorySystemSession session, List<ArtifactRepository> repositories );

    void injectProxy( RepositorySystemSession session, List<ArtifactRepository> repositories );

    void injectAuthentication( RepositorySystemSession session, List<ArtifactRepository> repositories );

    ArtifactResolutionResult resolve( ArtifactResolutionRequest request );

    // Install

    // Deploy

    // Map types of artifacts

    //
    // Raw file transfers
    //
    void publish( ArtifactRepository repository, File source, String remotePath,
                  ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException;

    void retrieve( ArtifactRepository repository, File destination, String remotePath,
                   ArtifactTransferListener transferListener )
        throws ArtifactTransferFailedException, ArtifactDoesNotExistException;

}
