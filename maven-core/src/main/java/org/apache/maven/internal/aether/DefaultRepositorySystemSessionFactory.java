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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.model.ModelBase;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.graph.version.*;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.eclipse.aether.version.VersionScheme;
import org.eclipse.sisu.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.3.0
 */
@Named
public class DefaultRepositorySystemSessionFactory {
    /**
     * User property for version filters expression, a comma separated list of filters to apply. By default no version
     * filter is applied (like in Maven 3).
     * <p>
     * Supported filters:
     * <ul>
     *     <li>"h" or "h(num)" - highest version or top list of highest ones filter</li>
     *     <li>"l" or "l(num)" - lowest version or bottom list of lowest ones filter</li>
     *     <li>"s" - contextual snapshot filter</li>
     *     <li>"e(G:A:V)" - predicate filter (leaves out G:A:V from range, if hit, V can be range)</li>
     * </ul>
     * Example filter expression: {@code "h(5),s,e(org.foo:bar:1)} will cause: ranges are filtered for "top 5" (instead
     * full range), snapshots are banned if root project is not a snapshot, and if range for {@code org.foo:bar} is
     * being processed, version 1 is omitted.
     *
     * @since 4.0.0
     */
    private static final String MAVEN_VERSION_FILTERS = "maven.versionFilters";

    /**
     * User property for chained LRM: list of "tail" local repository paths (separated by comma), to be used with
     * {@link ChainedLocalRepositoryManager}.
     * Default value: {@code null}, no chained LRM is used.
     *
     * @since 3.9.0
     */
    private static final String MAVEN_REPO_LOCAL_TAIL = "maven.repo.local.tail";

    /**
     * User property for reverse dependency tree. If enabled, Maven will record ".tracking" directory into local
     * repository with "reverse dependency tree", essentially explaining WHY given artifact is present in local
     * repository.
     * Default: {@code false}, will not record anything.
     *
     * @since 3.9.0
     */
    private static final String MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE = "maven.repo.local.recordReverseTree";

    private static final String MAVEN_RESOLVER_TRANSPORT_KEY = "maven.resolver.transport";

    private static final String MAVEN_RESOLVER_TRANSPORT_DEFAULT = "default";

    private static final String MAVEN_RESOLVER_TRANSPORT_WAGON = "wagon";

    private static final String MAVEN_RESOLVER_TRANSPORT_APACHE = "apache";

    private static final String MAVEN_RESOLVER_TRANSPORT_JDK = "jdk";

    /**
     * This name for Apache HttpClient transport is deprecated.
     *
     * @deprecated Renamed to {@link #MAVEN_RESOLVER_TRANSPORT_APACHE}
     */
    @Deprecated
    private static final String MAVEN_RESOLVER_TRANSPORT_NATIVE = "native";

    private static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";

    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";

    private static final String APACHE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.ApacheTransporterFactory";

    private static final String JDK_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.JdkTransporterFactory";

    private static final String FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";

    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtifactHandlerManager artifactHandlerManager;

    private final RepositorySystem repoSystem;

    private final WorkspaceReader workspaceRepository;

    private final SettingsDecrypter settingsDecrypter;

    private final EventSpyDispatcher eventSpyDispatcher;

    private final RuntimeInformation runtimeInformation;

    private final TypeRegistry typeRegistry;

    private final VersionScheme versionScheme;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public DefaultRepositorySystemSessionFactory(
            ArtifactHandlerManager artifactHandlerManager,
            RepositorySystem repoSystem,
            @Nullable @Named("ide") WorkspaceReader workspaceRepository,
            SettingsDecrypter settingsDecrypter,
            EventSpyDispatcher eventSpyDispatcher,
            RuntimeInformation runtimeInformation,
            TypeRegistry typeRegistry,
            VersionScheme versionScheme) {
        this.artifactHandlerManager = artifactHandlerManager;
        this.repoSystem = repoSystem;
        this.workspaceRepository = workspaceRepository;
        this.settingsDecrypter = settingsDecrypter;
        this.eventSpyDispatcher = eventSpyDispatcher;
        this.runtimeInformation = runtimeInformation;
        this.typeRegistry = typeRegistry;
        this.versionScheme = versionScheme;
    }

