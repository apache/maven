package org.apache.maven.api;

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

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.DependencyCoordinateFactory;
import org.apache.maven.api.settings.Settings;

/**
 * The session to install / deploy / resolve artifacts and dependencies.
 *
 * @since 4.0
 */
@Experimental
@ThreadSafe
public interface Session
{

    @Nonnull
    Settings getSettings();

    @Nonnull
    LocalRepository getLocalRepository();

    @Nonnull
    List<RemoteRepository> getRemoteRepositories();

    @Nonnull
    SessionData getData();

    @Nonnull
    Map<String, String> getUserProperties();

    @Nonnull
    Map<String, String> getSystemProperties();

    /**
     * Returns the current maven version
     * @return the maven version, never {@code null}.
     */
    @Nonnull
    String getMavenVersion();

    int getDegreeOfConcurrency();

    @Nonnull
    Instant getStartTime();

    @Nonnull
    Path getMultiModuleProjectDirectory();

    @Nonnull
    Path getExecutionRootDirectory();

    @Nonnull
    List<Project> getProjects();

    /**
     * Returns the plugin context for mojo being executed and the specified
     * {@link Project}, never returns {@code null} as if context not present, creates it.
     *
     * <strong>Implementation note:</strong> while this method return type is {@link Map}, the returned map instance
     * implements {@link java.util.concurrent.ConcurrentMap} as well.
     *
     * @throws org.apache.maven.api.services.MavenException if not called from the within a mojo execution
     */
    @Nonnull
    Map<String, Object> getPluginContext( @Nonnull Project project );

    /**
     * Retrieves the service for the interface
     *
     * @throws NoSuchElementException if the service could not be found
     */
    @Nonnull
    <T extends Service> T getService( @Nonnull Class<T> clazz );

    /**
     * Creates a derived session using the given local repository.
     *
     * @param localRepository the new local repository
     * @return the derived session
     * @throws NullPointerException if {@code localRepository} is null
     */
    @Nonnull
    Session withLocalRepository( @Nonnull LocalRepository localRepository );

    /**
     * Creates a derived session using the given remote repositories.
     *
     * @param repositories the new list of remote repositories
     * @return the derived session
     * @throws NullPointerException if {@code repositories} is null
     */
    @Nonnull
    Session withRemoteRepositories( @Nonnull List<RemoteRepository> repositories );

    /**
     * Register the given listener which will receive all events.
     *
     * @param listener the listener to register
     * @throws NullPointerException if {@code listener} is null
     */
    void registerListener( @Nonnull Listener listener );

    /**
     * Unregisters a previously registered listener.
     *
     * @param listener the listener to unregister
     * @throws NullPointerException if {@code listener} is null
     */
    void unregisterListener( @Nonnull Listener listener );

    /**
     * Returns the list of registered listeners.
     *
     * @return an immutable collection of listeners, never {@code null}
     */
    @Nonnull
    Collection<Listener> getListeners();

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createLocal(...)</code>
     * @see org.apache.maven.api.services.RepositoryFactory#createLocal(Path)
     */
    LocalRepository createLocalRepository( Path path );

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     * @see org.apache.maven.api.services.RepositoryFactory#createRemote(String, String)
     */
    @Nonnull
    RemoteRepository createRemoteRepository( @Nonnull String id, @Nonnull String url );

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     * @see org.apache.maven.api.services.RepositoryFactory#createRemote(Repository)
     */
    @Nonnull
    RemoteRepository createRemoteRepository( @Nonnull Repository repository );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate( String groupId, String artifactId, String version, String extension );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate( String groupId, String artifactId, String version, String classifier,
                                                 String extension, String type );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate( Artifact artifact );

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     * @see DependencyCoordinateFactory#create(Session, ArtifactCoordinate)
     */
    @Nonnull
    DependencyCoordinate createDependencyCoordinate( @Nonnull ArtifactCoordinate coordinate );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String)
     */
    Artifact createArtifact( String groupId, String artifactId, String version, String extension );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    Artifact createArtifact( String groupId, String artifactId, String version, String classifier,
                             String extension, String type );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Artifact resolveArtifact( ArtifactCoordinate coordinate );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Collection<Artifact> resolveArtifacts( ArtifactCoordinate... coordinates );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Collection<Artifact> resolveArtifacts( Collection<? extends ArtifactCoordinate> coordinates );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Artifact resolveArtifact( Artifact artifact );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Collection<Artifact> resolveArtifacts( Artifact... artifacts );

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     * @see org.apache.maven.api.services.ArtifactInstaller#install(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactInstallerException if the artifacts installation failed
     */
    void installArtifacts( Artifact... artifacts );

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     * @see org.apache.maven.api.services.ArtifactInstaller#install(Session, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactInstallerException if the artifacts installation failed
     */
    void installArtifacts( Collection<Artifact> artifacts );

    /**
     * Shortcut for <code>getService(ArtifactDeployer.class).deploy(...)</code>
     * @see org.apache.maven.api.services.ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     *
     * @throws org.apache.maven.api.services.ArtifactDeployerException if the artifacts deployment failed
     */
    void deployArtifact( RemoteRepository repository, Artifact... artifacts );

    /**
     * Shortcut for <code>getService(ArtifactManager.class).setPath(...)</code>
     * @see org.apache.maven.api.services.ArtifactManager#setPath(Artifact, Path)
     */
    void setArtifactPath( @Nonnull Artifact artifact, @Nonnull Path path );

    /**
     * Shortcut for <code>getService(ArtifactManager.class).getPath(...)</code>
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    Optional<Path> getArtifactPath( @Nonnull Artifact artifact );

    /**
     * Shortcut for <code>getService(ArtifactManager.class).isSnapshot(...)</code>
     * @see org.apache.maven.api.services.VersionParser#isSnapshot(String)
     */
    boolean isVersionSnapshot( @Nonnull String version );

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, Artifact)
     *
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies( @Nonnull Artifact artifact );

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, Project)
     *
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies( @Nonnull Project project );

    /**
     * Shortcut for <code>getService(DependencyResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, DependencyCoordinate)
     *
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies( @Nonnull DependencyCoordinate dependency );

    Path getPathForLocalArtifact( @Nonnull Artifact artifact );

    Path getPathForLocalMetadata( Metadata metadata );

    Path getPathForRemoteArtifact( RemoteRepository remote, Artifact artifact );

    Path getPathForRemoteMetadata( RemoteRepository remote, Metadata metadata );

    /**
     * Shortcut for <code>getService(VersionParser.class).parseVersion(...)</code>
     * @see org.apache.maven.api.services.VersionParser#parseVersion(String)
     *
     * @throws org.apache.maven.api.services.VersionParserException if the parsing failed
     */
    @Nonnull
    Version parseVersion( @Nonnull String version );

    /**
     * Shortcut for <code>getService(VersionParser.class).parseVersionRange(...)</code>
     * @see org.apache.maven.api.services.VersionParser#parseVersionRange(String)
     *
     * @throws org.apache.maven.api.services.VersionParserException if the parsing failed
     */
    @Nonnull
    VersionRange parseVersionRange( @Nonnull String versionRange );
}
