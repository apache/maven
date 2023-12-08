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
package org.apache.maven.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.api.Repository;
import org.apache.maven.api.Version;
import org.apache.maven.api.services.VersionRangeResolver;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.api.services.VersionRangeResolverRequest;
import org.apache.maven.api.services.VersionRangeResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

import static org.apache.maven.internal.impl.Utils.map;
import static org.apache.maven.internal.impl.Utils.nonNull;

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
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());

        try {
            VersionRangeResult res = repositorySystem.resolveVersionRange(
                    session.getSession(),
                    new VersionRangeRequest(
                            session.toArtifact(request.getArtifactCoordinate()),
                            session.toRepositories(session.getRemoteRepositories()),
                            null));

            Map<String, ArtifactRepository> repos =
                    res.getVersions().stream().collect(Collectors.toMap(v -> v.toString(), res::getRepository));

            return new VersionRangeResolverResult() {
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
                    if (repo instanceof org.eclipse.aether.repository.LocalRepository) {
                        return Optional.of(
                                new DefaultLocalRepository((org.eclipse.aether.repository.LocalRepository) repo));
                    } else if (repo instanceof org.eclipse.aether.repository.RemoteRepository) {
                        return Optional.of(
                                new DefaultRemoteRepository((org.eclipse.aether.repository.RemoteRepository) repo));
                    } else {
                        return Optional.empty();
                    }
                }
            };
        } catch (VersionRangeResolutionException e) {
            throw new VersionRangeResolverException("Unable to resolve version range", e);
        }
    }
}
