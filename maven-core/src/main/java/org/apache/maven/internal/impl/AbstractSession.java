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
package org.apache.maven.internal.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.maven.api.*;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.*;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;

import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

public abstract class AbstractSession implements InternalSession {

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<org.eclipse.aether.graph.DependencyNode, Node> allNodes =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<org.eclipse.aether.artifact.Artifact, Artifact> allArtifacts =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<org.eclipse.aether.repository.RemoteRepository, RemoteRepository> allRepositories =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<String, Project> allProjects = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<org.eclipse.aether.graph.Dependency, Dependency> allDependencies =
            Collections.synchronizedMap(new WeakHashMap<>());

    public RemoteRepository getRemoteRepository(org.eclipse.aether.repository.RemoteRepository repository) {
        return allRepositories.computeIfAbsent(repository, DefaultRemoteRepository::new);
    }

    public Node getNode(org.eclipse.aether.graph.DependencyNode node) {
        return getNode(node, false);
    }

    public Node getNode(org.eclipse.aether.graph.DependencyNode node, boolean verbose) {
        return allNodes.computeIfAbsent(node, n -> new DefaultNode(this, n, verbose));
    }

    @Nonnull
    public Artifact getArtifact(@Nonnull org.eclipse.aether.artifact.Artifact artifact) {
        return allArtifacts.computeIfAbsent(artifact, a -> new DefaultArtifact(this, a));
    }

    @Nonnull
    public Dependency getDependency(@Nonnull org.eclipse.aether.graph.Dependency dependency) {
        return allDependencies.computeIfAbsent(dependency, d -> new DefaultDependency(this, d));
    }

    public List<Project> getProjects(List<MavenProject> projects) {
        return projects == null ? null : map(projects, this::getProject);
    }

    public Project getProject(MavenProject project) {
        return allProjects.computeIfAbsent(project.getId(), id -> new DefaultProject(this, project));
    }

    public List<org.eclipse.aether.repository.RemoteRepository> toRepositories(List<RemoteRepository> repositories) {
        return repositories == null ? null : map(repositories, this::toRepository);
    }

