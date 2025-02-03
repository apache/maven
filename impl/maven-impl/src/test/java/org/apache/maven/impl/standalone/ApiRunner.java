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
package org.apache.maven.impl.standalone;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.model.PluginContainer;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.LifecycleRegistry;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.PackagingRegistry;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.TypeProvider;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.impl.AbstractSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.di.SessionScope;
import org.apache.maven.impl.resolver.scopes.Maven4ScopeManagerConfiguration;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;

public class ApiRunner {

    /**
     * Create a new session.
     */
    public static Session createSession() {
        return createSession(null);
    }

    /**
     * Create a new session.
     */
    public static Session createSession(Consumer<Injector> injectorConsumer) {
        return createSession(injectorConsumer, null);
    }

    public static Session createSession(Consumer<Injector> injectorConsumer, Path localRepo) {
        Injector injector = Injector.create();
        injector.bindInstance(Injector.class, injector);
        injector.bindImplicit(ApiRunner.class);
        injector.bindImplicit(RepositorySystemSupplier.class);
        injector.bindInstance(LocalRepoProvider.class, () -> localRepo);
        injector.discover(ApiRunner.class.getClassLoader());
        if (injectorConsumer != null) {
            injectorConsumer.accept(injector);
        }
        Session session = injector.getInstance(Session.class);
        SessionScope scope = new SessionScope();
        scope.enter();
        scope.seed(Session.class, session);
        injector.bindScope(SessionScoped.class, scope);
        return session;
    }

    interface LocalRepoProvider {
        Path getLocalRepo();
    }

    static class DefaultSession extends AbstractSession {

        private final Map<String, String> systemProperties;
        private final Instant startTime = MonotonicClock.now();

        DefaultSession(RepositorySystemSession session, RepositorySystem repositorySystem, Lookup lookup) {
            this(session, repositorySystem, Collections.emptyList(), null, lookup);
        }

        protected DefaultSession(
                RepositorySystemSession session,
                RepositorySystem repositorySystem,
                List<RemoteRepository> repositories,
                List<org.eclipse.aether.repository.RemoteRepository> resolverRepositories,
                Lookup lookup) {
            super(session, repositorySystem, repositories, resolverRepositories, lookup);
            systemProperties = System.getenv().entrySet().stream()
                    .collect(Collectors.toMap(e -> "env." + e.getKey(), Map.Entry::getValue));
            System.getProperties().forEach((k, v) -> systemProperties.put(k.toString(), v.toString()));
        }

        @Override
        protected Session newSession(RepositorySystemSession session, List<RemoteRepository> repositories) {
            return new DefaultSession(session, repositorySystem, repositories, null, lookup);
        }

        @Override
        public Settings getSettings() {
            return Settings.newInstance();
        }

        @Override
        public Map<String, String> getUserProperties() {
            return Map.of();
        }

        @Override
        public Map<String, String> getSystemProperties() {
            return systemProperties;
        }

        @Override
        public Map<String, String> getEffectiveProperties(Project project) {
            HashMap<String, String> result = new HashMap<>(getSystemProperties());
            if (project != null) {
                result.putAll(project.getModel().getProperties());
            }
            result.putAll(getUserProperties());
            return result;
        }

        @Override
        public Version getMavenVersion() {
            return null;
        }

        @Override
        public int getDegreeOfConcurrency() {
            return 0;
        }

        @Override
        public Instant getStartTime() {
            return startTime;
        }

        @Override
        public Path getTopDirectory() {
            return null;
        }

        @Override
        public Path getRootDirectory() {
            throw new IllegalStateException();
        }

        @Override
        public List<Project> getProjects() {
            return List.of();
        }

        @Override
        public Map<String, Object> getPluginContext(Project project) {
            throw new UnsupportedInStandaloneModeException();
        }
    }

    @Provides
    @SuppressWarnings("unused")
    static Lookup newLookup(Injector injector) {
        return new Lookup() {
            @Override
            public <T> T lookup(Class<T> type) {
                try {
                    return injector.getInstance(type);
                } catch (DIException e) {
                    throw new MavenException("Unable to locate instance of type " + type, e);
                }
            }

            @Override
            public <T> T lookup(Class<T> type, String name) {
                try {
                    return injector.getInstance(Key.of(type, name));
                } catch (DIException e) {
                    throw new MavenException("Unable to locate instance of type " + type, e);
                }
            }

            @Override
            public <T> Optional<T> lookupOptional(Class<T> type) {
                try {
                    return Optional.of(injector.getInstance(type));
                } catch (DIException e) {
                    return Optional.empty();
                }
            }

            @Override
            public <T> Optional<T> lookupOptional(Class<T> type, String name) {
                try {
                    return Optional.of(injector.getInstance(Key.of(type, name)));
                } catch (DIException e) {
                    return Optional.empty();
                }
            }

            @Override
            public <T> List<T> lookupList(Class<T> type) {
                return injector.getInstance(new Key<List<T>>() {});
            }

            @Override
            public <T> Map<String, T> lookupMap(Class<T> type) {
                return injector.getInstance(new Key<Map<String, T>>() {});
            }
        };
    }

    @Provides
    @SuppressWarnings("unused")
    static ArtifactManager newArtifactManager() {
        return new ArtifactManager() {
            private final Map<Artifact, Path> paths = new ConcurrentHashMap<>();

            @Override
            public Optional<Path> getPath(Artifact artifact) {
                return Optional.ofNullable(paths.get(artifact));
            }

            @Override
            public void setPath(ProducedArtifact artifact, Path path) {
                paths.put(artifact, path);
            }
        };
    }

