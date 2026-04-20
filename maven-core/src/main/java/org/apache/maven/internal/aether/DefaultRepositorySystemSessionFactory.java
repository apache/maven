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
package org.apache.maven.internal.aether;

import javax.inject.Inject;
import javax.inject.Named;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.ModelBase;
import org.apache.maven.repository.internal.MavenSessionBuilderSupplier;
import org.apache.maven.repository.internal.scopes.Maven3ScopeManagerConfiguration;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.impl.scope.InternalScopeManager;
import org.eclipse.aether.internal.impl.scope.ScopeManagerImpl;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.graph.manager.TransitiveDependencyManager;
import org.eclipse.aether.util.graph.version.ChainedVersionFilter;
import org.eclipse.aether.util.graph.version.ContextualSnapshotVersionFilter;
import org.eclipse.aether.util.graph.version.HighestVersionFilter;
import org.eclipse.aether.util.graph.version.LowestVersionFilter;
import org.eclipse.aether.util.graph.version.PredicateVersionFilter;
import org.eclipse.aether.util.graph.version.SnapshotVersionFilter;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.sisu.Nullable;

/**
 * @since 3.3.0
 */
@Named
public class DefaultRepositorySystemSessionFactory {
    /**
     * User property for chained LRM: list of "tail" local repository paths (separated by comma), to be used with
     * {@link ChainedLocalRepositoryManager}.
     * Default value: {@code null}, no chained LRM is used.
     *
     * @since 3.9.0
     */
    private static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    /**
     * User property for chained LRM: the new "head" local repository to use, and "push" the existing into tail.
     * Similar to <code>maven.repo.local.tail</code>, this property may contain comma separated list of paths to be
     * used as local repositories (combine with chained local repository), but while latter is "appending" this
     * one is "prepending".
     *
     * @since 3.9.10
     */
    public static final String MAVEN_REPO_LOCAL_HEAD = "maven.repo.local.head";

    /**
     * User property for chained LRM: should artifact availability be ignored in tail local repositories or not.
     * Default: {@code true}, will ignore availability from tail local repositories.
     *
     * @since 3.9.0
     */
    private static final String MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY = "maven.repo.local.tail.ignoreAvailability";

    /**
     * User property for reverse dependency tree. If enabled, Maven will record ".tracking" directory into local
     * repository with "reverse dependency tree", essentially explaining WHY given artifact is present in local
     * repository.
     * Default: {@code false}, will not record anything.
     *
     * @since 3.9.0
     */
    private static final String MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE = "maven.repo.local.recordReverseTree";

    /**
     * User property for version filter expression used in session, applied to resolving ranges: a semicolon separated
     * list of filters to apply. By default, no version filter is applied (like in Maven 3).
     * <br/>
     * Supported filters:
     * <ul>
     *     <li>{@code "h"} or {@code "h(num[@G[:A]])"} - highest version or top list of highest ones filter</li>
     *     <li>{@code "l"} or {@code "l(num[@G[:A]])"} - lowest version or bottom list of lowest ones filter</li>
     *     <li>{@code "s"} - contextual snapshot filter</li>
     *     <li>{@code "ns"} - unconditional snapshot filter (no snapshots selected from ranges)</li>
     *     <li>{@code "e(G:A:V)"} - predicate filter (excludes G:A:V from range, if hit, V can be version constraint)</li>
     *     <li>{@code "i(G:A:V)"} - predicate filter (includes G:A:V from range, if hit, V can be version constraint)</li>
     * </ul>
     * Example filter expression: <code>"h(5);s;e(org.foo:bar:1)</code> will cause: ranges are filtered for "top 5" (instead
     * full range), snapshots are banned if root project is not a snapshot, and if range for <code>org.foo:bar</code> is
     * being processed, version 1 is omitted. Value in this property builds
     * <code>org.eclipse.aether.collection.VersionFilter</code> instance.
     *
     * @since 3.10.0
     */
    private static final String MAVEN_VERSION_FILTER = "maven.session.versionFilter";

    /**
     * User property for selecting dependency manager behavior regarding transitive dependencies and dependency
     * management entries in their POMs. Maven 3 targeted full backward compatibility with Maven 2. Hence, it ignored
     * dependency management entries in transitive dependency POMs. Maven 4 enables "transitivity" by default. Hence
     * unlike Maven 3, it obeys dependency management entries deep in the dependency graph as well.
     * <br/>
     * Default (behave as whole Maven 3.x line): <code>"false"</code>.
     *
     * @since 3.10.0
     */
    public static final String MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY =
            "maven.resolver.dependencyManagerTransitivity";

