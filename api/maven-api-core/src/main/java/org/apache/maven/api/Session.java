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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.apache.maven.api.model.Repository;
import org.apache.maven.api.settings.Settings;

/**
 * The session to install / deploy / resolve artifacts and dependencies.
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
    Properties getUserProperties();

    @Nonnull
    Properties getSystemProperties();

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
    Artifact createArtifact( String groupId, String artifactId, String version, String extension );

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    Artifact createArtifact( String groupId, String artifactId, String version, String classifier,
                             String extension, String type );

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Artifact)
     *
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Artifact resolveArtifact( Artifact artifact );

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
     * @see org.apache.maven.api.services.ArtifactManager#isSnapshot(String)
     */
    boolean isVersionSnapshot( @Nonnull String version );

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     * @see org.apache.maven.api.services.DependencyFactory#create(Session, Artifact)
     */
    @Nonnull
    Dependency createDependency( @Nonnull Artifact artifact );

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
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, Dependency)
     *
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies( @Nonnull Dependency dependency );

    /**
     * Shortcut for <code>getService(DependencyResolver.class).resolve(...)</code>
     * @see org.apache.maven.api.services.DependencyResolver#resolve(Session, Dependency, Predicate)
     *
     * @throws org.apache.maven.api.services.DependencyResolverException if the dependency resolution failed
     */
    @Nonnull
    Node resolveDependencies( @Nonnull Dependency dependency );

    Path getPathForLocalArtifact( @Nonnull Artifact artifact );

    Path getPathForLocalMetadata( Metadata metadata );

    Path getPathForRemoteArtifact( RemoteRepository remote, Artifact artifact );

    Path getPathForRemoteMetadata( RemoteRepository remote, Metadata metadata );

}
