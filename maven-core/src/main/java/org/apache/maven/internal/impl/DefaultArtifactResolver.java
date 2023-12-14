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

import javax.inject.Named;
import javax.inject.Singleton;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import static org.apache.maven.internal.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultArtifactResolver implements ArtifactResolver {

    @Override
    public ArtifactResolverResult resolve(ArtifactResolverRequest request)
            throws ArtifactResolverException, IllegalArgumentException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());
        try {
            Map<Artifact, Path> paths = new HashMap<>();
            ArtifactManager artifactManager = session.getService(ArtifactManager.class);
            List<RemoteRepository> repositories = session.toRepositories(session.getRemoteRepositories());
            List<ArtifactRequest> requests = new ArrayList<>();
            for (ArtifactCoordinate coord : request.getCoordinates()) {
                org.eclipse.aether.artifact.Artifact aetherArtifact = session.toArtifact(coord);
                Artifact artifact = session.getArtifact(aetherArtifact);
                Path path = artifactManager.getPath(artifact).orElse(null);
                if (path != null) {
                    paths.put(artifact, path);
                } else {
                    requests.add(new ArtifactRequest(aetherArtifact, repositories, null));
                }
            }
            if (!requests.isEmpty()) {
                List<ArtifactResult> results =
                        session.getRepositorySystem().resolveArtifacts(session.getSession(), requests);
                for (ArtifactResult result : results) {
                    Artifact artifact = session.getArtifact(result.getArtifact());
                    Path path = result.getArtifact().getFile().toPath();
                    artifactManager.setPath(artifact, path);
                    paths.put(artifact, path);
                }
            }
            return () -> paths;
        } catch (ArtifactResolutionException e) {
            throw new ArtifactResolverException("Unable to resolve artifact", e);
        }
    }
}
