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
package org.apache.maven.api;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.DependencyCoordinateFactory;
import org.apache.maven.api.settings.Settings;

/**
 * The session to install / deploy / resolve artifacts and dependencies.
 *
 * @since 4.0.0
 */
@Experimental
@ThreadSafe
public interface Session {

    @Nonnull
    Settings getSettings();

    @Nonnull
    LocalRepository getLocalRepository();

    @Nonnull
    List<RemoteRepository> getRemoteRepositories();

    @Nonnull
    SessionData getData();

    /**
     * Returns immutable user properties to use for interpolation. The user properties have been configured directly
     * by the user, e.g. via the {@code -Dkey=value} parameter on the command line.
     *
     * @return the user properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getUserProperties();

    /**
     * Returns immutable system properties to use for interpolation. The system properties are collected from the
     * runtime environment such as {@link System#getProperties()} and environment variables
     * (prefixed with {@code env.}).
     *
     * @return the system properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getSystemProperties();

    /**
     * Each invocation computes a new map of effective properties. To be used in interpolation.
     * <p>
     * Effective properties are computed from system, user and optionally project properties, layered with
     * defined precedence onto each other to achieve proper precedence. Precedence is defined as:
     * <ul>
     *     <li>System properties (lowest precedence)</li>
     *     <li>Project properties (optional)</li>
     *     <li>User properties (highest precedence)</li>
     * </ul>
     * Note: Project properties contains properties injected from profiles, if applicable. Their precedence is
     * {@code profile > project}, hence active profile property may override project property.
     * <p>
     * The caller of this method should decide whether there is a project in scope (hence, a project instance
     * needs to be passed) or not.
     *
     * @param project {@link Project} or {@code null}.
     * @return the effective properties, never {@code null}
     */
    @Nonnull
    Map<String, String> getEffectiveProperties(@Nullable Project project);

    /**
     * Returns the current maven version
     * @return the maven version, never {@code null}
     */
    @Nonnull
    Version getMavenVersion();

    int getDegreeOfConcurrency();

    @Nonnull
    Instant getStartTime();

    /**
     * Gets the directory of the topmost project being built, usually the current directory or the
     * directory pointed at by the {@code -f/--file} command line argument.
     */
    @Nonnull
    Path getTopDirectory();

    /**
     * Gets the root directory of the session, which is the root directory for the top directory project.
     *
     * @throws IllegalStateException if the root directory could not be found
     * @see #getTopDirectory()
     * @see Project#getRootDirectory()
     */
    @Nonnull
    Path getRootDirectory();

    @Nonnull
    List<Project> getProjects();

    /**
     * Returns the plugin context for mojo being executed and the specified
     * {@link Project}, never returns {@code null} as if context not present, creates it.
     *
     * <strong>Implementation note:</strong> while this method return type is {@link Map}, the
     * returned map instance implements {@link java.util.concurrent.ConcurrentMap} as well.
     *
     * @throws org.apache.maven.api.services.MavenException if not called from the within a mojo execution
     */
    @Nonnull
    Map<String, Object> getPluginContext(@Nonnull Project project);

    /**
     * Retrieves the service for the interface
     *
     * @throws NoSuchElementException if the service could not be found
     */
    @Nonnull
    <T extends Service> T getService(@Nonnull Class<T> clazz);

    /**
     * Creates a derived session using the given local repository.
     *
     * @param localRepository the new local repository
     * @return the derived session
     * @throws NullPointerException if {@code localRepository} is null
     */
    @Nonnull
    Session withLocalRepository(@Nonnull LocalRepository localRepository);

    /**
     * Creates a derived session using the given remote repositories.
     *
     * @param repositories the new list of remote repositories
     * @return the derived session
     * @throws NullPointerException if {@code repositories} is null
     */
    @Nonnull
    Session withRemoteRepositories(@Nonnull List<RemoteRepository> repositories);

    /**
     * Register the given listener which will receive all events.
     *
     * @param listener the listener to register
     * @throws NullPointerException if {@code listener} is null
     */
    void registerListener(@Nonnull Listener listener);

    /**
     * Unregisters a previously registered listener.
     *
     * @param listener the listener to unregister
     * @throws NullPointerException if {@code listener} is null
     */
    void unregisterListener(@Nonnull Listener listener);

    /**
     * Returns the list of registered listeners.
     *
     * @return an immutable collection of listeners, never {@code null}
     */
    @Nonnull
    Collection<Listener> getListeners();

    /**
     * Shortcut for {@code getService(RepositoryFactory.class).createLocal(...)}.
     *
     * @see org.apache.maven.api.services.RepositoryFactory#createLocal(Path)
     */
    LocalRepository createLocalRepository(Path path);

    /**
     * Shortcut for {@code getService(RepositoryFactory.class).createRemote(...)}.
     *
     * @see org.apache.maven.api.services.RepositoryFactory#createRemote(String, String)
     */
    @Nonnull
    RemoteRepository createRemoteRepository(@Nonnull String id, @Nonnull String url);