    private static final String MAVEN_RESOLVER_TRANSPORT_KEY = "maven.resolver.transport";

    private static final String MAVEN_RESOLVER_TRANSPORT_DEFAULT = "default";

    private static final String MAVEN_RESOLVER_TRANSPORT_WAGON = "wagon";

    private static final String MAVEN_RESOLVER_TRANSPORT_APACHE = "native";

    private static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";

    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";

    private static final String APACHE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.ApacheTransporterFactory";

    private static final String FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";

    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    @Inject
    private Logger logger;

    @Inject
    private ArtifactHandlerManager artifactHandlerManager;

    @Inject
    private RepositorySystem repoSystem;

    @Inject
    @Nullable
    @Named("ide")
    private WorkspaceReader workspaceRepository;

    @Inject
    private SettingsDecrypter settingsDecrypter;

    @Inject
    private EventSpyDispatcher eventSpyDispatcher;

    @Inject
    MavenRepositorySystem mavenRepositorySystem;

    @Inject
    private RuntimeInformation runtimeInformation;

    private final GenericVersionScheme versionScheme = new GenericVersionScheme();

    private final InternalScopeManager scopeManager = new ScopeManagerImpl(Maven3ScopeManagerConfiguration.INSTANCE);

    @SuppressWarnings("checkstyle:methodlength")
    public RepositorySystemSession.SessionBuilder newRepositorySession(MavenExecutionRequest request) {
        // config
        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, request.isInteractiveMode());
        configProps.put("maven.startTime", request.getStartTime());
        // First add properties populated from settings.xml
        configProps.putAll(getPropertiesFromRequestedProfiles(request));
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(request.getSystemProperties());
        configProps.putAll(request.getUserProperties());

        RepositorySystemSession.SessionBuilder mainSessionBuilder =
                new MavenSessionBuilderSupplier(repoSystem, scopeManager).get();
        mainSessionBuilder.setCache(request.getRepositoryCache());

        mainSessionBuilder.setOffline(request.isOffline());
        mainSessionBuilder.setChecksumPolicy(request.getGlobalChecksumPolicy());

        if (request.getArtifactsUpdatePolicy() == null && request.getMetadataUpdatePolicy() == null) {
            // we go "old" way
            if (request.isNoSnapshotUpdates()) {
                mainSessionBuilder.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
            } else if (request.isUpdateSnapshots()) {
                mainSessionBuilder.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
            } else {
                mainSessionBuilder.setUpdatePolicy(null);
            }
        } else {
            // we go "new" way
            if (request.getArtifactsUpdatePolicy() != null) {
                mainSessionBuilder.setArtifactUpdatePolicy(request.getArtifactsUpdatePolicy());
            }
            if (request.getMetadataUpdatePolicy() != null) {
                mainSessionBuilder.setMetadataUpdatePolicy(request.getMetadataUpdatePolicy());
            }
        }

        int errorPolicy = 0;
        errorPolicy |= request.isCacheNotFound()
                ? ResolutionErrorPolicy.CACHE_NOT_FOUND
                : ResolutionErrorPolicy.CACHE_DISABLED;
        errorPolicy |= request.isCacheTransferError()
                ? ResolutionErrorPolicy.CACHE_TRANSFER_ERROR
                : ResolutionErrorPolicy.CACHE_DISABLED;
        mainSessionBuilder.setResolutionErrorPolicy(
                new SimpleResolutionErrorPolicy(errorPolicy, errorPolicy | ResolutionErrorPolicy.CACHE_NOT_FOUND));

        mainSessionBuilder.setArtifactTypeRegistry(RepositoryUtils.newArtifactTypeRegistry(artifactHandlerManager));

