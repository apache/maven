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
package org.apache.maven.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Dependency;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.DependencyScope;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Language;
import org.apache.maven.api.Listener;
import org.apache.maven.api.LocalRepository;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Node;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.PathType;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.ProjectScope;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.SessionData;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.VersionConstraint;
import org.apache.maven.api.VersionRange;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ArtifactCoordinatesFactory;
import org.apache.maven.api.services.ArtifactDeployer;
import org.apache.maven.api.services.ArtifactDeployerException;
import org.apache.maven.api.services.ArtifactFactory;
import org.apache.maven.api.services.ArtifactInstaller;
import org.apache.maven.api.services.ArtifactInstallerException;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.DependencyCoordinatesFactory;
import org.apache.maven.api.services.DependencyResolver;
import org.apache.maven.api.services.DependencyResolverException;
import org.apache.maven.api.services.DependencyResolverRequest;
import org.apache.maven.api.services.LanguageRegistry;
import org.apache.maven.api.services.LocalRepositoryManager;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.LookupException;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.PathScopeRegistry;
import org.apache.maven.api.services.ProjectScopeRegistry;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.RequestTrace;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.VersionRangeResolver;
import org.apache.maven.api.services.VersionResolver;
import org.apache.maven.api.services.VersionResolverException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.transfer.TransferResource;

import static org.apache.maven.impl.Utils.map;
import static org.apache.maven.impl.Utils.nonNull;

public abstract class AbstractSession implements InternalSession {