    @Deprecated
    public RepositorySystemSession newRepositorySession(MavenExecutionRequest request) {
        return newRepositorySessionBuilder(request).build();
    }

    @SuppressWarnings("checkstyle:methodLength")
    public SessionBuilder newRepositorySessionBuilder(MavenExecutionRequest request) {
        SessionBuilder session = MavenRepositorySystemUtils.newSession(
                repoSystem.createSessionBuilder(), new TypeRegistryAdapter(typeRegistry));
        session.setCache(request.getRepositoryCache());

        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, request.isInteractiveMode());
        configProps.put("maven.startTime", request.getStartTime());
        // First add properties populated from settings.xml
        configProps.putAll(getPropertiesFromRequestedProfiles(request));
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(request.getSystemProperties());
        configProps.putAll(request.getUserProperties());

        session.setOffline(request.isOffline());
        session.setChecksumPolicy(request.getGlobalChecksumPolicy());
        session.setUpdatePolicy(
                request.isNoSnapshotUpdates()
                        ? RepositoryPolicy.UPDATE_POLICY_NEVER
                        : request.isUpdateSnapshots() ? RepositoryPolicy.UPDATE_POLICY_ALWAYS : null);

        int errorPolicy = 0;
        errorPolicy |= request.isCacheNotFound()
                ? ResolutionErrorPolicy.CACHE_NOT_FOUND
                : ResolutionErrorPolicy.CACHE_DISABLED;
        errorPolicy |= request.isCacheTransferError()
                ? ResolutionErrorPolicy.CACHE_TRANSFER_ERROR
                : ResolutionErrorPolicy.CACHE_DISABLED;
        session.setResolutionErrorPolicy(
                new SimpleResolutionErrorPolicy(errorPolicy, errorPolicy | ResolutionErrorPolicy.CACHE_NOT_FOUND));

