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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Constants;
import org.apache.maven.api.Lifecycle;
import org.apache.maven.api.MonotonicClock;
import org.apache.maven.api.Packaging;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.Type;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
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
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.settings.Mirror;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.api.spi.TypeProvider;
import org.apache.maven.api.toolchain.ToolchainModel;
import org.apache.maven.di.Injector;
import org.apache.maven.di.Key;
import org.apache.maven.di.impl.DIException;
import org.apache.maven.di.impl.InjectorImpl;
import org.apache.maven.impl.AbstractSession;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.di.SessionScope;
import org.apache.maven.impl.model.DefaultInterpolator;
import org.apache.maven.impl.resolver.MavenSessionBuilderSupplier;
import org.apache.maven.impl.resolver.scopes.Maven4ScopeManagerConfiguration;
import org.codehaus.plexus.components.secdispatcher.Cipher;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.MasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.cipher.AESGCMNoPadding;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.LegacyDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.dispatchers.MasterDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.sources.EnvMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.GpgAgentMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.PinEntryMasterSource;
import org.codehaus.plexus.components.secdispatcher.internal.sources.SystemPropertyMasterSource;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * Provides functionality for running Maven API in a standalone mode.
 * <p>
 * This class serves as the main entry point for executing Maven operations outside
 * of the standard Maven build environment. It provides methods for creating and
 * managing Maven sessions in a simplified context, suitable for tools, IDE integrations,
 * and specialized execution scenarios.
 * </p>
 *
 * <p>The standalone session reads and applies the user's {@code settings.xml} including:</p>
 * <ul>
 *   <li>Local repository location</li>
 *   <li>Server authentication (username, password, private key)</li>
 *   <li>Proxy configuration</li>
 *   <li>Mirror configuration</li>
 *   <li>Repository definitions from active profiles</li>
 *   <li>Offline mode</li>
 * </ul>
 *
 * <p>It also loads {@code maven-system.properties} from the Maven configuration directory
 * ({@code ${maven.home}/conf} or as configured via {@code maven.installation.conf}/{@code maven.conf})
 * and from the user's Maven home ({@code ~/.m2/}), merging both into the session's system properties.
 * User properties are loaded from {@code ~/.m2/maven-user.properties} and exposed via
 * {@link Session#getUserProperties()}.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Session session = ApiRunner.createSession();
 * // Use session for Maven operations
 * </pre>
 *
 * <p>
 * The standalone mode provides a subset of Maven's functionality, with some
 * features being unavailable or simplified. Operations not supported in
 * standalone mode will throw {@link UnsupportedInStandaloneModeException}.
 * </p>
 *
 * @since 4.0.0
 */
public class ApiRunner {

    /**
     * Controls how the standalone session handles settings encryption/decryption.
     *
     * <p>Maven settings ({@code settings.xml}) may contain encrypted server passwords.
     * Decryption requires the {@code plexus-sec-dispatcher} library on the classpath.
     * This enum lets callers control the behavior when dispatchers are (or are not) available.</p>
     */
    public enum SecurityMode {
        /** Do not attempt to configure security dispatchers. Encrypted passwords are passed through as-is. */
        NONE,
        /** Try to configure security dispatchers; silently skip if the required classes are not on the classpath. */
        IF_AVAILABLE,
        /**
         * Try to configure security dispatchers; warn to {@code System.err} if the required classes
         * are not on the classpath. This is the default.
         */
        IF_AVAILABLE_WARN,
        /** Security dispatchers are required; throw {@link MavenException} if they cannot be configured. */
        REQUIRED
    }

    /**
     * Creates a new Maven session with default configuration.
     *
     * @return a new {@link Session} instance
     */
    public static Session createSession() {
        return createSession(null);
    }

    /**
     * Creates a new Maven session with custom injector configuration.
     *
     * @param injectorConsumer consumer function to customize the injector
     * @return a new {@link Session} instance
     */
    public static Session createSession(Consumer<Injector> injectorConsumer) {
        return createSession(injectorConsumer, null);
    }

    /**
     * Creates a new Maven session with custom injector configuration and local repository path.
     *
     * @param injectorConsumer consumer function to customize the injector
     * @param localRepo path to the local repository
     * @return a new {@link Session} instance
     */
    public static Session createSession(Consumer<Injector> injectorConsumer, Path localRepo) {
        return createSession(injectorConsumer, localRepo, SecurityMode.IF_AVAILABLE_WARN);
    }

    /**
     * Creates a new Maven session with custom injector configuration, local repository path,
     * and security mode.
     *
     * @param injectorConsumer consumer function to customize the injector
     * @param localRepo path to the local repository
     * @param securityMode controls how encrypted passwords in settings are handled
     * @return a new {@link Session} instance
     */
    public static Session createSession(
            Consumer<Injector> injectorConsumer, Path localRepo, SecurityMode securityMode) {
        Injector injector = Injector.create();
        injector.bindInstance(Injector.class, injector);
        injector.bindImplicit(ApiRunner.class);
        injector.bindImplicit(RepositorySystemSupplier.class);
        injector.bindInstance(LocalRepoProvider.class, () -> localRepo);
        injector.discover(ApiRunner.class.getClassLoader());
        configureSecurityDispatchers(injector, securityMode != null ? securityMode : SecurityMode.IF_AVAILABLE_WARN);
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

    /**
     * Attempts to bind security dispatcher classes for settings password decryption.
     * The dispatchers (legacy and master) are loaded from the {@code plexus-sec-dispatcher}
     * library. If the library is not on the classpath, the behavior depends on the
     * {@link SecurityMode}.
     *
     * <p>If dispatchers are already bound (e.g., via {@code discover()} in tests or by a
     * custom {@code injectorConsumer}), this method is a no-op to avoid duplicate bindings.</p>
     */
    private static void configureSecurityDispatchers(Injector injector, SecurityMode mode) {
        if (mode == SecurityMode.NONE) {
            return;
        }
        try {
            Class.forName("org.codehaus.plexus.components.secdispatcher.Dispatcher");
            // Skip if dispatchers are already bound (e.g., from discover() scanning test classes)
            if (!hasExistingDispatchers(injector)) {
                injector.bindImplicit(SecDispatcherBindings.class);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            switch (mode) {
                case REQUIRED:
                    throw new MavenException(
                            "Security dispatchers required but plexus-sec-dispatcher is not on the classpath", e);
                case IF_AVAILABLE_WARN:
                    System.err.println("WARNING: plexus-sec-dispatcher not available on classpath; "
                            + "encrypted passwords in settings.xml will not be decrypted");
                    break;
                default:
                    break;
            }
        }
    }

    private static boolean hasExistingDispatchers(Injector injector) {
        if (injector instanceof InjectorImpl impl) {
            Set<?> bindings = impl.getAllBindings(Dispatcher.class);
            return bindings != null && !bindings.isEmpty();
        }
        return false;
    }

    /**
     * Interface for providing the local repository path.
     */
    interface LocalRepoProvider {
        /**
         * Gets the path to the local repository.
         *
         * @return the local repository path
         */
        Path getLocalRepo();
    }

    /**
     * Default implementation of the Maven session for standalone mode.
     */
    static class DefaultSession extends AbstractSession {

        private final Map<String, String> systemProperties;
        private final Instant startTime = MonotonicClock.now();
        private Settings settings;
        private Version mavenVersion;
        private Map<String, String> userProperties = Map.of();

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
            DefaultSession newSession = new DefaultSession(session, repositorySystem, repositories, null, lookup);
            newSession.settings = this.settings;
            newSession.mavenVersion = this.mavenVersion;
            newSession.userProperties = this.userProperties;
            return newSession;
        }

        void setSettings(Settings settings) {
            this.settings = settings;
        }

        void setMavenVersion(Version mavenVersion) {
            this.mavenVersion = mavenVersion;
        }

        void setUserProperties(Map<String, String> userProperties) {
            this.userProperties = userProperties != null ? userProperties : Map.of();
        }

        @Override
        @Nonnull
        public Settings getSettings() {
            return settings != null ? settings : Settings.newInstance();
        }

        @Override
        @Nonnull
        public Collection<ToolchainModel> getToolchains() {
            return List.of();
        }

        @Override
        public Map<String, String> getUserProperties() {
            return userProperties;
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
            return mavenVersion;
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

        Path userHome = Paths.get(properties.get("user.home"));
        Path mavenUserHome = userHome.resolve(".m2");
        Path mavenSystemHome = properties.containsKey("maven.home")
                ? Paths.get(properties.get("maven.home"))
                : properties.containsKey("env.MAVEN_HOME") ? Paths.get(properties.get("env.MAVEN_HOME")) : null;

        // Load maven-system.properties: installation-level first (sets paths like maven.user.conf),
        // then user-level (~/.m2/) for custom overrides.  The installation-level file is a bootstrapper
        // that in the CLI includes user and project-level files via ${includes}; since we don't support
        // that directive, we load the user-level file explicitly.
        Path mavenConf = resolveMavenConf(properties);
        if (mavenConf != null) {
            Map<String, String> systemFileProps =
                    loadMavenProperties(mavenConf.resolve("maven-system.properties"), properties);
            properties.putAll(systemFileProps);
        }
        properties.putAll(loadMavenProperties(mavenUserHome.resolve("maven-system.properties"), properties));

        // Load maven-user.properties from the user-level location (~/.m2/) only.
        // The installation-level maven-user.properties (in ${maven.conf}/) contains Maven-internal
        // configuration (cache config, conflict resolver) that is not appropriate for standalone use;
        // it is a CLI bootstrapper that includes user and project-level files via ${includes}.
        Map<String, String> userProperties = new HashMap<>();
        userProperties.putAll(loadMavenProperties(mavenUserHome.resolve("maven-user.properties"), properties));

        // Configure the resolver session with dependency resolution machinery
        MavenSessionBuilderSupplier sessionBuilderSupplier = new MavenSessionBuilderSupplier(system, false);
        DefaultRepositorySystemSession rsession = new DefaultRepositorySystemSession(h -> false);
        rsession.setScopeManager(new ScopeManagerImpl(Maven4ScopeManagerConfiguration.INSTANCE));
        rsession.setDependencyTraverser(sessionBuilderSupplier.getDependencyTraverser());
        rsession.setDependencyManager(sessionBuilderSupplier.getDependencyManager(true));
        rsession.setDependencySelector(sessionBuilderSupplier.getDependencySelector());
        rsession.setDependencyGraphTransformer(sessionBuilderSupplier.getDependencyGraphTransformer());
        rsession.setArtifactTypeRegistry(sessionBuilderSupplier.getArtifactTypeRegistry());
        rsession.setArtifactDescriptorPolicy(sessionBuilderSupplier.getArtifactDescriptorPolicy());
        rsession.setSystemProperties(properties);
        rsession.setUserProperties(userProperties);
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

        // Store the effective settings and user properties on the session
        session.setSettings(settings);
        session.setUserProperties(userProperties);

        // Set the Maven version
        session.setMavenVersion(detectMavenVersion(lookup));

        // local repository
        String localRepository = settings.getLocalRepository() != null
                        && !settings.getLocalRepository().isEmpty()
                ? settings.getLocalRepository()
                : localRepoProvider != null && localRepoProvider.getLocalRepo() != null
                        ? localRepoProvider.getLocalRepo().toString()
                        : mavenUserHome.resolve("repository").toString();
        LocalRepositoryManager llm = system.newLocalRepositoryManager(rsession, new LocalRepository(localRepository));
        rsession.setLocalRepositoryManager(llm);

        // Apply offline mode from settings
        if (settings.isOffline()) {
            rsession.setOffline(true);
        }

        // Apply proxy configuration from settings
        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : settings.getProxies()) {
            if (proxy.isActive()) {
                AuthenticationBuilder authBuilder = new AuthenticationBuilder();
                authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
                proxySelector.add(
                        new org.eclipse.aether.repository.Proxy(
                                proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                        proxy.getNonProxyHosts());
            }
        }
        rsession.setProxySelector(proxySelector);

        // Apply mirror configuration from settings
        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(
                    mirror.getId(),
                    mirror.getUrl(),
                    mirror.getLayout(),
                    false,
                    mirror.isBlocked(),
                    mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        rsession.setMirrorSelector(mirrorSelector);

        // Apply server authentication from settings
        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : settings.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());
        }
        rsession.setAuthenticationSelector(authSelector);

        // Build repositories from active profiles in settings
        SettingsBuilder settingsBuilder = session.getService(SettingsBuilder.class);
        Profile profile = settingsBuilder.convert(org.apache.maven.api.settings.Profile.newBuilder()
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
    }

    /**
     * Resolves the Maven configuration directory following the same lookup order as the Maven CLI:
     * {@code maven.installation.conf} → {@code maven.conf} → {@code ${maven.home}/conf} → {@code ${MAVEN_HOME}/conf}.
     *
     * @param properties the system properties (env + Java system properties)
     * @return the maven conf directory path, or {@code null} if it cannot be determined
     */
    private static Path resolveMavenConf(Map<String, String> properties) {
        String installConf = properties.get(Constants.MAVEN_INSTALLATION_CONF);
        if (installConf != null) {
            return Paths.get(installConf);
        }
        String mavenConf = properties.get("maven.conf");
        if (mavenConf != null) {
            return Paths.get(mavenConf);
        }
        String mavenHome = properties.get(Constants.MAVEN_HOME);
        if (mavenHome != null) {
            return Paths.get(mavenHome).resolve("conf");
        }
        String envMavenHome = properties.get("env.MAVEN_HOME");
        if (envMavenHome != null) {
            return Paths.get(envMavenHome).resolve("conf");
        }
        return null;
    }

    /**
     * Loads properties from a file and interpolates {@code ${...}} references against the given
     * fallback properties. If the file does not exist or cannot be read, an empty map is returned.
     *
     * @param path the properties file to load
     * @param fallback fallback values for interpolation (typically system properties)
     * @return the loaded and interpolated properties
     */
    private static Map<String, String> loadMavenProperties(Path path, Map<String, String> fallback) {
        if (path == null || !Files.exists(path)) {
            return new HashMap<>();
        }
        Properties fileProps = new Properties();
        try (InputStream is = Files.newInputStream(path)) {
            fileProps.load(is);
        } catch (IOException e) {
            // Silently ignore: properties files are optional configuration and the standalone
            // API has no logger.  Failing to read a file (permissions, concurrent deletion)
            // should not prevent session creation — callers get the same behavior as if the
            // file did not exist.
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        fileProps.forEach((k, v) -> result.put(k.toString(), v.toString()));
        // Interpolate ${...} references against the loaded properties + fallback
        for (Map.Entry<String, String> entry : result.entrySet()) {
            String value = entry.getValue();
            if (value != null && value.contains("${")) {
                entry.setValue(
                        DefaultInterpolator.substVars(value, entry.getKey(), null, result, fallback::get, null, false));
            }
        }
        return result;
    }

    /**
     * Detects the Maven version by reading the pom.properties resource from the classpath.
     * Falls back to reading from maven-impl's own pom.properties if maven-core is not available.
     * If no version can be determined, returns {@code 0.0.0} as a sentinel value so that
     * {@link Session#getMavenVersion()} is never null.
     *
     * @param lookup the lookup service
     * @return the detected Maven version, never {@code null}
     */
    @Nonnull
    private static Version detectMavenVersion(Lookup lookup) {
        String version = loadVersionFromProperties("META-INF/maven/org.apache.maven/maven-core/pom.properties");
        if (version == null) {
            version = loadVersionFromProperties("META-INF/maven/org.apache.maven/maven-impl/pom.properties");
        }
        if (version == null) {
            version = "0.0.0";
        }
        try {
            return lookup.lookup(VersionParser.class).parseVersion(version);
        } catch (Exception e) {
            // Should not happen with "0.0.0", but be safe
            return lookup.lookup(VersionParser.class).parseVersion("0.0.0");
        }
    }

    private static String loadVersionFromProperties(String resource) {
        try (InputStream is = ApiRunner.class.getResourceAsStream("/" + resource)) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String version = props.getProperty("version", "").trim();
                if (!version.isEmpty() && !version.startsWith("${")) {
                    return version;
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return null;
    }

    record DumbPackaging(String id, Type type, Map<String, PluginContainer> plugins) implements Packaging {}

    /**
     * Provides the security dispatcher bindings needed for decrypting encrypted passwords
     * in settings.xml. This class is only loaded when {@code plexus-sec-dispatcher} is confirmed
     * to be on the classpath (see {@link #configureSecurityDispatchers}).
     *
     * <p>Supports both legacy ({@code {...}}) and Maven 4 master-key-based encrypted passwords.</p>
     */
    @SuppressWarnings("unused")
    static class SecDispatcherBindings {

        @Provides
        @Named(LegacyDispatcher.NAME)
        static Dispatcher legacyDispatcher() {
            return new LegacyDispatcher();
        }

        @Provides
        @Named(MasterDispatcher.NAME)
        static Dispatcher masterDispatcher(Map<String, Cipher> ciphers, Map<String, MasterSource> sources) {
            return new MasterDispatcher(ciphers, sources);
        }

        @Provides
        @Named(AESGCMNoPadding.CIPHER_ALG)
        static Cipher aesCipher() {
            return new AESGCMNoPadding();
        }

        @Provides
        @Named(EnvMasterSource.NAME)
        static MasterSource envSource() {
            return new EnvMasterSource();
        }

        @Provides
        @Named(GpgAgentMasterSource.NAME)
        static MasterSource gpgAgentSource() {
            return new GpgAgentMasterSource();
        }

        @Provides
        @Named(PinEntryMasterSource.NAME)
        static MasterSource pinEntrySource() {
            return new PinEntryMasterSource();
        }

        @Provides
        @Named(SystemPropertyMasterSource.NAME)
        static MasterSource systemPropertySource() {
            return new SystemPropertyMasterSource();
        }
    }
}
