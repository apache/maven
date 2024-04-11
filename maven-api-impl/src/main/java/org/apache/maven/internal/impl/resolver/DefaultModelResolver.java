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
package org.apache.maven.internal.impl.resolver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.maven.api.ArtifactCoordinate;
import org.apache.maven.api.Session;
import org.apache.maven.api.Version;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Repository;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ModelResolver;
import org.apache.maven.api.services.ModelResolverException;
import org.apache.maven.api.services.ModelSource;
import org.apache.maven.api.services.VersionRangeResolverException;
import org.apache.maven.internal.impl.InternalSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A model resolver to assist building of dependency POMs. This resolver gives priority to those repositories that have
 * been initially specified and repositories discovered in dependency POMs are recessively merged into the search chain.
 *
 * @see DefaultArtifactDescriptorReader
 */
public class DefaultModelResolver implements ModelResolver {

    private List<RemoteRepository> repositories;

    private final List<RemoteRepository> externalRepositories;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    @Inject
    public DefaultModelResolver(RemoteRepositoryManager remoteRepositoryManager) {
        this(remoteRepositoryManager, List.of());
    }

    public DefaultModelResolver(RemoteRepositoryManager remoteRepositoryManager, List<RemoteRepository> repositories) {
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.repositories = repositories;
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));

        this.repositoryIds = new HashSet<>();
    }

    private DefaultModelResolver(DefaultModelResolver original) {
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.repositories = new ArrayList<>(original.repositories);
        this.externalRepositories = original.externalRepositories;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
    }

    @Override
    public void addRepository(@Nonnull Session session, Repository repository) {
        addRepository(session, repository, false);
    }

    @Override
    public void addRepository(Session session, Repository repository, boolean replace) {
        RepositorySystemSession rsession = InternalSession.from(session).getSession();
        if (rsession.isIgnoreArtifactDescriptorRepositories()) {
            return;
        }

        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            removeMatchingRepository(repositories, repository.getId());
        }

        List<RemoteRepository> newRepositories =
                Collections.singletonList(ArtifactDescriptorUtils.toRemoteRepository(repository));

        this.repositories =
                remoteRepositoryManager.aggregateRepositories(rsession, repositories, newRepositories, true);
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator<RemoteRepository> iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository remoteRepository = iterator.next();
            if (remoteRepository.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    @Override
    public ModelResolver newCopy() {
        return new DefaultModelResolver(this);
    }

    @Override
    public ModelSource resolveModel(Session session, String groupId, String artifactId, String version)
            throws ModelResolverException {
        try {
            session = session.withRemoteRepositories(repositories.stream()
                    .map(InternalSession.from(session)::getRemoteRepository)
                    .toList());
            Map.Entry<org.apache.maven.api.Artifact, Path> resolved =
                    session.resolveArtifact(session.createArtifactCoordinate(groupId, artifactId, version, "pom"));
            return ModelSource.fromPath(resolved.getValue(), groupId + ":" + artifactId + ":" + version);
        } catch (ArtifactResolverException e) {
            throw new ModelResolverException(
                    e.getMessage() + " (remote repositories: "
                            + session.getRemoteRepositories().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", "))
                            + ")",
                    groupId,
                    artifactId,
                    version,
                    e);
        }
    }

    @Override
    public ModelSource resolveModel(Session session, Parent parent, AtomicReference<Parent> modified)
            throws ModelResolverException {
        try {
            session = session.withRemoteRepositories(repositories.stream()
                    .map(InternalSession.from(session)::getRemoteRepository)
                    .toList());
            ArtifactCoordinate coord = session.createArtifactCoordinate(
                    parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom");
            if (coord.getVersion().getVersionRange() != null
                    && coord.getVersion().getVersionRange().getUpperBoundary() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new ModelResolverException(
                        String.format(
                                "The requested parent version range '%s' does not specify an upper bound",
                                parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }
            List<Version> versions = session.resolveVersionRange(coord);
            if (versions.isEmpty()) {
                throw new ModelResolverException(
                        String.format(
                                "No versions matched the requested parent version range '%s'", parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }
            String newVersion = versions.get(versions.size() - 1).asString();
            if (!parent.getVersion().equals(newVersion)) {
                modified.set(parent.withVersion(newVersion));
            }

            return resolveModel(session, parent.getGroupId(), parent.getArtifactId(), newVersion);
        } catch (final VersionRangeResolverException e) {
            throw new ModelResolverException(
                    e.getMessage() + " (remote repositories: "
                            + session.getRemoteRepositories().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", "))
                            + ")",
                    parent.getGroupId(),
                    parent.getArtifactId(),
                    parent.getVersion(),
                    e);
        }
    }

    @Override
    public ModelSource resolveModel(Session session, Dependency dependency, AtomicReference<Dependency> modified)
            throws ModelResolverException {
        try {
            session = session.withRemoteRepositories(repositories.stream()
                    .map(InternalSession.from(session)::getRemoteRepository)
                    .toList());
            ArtifactCoordinate coord = session.createArtifactCoordinate(
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "pom");
            if (coord.getVersion().getVersionRange() != null
                    && coord.getVersion().getVersionRange().getUpperBoundary() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new ModelResolverException(
                        String.format(
                                "The requested dependency version range '%s' does not specify an upper bound",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }
            List<Version> versions = session.resolveVersionRange(coord);
            if (versions.isEmpty()) {
                throw new ModelResolverException(
                        String.format(
                                "No versions matched the requested dependency version range '%s'",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            String newVersion = versions.get(versions.size() - 1).toString();
            if (!dependency.getVersion().equals(newVersion)) {
                modified.set(dependency.withVersion(newVersion));
            }

            return resolveModel(session, dependency.getGroupId(), dependency.getArtifactId(), newVersion);
        } catch (VersionRangeResolverException e) {
            throw new ModelResolverException(
                    e.getMessage() + " (remote repositories: "
                            + session.getRemoteRepositories().stream()
                                    .map(Object::toString)
                                    .collect(Collectors.joining(", "))
                            + ")",
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    e);
        }
    }
}
