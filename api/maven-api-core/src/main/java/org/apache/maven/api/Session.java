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

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.apache.maven.api.services.DependencyCollector;
import org.apache.maven.api.services.DependencyCollectorException;
import org.apache.maven.api.services.DependencyCollectorResult;
import org.apache.maven.api.services.DependencyFactory;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverResult;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.Service;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.settings.Settings;

/**
 * The session to install / deploy / resolve artifacts and dependencies.
 */
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
     * Retrieves the service for the interface
     *
     * @throws NoSuchElementException if the service could not be found
     */
    @Nonnull
    <T extends Service> T getService( Class<T> clazz );

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
     * @see RepositoryFactory#createLocal(Path)
     */
    default LocalRepository createLocalRepository( Path path )
    {
        return getService( RepositoryFactory.class ).createLocal( path );
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     * @see RepositoryFactory#createRemote(String, String)
     */
    @Nonnull
    default RemoteRepository createRemoteRepository( @Nonnull String id, @Nonnull String url )
    {
        return getService( RepositoryFactory.class )
                .createRemote( id, url );
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     * @see RepositoryFactory#createRemote(Repository)
     */
    @Nonnull
    default RemoteRepository createRemoteRepository( @Nonnull Repository repository )
    {
        return getService( RepositoryFactory.class )
                .createRemote( repository );
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see ArtifactFactory#create(Session, String, String, String, String)
     */
    default Artifact createArtifact( String groupId, String artifactId, String version, String extension )
    {
        return getService( ArtifactFactory.class )
                .create( this, groupId, artifactId, version, extension );
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     * @see ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    default Artifact createArtifact( String groupId, String artifactId, String version, String classifier,
                                     String extension, String type )
    {
        return getService( ArtifactFactory.class )
                .create( this, groupId, artifactId, version, classifier, extension, type );
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     * @see ArtifactResolver#resolve(Session, Artifact)
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     */
    default ArtifactResolverResult resolveArtifact( Artifact artifact )
    {
        return getService( ArtifactResolver.class )
                .resolve( this, artifact );
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     * @see ArtifactInstaller#install(Session, Collection)
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     */
    default void installArtifacts( Artifact... artifacts )
    {
        installArtifacts( Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     * @see ArtifactInstaller#install(Session, Collection)
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     */
    default void installArtifacts( Collection<Artifact> artifacts )
    {
        getService( ArtifactInstaller.class )
                .install( this, artifacts );
    }

    /**
     * Shortcut for <code>getService(ArtifactDeployer.class).deploy(...)</code>
     * @see ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     *
     * @throws ArtifactDeployerException if the artifacts deployment failed
     */
    default void deployArtifact( RemoteRepository repository, Artifact... artifacts )
    {
        getService( ArtifactDeployer.class )
                .deploy( this, repository, Arrays.asList( artifacts ) );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).setPath(...)</code>
     * @see ArtifactManager#setPath(Artifact, Path)
     */
    default void setArtifactPath( @Nonnull Artifact artifact, @Nonnull Path path )
    {
        getService( ArtifactManager.class )
                .setPath( artifact, path );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).getPath(...)</code>
     * @see ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    default Optional<Path> getArtifactPath( @Nonnull Artifact artifact )
    {
        return getService( ArtifactManager.class )
                .getPath( artifact );
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).isSnapshot(...)</code>
     * @see ArtifactManager#isSnapshot(String)
     */
    default boolean isVersionSnapshot( @Nonnull String version )
    {
        return getService( ArtifactManager.class )
                .isSnapshot( version );
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     * @see DependencyFactory#create(Session, Artifact)
     */
    @Nonnull
    default Dependency createDependency( @Nonnull Artifact artifact )
    {
        return getService( DependencyFactory.class )
                .create( this, artifact );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see DependencyCollector#collect(Session, Artifact)
     *
     * @throws DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    default DependencyCollectorResult collectDependencies( @Nonnull Artifact artifact )
    {
        return getService( DependencyCollector.class )
                .collect( this, artifact );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see DependencyCollector#collect(Session, Project)
     *
     * @throws DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    default DependencyCollectorResult collectDependencies( @Nonnull Project project )
    {
        return getService( DependencyCollector.class )
                .collect( this, project );
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     * @see DependencyCollector#collect(Session, Dependency)
     *
     * @throws DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    default DependencyCollectorResult collectDependencies( @Nonnull Dependency dependency )
    {
        return getService( DependencyCollector.class )
                .collect( this, dependency );
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).resolve(...)</code>
     * @see DependencyResolver#resolve(Session, Dependency, Predicate)
     *
     * @throws DependencyResolverException if the dependency resolution failed
     */
    @Nonnull
    default DependencyResolverResult resolveDependencies( @Nonnull Dependency dependency )
    {
        return getService( DependencyResolver.class )
                .resolve( this, dependency, null );
    }

    default Path getPathForLocalArtifact( @Nonnull Artifact artifact )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForLocalArtifact( this, getLocalRepository(), artifact );
    }

    default Path getPathForLocalMetadata( Metadata metadata )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForLocalMetadata( this, getLocalRepository(), metadata );
    }

    default Path getPathForRemoteArtifact( RemoteRepository remote, Artifact artifact )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForRemoteArtifact( this, getLocalRepository(), remote, artifact );
    }

    default Path getPathForRemoteMetadata( RemoteRepository remote, Metadata metadata )
    {
        return getService( LocalRepositoryManager.class )
                .getPathForRemoteMetadata( this, getLocalRepository(), remote, metadata );
    }

}
