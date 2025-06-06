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
package org.apache.maven.impl.resolver;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.RemoteRepository;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.Sources;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.api.services.model.ModelResolver;
import org.apache.maven.api.services.model.ModelResolverException;
import org.apache.maven.impl.InternalSession;
import org.apache.maven.impl.RequestTraceHelper;

/**
 * A model resolver to assist building of dependency POMs.
 *
 * @see DefaultArtifactDescriptorReader
 */
@Named
@Singleton
public class DefaultModelResolver implements ModelResolver {

    @Nonnull
    @Override
    public ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull Parent parent,
            @Nonnull AtomicReference<Parent> modified)
            throws ModelResolverException {
        ModelResolverResult result = resolveModel(
                new ModelResolverRequest(
                        session,
                        null,
                        repositories,
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion(),
                        null),
                parent.getLocation("version"),
                "parent");
        if (result.version() != null) {
            modified.set(parent.withVersion(result.version()));
        }
        return result.source();
    }

    @Nonnull
    @Override
    public ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull Dependency dependency,
            @Nonnull AtomicReference<Dependency> modified)
            throws ModelResolverException {
        ModelResolverResult result = resolveModel(
                new ModelResolverRequest(
                        session,
                        null,
                        repositories,
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion(),
                        dependency.getClassifier()),
                dependency.getLocation("version"),
                "dependency");
        if (result.version() != null) {
            modified.set(dependency.withVersion(result.version()));
        }
        return result.source();
    }

    @Nonnull
    @Override
    public ModelResolverResult resolveModel(@Nonnull ModelResolverRequest request) throws ModelResolverException {
        return resolveModel(request, null, null);
    }

    public ModelResolverResult resolveModel(
            @Nonnull ModelResolverRequest request, InputLocation location, String modelType)
            throws ModelResolverException {
        return InternalSession.from(request.session()).request(request, r -> doResolveModel(r, location, modelType));
    }

    public ModelResolverResult doResolveModel(
            @Nonnull ModelResolverRequest request, InputLocation location, String modelType)
            throws ModelResolverException {
        Session session = request.session();
        String groupId = request.groupId();
        String artifactId = request.artifactId();
        String version = request.version();
        String classifier = request.classifier();
        List<RemoteRepository> repositories = request.repositories();

        RequestTraceHelper.ResolverTrace trace = RequestTraceHelper.enter(session, request);
        try {
            ArtifactCoordinates coords =
                    session.createArtifactCoordinates(groupId, artifactId, version, classifier, "pom", null);
            if (coords.getVersionConstraint().getVersionRange() != null
                    && coords.getVersionConstraint().getVersionRange().getUpperBoundary() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new ModelResolverException(
                        "The requested " + (modelType != null ? modelType + " " : "") + "version range '" + version
                                + "'"
                                + (location != null ? " (at " + location + ")" : "")
                                + " does not specify an upper bound",
                        groupId,
                        artifactId,
                        version);
            }
            String newVersion = session.resolveHighestVersion(coords, repositories)
                    .orElseThrow(() -> new ModelResolverException(
                            "No versions matched the requested " + (modelType != null ? modelType + " " : "")
                                    + "version range '" + version + "'",
                            groupId,
                            artifactId,
                            version))
                    .toString();
            String resultVersion = version.equals(newVersion) ? null : newVersion;
            Path path = getPath(session, repositories, groupId, artifactId, newVersion, classifier);
            return new ModelResolverResult(
                    request,
                    Sources.resolvedSource(path, groupId + ":" + artifactId + ":" + newVersion),
                    resultVersion);
        } catch (VersionRangeResolverException | ArtifactResolverException e) {
            throw new ModelResolverException(
                    e.getMessage() + " (remote repositories: "
                            + (repositories != null ? repositories : session.getRemoteRepositories())
                                    .stream().map(Object::toString).collect(Collectors.joining(", "))
                            + ")",
                    groupId,
                    artifactId,
                    version,
                    e);
        } finally {
            RequestTraceHelper.exit(trace);
        }
    }

    protected Path getPath(
            Session session,
            List<RemoteRepository> repositories,
            String groupId,
            String artifactId,
            String version,
            String classifier) {
        DownloadedArtifact resolved = session.resolveArtifact(
                session.createArtifactCoordinates(groupId, artifactId, version, classifier, "pom", null), repositories);
        return resolved.getPath();
    }
}
