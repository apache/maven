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
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
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
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
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

    @SuppressWarnings("checkstyle:methodlength")
    public RepositorySystemSession.SessionBuilder newRepositorySession(MavenExecutionRequest request) {
        RepositorySystemSession.SessionBuilder mainSessionBuilder = MavenRepositorySystemUtils.newSession(repoSystem);
        mainSessionBuilder.setCache(request.getRepositoryCache());

        Map<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, request.isInteractiveMode());
        configProps.put("maven.startTime", request.getStartTime());
        // First add properties populated from settings.xml
        configProps.putAll(getPropertiesFromRequestedProfiles(request));
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(request.getSystemProperties());
        configProps.putAll(request.getUserProperties());

        mainSessionBuilder.setOffline(request.isOffline());
        mainSessionBuilder.setChecksumPolicy(request.getGlobalChecksumPolicy());
        if (request.isNoSnapshotUpdates()) {
            mainSessionBuilder.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
        } else if (request.isUpdateSnapshots()) {
            mainSessionBuilder.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        } else {
            mainSessionBuilder.setUpdatePolicy(null);
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