    protected final RepositorySystemSession session;
    protected final RepositorySystem repositorySystem;
    protected final List<RemoteRepository> repositories;
    protected final Lookup lookup;
    private final Map<Class<? extends Service>, Service> services = new ConcurrentHashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<org.eclipse.aether.graph.DependencyNode, Node> allNodes =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Class<? extends Artifact>, Map<org.eclipse.aether.artifact.Artifact, Artifact>> allArtifacts =
            new ConcurrentHashMap<>();
    private final Map<org.eclipse.aether.repository.RemoteRepository, RemoteRepository> allRepositories =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<org.eclipse.aether.graph.Dependency, Dependency> allDependencies =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        TransferResource.setClock(MonotonicClock.get());
    }

    public AbstractSession(
            RepositorySystemSession session,
            RepositorySystem repositorySystem,
            List<RemoteRepository> repositories,
            List<org.eclipse.aether.repository.RemoteRepository> resolverRepositories,
            Lookup lookup) {
        this.session = nonNull(session, "session");
        this.repositorySystem = repositorySystem;
        this.repositories = getRepositories(repositories, resolverRepositories);
        this.lookup = lookup;
    }

    @Override
    public RemoteRepository getRemoteRepository(org.eclipse.aether.repository.RemoteRepository repository) {
        return allRepositories.computeIfAbsent(repository, DefaultRemoteRepository::new);
    }

    @Override
    public Node getNode(org.eclipse.aether.graph.DependencyNode node) {
        return getNode(node, false);
    }

    @Override
    public Node getNode(org.eclipse.aether.graph.DependencyNode node, boolean verbose) {
        return allNodes.computeIfAbsent(node, n -> new DefaultNode(this, n, verbose));
    }

    @Nonnull
    @Override
    public Artifact getArtifact(@Nonnull org.eclipse.aether.artifact.Artifact artifact) {
        return getArtifact(Artifact.class, artifact);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Artifact> T getArtifact(Class<T> clazz, org.eclipse.aether.artifact.Artifact artifact) {
        Map<org.eclipse.aether.artifact.Artifact, Artifact> map =
                allArtifacts.computeIfAbsent(clazz, c -> Collections.synchronizedMap(new WeakHashMap<>()));
        if (clazz == Artifact.class) {
            return (T) map.computeIfAbsent(artifact, a -> new DefaultArtifact(this, a));
        } else if (clazz == DownloadedArtifact.class) {
            if (artifact.getPath() == null) {
                throw new IllegalArgumentException("The given artifact is not resolved");
            } else {
                return (T) map.computeIfAbsent(artifact, a -> new DefaultDownloadedArtifact(this, a));
            }
        } else if (clazz == ProducedArtifact.class) {
            return (T) map.computeIfAbsent(artifact, a -> new DefaultProducedArtifact(this, a));
        } else {
            throw new IllegalArgumentException("Unsupported Artifact class: " + clazz);
        }
    }

    @Nonnull
    @Override
    public Dependency getDependency(@Nonnull org.eclipse.aether.graph.Dependency dependency) {
        return allDependencies.computeIfAbsent(dependency, d -> new DefaultDependency(this, d));
    }

    @Override
    public List<org.eclipse.aether.repository.RemoteRepository> toRepositories(List<RemoteRepository> repositories) {
        return repositories == null ? null : map(repositories, this::toRepository);
    }

    @Override
    public org.eclipse.aether.repository.RemoteRepository toRepository(RemoteRepository repository) {
        if (repository instanceof DefaultRemoteRepository defaultRemoteRepository) {
            return defaultRemoteRepository.getRepository();
        } else {
            // TODO
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    @Override
    public org.eclipse.aether.repository.LocalRepository toRepository(LocalRepository repository) {
        if (repository instanceof DefaultLocalRepository defaultLocalRepository) {
            return defaultLocalRepository.getRepository();
        } else {
            // TODO
            throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    @Override
    public List<org.eclipse.aether.graph.Dependency> toDependencies(
            Collection<DependencyCoordinates> dependencies, boolean managed) {
        return dependencies == null ? null : map(dependencies, d -> toDependency(d, managed));
    }

    static List<RemoteRepository> getRepositories(
            List<RemoteRepository> repositories,
            List<org.eclipse.aether.repository.RemoteRepository> resolverRepositories) {
        if (repositories != null) {
            return repositories;
        } else if (resolverRepositories != null) {
            return map(resolverRepositories, DefaultRemoteRepository::new);
        } else {
            throw new IllegalArgumentException("no remote repositories provided");
        }
    }

    @Nonnull
    @Override
    public List<RemoteRepository> getRemoteRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    @Nonnull
    @Override
    public SessionData getData() {
        org.eclipse.aether.SessionData data = session.getData();
        return new SessionData() {
            @Override
            public <T> void set(@Nonnull Key<T> key, @Nullable T value) {
                data.set(key, value);
            }

            @Override
            public <T> boolean replace(@Nonnull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
                return data.set(key, oldValue, newValue);
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(@Nonnull Key<T> key) {
                return (T) data.get(key);
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public <T> T computeIfAbsent(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
                return (T) data.computeIfAbsent(key, (Supplier<Object>) supplier);
            }
        };
    }

    @Nonnull
    @Override
    public LocalRepository getLocalRepository() {
        return new DefaultLocalRepository(session.getLocalRepository());
    }

    @Nonnull
    @Override
    public Session withLocalRepository(@Nonnull LocalRepository localRepository) {
        nonNull(localRepository, "localRepository");
        if (session.getLocalRepository() != null
                && Objects.equals(session.getLocalRepository().getBasePath(), localRepository.getPath())) {
            return this;
        }
        org.eclipse.aether.repository.LocalRepository repository = toRepository(localRepository);
        org.eclipse.aether.repository.LocalRepositoryManager localRepositoryManager =
                repositorySystem.newLocalRepositoryManager(session, repository);

        RepositorySystemSession repoSession =
                new DefaultRepositorySystemSession(session).setLocalRepositoryManager(localRepositoryManager);
        return newSession(repoSession, repositories);
    }

    @Nonnull
    @Override
    public Session withRemoteRepositories(@Nonnull List<RemoteRepository> repositories) {
        return newSession(session, repositories);
    }

    protected abstract Session newSession(RepositorySystemSession session, List<RemoteRepository> repositories);

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Service> T getService(Class<T> clazz) throws NoSuchElementException {
        T t = (T) services.computeIfAbsent(clazz, this::lookup);
        if (t == null) {
            throw new NoSuchElementException(clazz.getName());
        }
        return t;
    }

    private Service lookup(Class<? extends Service> c) {
        try {
            return lookup.lookup(c);
        } catch (LookupException e) {
            throw new NoSuchElementException(c.getName(), e);
        }
    }

    @Nonnull
    @Override
    public RepositorySystemSession getSession() {
        return session;
    }

    @Nonnull
    @Override
    public RepositorySystem getRepositorySystem() {
        return repositorySystem;
    }

    @Override
    public org.eclipse.aether.graph.Dependency toDependency(DependencyCoordinates dependency, boolean managed) {
        org.eclipse.aether.graph.Dependency dep;
        if (dependency instanceof AetherDependencyWrapper wrapper) {
            dep = wrapper.dependency;
        } else {
            Type type = dependency.getType();
            dep = new org.eclipse.aether.graph.Dependency(
                    new org.eclipse.aether.artifact.DefaultArtifact(
                            dependency.getGroupId(),
                            dependency.getArtifactId(),
                            dependency.getClassifier(),
                            type.getExtension(),
                            dependency.getVersionConstraint().toString(),
                            Map.of("type", type.id()),
                            (ArtifactType) null),
                    dependency.getScope().id(),
                    dependency.getOptional(),
                    map(dependency.getExclusions(), this::toExclusion));
        }
        if (!managed && "".equals(dep.getScope())) {
            dep = dep.setScope(DependencyScope.COMPILE.id());
        }
        return dep;
    }

    private org.eclipse.aether.graph.Exclusion toExclusion(Exclusion exclusion) {
        return new org.eclipse.aether.graph.Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }

    @Override
    public List<org.eclipse.aether.artifact.Artifact> toArtifacts(Collection<? extends Artifact> artifacts) {
        return artifacts == null ? null : map(artifacts, this::toArtifact);
    }

    @Override
    public org.eclipse.aether.artifact.Artifact toArtifact(Artifact artifact) {
        Path path = getService(ArtifactManager.class).getPath(artifact).orElse(null);
        if (artifact instanceof DefaultArtifact defaultArtifact) {
            org.eclipse.aether.artifact.Artifact a = defaultArtifact.getArtifact();
            if (Objects.equals(path, a.getPath())) {
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
                path);
    }

    @Override
    public org.eclipse.aether.artifact.Artifact toArtifact(ArtifactCoordinates coords) {
        if (coords instanceof DefaultArtifactCoordinates defaultArtifactCoordinates) {
            return defaultArtifactCoordinates.getCoordinates();
        }
        return new org.eclipse.aether.artifact.DefaultArtifact(
                coords.getGroupId(),
                coords.getArtifactId(),
                coords.getClassifier(),
                coords.getExtension(),
                coords.getVersionConstraint().toString(),
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
     * Shortcut for <code>getService(ArtifactCoordinatesFactory.class).create(...)</code>
     *
     * @see ArtifactFactory#create(Session, String, String, String, String)
     */
    @Override
    public ArtifactCoordinates createArtifactCoordinates(
            String groupId, String artifactId, String version, String extension) {
        return getService(ArtifactCoordinatesFactory.class).create(this, groupId, artifactId, version, extension);
    }

    /**
     * Shortcut for <code>getService(ArtifactCoordinatesFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinatesFactory#create(Session, String)
     */
    @Override
    public ArtifactCoordinates createArtifactCoordinates(String coordString) {
        return getService(ArtifactCoordinatesFactory.class).create(this, coordString);
    }

    /**
     * Shortcut for <code>getService(ArtifactCoordinatesFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinatesFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public ArtifactCoordinates createArtifactCoordinates(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return getService(ArtifactCoordinatesFactory.class)
                .create(this, groupId, artifactId, version, classifier, extension, type);
    }

    /**
     * Shortcut for <code>getService(ArtifactCoordinatesFactory.class).create(...)</code>
     *
     * @see ArtifactCoordinatesFactory#create(Session, String, String, String, String, String, String)
     */
    @Override
    public ArtifactCoordinates createArtifactCoordinates(Artifact artifact) {
        return getService(ArtifactCoordinatesFactory.class)
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
     * Shortcut for <code>getService(ArtifactFactory.class).createProduced(...)</code>
     *
     * @see ArtifactFactory#createProduced(Session, String, String, String, String)
     */
    @Override
    public ProducedArtifact createProducedArtifact(
            String groupId, String artifactId, String version, String extension) {
        return getService(ArtifactFactory.class).createProduced(this, groupId, artifactId, version, extension);
    }

    /**
     * Shortcut for <code>getService(ArtifactFactory.class).createProduced(...)</code>
     *
     * @see ArtifactFactory#createProduced(Session, String, String, String, String, String, String)
     */
    @Override
    public ProducedArtifact createProducedArtifact(
            String groupId, String artifactId, String version, String classifier, String extension, String type) {
        return getService(ArtifactFactory.class)
                .createProduced(this, groupId, artifactId, version, classifier, extension, type);
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public DownloadedArtifact resolveArtifact(ArtifactCoordinates coordinates) {
        return getService(ArtifactResolver.class)
                .resolve(this, Collections.singletonList(coordinates))
                .getArtifacts()
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
    public DownloadedArtifact resolveArtifact(ArtifactCoordinates coordinates, List<RemoteRepository> repositories) {
        return getService(ArtifactResolver.class)
                .resolve(this, Collections.singletonList(coordinates), repositories)
                .getArtifacts()
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
    public Collection<DownloadedArtifact> resolveArtifacts(ArtifactCoordinates... coordinates) {
        return resolveArtifacts(Arrays.asList(coordinates));
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Collection<DownloadedArtifact> resolveArtifacts(Collection<? extends ArtifactCoordinates> coordinates) {
        return getService(ArtifactResolver.class).resolve(this, coordinates).getArtifacts();
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public Collection<DownloadedArtifact> resolveArtifacts(
            Collection<? extends ArtifactCoordinates> coordinates, List<RemoteRepository> repositories) {
        return getService(ArtifactResolver.class)
                .resolve(this, coordinates, repositories)
                .getArtifacts();
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public DownloadedArtifact resolveArtifact(Artifact artifact) {
        ArtifactCoordinates coordinates =
                getService(ArtifactCoordinatesFactory.class).create(this, artifact);
        return resolveArtifact(coordinates);
    }

    /**
     * Shortcut for <code>getService(ArtifactResolver.class).resolve(...)</code>
     *
     * @throws ArtifactResolverException if the artifact resolution failed
     * @see ArtifactResolver#resolve(Session, Collection)
     */
    @Override
    public DownloadedArtifact resolveArtifact(Artifact artifact, List<RemoteRepository> repositories) {
        ArtifactCoordinates coordinates =
                getService(ArtifactCoordinatesFactory.class).create(this, artifact);
        return resolveArtifact(coordinates, repositories);
    }

    @Override
    public Collection<DownloadedArtifact> resolveArtifacts(Artifact... artifacts) {
        ArtifactCoordinatesFactory acf = getService(ArtifactCoordinatesFactory.class);
        List<ArtifactCoordinates> coords =
                Arrays.stream(artifacts).map(a -> acf.create(this, a)).collect(Collectors.toList());
        return resolveArtifacts(coords);
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts(ProducedArtifact... artifacts) {
        installArtifacts(Arrays.asList(artifacts));
    }

    /**
     * Shortcut for {@code getService(ArtifactInstaller.class).install(...)}
     *
     * @throws ArtifactInstallerException if the artifacts installation failed
     * @see ArtifactInstaller#install(Session, Collection)
     */
    @Override
    public void installArtifacts(Collection<ProducedArtifact> artifacts) {
        getService(ArtifactInstaller.class).install(this, artifacts);
    }

    /**
     * Shortcut for <code>getService(ArtifactDeployer.class).deploy(...)</code>
     *
     * @throws ArtifactDeployerException if the artifacts deployment failed
     * @see ArtifactDeployer#deploy(Session, RemoteRepository, Collection)
     */
    @Override
    public void deployArtifact(RemoteRepository repository, ProducedArtifact... artifacts) {
        getService(ArtifactDeployer.class).deploy(this, repository, Arrays.asList(artifacts));
    }

    /**
     * Shortcut for <code>getService(ArtifactManager.class).setPath(...)</code>
     *
     * @see ArtifactManager#setPath(ProducedArtifact, Path)
     */
    @Override
    public void setArtifactPath(@Nonnull ProducedArtifact artifact, @Nonnull Path path) {
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
     * @see DependencyCoordinatesFactory#create(Session, ArtifactCoordinates)
     */
    @Nonnull
    @Override
    public DependencyCoordinates createDependencyCoordinates(@Nonnull ArtifactCoordinates coordinates) {
        return getService(DependencyCoordinatesFactory.class).create(this, coordinates);
    }

    /**
     * Shortcut for <code>getService(DependencyFactory.class).create(...)</code>
     *
     * @see DependencyCoordinatesFactory#create(Session, ArtifactCoordinates)
     */
    @Nonnull
    @Override
    public DependencyCoordinates createDependencyCoordinates(@Nonnull Dependency dependency) {
        return getService(DependencyCoordinatesFactory.class).create(this, dependency);
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).collect(...)</code>
     *
     * @throws DependencyResolverException if the dependency collection failed
     * @see DependencyResolver#collect(Session, Artifact, PathScope)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull Artifact artifact, @Nonnull PathScope scope) {
        return getService(DependencyResolver.class)
                .collect(this, artifact, scope)
                .getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).collect(...)</code>
     *
     * @throws DependencyResolverException if the dependency collection failed
     * @see DependencyResolver#collect(Session, Project, PathScope)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull Project project, @Nonnull PathScope scope) {
        return getService(DependencyResolver.class)
                .collect(this, project, scope)
                .getRoot();
    }

    /**
     * Shortcut for <code>getService(DependencyResolver.class).collect(...)</code>
     *
     * @throws DependencyResolverException if the dependency collection failed
     * @see DependencyResolver#collect(Session, DependencyCoordinates, PathScope)
     */
    @Nonnull
    @Override
    public Node collectDependencies(@Nonnull DependencyCoordinates dependency, @Nonnull PathScope scope) {
        Node root = getService(DependencyResolver.class)
                .collect(this, dependency, scope)
                .getRoot();
        return root.getChildren().iterator().next();
    }

    @Nonnull
    @Override
    public List<Node> flattenDependencies(@Nonnull Node node, @Nonnull PathScope scope) {
        return getService(DependencyResolver.class).flatten(this, node, scope);
    }

    @Override
    public List<Path> resolveDependencies(DependencyCoordinates dependency) {
        return getService(DependencyResolver.class).resolve(this, dependency).getPaths();
    }

    @Override
    public List<Path> resolveDependencies(List<DependencyCoordinates> dependencies) {
        return getService(DependencyResolver.class).resolve(this, dependencies).getPaths();
    }

    @Override
    public List<Path> resolveDependencies(Project project, PathScope scope) {
        return getService(DependencyResolver.class)
                .resolve(this, project, scope)
                .getPaths();
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            @Nonnull DependencyCoordinates dependency,
            @Nonnull PathScope scope,
            @Nonnull Collection<PathType> desiredTypes) {
        return getService(DependencyResolver.class)
                .resolve(DependencyResolverRequest.builder()
                        .session(this)
                        .requestType(DependencyResolverRequest.RequestType.RESOLVE)
                        .dependency(dependency)
                        .pathScope(scope)
                        .pathTypeFilter(desiredTypes)
                        .build())
                .getDispatchedPaths();
    }

    @Override
    public Map<PathType, List<Path>> resolveDependencies(
            @Nonnull Project project, @Nonnull PathScope scope, @Nonnull Collection<PathType> desiredTypes) {
        return getService(DependencyResolver.class)
                .resolve(DependencyResolverRequest.builder()
                        .session(this)
                        .requestType(DependencyResolverRequest.RequestType.RESOLVE)
                        .project(project)
                        .pathScope(scope)
                        .pathTypeFilter(desiredTypes)
                        .build())
                .getDispatchedPaths();
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
    public Version resolveVersion(ArtifactCoordinates artifact) throws VersionResolverException {
        return getService(VersionResolver.class).resolve(this, artifact).getVersion();
    }

    @Override
    public List<Version> resolveVersionRange(ArtifactCoordinates artifact) throws VersionResolverException {
        return getService(VersionRangeResolver.class).resolve(this, artifact).getVersions();
    }

    @Override
    public List<Version> resolveVersionRange(ArtifactCoordinates artifact, List<RemoteRepository> repositories)
            throws VersionResolverException {
        return getService(VersionRangeResolver.class)
                .resolve(this, artifact, repositories)
                .getVersions();
    }

    @Override
    public Optional<Version> resolveHighestVersion(ArtifactCoordinates artifact, List<RemoteRepository> repositories)
            throws VersionResolverException {
        return getService(VersionRangeResolver.class)
                .resolve(this, artifact, repositories)
                .getHighestVersion();
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
        return DependencyScope.forId(nonNull(id, "id"));
    }

    @Override
    public PathScope requirePathScope(String id) {
        return getService(PathScopeRegistry.class).require(id);
    }

    @Override
    public void setCurrentTrace(RequestTrace trace) {
        getTraceHolder().set(trace);
    }

    @Override
    public RequestTrace getCurrentTrace() {
        return getTraceHolder().get();
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<RequestTrace> getTraceHolder() {
        org.eclipse.aether.SessionData data = session.getData();
        return (ThreadLocal<RequestTrace>) data.computeIfAbsent(RequestTrace.class, ThreadLocal::new);
    }
}
