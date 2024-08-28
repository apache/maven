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
package org.apache.maven.internal.impl.standalone;

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
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Provides;
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
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.ModelParser;
import org.apache.maven.api.spi.TypeProvider;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.internal.impl.AbstractSession;
import org.apache.maven.internal.impl.DefaultArtifactCoordinatesFactory;
import org.apache.maven.internal.impl.DefaultArtifactDeployer;
import org.apache.maven.internal.impl.DefaultArtifactFactory;
import org.apache.maven.internal.impl.DefaultArtifactInstaller;
import org.apache.maven.internal.impl.DefaultArtifactResolver;
import org.apache.maven.internal.impl.DefaultChecksumAlgorithmService;
import org.apache.maven.internal.impl.DefaultDependencyCoordinatesFactory;
import org.apache.maven.internal.impl.DefaultDependencyResolver;
import org.apache.maven.internal.impl.DefaultLocalRepositoryManager;
import org.apache.maven.internal.impl.DefaultMessageBuilderFactory;
import org.apache.maven.internal.impl.DefaultModelUrlNormalizer;
import org.apache.maven.internal.impl.DefaultModelVersionParser;
import org.apache.maven.internal.impl.DefaultModelXmlFactory;
import org.apache.maven.internal.impl.DefaultPluginConfigurationExpander;
import org.apache.maven.internal.impl.DefaultRepositoryFactory;
import org.apache.maven.internal.impl.DefaultSettingsBuilder;
import org.apache.maven.internal.impl.DefaultSettingsXmlFactory;
import org.apache.maven.internal.impl.DefaultSuperPomProvider;
import org.apache.maven.internal.impl.DefaultToolchainsBuilder;
import org.apache.maven.internal.impl.DefaultToolchainsXmlFactory;
import org.apache.maven.internal.impl.DefaultTransportProvider;
import org.apache.maven.internal.impl.DefaultUrlNormalizer;
import org.apache.maven.internal.impl.DefaultVersionParser;
import org.apache.maven.internal.impl.ExtensibleEnumRegistries;
import org.apache.maven.internal.impl.InternalSession;
import org.apache.maven.internal.impl.model.BuildModelTransformer;
import org.apache.maven.internal.impl.model.DefaultDependencyManagementImporter;
import org.apache.maven.internal.impl.model.DefaultDependencyManagementInjector;
import org.apache.maven.internal.impl.model.DefaultInheritanceAssembler;
import org.apache.maven.internal.impl.model.DefaultLifecycleBindingsInjector;
import org.apache.maven.internal.impl.model.DefaultModelBuilder;
import org.apache.maven.internal.impl.model.DefaultModelInterpolator;
import org.apache.maven.internal.impl.model.DefaultModelNormalizer;
import org.apache.maven.internal.impl.model.DefaultModelPathTranslator;
import org.apache.maven.internal.impl.model.DefaultModelProcessor;
import org.apache.maven.internal.impl.model.DefaultModelValidator;
import org.apache.maven.internal.impl.model.DefaultModelVersionProcessor;
import org.apache.maven.internal.impl.model.DefaultPathTranslator;
import org.apache.maven.internal.impl.model.DefaultPluginManagementInjector;
import org.apache.maven.internal.impl.model.DefaultProfileInjector;
import org.apache.maven.internal.impl.model.DefaultProfileSelector;
import org.apache.maven.internal.impl.model.DefaultRootLocator;
import org.apache.maven.internal.impl.model.ProfileActivationFilePathInterpolator;
import org.apache.maven.internal.impl.resolver.DefaultVersionRangeResolver;
import org.apache.maven.internal.impl.resolver.DefaultVersionResolver;
import org.apache.maven.internal.impl.resolver.MavenVersionScheme;
import org.apache.maven.internal.impl.resolver.type.DefaultTypeProvider;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;

public class ApiRunner {