        session.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(
                request.isIgnoreMissingArtifactDescriptor(), request.isIgnoreInvalidArtifactDescriptor()));

        VersionFilter versionFilter = buildVersionFilter(configProps);
        if (versionFilter != null) {
            session.setVersionFilter(versionFilter);
        }

        session.setArtifactTypeRegistry(RepositoryUtils.newArtifactTypeRegistry(artifactHandlerManager));

        session.setWorkspaceReader(
                request.getWorkspaceReader() != null ? request.getWorkspaceReader() : workspaceRepository);

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(request.getProxies());
        decrypt.setServers(request.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (logger.isDebugEnabled()) {
            for (SettingsProblem problem : decrypted.getProblems()) {
                logger.debug(problem.getMessage(), problem.getException());
            }
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
        session.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : decrypted.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        // Note: we do NOT use WagonTransportConfigurationKeys here as Maven Core does NOT depend on Wagon Transport
        // and this is okay and "good thing".
        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                XmlNode dom = server.getDelegate().getConfiguration();
                List<XmlNode> children = dom.getChildren().stream()
                        .filter(c -> !"wagonProvider".equals(c.getName()))
                        .collect(Collectors.toList());
                dom = new XmlNodeImpl(dom.getName(), null, null, children, null);
                PlexusConfiguration config = XmlPlexusConfiguration.toPlexusConfiguration(dom);
                configProps.put("aether.transport.wagon.config." + server.getId(), config);

                // Translate to proper resolver configuration properties as well (as Plexus XML above is Wagon specific
                // only) but support only configuration/httpConfiguration/all, see
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
                                logger.warn("Settings for server {} uses legacy format", server.getId());
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
                                logger.warn("Settings for server {} uses legacy format", server.getId());
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
        session.setAuthenticationSelector(authSelector);

        Object transport = configProps.getOrDefault(MAVEN_RESOLVER_TRANSPORT_KEY, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
        if (MAVEN_RESOLVER_TRANSPORT_DEFAULT.equals(transport)) {
            // The "default" mode (user did not set anything) from now on defaults to AUTO
        } else if (MAVEN_RESOLVER_TRANSPORT_JDK.equals(transport)) {
            // Make sure (whatever extra priority is set) that resolver file/jdk is selected
            configProps.put(FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(JDK_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_APACHE.equals(transport)
                || MAVEN_RESOLVER_TRANSPORT_NATIVE.equals(transport)) {
            if (MAVEN_RESOLVER_TRANSPORT_NATIVE.equals(transport)) {
                logger.warn(
                        "Transport name '{}' is DEPRECATED/RENAMED, use '{}' instead",
                        MAVEN_RESOLVER_TRANSPORT_NATIVE,
                        MAVEN_RESOLVER_TRANSPORT_APACHE);
            }
            // Make sure (whatever extra priority is set) that resolver file/apache is selected
            configProps.put(FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(APACHE_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_WAGON.equals(transport)) {
            // Make sure (whatever extra priority is set) that wagon is selected
            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (!MAVEN_RESOLVER_TRANSPORT_AUTO.equals(transport)) {
            throw new IllegalArgumentException("Unknown resolver transport '" + transport
                    + "'. Supported transports are: " + MAVEN_RESOLVER_TRANSPORT_WAGON + ", "
                    + MAVEN_RESOLVER_TRANSPORT_APACHE + ", " + MAVEN_RESOLVER_TRANSPORT_JDK + ", "
                    + MAVEN_RESOLVER_TRANSPORT_AUTO);
        }

        session.setUserProperties(request.getUserProperties());
        session.setSystemProperties(request.getSystemProperties());
        session.setConfigProperties(configProps);

        session.setTransferListener(request.getTransferListener());

        RepositoryListener repositoryListener = eventSpyDispatcher.chainListener(new LoggingRepositoryListener(logger));

        boolean recordReverseTree = configProps.containsKey(MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE)
                && Boolean.parseBoolean((String) configProps.get(MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE));
        if (recordReverseTree) {
            repositoryListener = new ChainedRepositoryListener(repositoryListener, new ReverseTreeRepositoryListener());
        }
        session.setRepositoryListener(repositoryListener);

        injectMirror(request.getRemoteRepositories(), request.getMirrors());
        injectProxy(proxySelector, request.getRemoteRepositories());
        injectAuthentication(authSelector, request.getRemoteRepositories());

        injectMirror(request.getPluginArtifactRepositories(), request.getMirrors());
        injectProxy(proxySelector, request.getPluginArtifactRepositories());
        injectAuthentication(authSelector, request.getPluginArtifactRepositories());

        ArrayList<File> paths = new ArrayList<>();
        paths.add(new File(request.getLocalRepository().getBasedir()));
        String localRepoTail = (String) configProps.get(MAVEN_REPO_LOCAL_TAIL);
        if (localRepoTail != null) {
            Arrays.stream(localRepoTail.split(","))
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .map(File::new)
                    .forEach(paths::add);
        }
        session.withLocalRepositoryBaseDirectories(paths);

        return session;
    }

    private VersionFilter buildVersionFilter(Map<Object, Object> configProps) {
        ArrayList<VersionFilter> filters = new ArrayList<>();
        String filterExpression = (String) configProps.get(MAVEN_VERSION_FILTERS);
        if (filterExpression != null) {
            List<String> expressions = Arrays.stream(filterExpression.split(","))
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .collect(Collectors.toList());
            for (String expression : expressions) {
                if ("h".equals(expression)) {
                    filters.add(new HighestVersionFilter());
                } else if (expression.startsWith("h(") && expression.endsWith(")")) {
                    int num = Integer.parseInt(expression.substring(2, expression.length() - 1));
                    // MRESOLVER-450
                    // filters.add(new HighestVersionFilter(num));
                } else if ("l".equals(expression)) {
                    filters.add(new LowestVersionFilter());
                } else if (expression.startsWith("l(") && expression.endsWith(")")) {
                    int num = Integer.parseInt(expression.substring(2, expression.length() - 1));
                    // MRESOLVER-450
                    // filters.add(new LowestVersionFilter(num));
                } else if ("s".equals(expression)) {
                    filters.add(new ContextualSnapshotVersionFilter());
                } else if (expression.startsWith("e(") && expression.endsWith(")")) {
                    Artifact artifact = new DefaultArtifact(expression.substring(2, expression.length() - 1));
                    VersionRange versionRange =
                            artifact.getVersion().contains(",") ? parseVersionRange(artifact.getVersion()) : null;
                    Predicate<Artifact> predicate = a -> {
                        if (artifact.getGroupId().equals(a.getGroupId())
                                && artifact.getArtifactId().equals(a.getArtifactId())) {
                            if (versionRange != null) {
                                Version v = parseVersion(a.getVersion());
                                return !versionRange.containsVersion(v);
                            } else {
                                return !artifact.getVersion().equals(a.getVersion());
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

    private VersionRange parseVersionRange(String spec) {
        try {
            return versionScheme.parseVersionRange(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<?, ?> getPropertiesFromRequestedProfiles(MavenExecutionRequest request) {
        HashSet<String> activeProfileId =
                new HashSet<>(request.getProfileActivation().getRequiredActiveProfileIds());
        activeProfileId.addAll(request.getProfileActivation().getOptionalActiveProfileIds());

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

    private void injectMirror(List<ArtifactRepository> repositories, List<Mirror> mirrors) {
        if (repositories != null && mirrors != null) {
            for (ArtifactRepository repository : repositories) {
                Mirror mirror = MavenRepositorySystem.getMirror(repository, mirrors);
                injectMirror(repository, mirror);
            }
        }
    }

    private void injectMirror(ArtifactRepository repository, Mirror mirror) {
        if (mirror != null) {
            ArtifactRepository original = MavenRepositorySystem.createArtifactRepository(
                    repository.getId(),
                    repository.getUrl(),
                    repository.getLayout(),
                    repository.getSnapshots(),
                    repository.getReleases());

            repository.setMirroredRepositories(Collections.singletonList(original));

            repository.setId(mirror.getId());
            repository.setUrl(mirror.getUrl());

            if (mirror.getLayout() != null && !mirror.getLayout().isEmpty()) {
                repository.setLayout(original.getLayout());
            }

            repository.setBlocked(mirror.isBlocked());
        }
    }

    private void injectProxy(ProxySelector selector, List<ArtifactRepository> repositories) {
        if (repositories != null && selector != null) {
            for (ArtifactRepository repository : repositories) {
                repository.setProxy(getProxy(selector, repository));
            }
        }
    }

    private org.apache.maven.repository.Proxy getProxy(ProxySelector selector, ArtifactRepository repository) {
        if (selector != null) {
            RemoteRepository repo = RepositoryUtils.toRepo(repository);
            org.eclipse.aether.repository.Proxy proxy = selector.getProxy(repo);
            if (proxy != null) {
                org.apache.maven.repository.Proxy p = new org.apache.maven.repository.Proxy();
                p.setHost(proxy.getHost());
                p.setProtocol(proxy.getType());
                p.setPort(proxy.getPort());
                if (proxy.getAuthentication() != null) {
                    repo = new RemoteRepository.Builder(repo).setProxy(proxy).build();
                    AuthenticationContext authCtx = AuthenticationContext.forProxy(null, repo);
                    p.setUserName(authCtx.get(AuthenticationContext.USERNAME));
                    p.setPassword(authCtx.get(AuthenticationContext.PASSWORD));
                    p.setNtlmDomain(authCtx.get(AuthenticationContext.NTLM_DOMAIN));
                    p.setNtlmHost(authCtx.get(AuthenticationContext.NTLM_WORKSTATION));
                    authCtx.close();
                }
                return p;
            }
        }
        return null;
    }

    public void injectAuthentication(AuthenticationSelector selector, List<ArtifactRepository> repositories) {
        if (repositories != null && selector != null) {
            for (ArtifactRepository repository : repositories) {
                repository.setAuthentication(getAuthentication(selector, repository));
            }
        }
    }

    private Authentication getAuthentication(AuthenticationSelector selector, ArtifactRepository repository) {
        if (selector != null) {
            RemoteRepository repo = RepositoryUtils.toRepo(repository);
            org.eclipse.aether.repository.Authentication auth = selector.getAuthentication(repo);
            if (auth != null) {
                repo = new RemoteRepository.Builder(repo)
                        .setAuthentication(auth)
                        .build();
                AuthenticationContext authCtx = AuthenticationContext.forRepository(null, repo);
                Authentication result = new Authentication(
                        authCtx.get(AuthenticationContext.USERNAME), authCtx.get(AuthenticationContext.PASSWORD));
                result.setPrivateKey(authCtx.get(AuthenticationContext.PRIVATE_KEY_PATH));
                result.setPassphrase(authCtx.get(AuthenticationContext.PRIVATE_KEY_PASSPHRASE));
                authCtx.close();
                return result;
            }
        }
        return null;
    }
}