        if (request.getWorkspaceReader() != null) {
            mainSessionBuilder.setWorkspaceReader(request.getWorkspaceReader());
        } else {
            mainSessionBuilder.setWorkspaceReader(workspaceRepository);
        }

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(request.getProxies());
        decrypt.setServers(request.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (logger.isDebugEnabled()) {
            for (SettingsProblem problem : decrypted.getProblems()) {
                logger.debug(problem.getMessage(), problem.getException());
            }
        }

        VersionFilter versionFilter = buildVersionFilter((String) configProps.get(MAVEN_VERSION_FILTER));
        if (versionFilter != null) {
            mainSessionBuilder.setVersionFilter(versionFilter);
        }

        boolean dependencyManagerTransitivity =
                ConfigUtils.getBoolean(configProps, false, MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY);
        if (dependencyManagerTransitivity) {
            mainSessionBuilder.setDependencyManager(new TransitiveDependencyManager(scopeManager));
        }

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : request.getMirrors()) {
            mirrorSelector.add(
                    mirror.getId(),
                    mirror.getUrl(),
                    mirror.getLayout(),
                    false,
                    mirror.isBlocked(),
                    mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        mainSessionBuilder.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : decrypted.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        mainSessionBuilder.setProxySelector(proxySelector);

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for (int i = dom.getChildCount() - 1; i >= 0; i--) {
                    Xpp3Dom child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    }
                }

                XmlPlexusConfiguration config = new XmlPlexusConfiguration(dom);
                configProps.put("aether.transport.wagon.config." + server.getId(), config);

                // Translate to proper resolver configuration properties as well (as Plexus XML above is Wagon specific
                // only), but support only configuration/httpConfiguration/all, see
                // https://maven.apache.org/guides/mini/guide-http-settings.html
                Map<String, String> headers = null;
                Integer connectTimeout = null;
                Integer requestTimeout = null;

                PlexusConfiguration httpHeaders = config.getChild("httpHeaders", false);
                if (httpHeaders != null) {
                    PlexusConfiguration[] properties = httpHeaders.getChildren("property");
                    if (properties != null && properties.length > 0) {
                        headers = new HashMap<>();
                        for (PlexusConfiguration property : properties) {
                            headers.put(
                                    property.getChild("name").getValue(),
                                    property.getChild("value").getValue());
                        }
                    }
                }

                PlexusConfiguration connectTimeoutXml = config.getChild("connectTimeout", false);
                if (connectTimeoutXml != null) {
                    connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                } else {
                    // fallback configuration name
                    PlexusConfiguration httpConfiguration = config.getChild("httpConfiguration", false);
                    if (httpConfiguration != null) {
                        PlexusConfiguration httpConfigurationAll = httpConfiguration.getChild("all", false);
                        if (httpConfigurationAll != null) {
                            connectTimeoutXml = httpConfigurationAll.getChild("connectionTimeout", false);
                            if (connectTimeoutXml != null) {
                                connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                                logger.warn("Settings for server " + server.getId() + " uses legacy format");
                            }
                        }
                    }
                }

                PlexusConfiguration requestTimeoutXml = config.getChild("requestTimeout", false);
                if (requestTimeoutXml != null) {
                    requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                } else {
                    // fallback configuration name
                    PlexusConfiguration httpConfiguration = config.getChild("httpConfiguration", false);
                    if (httpConfiguration != null) {
                        PlexusConfiguration httpConfigurationAll = httpConfiguration.getChild("all", false);
                        if (httpConfigurationAll != null) {
                            requestTimeoutXml = httpConfigurationAll.getChild("readTimeout", false);
                            if (requestTimeoutXml != null) {
                                requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                                logger.warn("Settings for server " + server.getId() + " uses legacy format");
                            }
                        }
                    }
                }

                // org.eclipse.aether.ConfigurationProperties.HTTP_HEADERS => Map<String, String>
                if (headers != null) {
                    configProps.put(ConfigurationProperties.HTTP_HEADERS + "." + server.getId(), headers);
                }
                // org.eclipse.aether.ConfigurationProperties.CONNECT_TIMEOUT => int
                if (connectTimeout != null) {
                    configProps.put(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId(), connectTimeout);
                }
                // org.eclipse.aether.ConfigurationProperties.REQUEST_TIMEOUT => int
                if (requestTimeout != null) {
                    configProps.put(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId(), requestTimeout);
                }
            }