    /**
     * Create a new session.
     */
    public static Session createSession() {
        Injector injector = Injector.create();
        injector.bindInstance(Injector.class, injector);
        injector.bindImplicit(ApiRunner.class);
        injector.bindImplicit(DefaultArtifactCoordinatesFactory.class);
        injector.bindImplicit(DefaultArtifactDeployer.class);
        injector.bindImplicit(DefaultArtifactFactory.class);
        injector.bindImplicit(DefaultArtifactInstaller.class);
        injector.bindImplicit(DefaultArtifactResolver.class);
        injector.bindImplicit(DefaultChecksumAlgorithmService.class);
        injector.bindImplicit(DefaultDependencyResolver.class);
        injector.bindImplicit(DefaultDependencyCoordinatesFactory.class);
        injector.bindImplicit(DefaultLocalRepositoryManager.class);
        injector.bindImplicit(DefaultMessageBuilderFactory.class);
        injector.bindImplicit(DefaultModelXmlFactory.class);
        injector.bindImplicit(DefaultRepositoryFactory.class);
        injector.bindImplicit(DefaultSettingsBuilder.class);
        injector.bindImplicit(DefaultSettingsXmlFactory.class);
        injector.bindImplicit(DefaultToolchainsBuilder.class);
        injector.bindImplicit(DefaultToolchainsXmlFactory.class);
        injector.bindImplicit(DefaultTransportProvider.class);
        injector.bindImplicit(DefaultVersionParser.class);
        injector.bindImplicit(DefaultVersionRangeResolver.class);
        injector.bindImplicit(org.apache.maven.internal.impl.DefaultVersionParser.class);
        injector.bindImplicit(org.apache.maven.internal.impl.DefaultVersionRangeResolver.class);
        injector.bindImplicit(DefaultVersionResolver.class);
        injector.bindImplicit(ExtensibleEnumRegistries.class);
        injector.bindImplicit(DefaultTypeProvider.class);

        injector.bindImplicit(MavenVersionScheme.class);
        injector.bindImplicit(BuildModelTransformer.class);
        injector.bindImplicit(DefaultDependencyManagementImporter.class);
        injector.bindImplicit(DefaultDependencyManagementInjector.class);
        injector.bindImplicit(DefaultModelBuilder.class);
        injector.bindImplicit(DefaultModelProcessor.class);
        injector.bindImplicit(DefaultModelValidator.class);
        injector.bindImplicit(DefaultModelVersionProcessor.class);
        injector.bindImplicit(DefaultModelNormalizer.class);
        injector.bindImplicit(DefaultModelInterpolator.class);
        injector.bindImplicit(DefaultPathTranslator.class);
        injector.bindImplicit(DefaultRootLocator.class);
        injector.bindImplicit(DefaultModelPathTranslator.class);
        injector.bindImplicit(DefaultUrlNormalizer.class);
        injector.bindImplicit(DefaultModelUrlNormalizer.class);
        injector.bindImplicit(DefaultSuperPomProvider.class);
        injector.bindImplicit(DefaultInheritanceAssembler.class);
        injector.bindImplicit(DefaultProfileInjector.class);
        injector.bindImplicit(DefaultProfileSelector.class);
        injector.bindImplicit(DefaultPluginManagementInjector.class);
        injector.bindImplicit(DefaultLifecycleBindingsInjector.class);
        injector.bindImplicit(DefaultPluginConfigurationExpander.class);
        injector.bindImplicit(ProfileActivationFilePathInterpolator.class);
        injector.bindImplicit(DefaultModelVersionParser.class);

        injector.bindImplicit(ProfileActivator.class);
        injector.bindImplicit(ModelParser.class);

        return injector.getInstance(Session.class);
    }

    static class DefaultSession extends AbstractSession {

        private final Map<String, String> systemProperties;
        private Instant startTime = Instant.now();

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
                    .collect(Collectors.toMap(e -> "env." + e.getKey(), e -> e.getValue()));
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
            return null;
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
    static PackagingRegistry newPackagingRegistry(TypeRegistry typeRegistry) {
        return id -> Optional.of(new DumbPackaging(id, typeRegistry.require(id), Map.of()));
    }

    @Provides
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
    static RepositorySystemSupplier newRepositorySystemSupplier() {
        return new RepositorySystemSupplier();
    }

    @Provides
    static RepositorySystem newRepositorySystem(RepositorySystemSupplier repositorySystemSupplier) {
        return repositorySystemSupplier.getRepositorySystem();
    }

    @Provides
    static RemoteRepositoryManager newRemoteRepositoryManager(RepositorySystemSupplier repositorySystemSupplier) {
        return repositorySystemSupplier.getRemoteRepositoryManager();
    }

    @Provides
    static Session newSession(RepositorySystem system, Lookup lookup) {
        Map<String, String> properties = new HashMap<>();
        // Env variables prefixed with "env."
        System.getenv().forEach((k, v) -> properties.put("env." + k, v));
        // Java System properties
        System.getProperties().forEach((k, v) -> properties.put(k.toString(), v.toString()));

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
                ? settings.getLocalRepository()
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