    /**
     * Shortcut for {@code getService(RepositoryFactory.class).createRemote(...)}.
     *
     * @see org.apache.maven.api.services.RepositoryFactory#createRemote(Repository)
     */
    @Nonnull
    RemoteRepository createRemoteRepository(@Nonnull Repository repository);

    /**
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactCoordinateFactory#create(Session, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate(String groupId, String artifactId, String version, String extension);

    /**
     * Creates a coordinate out of string that is formatted like:
     * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     * <p>
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @param coordString the string having "standard" coordinate.
     * @return an {@code ArtifactCoordinate}, never {@code null}
     * @see org.apache.maven.api.services.ArtifactCoordinateFactory#create(Session, String)
     */
    ArtifactCoordinate createArtifactCoordinate(String coordString);

    /**
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactCoordinateFactory#create(Session, String, String, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate(
            String groupId, String artifactId, String version, String classifier, String extension, String type);

    /**
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactCoordinateFactory#create(Session, String, String, String, String, String, String)
     */
    ArtifactCoordinate createArtifactCoordinate(Artifact artifact);

    /**
     * Shortcut for {@code getService(DependencyFactory.class).create(...)}.
     *
     * @see DependencyCoordinateFactory#create(Session, ArtifactCoordinate)
     */
    @Nonnull
    DependencyCoordinate createDependencyCoordinate(@Nonnull ArtifactCoordinate coordinate);

    /**
     * Shortcut for {@code getService(DependencyFactory.class).create(...)}.
     *
     * @see DependencyCoordinateFactory#create(Session, Dependency)
     */
    @Nonnull
    DependencyCoordinate createDependencyCoordinate(@Nonnull Dependency dependency);

    /**
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String)
     */
    Artifact createArtifact(String groupId, String artifactId, String version, String extension);

    /**
     * Shortcut for {@code getService(ArtifactFactory.class).create(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    Artifact createArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type);

    /**
     * Shortcut for {@code getService(ArtifactResolver.class).resolve(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Map.Entry<Artifact, Path> resolveArtifact(ArtifactCoordinate coordinate);

    /**
     * Shortcut for {@code getService(ArtifactResolver.class).resolve(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Map<Artifact, Path> resolveArtifacts(ArtifactCoordinate... coordinates);

    /**
     * Shortcut for {@code getService(ArtifactResolver.class).resolve(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Map<Artifact, Path> resolveArtifacts(Collection<? extends ArtifactCoordinate> coordinates);

    /**
     * Shortcut for {@code getService(ArtifactResolver.class).resolve(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Map.Entry<Artifact, Path> resolveArtifact(Artifact artifact);

    /**
     * Shortcut for {@code getService(ArtifactResolver.class).resolve(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactResolver#resolve(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactResolverException if the artifact resolution failed
     */
    Map<Artifact, Path> resolveArtifacts(Artifact... artifacts);

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactInstaller#install(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactInstallerException if the artifacts installation failed
     */
    void installArtifacts(Artifact... artifacts);

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactInstaller#install(Session, Collection)
     * @throws org.apache.maven.api.services.ArtifactInstallerException if the artifacts installation failed
     */
    void installArtifacts(Collection<Artifact> artifacts);

    /**
     * Shortcut for {@code getService(ArtifactDeployer.class).deploy(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     * @throws org.apache.maven.api.services.ArtifactDeployerException if the artifacts deployment failed
     */
    void deployArtifact(RemoteRepository repository, Artifact... artifacts);

    /**
     * Shortcut for {@code getService(ArtifactManager.class).setPath(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactManager#setPath(Artifact, Path)
     */
    void setArtifactPath(@Nonnull Artifact artifact, @Nonnull Path path);

    /**
     * Shortcut for {@code getService(ArtifactManager.class).getPath(...)}.
     *
     * @see org.apache.maven.api.services.ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    Optional<Path> getArtifactPath(@Nonnull Artifact artifact);

    /**
     * Gets the relative path for a locally installed artifact. Note that the artifact need not actually exist yet at
     * the returned location, the path merely indicates where the artifact would eventually be stored.
     * <p>
     * Shortcut for {@code getService(LocalArtifactManager.class).getPathForLocalArtitact(...)}.
     *
     * @see org.apache.maven.api.services.LocalRepositoryManager#getPathForLocalArtifact(Session, LocalRepository, Artifact)
     */
    Path getPathForLocalArtifact(@Nonnull Artifact artifact);

    /**
     * Gets the relative path for an artifact cached from a remote repository.
     * Note that the artifact need not actually exist yet at the returned location,
     * the path merely indicates where the artifact would eventually be stored.
     * <p>
     * Shortcut for {@code getService(LocalArtifactManager.class).getPathForRemoteArtifact(...)}.
     *
     * @see org.apache.maven.api.services.LocalRepositoryManager#getPathForRemoteArtifact(Session, LocalRepository, RemoteRepository, Artifact)
     */
    @Nonnull
    Path getPathForRemoteArtifact(@Nonnull RemoteRepository remote, @Nonnull Artifact artifact);

