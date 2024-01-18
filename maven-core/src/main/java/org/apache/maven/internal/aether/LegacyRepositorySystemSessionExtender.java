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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Mirror;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * Extender that fills in legacy bits (using legacy code).
 *
 * @since 4.0.0
 */
@Named
@Singleton
class LegacyRepositorySystemSessionExtender implements RepositorySystemSessionExtender {
    @Override
    public void extend(
            MavenExecutionRequest mavenExecutionRequest,
            Map<String, Object> configProperties,
            MirrorSelector mirrorSelector,
            ProxySelector proxySelector,
            AuthenticationSelector authenticationSelector) {
        injectMirror(mavenExecutionRequest.getRemoteRepositories(), mavenExecutionRequest.getMirrors());
        injectProxy(proxySelector, mavenExecutionRequest.getRemoteRepositories());
        injectAuthentication(authenticationSelector, mavenExecutionRequest.getRemoteRepositories());

        injectMirror(mavenExecutionRequest.getPluginArtifactRepositories(), mavenExecutionRequest.getMirrors());
        injectProxy(proxySelector, mavenExecutionRequest.getPluginArtifactRepositories());
        injectAuthentication(authenticationSelector, mavenExecutionRequest.getPluginArtifactRepositories());
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

    private void injectAuthentication(AuthenticationSelector selector, List<ArtifactRepository> repositories) {
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
