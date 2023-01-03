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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.api.xml.XmlNode;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.eventspy.internal.EventSpyDispatcher;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.feature.Features;
import org.apache.maven.internal.xml.XmlNodeImpl;
import org.apache.maven.internal.xml.XmlPlexusConfiguration;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.building.TransformerContext;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.SessionData;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.localrepo.LocalRepositoryManagerFactory;
import org.eclipse.aether.transform.FileTransformer;
import org.eclipse.aether.transform.TransformException;
import org.eclipse.aether.util.ConfigUtils;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ChainedLocalRepositoryManager;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.eclipse.sisu.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

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

    private static final String MAVEN_RESOLVER_TRANSPORT_NATIVE = "native";

    private static final String MAVEN_RESOLVER_TRANSPORT_AUTO = "auto";

    private static final String WAGON_TRANSPORTER_PRIORITY_KEY = "aether.priority.WagonTransporterFactory";

    private static final String NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY = "aether.priority.HttpTransporterFactory";

    private static final String NATIVE_FILE_TRANSPORTER_PRIORITY_KEY = "aether.priority.FileTransporterFactory";

    private static final String RESOLVER_MAX_PRIORITY = String.valueOf(Float.MAX_VALUE);

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ArtifactHandlerManager artifactHandlerManager;

    private final RepositorySystem repoSystem;

    private final LocalRepositoryManagerFactory simpleLocalRepoMgrFactory;

    private final WorkspaceReader workspaceRepository;

    private final SettingsDecrypter settingsDecrypter;

    private final EventSpyDispatcher eventSpyDispatcher;

    private final MavenRepositorySystem mavenRepositorySystem;

    private final RuntimeInformation runtimeInformation;

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Inject
    public DefaultRepositorySystemSessionFactory(
            ArtifactHandlerManager artifactHandlerManager,
            RepositorySystem repoSystem,
            @Nullable @Named("simple") LocalRepositoryManagerFactory simpleLocalRepoMgrFactory,
            @Nullable @Named("ide") WorkspaceReader workspaceRepository,
            SettingsDecrypter settingsDecrypter,
            EventSpyDispatcher eventSpyDispatcher,
            MavenRepositorySystem mavenRepositorySystem,
            RuntimeInformation runtimeInformation) {
        this.artifactHandlerManager = artifactHandlerManager;
        this.repoSystem = repoSystem;
        this.simpleLocalRepoMgrFactory = simpleLocalRepoMgrFactory;
        this.workspaceRepository = workspaceRepository;
        this.settingsDecrypter = settingsDecrypter;
        this.eventSpyDispatcher = eventSpyDispatcher;
        this.mavenRepositorySystem = mavenRepositorySystem;
        this.runtimeInformation = runtimeInformation;
    }

    @SuppressWarnings("checkstyle:methodLength")
    public DefaultRepositorySystemSession newRepositorySession(MavenExecutionRequest request) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
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

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : decrypted.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                XmlNode dom = ((org.codehaus.plexus.util.xml.Xpp3Dom) server.getConfiguration()).getDom();
                List<XmlNode> children = dom.getChildren().stream()
                        .filter(c -> !"wagonProvider".equals(c.getName()))
                        .collect(Collectors.toList());
                dom = new XmlNodeImpl(dom.getName(), null, null, children, null);
                PlexusConfiguration config = XmlPlexusConfiguration.toPlexusConfiguration(dom);
                configProps.put("aether.connector.wagon.config." + server.getId(), config);

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

            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        Object transport = configProps.getOrDefault(MAVEN_RESOLVER_TRANSPORT_KEY, MAVEN_RESOLVER_TRANSPORT_DEFAULT);
        if (MAVEN_RESOLVER_TRANSPORT_DEFAULT.equals(transport)) {
            // The "default" mode (user did not set anything) from now on defaults to AUTO
        } else if (MAVEN_RESOLVER_TRANSPORT_NATIVE.equals(transport)) {
            // Make sure (whatever extra priority is set) that resolver native is selected
            configProps.put(NATIVE_FILE_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
            configProps.put(NATIVE_HTTP_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (MAVEN_RESOLVER_TRANSPORT_WAGON.equals(transport)) {
            // Make sure (whatever extra priority is set) that wagon is selected
            configProps.put(WAGON_TRANSPORTER_PRIORITY_KEY, RESOLVER_MAX_PRIORITY);
        } else if (!MAVEN_RESOLVER_TRANSPORT_AUTO.equals(transport)) {
            throw new IllegalArgumentException("Unknown resolver transport '" + transport
                    + "'. Supported transports are: " + MAVEN_RESOLVER_TRANSPORT_WAGON + ", "
                    + MAVEN_RESOLVER_TRANSPORT_NATIVE + ", " + MAVEN_RESOLVER_TRANSPORT_AUTO);
        }

        session.setUserProperties(request.getUserProperties());
        session.setSystemProperties(request.getSystemProperties());
        session.setConfigProperties(configProps);

        session.setTransferListener(request.getTransferListener());

        session.setRepositoryListener(eventSpyDispatcher.chainListener(new LoggingRepositoryListener(logger)));

        boolean recordReverseTree = ConfigUtils.getBoolean(session, false, MAVEN_REPO_LOCAL_RECORD_REVERSE_TREE);
        if (recordReverseTree) {
            session.setRepositoryListener(new ChainedRepositoryListener(
                    session.getRepositoryListener(), new ReverseTreeRepositoryListener()));
        }

        mavenRepositorySystem.injectMirror(request.getRemoteRepositories(), request.getMirrors());
        mavenRepositorySystem.injectProxy(session, request.getRemoteRepositories());
        mavenRepositorySystem.injectAuthentication(session, request.getRemoteRepositories());

        mavenRepositorySystem.injectMirror(request.getPluginArtifactRepositories(), request.getMirrors());
        mavenRepositorySystem.injectProxy(session, request.getPluginArtifactRepositories());
        mavenRepositorySystem.injectAuthentication(session, request.getPluginArtifactRepositories());

        setUpLocalRepositoryManager(request, session);

        if (Features.buildConsumer(request.getUserProperties()).isActive()) {
            session.setFileTransformerManager(a -> getTransformersForArtifact(a, session.getData()));
        }

        return session;
    }

    private void setUpLocalRepositoryManager(MavenExecutionRequest request, DefaultRepositorySystemSession session) {
        LocalRepository localRepo =
                new LocalRepository(request.getLocalRepository().getBasedir());

        if (request.isUseLegacyLocalRepository()) {
            try {
                session.setLocalRepositoryManager(simpleLocalRepoMgrFactory.newInstance(session, localRepo));
                logger.info("Disabling enhanced local repository: using legacy is strongly discouraged to ensure"
                        + " build reproducibility.");
            } catch (NoLocalRepositoryManagerException e) {
                logger.error("Failed to configure legacy local repository: falling back to default");
                session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, localRepo));
            }
        } else {
            LocalRepositoryManager lrm = repoSystem.newLocalRepositoryManager(session, localRepo);

            String localRepoTail = ConfigUtils.getString(session, null, MAVEN_REPO_LOCAL_TAIL);
            if (localRepoTail != null) {
                boolean ignoreTailAvailability =
                        ConfigUtils.getBoolean(session, true, MAVEN_REPO_LOCAL_TAIL_IGNORE_AVAILABILITY);
                List<LocalRepositoryManager> tail = new ArrayList<>();
                List<String> paths = Arrays.stream(localRepoTail.split(","))
                        .filter(p -> p != null && !p.trim().isEmpty())
                        .collect(toList());
                for (String path : paths) {
                    tail.add(repoSystem.newLocalRepositoryManager(session, new LocalRepository(path)));
                }
                session.setLocalRepositoryManager(new ChainedLocalRepositoryManager(lrm, tail, ignoreTailAvailability));
            } else {
                session.setLocalRepositoryManager(lrm);
            }
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

    private Collection<FileTransformer> getTransformersForArtifact(
            final Artifact artifact, final SessionData sessionData) {
        TransformerContext context = (TransformerContext) sessionData.get(TransformerContext.KEY);
        Collection<FileTransformer> transformers = new ArrayList<>();

        // In case of install:install-file there's no transformer context, as the goal is unrelated to the lifecycle.
        if ("pom".equals(artifact.getExtension()) && context != null) {
            transformers.add(new FileTransformer() {
                @Override
                public InputStream transformData(File pomFile) throws IOException, TransformException {
                    try {
                        return new ConsumerModelSourceTransformer().transform(pomFile.toPath(), context);
                    } catch (XmlPullParserException e) {
                        throw new TransformException(e);
                    }
                }

                @Override
                public Artifact transformArtifact(Artifact artifact) {
                    return artifact;
                }
            });
        }
        return Collections.unmodifiableCollection(transformers);
    }
}
