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
package org.apache.maven.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Repository;
import org.apache.maven.api.Version;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.VersionRangeResolver;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.api.services.VersionRangeResolverRequest;
import org.apache.maven.api.services.VersionRangeResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.impl.ImplUtils.map;

@Named
@Singleton
public class DefaultVersionRangeResolver implements VersionRangeResolver {

    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultVersionRangeResolver(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    @Override
    public VersionRangeResolverResult resolve(VersionRangeResolverRequest request)
            throws VersionRangeResolverException {
        requireNonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());
        return session.request(request, this::doResolve);
    }

    public VersionRangeResolverResult doResolve(VersionRangeResolverRequest request)
            throws VersionRangeResolverException {
        InternalSession session = InternalSession.from(request.getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            VersionRangeResult res = repositorySystem.resolveVersionRange(
                    session.getSession(),
                    new VersionRangeRequest(
                                    session.toArtifact(request.getArtifactCoordinates()),
                                    session.toResolvingRepositories(
                                            request.getRepositories() != null
                                                    ? request.getRepositories()
                                                    : session.getRemoteRepositories()),
                                    toResolver(request.getNature()),
                                    trace.context())
                            .setTrace(trace.trace()));

            Map<String, ArtifactRepository> repos = res.getVersions().stream()
                    .filter(v -> res.getRepository(v) != null)
                    .collect(Collectors.toMap(v -> v.toString(), res::getRepository));

            return new VersionRangeResolverResult() {
                @Override
                public VersionRangeResolverRequest getRequest() {
                    return request;
                }

                @Override
                public List<Exception> getExceptions() {
                    return res.getExceptions();
                }

                @Override
                public List<Version> getVersions() {
                    return map(res.getVersions(), v -> session.parseVersion(v.toString()));
                }

                @Override
                public Optional<Repository> getRepository(Version version) {
                    ArtifactRepository repo = repos.get(version.toString());
                    if (repo instanceof org.eclipse.aether.repository.LocalRepository localRepository) {
                        return Optional.of(new DefaultLocalRepository(localRepository));
                    } else if (repo instanceof org.eclipse.aether.repository.RemoteRepository remoteRepository) {
                        return Optional.of(new DefaultRemoteRepository(remoteRepository));
                    } else {
                        return Optional.empty();
                    }
                }
            };
        } catch (VersionRangeResolutionException e) {
            throw new VersionRangeResolverException("Unable to resolve version range", e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    private Metadata.Nature toResolver(VersionRangeResolverRequest.Nature nature) {
        return switch (nature) {
            case RELEASE_OR_SNAPSHOT -> Metadata.Nature.RELEASE_OR_SNAPSHOT;
            case SNAPSHOT -> Metadata.Nature.SNAPSHOT;
            case RELEASE -> Metadata.Nature.RELEASE;
        };
    }
}
