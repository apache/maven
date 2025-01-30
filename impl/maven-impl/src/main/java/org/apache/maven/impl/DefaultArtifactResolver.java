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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.api.services.ArtifactResolver;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ArtifactResolverRequest;
import org.apache.maven.api.services.ArtifactResolverResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import static org.apache.maven.impl.Utils.nonNull;

@Named
@Singleton
public class DefaultArtifactResolver implements ArtifactResolver {

    @Override
    public ArtifactResolverResult resolve(ArtifactResolverRequest request)
            throws ArtifactResolverException, IllegalArgumentException {
        nonNull(request, "request");
        InternalSession session = InternalSession.from(request.getSession());
        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            Map<DownloadedArtifact, Path> paths = new HashMap<>();
            ArtifactManager artifactManager = session.getService(ArtifactManager.class);
            List<RemoteRepository> repositories = session.toRepositories(
                    request.getRepositories() != null ? request.getRepositories() : session.getRemoteRepositories());
            List<ArtifactRequest> requests = new ArrayList<>();
            for (ArtifactCoordinates coords : request.getCoordinates()) {
                org.eclipse.aether.artifact.Artifact aetherArtifact = session.toArtifact(coords);
                Artifact artifact = session.getArtifact(aetherArtifact);
                Path path = artifactManager.getPath(artifact).orElse(null);
                if (path != null) {
                    if (aetherArtifact.getPath() == null) {
                        aetherArtifact = aetherArtifact.setPath(path);
                    }
                    DownloadedArtifact resolved = session.getArtifact(DownloadedArtifact.class, aetherArtifact);
                    paths.put(resolved, path);
                } else {
                    requests.add(
                            new ArtifactRequest(aetherArtifact, repositories, trace.context()).setTrace(trace.trace()));
                }
            }
            if (!requests.isEmpty()) {
                List<ArtifactResult> results =
                        session.getRepositorySystem().resolveArtifacts(session.getSession(), requests);
                for (ArtifactResult result : results) {
                    DownloadedArtifact artifact = session.getArtifact(DownloadedArtifact.class, result.getArtifact());
                    Path path = result.getArtifact().getPath();
                    paths.put(artifact, path);
                }
            }
            return new DefaultArtifactResolverResult(request, paths);
        } catch (ArtifactResolutionException e) {
            throw new ArtifactResolverException("Unable to resolve artifact: " + e.getMessage(), e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    static class DefaultArtifactResolverResult implements ArtifactResolverResult {

        final ArtifactResolverRequest request;
        final Map<DownloadedArtifact, Path> paths;

        DefaultArtifactResolverResult(ArtifactResolverRequest request, Map<DownloadedArtifact, Path> paths) {
            this.request = request;
            this.paths = paths;
        }

        @Override
        public ArtifactResolverRequest getRequest() {
            return request;
        }

        @Override
        public Collection<DownloadedArtifact> getArtifacts() {
            return paths.keySet();
        }

        @Override
        public Path getPath(Artifact artifact) {
            return paths.get(artifact);
        }
    }
}
