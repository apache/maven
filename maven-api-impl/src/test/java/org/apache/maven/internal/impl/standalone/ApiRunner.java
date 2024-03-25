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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.RepositoryFactory;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.internal.impl.AbstractSession;
import org.apache.maven.internal.impl.DefaultArtifactCoordinateFactory;
import org.apache.maven.internal.impl.DefaultArtifactDeployer;
import org.apache.maven.internal.impl.DefaultArtifactFactory;
import org.apache.maven.internal.impl.DefaultArtifactInstaller;
import org.apache.maven.internal.impl.DefaultArtifactResolver;
import org.apache.maven.internal.impl.DefaultChecksumAlgorithmService;
import org.apache.maven.internal.impl.DefaultDependencyCollector;
import org.apache.maven.internal.impl.DefaultDependencyCoordinateFactory;
import org.apache.maven.internal.impl.DefaultLocalRepositoryManager;
import org.apache.maven.internal.impl.DefaultMessageBuilderFactory;
import org.apache.maven.internal.impl.DefaultModelXmlFactory;
import org.apache.maven.internal.impl.DefaultRepositoryFactory;
import org.apache.maven.internal.impl.DefaultSettingsBuilder;
import org.apache.maven.internal.impl.DefaultSettingsXmlFactory;
import org.apache.maven.internal.impl.DefaultToolchainsBuilder;
import org.apache.maven.internal.impl.DefaultToolchainsXmlFactory;
import org.apache.maven.internal.impl.DefaultTransportProvider;
import org.apache.maven.internal.impl.DefaultVersionParser;
import org.apache.maven.internal.impl.DefaultVersionRangeResolver;
import org.apache.maven.internal.impl.DefaultVersionResolver;
import org.apache.maven.model.path.DefaultPathTranslator;
import org.apache.maven.model.path.ProfileActivationFilePathInterpolator;
import org.apache.maven.model.profile.DefaultProfileSelector;
import org.apache.maven.model.profile.activation.FileProfileActivator;
import org.apache.maven.model.profile.activation.JdkVersionProfileActivator;
import org.apache.maven.model.profile.activation.OperatingSystemProfileActivator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.apache.maven.model.root.DefaultRootLocator;
import org.apache.maven.repository.internal.DefaultModelVersionParser;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.version.GenericVersionScheme;

public class ApiRunner {

    /**
     * Create a new session.
     */
    public static Session createSession() {
        Injector injector = Injector.create();
        injector.bindInstance(Injector.class, injector);
        injector.bindImplicit(ApiRunner.class);
        injector.bindImplicit(DefaultArtifactCoordinateFactory.class);
        injector.bindImplicit(DefaultArtifactDeployer.class);
        injector.bindImplicit(DefaultArtifactFactory.class);
        injector.bindImplicit(DefaultArtifactInstaller.class);
        injector.bindImplicit(DefaultArtifactResolver.class);
        injector.bindImplicit(DefaultChecksumAlgorithmService.class);
        injector.bindImplicit(DefaultDependencyCollector.class);
        injector.bindImplicit(DefaultDependencyCoordinateFactory.class);
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
        injector.bindImplicit(DefaultVersionResolver.class);

        return injector.getInstance(Session.class);
    }

    static class DefaultSession extends AbstractSession {

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
        }

        @Override
        protected Session newSession(RepositorySystemSession session, List<RemoteRepository> repositories) {
            return new DefaultSession(session, repositorySystem, repositories, null, lookup);
        }

        @Override
        public Settings getSettings() {
            return null;
        }

        @Override
        public Map<String, String> getUserProperties() {
            return null;
        }

        @Override
        public Map<String, String> getSystemProperties() {
            return null;
        }

        @Override
        public Map<String, String> getEffectiveProperties(Project project) {
            return null;
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
            return null;
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
            public void setPath(Artifact artifact, Path path) {
                paths.put(artifact, path);
            }
        };
    }

    @Provides
    static DefaultModelVersionParser newModelVersionParser() {
        return new DefaultModelVersionParser(new GenericVersionScheme());
    }

    @Provides
    static Session newSession(Lookup lookup) {
        Map<String, String> properties = new HashMap<>();
        // Env variables prefixed with "env."
        System.getenv().forEach((k, v) -> properties.put("env." + k, v));
        // Java System properties
        System.getProperties().forEach((k, v) -> properties.put(k.toString(), v.toString()));

        RepositorySystem system = new RepositorySystemSupplier().get();

        // SettingsDecrypter settingsDecrypter =
        // (SettingsDecrypter)Objects.requireNonNull(this.createSettingsDecrypter(preBoot));
        new DefaultProfileSelector(List.of(
                new JdkVersionProfileActivator(),
                new PropertyProfileActivator(),
                new OperatingSystemProfileActivator(),
                new FileProfileActivator(new ProfileActivationFilePathInterpolator(
                        new DefaultPathTranslator(), new DefaultRootLocator()))));

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
        //        settings.getDelegate().getRepositories().stream()
        //                        .map(r -> SettingsUtilsV4.)
        //        defaultSession.getService(RepositoryFactory.class).createRemote()
        return defaultSession;
    }

    static class UnsupportedInStandaloneModeException extends MavenException {}
}
