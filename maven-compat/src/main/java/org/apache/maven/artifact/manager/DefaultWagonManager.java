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
package org.apache.maven.artifact.manager;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.repository.MirrorSelector;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Manages <a href="https://maven.apache.org/wagon">Wagon</a> related operations in Maven.
 */
@Component(role = WagonManager.class)
public class DefaultWagonManager extends org.apache.maven.repository.legacy.DefaultWagonManager
        implements WagonManager {

    // NOTE: This must use a different field name than in the super class or IoC has no chance to inject the loggers
    @Requirement
    private Logger log;

    @Requirement
    private LegacySupport legacySupport;

    @Requirement
    private SettingsDecrypter settingsDecrypter;

    @Requirement
    private MirrorSelector mirrorSelector;

    @Requirement
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    public AuthenticationInfo getAuthenticationInfo(String id) {
        MavenSession session = legacySupport.getSession();

        if (session != null && id != null) {
            MavenExecutionRequest request = session.getRequest();

            if (request != null) {
                List<Server> servers = request.getServers();

                if (servers != null) {
                    for (Server server : servers) {
                        if (id.equalsIgnoreCase(server.getId())) {
                            SettingsDecryptionResult result =
                                    settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(server));
                            server = result.getServer();

                            AuthenticationInfo authInfo = new AuthenticationInfo();
                            authInfo.setUserName(server.getUsername());
                            authInfo.setPassword(server.getPassword());
                            authInfo.setPrivateKey(server.getPrivateKey());
                            authInfo.setPassphrase(server.getPassphrase());

                            return authInfo;
                        }
                    }
                }
            }
        }

        // empty one to prevent NPE
        return new AuthenticationInfo();
    }

    public ProxyInfo getProxy(String protocol) {
        MavenSession session = legacySupport.getSession();

        if (session != null && protocol != null) {
            MavenExecutionRequest request = session.getRequest();

            if (request != null) {
                List<Proxy> proxies = request.getProxies();

                if (proxies != null) {
                    for (Proxy proxy : proxies) {
                        if (proxy.isActive() && protocol.equalsIgnoreCase(proxy.getProtocol())) {
                            SettingsDecryptionResult result =
                                    settingsDecrypter.decrypt(new DefaultSettingsDecryptionRequest(proxy));
                            proxy = result.getProxy();

                            ProxyInfo proxyInfo = new ProxyInfo();
                            proxyInfo.setHost(proxy.getHost());
                            proxyInfo.setType(proxy.getProtocol());
                            proxyInfo.setPort(proxy.getPort());
                            proxyInfo.setNonProxyHosts(proxy.getNonProxyHosts());
                            proxyInfo.setUserName(proxy.getUsername());
                            proxyInfo.setPassword(proxy.getPassword());

                            return proxyInfo;
                        }
                    }
                }
            }
        }

        return null;
    }

    public void getArtifact(Artifact artifact, ArtifactRepository repository)
            throws TransferFailedException, ResourceDoesNotExistException {
        getArtifact(artifact, repository, null, false);
    }

    public void getArtifact(Artifact artifact, List<ArtifactRepository> remoteRepositories)
            throws TransferFailedException, ResourceDoesNotExistException {
        getArtifact(artifact, remoteRepositories, null, false);
    }

    @Deprecated
    public ArtifactRepository getMirrorRepository(ArtifactRepository repository) {

        Mirror mirror = mirrorSelector.getMirror(
                repository, legacySupport.getSession().getSettings().getMirrors());

        if (mirror != null) {
            String id = mirror.getId();
            if (id == null) {
                // TODO this should be illegal in settings.xml
                id = repository.getId();
            }

            log.debug("Using mirror: " + mirror.getUrl() + " (id: " + id + ")");

            repository = artifactRepositoryFactory.createArtifactRepository(
                    id, mirror.getUrl(), repository.getLayout(), repository.getSnapshots(), repository.getReleases());
        }
        return repository;
    }
}