    public org.eclipse.aether.repository.RemoteRepository toRepository(RemoteRepository repository) {
        if (repository instanceof DefaultRemoteRepository) {
            return ((DefaultRemoteRepository) repository).getRepository();
        } else {
            // TODO
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public org.eclipse.aether.repository.LocalRepository toRepository(LocalRepository repository) {
        if (repository instanceof DefaultLocalRepository) {
            return ((DefaultLocalRepository) repository).getRepository();
        } else {
            // TODO
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    public List<ArtifactRepository> toArtifactRepositories(List<RemoteRepository> repositories) {
        return repositories == null ? null : map(repositories, this::toArtifactRepository);
    }

    public abstract ArtifactRepository toArtifactRepository(RemoteRepository repository);

    public List<org.eclipse.aether.graph.Dependency> toDependencies(
            Collection<DependencyCoordinate> dependencies, boolean managed) {
        return dependencies == null ? null : map(dependencies, d -> toDependency(d, managed));
    }

    public abstract org.eclipse.aether.graph.Dependency toDependency(DependencyCoordinate dependency, boolean managed);

    public List<org.eclipse.aether.artifact.Artifact> toArtifacts(Collection<Artifact> artifacts) {
        return artifacts == null ? null : map(artifacts, this::toArtifact);
    }

    public org.eclipse.aether.artifact.Artifact toArtifact(Artifact artifact) {
        File file = getService(ArtifactManager.class)
                .getPath(artifact)
                .map(Path::toFile)
                .orElse(null);
        if (artifact instanceof DefaultArtifact) {
            org.eclipse.aether.artifact.Artifact a = ((DefaultArtifact) artifact).getArtifact();
            if (Objects.equals(file, a.getFile())) {
                return a;
            }
        }
        return new org.eclipse.aether.artifact.DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                artifact.getExtension(),
                artifact.getVersion().toString(),
                null,
                file);
    }

    public org.eclipse.aether.artifact.Artifact toArtifact(ArtifactCoordinate coord) {
        if (coord instanceof DefaultArtifactCoordinate) {
            return ((DefaultArtifactCoordinate) coord).getCoordinate();
        }
        return new org.eclipse.aether.artifact.DefaultArtifact(
                coord.getGroupId(),
                coord.getArtifactId(),
                coord.getClassifier(),
                coord.getExtension(),
                coord.getVersion().toString(),
                null,
                (File) null);
    }

    @Override
    public void registerListener(@Nonnull Listener listener) {
        listeners.add(nonNull(listener));
    }

    @Override
    public void unregisterListener(@Nonnull Listener listener) {
        listeners.remove(nonNull(listener));
    }

    @Nonnull
    @Override
    public Collection<Listener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    //
    // Shortcut implementations
    //

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createLocal(...)</code>
     *
     * @see RepositoryFactory#createLocal(Path)
     */
    @Override
    public LocalRepository createLocalRepository(Path path) {
        return getService(RepositoryFactory.class).createLocal(path);
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     *
     * @see RepositoryFactory#createRemote(String, String)
     */
    @Nonnull
    @Override
    public RemoteRepository createRemoteRepository(@Nonnull String id, @Nonnull String url) {
        return getService(RepositoryFactory.class).createRemote(id, url);
    }

    /**
     * Shortcut for <code>getService(RepositoryFactory.class).createRemote(...)</code>
     *
     * @see RepositoryFactory#createRemote(Repository)
     */
    @Nonnull
    @Override
    public RemoteRepository createRemoteRepository(@Nonnull Repository repository) {
        return getService(RepositoryFactory.class).createRemote(repository);
    }

    /**
     * Shortcut for <code>getService(CoordinateFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String)
     */
    @Override
    public ArtifactCoordinate createArtifactCoordinate(
            String groupId, String artifactId, String version, String extension) {
        return getService(ArtifactCoordinateFactory.class).create(this, groupId, artifactId, version, extension);
    }

    /**
     * Shortcut for <code>getService(CoordinateFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinateFactory#create(Session, String)
     */
    @Override
    public ArtifactCoordinate createArtifactCoordinate(String coordString) {
        return getService(ArtifactCoordinateFactory.class).create(this, coordString);
    }

    /**
     * Shortcut for <code>getService(CoordinateFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinateFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public ArtifactCoordinate createArtifactCoordinate(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return getService(ArtifactCoordinateFactory.class)
                .create(this, groupId, artifactId, version, classifier, extension, type);
    }

    /**
     * Shortcut for <code>getService(CoordinateFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinateFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public ArtifactCoordinate createArtifactCoordinate(Artifact artifact) {
        return getService(ArtifactCoordinateFactory.class)
                .create(
                        this,
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion().asString(),
                        artifact.getClassifier(),
                        artifact.getExtension(),
                        null);
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String)
     */
    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version, String extension) {
        return getService(ArtifactFactory.class).create(this, groupId, artifactId, version, extension);
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public Artifact createArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return getService(ArtifactFactory.class)
                .create(this, groupId, artifactId, version, classifier, extension, type);
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Map.Entry<Artifact, Path> resolveArtifact(ArtifactCoordinate coordinate) {
        return getService(ArtifactResolver.class)
                .resolve(this, Collections.singletonList(coordinate))
                .getArtifacts()
                .entrySet()
                .iterator()
                .next();
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Map<Artifact, Path> resolveArtifacts(ArtifactCoordinate... coordinates) {
        return resolveArtifacts(Arrays.asList(coordinates));
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Map<Artifact, Path> resolveArtifacts(Collection<? extends ArtifactCoordinate> coordinates) {
        return getService(ArtifactResolver.class).resolve(this, coordinates).getArtifacts();
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Map.Entry<Artifact, Path> resolveArtifact(Artifact artifact) {
        ArtifactCoordinate coordinate =
                getService(ArtifactCoordinateFactory.class).create(this, artifact);
        return resolveArtifact(coordinate);
    }

    @Override
    public Map<Artifact, Path> resolveArtifacts(Artifact... artifacts) {
        ArtifactCoordinateFactory acf = getService(ArtifactCoordinateFactory.class);
        List<ArtifactCoordinate> coords = map(Arrays.asList(artifacts), a -> acf.create(this, a));
        return resolveArtifacts(coords);
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts(Artifact... artifacts) {
        installArtifacts(Arrays.asList(artifacts));
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts(Collection<Artifact> artifacts) {
        getService(ArtifactInstaller.class).install(this, artifacts);
    }

    /**
     * Shortcut for <code>getService(ArtifactDeployer.class).deploy(...)</code>
     *
     * @throws ArtifactDeployerException if the artifacts deployment failed
     * @see ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     */
    @Override
    public void deployArtifact(RemoteRepository repository, Artifact... artifacts) {
        getService(ArtifactDeployer.class).deploy(this, repository, Arrays.asList(artifacts));
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).setPath(...)</code>
     *
     * @see ArtifactManager#setPath(Artifact, Path)
     */
    @Override
    public void setArtifactPath(@Nonnull Artifact artifact, @Nonnull Path path) {
        getService(ArtifactManager.class).setPath(artifact, path);
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).getPath(...)</code>
     *
     * @see ArtifactManager#getPath(Artifact)
     */
    @Nonnull
    @Override
    public Optional<Path> getArtifactPath(@Nonnull Artifact artifact) {
        return getService(ArtifactManager.class).getPath(artifact);
    }

    /**
     * Shortcut for <code>getService(VersionParser.class).isSnapshot(...)</code>
     *
     * @see VersionParser#isSnapshot(String)
     */
    @Override
    public boolean isVersionSnapshot(@Nonnull String version) {
        return getService(VersionParser.class).isSnapshot(version);
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     *
     * @see DependencyCoordinateFactory#create(Session, ArtifactCoordinate)
     */
    @Nonnull
    @Override
    public DependencyCoordinate createDependencyCoordinate(@Nonnull ArtifactCoordinate coordinate) {
        return getService(DependencyCoordinateFactory.class).create(this, coordinate);
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     *
     * @see DependencyCoordinateFactory#create(Session, ArtifactCoordinate)
     */
    @Nonnull
    public DependencyCoordinate createDependencyCoordinate(@Nonnull Dependency dependency) {
        return getService(DependencyCoordinateFactory.class).create(this, dependency);
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, Artifact)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull Artifact artifact) {
        return getService(DependencyCollector.class).collect(this, artifact).getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, Project)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull Project project) {
        return getService(DependencyCollector.class).collect(this, project).getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyCollector.class).collect(...)</code>
     *
     * @throws DependencyCollectorException if the dependency collection failed
     * @see DependencyCollector#collect(Session, DependencyCoordinate)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull DependencyCoordinate dependency) {
        return getService(DependencyCollector.class).collect(this, dependency).getRoot();
    }

    @Nonnull
    @Override
    public List<Node> flattenDependencies(@Nonnull Node node, @Nonnull PathScope scope) {
        return getService(DependencyResolver.class).flatten(this, node, scope);
    }

    @Override
    public List<Path> resolveDependencies(DependencyCoordinate dependency) {
        return getService(DependencyResolver.class).resolve(this, dependency).getPaths();
    }

    @Override
    public List<Path> resolveDependencies(List<DependencyCoordinate> dependencies) {
        return getService(DependencyResolver.class).resolve(this, dependencies).getPaths();
    }

    @Override
    public List<Path> resolveDependencies(Project project, PathScope scope) {
        return getService(DependencyResolver.class)
                .resolve(this, project, scope)
                .getPaths();
    }

    @Override
    public Path getPathForLocalArtifact(@Nonnull Artifact artifact) {
        return getService(LocalRepositoryManager.class).getPathForLocalArtifact(this, getLocalRepository(), artifact);
    }

    @Override
    public Path getPathForRemoteArtifact(RemoteRepository remote, Artifact artifact) {
        return getService(LocalRepositoryManager.class)
                .getPathForRemoteArtifact(this, getLocalRepository(), remote, artifact);
    }

    @Override
    public Version parseVersion(String version) {
        return getService(VersionParser.class).parseVersion(version);
    }

    @Override
    public VersionRange parseVersionRange(String versionRange) {
        return getService(VersionParser.class).parseVersionRange(versionRange);
    }

    @Override
    public VersionConstraint parseVersionConstraint(String versionConstraint) {
        return getService(VersionParser.class).parseVersionConstraint(versionConstraint);
    }

    @Override
    public Version resolveVersion(ArtifactCoordinate artifact) {
        return getService(VersionResolver.class).resolve(this, artifact).getVersion();
    }

    @Override
    public List<Version> resolveVersionRange(ArtifactCoordinate artifact) {
        return getService(VersionRangeResolver.class).resolve(this, artifact).getVersions();
    }

    @Override
    public Type requireType(String id) {
        return getService(TypeRegistry.class).require(id);
    }

    @Override
    public Language requireLanguage(String id) {
        return getService(LanguageRegistry.class).require(id);
    }

    @Override
    public Packaging requirePackaging(String id) {
        return getService(PackagingRegistry.class).require(id);
    }

    @Override
    public ProjectScope requireProjectScope(String id) {
        return getService(ProjectScopeRegistry.class).require(id);
    }

    @Override
    public DependencyScope requireDependencyScope(String id) {
        return DependencyScope.forId(id);
    }

    @Override
    public PathScope requirePathScope(String id) {
        return getService(PathScopeRegistry.class).require(id);
    }
}