    /**
     * Checks whether a given artifact version is considered a {@code SNAPSHOT} or not.
     * <p>
     * Shortcut for {@code getService(ArtifactManager.class).isSnapshot(...)}.
     * <p>
     * In case there is {@link Artifact} in scope, the recommended way to perform this check is
     * use of {@link Artifact#isSnapshot()} instead.
     *
     * @see org.apache.maven.api.services.VersionParser#isSnapshot(String)
     */
    boolean isVersionSnapshot(@Nonnull String version);

    /**
     * Shortcut for {@code getService(DependencyCollector.class).collect(...)}
     *
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, Artifact)
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies(@Nonnull Artifact artifact);

    /**
     * Shortcut for {@code getService(DependencyCollector.class).collect(...)}
     *
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, Project)
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies(@Nonnull Project project);

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph. Note that this operation is
     * only concerned about determining the coordinates of the transitive dependencies and does not actually resolve the
     * artifact files.
     * <p>
     * Shortcut for {@code getService(DependencyCollector.class).resolve(...)}
     *
     * @see org.apache.maven.api.services.DependencyCollector#collect(Session, DependencyCoordinate)
     * @throws org.apache.maven.api.services.DependencyCollectorException if the dependency collection failed
     */
    @Nonnull
    Node collectDependencies(@Nonnull DependencyCoordinate dependency);

    /**
     * Shortcut for {@code getService(DependencyResolver.class).flatten(...)}.
     *
     * @see org.apache.maven.api.services.DependencyResolver#flatten(Session, Node, PathScope)
     * @throws org.apache.maven.api.services.DependencyResolverException if the dependency flattening failed
     */
    @Nonnull
    List<Node> flattenDependencies(@Nonnull Node node, @Nonnull PathScope scope);

    @Nonnull
    List<Path> resolveDependencies(@Nonnull DependencyCoordinate dependencyCoordinate);

    @Nonnull
    List<Path> resolveDependencies(@Nonnull List<DependencyCoordinate> dependencyCoordinates);

    @Nonnull
    List<Path> resolveDependencies(@Nonnull Project project, @Nonnull PathScope scope);

    /**
     * Resolves an artifact's meta version (if any) to a concrete version. For example, resolves "1.0-SNAPSHOT"
     * to "1.0-20090208.132618-23" or "RELEASE"/"LATEST" to "2.0".
     * <p>
     * Shortcut for {@code getService(VersionResolver.class).resolve(...)}
     *
     * @see org.apache.maven.api.services.VersionResolver#resolve(Session, ArtifactCoordinate) (String)
     * @throws org.apache.maven.api.services.VersionResolverException if the resolution failed
     */
    @Nonnull
    Version resolveVersion(@Nonnull ArtifactCoordinate artifact);

    /**
     * Expands a version range to a list of matching versions, in ascending order.
     * For example, resolves "[3.8,4.0)" to "3.8", "3.8.1", "3.8.2".
     * The returned list of versions is only dependent on the configured repositories and their contents.
     * The supplied request may also refer to a single concrete version rather than a version range.
     * In this case though, the result contains simply the (parsed) input version, regardless of the
     * repositories and their contents.
     *
     * @return a list of resolved {@code Version}s.
     * @see org.apache.maven.api.services.VersionRangeResolver#resolve(Session, ArtifactCoordinate) (String)
     * @throws org.apache.maven.api.services.VersionRangeResolverException if the resolution failed
     */
    @Nonnull
    List<Version> resolveVersionRange(@Nonnull ArtifactCoordinate artifact);

    /**
     * Parses the specified version string, for example "1.0".
     * <p>
     * Shortcut for {@code getService(VersionParser.class).parseVersion(...)}.
     *
     * @see org.apache.maven.api.services.VersionParser#parseVersion(String)
     * @throws org.apache.maven.api.services.VersionParserException if the parsing failed
     */
    @Nonnull
    Version parseVersion(@Nonnull String version);

    /**
     * Parses the specified version range specification, for example "[1.0,2.0)".
     * <p>
     * Shortcut for {@code getService(VersionParser.class).parseVersionRange(...)}.
     *
     * @see org.apache.maven.api.services.VersionParser#parseVersionRange(String)
     * @throws org.apache.maven.api.services.VersionParserException if the parsing failed
     */
    @Nonnull
    VersionRange parseVersionRange(@Nonnull String versionRange);

    /**
     * Parses the specified version constraint specification, for example "1.0" or "[1.0,2.0)".
     * <p>
     * Shortcut for {@code getService(VersionParser.class).parseVersionConstraint(...)}.
     *
     * @see org.apache.maven.api.services.VersionParser#parseVersionConstraint(String)
     * @throws org.apache.maven.api.services.VersionParserException if the parsing failed
     */
    @Nonnull
    VersionConstraint parseVersionConstraint(@Nonnull String versionConstraint);

    Type requireType(String id);

    Language requireLanguage(String id);

    Packaging requirePackaging(String id);

    ProjectScope requireProjectScope(String id);

    DependencyScope requireDependencyScope(String id);

    PathScope requirePathScope(String id);
}
