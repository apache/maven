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
import java.util.function.Consumer;
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
        return resolveModel(
                session,
                repositories,
                parent.getGroupId(),
                parent.getArtifactId(),
                parent.getVersion(),
                "parent",
                null,
                parent.getLocation("version"),
                version -> modified.set(parent.withVersion(version)));
    }

    @Nonnull
    public ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull Dependency dependency,
            @Nonnull AtomicReference<Dependency> modified)
            throws ModelResolverException {
        return resolveModel(
                session,
                repositories,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                "dependency",
                dependency.getClassifier(),
                dependency.getLocation("version"),
                version -> modified.set(dependency.withVersion(version)));
    }

    @Override
    public ModelSource resolveModel(
            @Nonnull Session session,
            @Nullable List<RemoteRepository> repositories,
            @Nonnull String groupId,
            @Nonnull String artifactId,
            @Nonnull String version,
            @Nullable String classifier,
            @Nonnull Consumer<String> resolvedVersion)
            throws ModelResolverException {
        return resolveModel(
                session, repositories, groupId, artifactId, version, null, classifier, null, resolvedVersion);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public ModelSource resolveModel(
            Session session,
            List<RemoteRepository> repositories,
            String groupId,
            String artifactId,
            String version,
            String type,
            String classifier,
            InputLocation location,
            Consumer<String> resolvedVersion)
            throws ModelResolverException {
        try {
            ArtifactCoordinates coords =
                    session.createArtifactCoordinates(groupId, artifactId, version, classifier, "pom", null);
            if (coords.getVersionConstraint().getVersionRange() != null
                    && coords.getVersionConstraint().getVersionRange().getUpperBoundary() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new ModelResolverException(
                        "The requested " + (type != null ? type + " " : "") + "version range '" + version + "'"
                                + (location != null ? " (at " + location + ")" : "")
                                + " does not specify an upper bound",
                        groupId,
                        artifactId,
                        version);
            }
            String newVersion = session.resolveHighestVersion(coords, repositories)
                    .orElseThrow(() -> new ModelResolverException(
                            "No versions matched the requested " + (type != null ? type + " " : "") + "version range '"
                                    + version + "'",
                            groupId,
                            artifactId,
                            version))
                    .asString();
            if (!version.equals(newVersion)) {
                resolvedVersion.accept(newVersion);
            }

            Path path = getPath(session, repositories, groupId, artifactId, newVersion, classifier);
            return Sources.resolvedSource(path, groupId + ":" + artifactId + ":" + newVersion);
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
