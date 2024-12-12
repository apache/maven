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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.TypeRegistry;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.internal.impl.resolver.MavenSessionBuilderSupplier;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.model.ModelBase;
import org.apache.maven.resolver.RepositorySystemSessionFactory;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.VersionFilter;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.graph.version.ChainedVersionFilter;
import org.eclipse.aether.util.graph.version.ContextualSnapshotVersionFilter;
import org.eclipse.aether.util.graph.version.HighestVersionFilter;
import org.eclipse.aether.util.graph.version.LowestVersionFilter;
import org.eclipse.aether.util.graph.version.PredicateVersionFilter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * @since 3.3.0
 */
@Named
@Singleton
public class DefaultRepositorySystemSessionFactory implements RepositorySystemSessionFactory {

    public static final String MAVEN_RESOLVER_TRANSPORT_DEFAULT = "default";

    public static final String MAVEN_RESOLVER_TRANSPORT_WAGON = "wagon";

    public static final String MAVEN_RESOLVER_TRANSPORT_APACHE = "apache";

    public static final String MAVEN_RESOLVER_TRANSPORT_JDK = "jdk";

    /**
     * This name for Apache HttpClient transport is deprecated.
     *
     * @deprecated Renamed to {@link #MAVEN_RESOLVER_TRANSPORT_APACHE}
     */
    @Deprecated
    private static final String MAVEN_RESOLVER_TRANSPORT_NATIVE = "native";

    public static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";

    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";

    private static final String APACHE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.ApacheTransporterFactory";

    private static final String JDK_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.JdkTransporterFactory";

    private static final String FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";

    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RepositorySystem repoSystem;

    private final EventSpyDispatcher eventSpyDispatcher;

    private final RuntimeInformation runtimeInformation;

    private final TypeRegistry typeRegistry;

    private final VersionScheme versionScheme;

    private final Map<String, RepositorySystemSessionExtender> sessionExtenders;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    DefaultRepositorySystemSessionFactory(
            RepositorySystem repoSystem,
            EventSpyDispatcher eventSpyDispatcher,
            RuntimeInformation runtimeInformation,
            TypeRegistry typeRegistry,
            VersionScheme versionScheme,
            Map<String, RepositorySystemSessionExtender> sessionExtenders) {
        this.repoSystem = repoSystem;
        this.eventSpyDispatcher = eventSpyDispatcher;
        this.runtimeInformation = runtimeInformation;
        this.typeRegistry = typeRegistry;
        this.versionScheme = versionScheme;
        this.sessionExtenders = sessionExtenders;
    }

    @Deprecated
    public RepositorySystemSession newRepositorySession(MavenExecutionRequest request) {
        return newRepositorySessionBuilder(request).build();
    }

    @SuppressWarnings({"checkstyle:methodLength"})
    public SessionBuilder newRepositorySessionBuilder(MavenExecutionRequest request) {
        requireNonNull(request, "request");

        MavenSessionBuilderSupplier supplier = new MavenSessionBuilderSupplier(repoSystem);
        SessionBuilder sessionBuilder = supplier.get();
        sessionBuilder.setArtifactTypeRegistry(new TypeRegistryAdapter(typeRegistry)); // dynamic
        sessionBuilder.setCache(request.getRepositoryCache());

        // this map is read ONLY to get config from (profiles + env + system + user)
        Map<String, String> mergedProps = createMergedProperties(request);

        // configProps map is kept "pristine", is written ONLY, the mandatory resolver config
        Map<String, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, request.isInteractiveMode());
        configProps.put("maven.startTime", request.getStartTime());
        configProps.put(Constants.MAVEN_START_INSTANT, request.getStartInstant());

        sessionBuilder.setOffline(request.isOffline());
        sessionBuilder.setChecksumPolicy(request.getGlobalChecksumPolicy());
        sessionBuilder.setUpdatePolicy(
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
        sessionBuilder.setResolutionErrorPolicy(
                new SimpleResolutionErrorPolicy(errorPolicy, errorPolicy | ResolutionErrorPolicy.CACHE_NOT_FOUND));

        sessionBuilder.setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(
                request.isIgnoreMissingArtifactDescriptor(), request.isIgnoreInvalidArtifactDescriptor()));