    @Provides
    @SuppressWarnings("unused")
    static PackagingRegistry newPackagingRegistry(TypeRegistry typeRegistry) {
        return id -> Optional.of(new DumbPackaging(id, typeRegistry.require(id), Map.of()));
    }

    @Provides
    @SuppressWarnings("unused")
    static TypeRegistry newTypeRegistry(List<TypeProvider> providers) {
        return new TypeRegistry() {
            @Override
            public Optional<Type> lookup(String id) {
                return providers.stream()
                        .flatMap(p -> p.provides().stream())
                        .filter(t -> Objects.equals(id, t.id()))
                        .findAny();
            }
        };
    }

    @Provides
    @SuppressWarnings("unused")
    static LifecycleRegistry newLifecycleRegistry() {
        return new LifecycleRegistry() {

            @Override
            public Iterator<Lifecycle> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public Optional<Lifecycle> lookup(String id) {
                return Optional.empty();
            }

            @Override
            public List<String> computePhases(Lifecycle lifecycle) {
                return List.of();
            }
        };
    }

    @Provides
    @SuppressWarnings("unused")
    static Session newSession(RepositorySystem system, Lookup lookup, @Nullable LocalRepoProvider localRepoProvider) {
        Map<String, String> properties = new HashMap<>();
        // Env variables prefixed with "env."
        System.getenv().forEach((k, v) -> properties.put("env." + k, v));
        // Java System properties
        System.getProperties().forEach((k, v) -> properties.put(k.toString(), v.toString()));

        // Do not allow user settings to interfere with our unit tests
        // TODO: remove that when this go more public
        properties.put("user.home", "target");

        // SettingsDecrypter settingsDecrypter =
        // (SettingsDecrypter)Objects.requireNonNull(this.createSettingsDecrypter(preBoot));
        //        new DefaultProfileSelector(List.of(
        //                new JdkVersionProfileActivator(),
        //                new PropertyProfileActivator(),
        //                new OperatingSystemProfileActivator(),
        //                new FileProfileActivator(new ProfileActivationFilePathInterpolator(
        //                        new DefaultPathTranslator(), new DefaultRootLocator()))));

        Path userHome = Paths.get(properties.get("user.home"));
        Path mavenUserHome = userHome.resolve(".m2");
        Path mavenSystemHome = properties.containsKey("maven.home")
                ? Paths.get(properties.get("maven.home"))
                : properties.containsKey("env.MAVEN_HOME") ? Paths.get(properties.get("env.MAVEN_HOME")) : null;

        DefaultRepositorySystemSession rsession = new DefaultRepositorySystemSession(h -> false);
        rsession.setScopeManager(new ScopeManagerImpl(Maven4ScopeManagerConfiguration.INSTANCE));
        rsession.setSystemProperties(properties);
        rsession.setConfigProperties(properties);

        DefaultSession session = new DefaultSession(
                rsession,
                system,
                List.of(lookup.lookup(RepositoryFactory.class)
                        .createRemote("central", "https://repo.maven.apache.org/maven2")),
                null,
                lookup);

        Settings settings = session.getService(SettingsBuilder.class)
                .build(
                        session,
                        mavenSystemHome != null ? mavenSystemHome.resolve("settings.xml") : null,
                        mavenUserHome.resolve("settings.xml"))
                .getEffectiveSettings();

        settings.getProfiles();

        // local repository
        String localRepository = settings.getLocalRepository() != null
                        && !settings.getLocalRepository().isEmpty()
                ? settings.getLocalRepository()
                : localRepoProvider != null && localRepoProvider.getLocalRepo() != null
                        ? localRepoProvider.getLocalRepo().toString()
                        : mavenUserHome.resolve("repository").toString();
        LocalRepositoryManager llm = system.newLocalRepositoryManager(rsession, new LocalRepository(localRepository));
        rsession.setLocalRepositoryManager(llm);
        // active proxies
        // TODO
        // active profiles

        DefaultSession defaultSession = new DefaultSession(
                rsession,
                system,
                List.of(lookup.lookup(RepositoryFactory.class)
                        .createRemote("central", "https://repo.maven.apache.org/maven2")),
                null,
                lookup);

        Profile profile = session.getService(SettingsBuilder.class)
                .convert(org.apache.maven.api.settings.Profile.newBuilder()
                        .repositories(settings.getRepositories())
                        .pluginRepositories(settings.getPluginRepositories())
                        .build());
        RepositoryFactory repositoryFactory = session.getService(RepositoryFactory.class);
        List<RemoteRepository> repositories = profile.getRepositories().stream()
                .map(repositoryFactory::createRemote)
                .toList();
        InternalSession s = (InternalSession) session.withRemoteRepositories(repositories);
        InternalSession.associate(rsession, s);
        return s;

        // List<RemoteRepository> repositories = repositoryFactory.createRemote();

        //        session.getService(SettingsBuilder.class).convert()

        //        settings.getDelegate().getRepositories().stream()
        //                        .map(r -> SettingsUtilsV4.)
        //        defaultSession.getService(RepositoryFactory.class).createRemote()
        //        return defaultSession;
    }

    static class UnsupportedInStandaloneModeException extends MavenException {}

    record DumbPackaging(String id, Type type, Map<String, PluginContainer> plugins) implements Packaging {}
}