            configProps.put("aether.transport.wagon.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.transport.wagon.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        mainSessionBuilder.setAuthenticationSelector(authSelector);

        Object transport = configProps.getOrDefault(MAVEN_RESOLVER_TRANSPORT_KEY, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
        if (MAVEN_RESOLVER_TRANSPORT_DEFAULT.equals(transport)) {
            // The "default" mode (user did not set anything) from now on defaults to AUTO
        } else if (MAVEN_RESOLVER_TRANSPORT_APACHE.equals(transport)) {
            // Make sure (whatever extra priority is set) that resolver native is selected
            configProps.put(FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(APACHE_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_WAGON.equals(transport)) {
            // Make sure (whatever extra priority is set) that wagon is selected
            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (!MAVEN_RESOLVER_TRANSPORT_AUTO.equals(transport)) {
            throw new IllegalArgumentException("Unknown resolver transport '" + transport
                    + "'. Supported transports are: " + MAVEN_RESOLVER_TRANSPORT_WAGON + ", "
                    + MAVEN_RESOLVER_TRANSPORT_APACHE + ", " + MAVEN_RESOLVER_TRANSPORT_AUTO);
        }

        mainSessionBuilder.setUserProperties(request.getUserProperties());
        mainSessionBuilder.setSystemProperties(request.getSystemProperties());
        mainSessionBuilder.setConfigProperties(configProps);

        mainSessionBuilder.setTransferListener(request.getTransferListener());

        mainSessionBuilder.setRepositoryListener(
                eventSpyDispatcher.chainListener(new LoggingRepositoryListener(logger)));

        mainSessionBuilder.setIgnoreArtifactDescriptorRepositories(request.isIgnoreTransitiveRepositories());

        try (RepositorySystemSession.CloseableSession protoSession =
                setUpLocalRepositoryManager(request, repoSystem, mainSessionBuilder)) {
            boolean recordReverseTree =
                    ConfigUtils.getBoolean(protoSession, false, MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE);
            if (recordReverseTree) {
                mainSessionBuilder.setRepositoryListener(new ChainedRepositoryListener(
                        protoSession.getRepositoryListener(), new ReverseTreeRepositoryListener()));
            }

            mavenRepositorySystem.injectMirror(request.getRemoteRepositories(), request.getMirrors());
            mavenRepositorySystem.injectProxy(protoSession, request.getRemoteRepositories());
            mavenRepositorySystem.injectAuthentication(protoSession, request.getRemoteRepositories());

            mavenRepositorySystem.injectMirror(request.getPluginArtifactRepositories(), request.getMirrors());
            mavenRepositorySystem.injectProxy(protoSession, request.getPluginArtifactRepositories());
            mavenRepositorySystem.injectAuthentication(protoSession, request.getPluginArtifactRepositories());

            return mainSessionBuilder;
        }
    }

    private static RepositorySystemSession.CloseableSession setUpLocalRepositoryManager(
            MavenExecutionRequest request,
            RepositorySystem repoSystem,
            RepositorySystemSession.SessionBuilder sessionBuilder) {
        Path requestLocalRepositoryPath = resolve(request.getLocalRepository().getBasedir());
        RepositorySystemSession.CloseableSession protoSession = sessionBuilder
                .withLocalRepositoryBaseDirectories(requestLocalRepositoryPath)
                .build();
        LocalRepositoryManager lrm =
                setUpLocalRepositoryManager(request.getLocalRepository().getBasedir(), repoSystem, protoSession);
        sessionBuilder.setLocalRepositoryManager(lrm);
        return protoSession;
    }

    public static LocalRepositoryManager setUpLocalRepositoryManager(
            String localRepository, RepositorySystem repoSystem, RepositorySystemSession session) {
        List<Path> paths = new ArrayList<>();
        String localRepoHead = ConfigUtils.getString(session, null, MAVEN_REPO_LOCAL_HEAD);
        if (localRepoHead != null) {
            Arrays.stream(localRepoHead.split(","))
                    .filter(p -> !p.trim().isEmpty())
                    .map(DefaultRepositorySystemSessionFactory::resolve)
                    .forEach(paths::add);
        }

        paths.add(resolve(localRepository));

        String localRepoTail = ConfigUtils.getString(session, null, MAVEN_REPO_LOCAL_TAIL);
        if (localRepoTail != null) {
            Arrays.stream(localRepoTail.split(","))
                    .filter(p -> !p.trim().isEmpty())
                    .map(DefaultRepositorySystemSessionFactory::resolve)
                    .forEach(paths::add);
        }

        LocalRepository localRepo = new LocalRepository(paths.remove(0));
        LocalRepositoryManager lrm = repoSystem.newLocalRepositoryManager(session, localRepo);

        if (!paths.isEmpty()) {
            List<LocalRepositoryManager> tail = new ArrayList<>();
            for (Path path : paths) {
                tail.add(repoSystem.newLocalRepositoryManager(session, new LocalRepository(path)));
            }
            boolean ignoreTailAvailability =
                    ConfigUtils.getBoolean(session, true, MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY);

            lrm = new ChainedLocalRepositoryManager(lrm, tail, ignoreTailAvailability);
        }
        return lrm;
    }

    public static Path resolve(String string) {
        if (string.startsWith("~/") || string.startsWith("~\\")) {
            // resolve based on $HOME
            return Paths.get(System.getProperty("user.home"))
                    .resolve(string.substring(2))
                    .normalize()
                    .toAbsolutePath();
        } else {
            // resolve based on $CWD
            return Paths.get(string).normalize().toAbsolutePath();
        }
    }

    /**
     * Visible for testing.
     */
    VersionFilter buildVersionFilter(String filterExpression) {
        ArrayList<VersionFilter> filters = new ArrayList<>();
        if (filterExpression != null) {
            List<String> expressions = Arrays.stream(filterExpression.split(";"))
                    .filter(s -> !s.trim().isEmpty())
                    .collect(Collectors.toList());
            for (String expression : expressions) {
                if ("h".equals(expression)) {
                    filters.add(new HighestVersionFilter());
                } else if ("l".equals(expression)) {
                    filters.add(new LowestVersionFilter());
                } else if ((expression.startsWith("h(") || expression.startsWith("l(")) && expression.endsWith(")")) {
                    Function<Integer, VersionFilter> filterSupplier =
                            n -> expression.startsWith("h(") ? new HighestVersionFilter(n) : new LowestVersionFilter(n);
                    String inner = expression.substring(2, expression.length() - 1);
                    int num;
                    String g;
                    String a;
                    if (inner.contains("@")) {
                        num = Integer.parseInt(inner.substring(0, inner.indexOf('@')));
                        String remainder = inner.substring(inner.indexOf('@') + 1);
                        if (remainder.contains(":")) {
                            g = remainder.substring(0, remainder.indexOf(':'));
                            a = remainder.substring(remainder.indexOf(':') + 1);
                        } else {
                            g = remainder;
                            a = null;
                        }
                    } else {
                        num = Integer.parseInt(inner);
                        g = null;
                        a = null;
                    }
                    if (g == null) {
                        filters.add(filterSupplier.apply(num));
                    } else {
                        VersionFilter versionFilter = filterSupplier.apply(num);
                        filters.add(new VersionFilter() {
                            @Override
                            public void filterVersions(VersionFilterContext context) throws RepositoryException {
                                Artifact dependencyArtifact =
                                        context.getDependency().getArtifact();
                                if (g.equals(dependencyArtifact.getGroupId())
                                        && (a == null || a.equals(dependencyArtifact.getArtifactId()))) {
                                    versionFilter.filterVersions(context);
                                }
                            }

                            @Override
                            public VersionFilter deriveChildFilter(DependencyCollectionContext context) {
                                return this;
                            }
                        });
                    }
                } else if ("s".equals(expression)) {
                    filters.add(new ContextualSnapshotVersionFilter());
                } else if ("ns".equals(expression)) {
                    filters.add(new SnapshotVersionFilter());
                } else if ((expression.startsWith("e(") || (expression.startsWith("i("))) && expression.endsWith(")")) {
                    Artifact artifact = new DefaultArtifact(expression.substring(2, expression.length() - 1));
                    VersionConstraint versionConstraint = parseVersionConstraint(artifact.getVersion());
                    Predicate<Artifact> predicate = a -> {
                        if (artifact.getGroupId().equals(a.getGroupId())
                                && artifact.getArtifactId().equals(a.getArtifactId())) {
                            if (expression.startsWith("e(")) {
                                // exclude
                                return !versionConstraint.containsVersion(parseVersion(a.getVersion()));
                            } else {
                                // include
                                return versionConstraint.containsVersion(parseVersion(a.getVersion()));
                            }
                        }
                        return true;
                    };
                    filters.add(new PredicateVersionFilter(predicate));
                } else {
                    throw new IllegalArgumentException("Unsupported filter expression: " + expression);
                }
            }
        }
        if (filters.isEmpty()) {
            return null;
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return ChainedVersionFilter.newInstance(filters);
        }
    }

    private Version parseVersion(String spec) {
        try {
            return versionScheme.parseVersion(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    private VersionConstraint parseVersionConstraint(String spec) {
        try {
            return versionScheme.parseVersionConstraint(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<?, ?> getPropertiesFromRequestedProfiles(MavenExecutionRequest request) {

        List<String> activeProfileId = request.getActiveProfiles();

        return request.getProfiles().stream()
                .filter(profile -> activeProfileId.contains(profile.getId()))
                .map(ModelBase::getProperties)
                .flatMap(properties -> properties.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k2));
    }

    private String getUserAgent() {
        String version = runtimeInformation.getMavenVersion();
        version = version.isEmpty() ? version : "/" + version;
        return "Apache-Maven" + version + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }
}