        VersionFilter versionFilter = buildVersionFilter(mergedProps.get(Constants.MAVEN_VERSION_FILTER));
        if (versionFilter != null) {
            sessionBuilder.setVersionFilter(versionFilter);
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
        sessionBuilder.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : request.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        sessionBuilder.setProxySelector(proxySelector);

        // Note: we do NOT use WagonTransportConfigurationKeys here as Maven Core does NOT depend on Wagon Transport
        // and this is okay and "good thing".
        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : request.getServers()) {
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
        sessionBuilder.setAuthenticationSelector(authSelector);

        Object transport =
                mergedProps.getOrDefault(Constants.MAVEN_RESOLVER_TRANSPORT, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
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

        sessionBuilder.setIgnoreArtifactDescriptorRepositories(request.isIgnoreTransitiveRepositories());

        sessionBuilder.setTransferListener(request.getTransferListener());

        RepositoryListener repositoryListener = eventSpyDispatcher.chainListener(new LoggingRepositoryListener(logger));

        boolean recordReverseTree = Boolean.parseBoolean(
                mergedProps.getOrDefault(Constants.MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE, Boolean.FALSE.toString()));
        if (recordReverseTree) {
            repositoryListener = new ChainedRepositoryListener(repositoryListener, new ReverseTreeRepositoryListener());
        }
        sessionBuilder.setRepositoryListener(repositoryListener);

        // may be overridden
        String resolverDependencyManagerTransitivity = mergedProps.getOrDefault(
                Constants.MAVEN_RESOLVER_DEPENDENCY_MANAGER_TRANSITIVITY, Boolean.TRUE.toString());
        sessionBuilder.setDependencyManager(
                supplier.getDependencyManager(Boolean.parseBoolean(resolverDependencyManagerTransitivity)));

        ArrayList<Path> paths = new ArrayList<>();
        String localRepoHead = mergedProps.get(Constants.MAVEN_REPO_LOCAL_HEAD);
        if (localRepoHead != null) {
            Arrays.stream(localRepoHead.split(","))
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .map(this::resolve)
                    .forEach(paths::add);
        }
        paths.add(Paths.get(request.getLocalRepository().getBasedir()));
        String localRepoTail = mergedProps.get(Constants.MAVEN_REPO_LOCAL_TAIL);
        if (localRepoTail != null) {
            Arrays.stream(localRepoTail.split(","))
                    .filter(p -> p != null && !p.trim().isEmpty())
                    .map(this::resolve)
                    .forEach(paths::add);
        }
        sessionBuilder.withLocalRepositoryBaseDirectories(paths);
        // Pass over property supported by Maven 3.9.x
        if (mergedProps.containsKey(Constants.MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY)) {
            configProps.put(
                    ChainedLocalRepositoryManager.CONFIG_PROP_IGNORE_TAIL_AVAILABILITY,
                    mergedProps.get(Constants.MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY));
        }

        for (RepositorySystemSessionExtender extender : sessionExtenders.values()) {
            extender.extend(request, configProps, mirrorSelector, proxySelector, authSelector);
        }

        // at this point we have "config" with pure MANDATORY resolver config, so resolver final config properties are
        // mergedProperties + configProperties
        HashMap<String, Object> finalConfigProperties = new HashMap<>();
        finalConfigProperties.putAll(mergedProps);
        finalConfigProperties.putAll(configProps);

        sessionBuilder.setUserProperties(request.getUserProperties());
        sessionBuilder.setSystemProperties(request.getSystemProperties());
        sessionBuilder.setConfigProperties(finalConfigProperties);

        return sessionBuilder;
    }

    private Path resolve(String string) {
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

    private VersionFilter buildVersionFilter(String filterExpression) {
        ArrayList<VersionFilter> filters = new ArrayList<>();
        if (filterExpression != null) {
            List<String> expressions = Arrays.stream(filterExpression.split(";"))
                    .filter(s -> s != null && !s.trim().isEmpty())
                    .toList();
            for (String expression : expressions) {
                if ("h".equals(expression)) {
                    filters.add(new HighestVersionFilter());
                } else if (expression.startsWith("h(") && expression.endsWith(")")) {
                    int num = Integer.parseInt(expression.substring(2, expression.length() - 1));
                    filters.add(new HighestVersionFilter(num));
                } else if ("l".equals(expression)) {
                    filters.add(new LowestVersionFilter());
                } else if (expression.startsWith("l(") && expression.endsWith(")")) {
                    int num = Integer.parseInt(expression.substring(2, expression.length() - 1));
                    filters.add(new LowestVersionFilter(num));
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, String> createMergedProperties(MavenExecutionRequest request) {
        // this throwaway map is really ONLY to get config from (profiles + env + system + user)
        Map<String, String> mergedProps = new HashMap<>();
        mergedProps.putAll(getPropertiesFromRequestedProfiles(request));
        mergedProps.putAll(new HashMap<String, String>((Map) request.getSystemProperties()));
        mergedProps.putAll(new HashMap<String, String>((Map) request.getUserProperties()));
        return mergedProps;
    }

    private Map<String, String> getPropertiesFromRequestedProfiles(MavenExecutionRequest request) {
        HashSet<String> activeProfileId =
                new HashSet<>(request.getProfileActivation().getRequiredActiveProfileIds());
        activeProfileId.addAll(request.getProfileActivation().getOptionalActiveProfileIds());

        return request.getProfiles().stream()
                .filter(profile -> activeProfileId.contains(profile.getId()))
                .map(ModelBase::getProperties)
                .flatMap(properties -> properties.entrySet().stream())
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()), (k1, k2) -> k2));
    }

    private String getUserAgent() {
        String version = runtimeInformation.getMavenVersion();
        version = version.isEmpty() ? version : "/" + version;
        return "Apache-Maven" + version + " (Java " + System.getProperty("java.version") + "; "
                + System.getProperty("os.name") + " " + System.getProperty("os.version") + ")";
    }
}
