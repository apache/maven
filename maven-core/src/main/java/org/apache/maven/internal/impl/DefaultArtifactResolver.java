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

import static org.apache.maven.internal.impl.Utils.cast;
import static org.apache.maven.internal.impl.Utils.nonNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.api.Artifact;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

@Named
@Singleton
public class DefaultArtifactResolver implements ArtifactResolver {
    private final RepositorySystem repositorySystem;

    @Inject
    DefaultArtifactResolver(@Nonnull RepositorySystem repositorySystem) {
        this.repositorySystem = nonNull(repositorySystem, "repositorySystem can not be null");
    }

    @Override
    public ArtifactResolverResult resolve(ArtifactResolverRequest request)
            throws ArtifactResolverException, IllegalArgumentException {
        nonNull(request, "request can not be null");
        DefaultSession session =
                cast(DefaultSession.class, request.getSession(), "request.session should be a " + DefaultSession.class);
        try {
            List<RemoteRepository> repositories = session.toRepositories(session.getRemoteRepositories());
            List<ArtifactRequest> requests = request.getCoordinates().stream()
                    .map(coord -> new ArtifactRequest(session.toArtifact(coord), repositories, null))
                    .collect(Collectors.toList());
            List<ArtifactResult> results = repositorySystem.resolveArtifacts(session.getSession(), requests);
            Map<Artifact, Path> paths = new HashMap<>();
            for (ArtifactResult result : results) {
                Artifact artifact = session.getArtifact(result.getArtifact());
                Path path = result.getArtifact().getFile().toPath();
                session.getService(ArtifactManager.class).setPath(artifact, path);
                paths.put(artifact, path);
            }
            return new ArtifactResolverResult() {
                @Override
                public Map<Artifact, Path> getArtifacts() {
                    return paths;
                }
            };
        } catch (ArtifactResolutionException e) {
            throw new ArtifactResolverException("Unable to resolve artifact", e);
        }
    }
}
