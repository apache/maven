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
package org.apache.maven.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.model.Repository;
import org.apache.maven.internal.impl.resolver.ArtifactDescriptorUtils;
import org.apache.maven.model.building.ArtifactModelSource;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * A model resolver to assist building of projects. This resolver gives priority to those repositories that have been
 * declared in the POM.
 *
 */
public class ProjectModelResolver implements ModelResolver {

    private static final int MAX_CAP = 0x7fff;

    private final RepositorySystemSession session;

    private final RequestTrace trace;

    private final String context = "project";

    private List<RemoteRepository> repositories;

    private List<RemoteRepository> pomRepositories;

    private final List<RemoteRepository> externalRepositories;

    private final RepositorySystem resolver;

    private final RemoteRepositoryManager remoteRepositoryManager;

    private final Set<String> repositoryIds;

    private final ReactorModelPool modelPool;

    private final ProjectBuildingRequest.RepositoryMerging repositoryMerging;

    private final Map<String, Future<Result>> parentCache;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public ProjectModelResolver(
            RepositorySystemSession session,
            RequestTrace trace,
            RepositorySystem resolver,
            RemoteRepositoryManager remoteRepositoryManager,
            List<RemoteRepository> repositories,
            ProjectBuildingRequest.RepositoryMerging repositoryMerging,
            ReactorModelPool modelPool,
            Map<String, Object> parentCache) {
        this.session = session;
        this.trace = trace;
        this.resolver = resolver;
        this.remoteRepositoryManager = remoteRepositoryManager;
        this.pomRepositories = new ArrayList<>();
        this.externalRepositories = Collections.unmodifiableList(new ArrayList<>(repositories));
        this.repositories = new ArrayList<>();
        this.repositories.addAll(externalRepositories);
        this.repositoryMerging = repositoryMerging;
        this.repositoryIds = new HashSet<>();
        this.modelPool = modelPool;
        this.parentCache = parentCache != null ? (Map) parentCache : new ConcurrentHashMap<>();
    }

    private ProjectModelResolver(ProjectModelResolver original) {
        this.session = original.session;
        this.trace = original.trace;
        this.resolver = original.resolver;
        this.remoteRepositoryManager = original.remoteRepositoryManager;
        this.pomRepositories = new ArrayList<>(original.pomRepositories);
        this.externalRepositories = original.externalRepositories;
        this.repositories = new ArrayList<>(original.repositories);
        this.repositoryMerging = original.repositoryMerging;
        this.repositoryIds = new HashSet<>(original.repositoryIds);
        this.modelPool = original.modelPool;
        this.parentCache = original.parentCache;
    }

    public void addRepository(Repository repository) throws InvalidRepositoryException {
        addRepository(repository, false);
    }

    @Override
    public void addRepository(final Repository repository, boolean replace) throws InvalidRepositoryException {
        if (!repositoryIds.add(repository.getId())) {
            if (!replace) {
                return;
            }

            // Remove any previous repository with this Id
            removeMatchingRepository(repositories, repository.getId());
            removeMatchingRepository(pomRepositories, repository.getId());
        }

        List<RemoteRepository> newRepositories =
                Collections.singletonList(ArtifactDescriptorUtils.toRemoteRepository(repository));

        if (ProjectBuildingRequest.RepositoryMerging.REQUEST_DOMINANT.equals(repositoryMerging)) {
            repositories = remoteRepositoryManager.aggregateRepositories(session, repositories, newRepositories, true);
        } else {
            pomRepositories =
                    remoteRepositoryManager.aggregateRepositories(session, pomRepositories, newRepositories, true);
            repositories = remoteRepositoryManager.aggregateRepositories(
                    session, pomRepositories, externalRepositories, false);
        }
    }

    private static void removeMatchingRepository(Iterable<RemoteRepository> repositories, final String id) {
        Iterator<RemoteRepository> iterator = repositories.iterator();
        while (iterator.hasNext()) {
            RemoteRepository next = iterator.next();
            if (next.getId().equals(id)) {
                iterator.remove();
            }
        }
    }

    public ModelResolver newCopy() {
        return new ProjectModelResolver(this);
    }

    public ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        Artifact pomArtifact = new DefaultArtifact(groupId, artifactId, "", "pom", version);

        try {
            ArtifactRequest request = new ArtifactRequest(pomArtifact, repositories, context);
            request.setTrace(trace);
            pomArtifact = resolver.resolveArtifact(session, request).getArtifact();
        } catch (ArtifactResolutionException e) {
            throw new UnresolvableModelException(e.getMessage(), groupId, artifactId, version, e);
        }

        return new ArtifactModelSource(pomArtifact.getFile(), groupId, artifactId, version);
    }

    record Result(ModelSource source, Parent parent, Exception e) {}

    @Override
    public ModelSource resolveModel(final Parent parent, AtomicReference<Parent> modified)
            throws UnresolvableModelException {
        Result result;
        try {
            Future<Result> future = parentCache.computeIfAbsent(parent.getId(), id -> {
                ForkJoinPool pool = new ForkJoinPool(MAX_CAP);
                ForkJoinTask<Result> task = new ForkJoinTask<>() {
                    Result result;

                    @Override
                    public Result getRawResult() {
                        return result;
                    }

                    @Override
                    protected void setRawResult(Result result) {
                        this.result = result;
                    }

                    @Override
                    protected boolean exec() {
                        try {
                            AtomicReference<Parent> modified = new AtomicReference<>();
                            ModelSource source = doResolveModel(parent, modified);
                            result = new Result(source, modified.get(), null);
                        } catch (Exception e) {
                            result = new Result(null, null, e);
                        } finally {
                            pool.shutdown();
                        }
                        return true;
                    }
                };
                pool.submit(task);
                return task;
            });
            result = future.get();
        } catch (Exception e) {
            throw new UnresolvableModelException(e, parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }
        if (result.e != null) {
            uncheckedThrow(result.e);
            return null;
        } else {
            if (result.parent != null && modified != null) {
                modified.set(result.parent);
            }
            return result.source;
        }
    }

    static <T extends Throwable> void uncheckedThrow(Throwable t) throws T {
        throw (T) t; // rely on vacuous cast
    }

    private ModelSource doResolveModel(Parent parent, AtomicReference<Parent> modified)
            throws UnresolvableModelException {
        try {
            final Artifact artifact =
                    new DefaultArtifact(parent.getGroupId(), parent.getArtifactId(), "", "pom", parent.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = resolver.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested parent version range '%s'", parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-2199 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested parent version range '%s' does not specify an upper bound",
                                parent.getVersion()),
                        parent.getGroupId(),
                        parent.getArtifactId(),
                        parent.getVersion());
            }

            String newVersion = versionRangeResult.getHighestVersion().toString();
            if (!parent.getVersion().equals(newVersion)) {
                modified.set(parent.withVersion(newVersion));
            }

            return resolveModel(parent.getGroupId(), parent.getArtifactId(), newVersion);
        } catch (final VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(final Dependency dependency, AtomicReference<Dependency> modified)
            throws UnresolvableModelException {
        try {
            final Artifact artifact = new DefaultArtifact(
                    dependency.getGroupId(), dependency.getArtifactId(), "", "pom", dependency.getVersion());

            final VersionRangeRequest versionRangeRequest = new VersionRangeRequest(artifact, repositories, context);
            versionRangeRequest.setTrace(trace);

            final VersionRangeResult versionRangeResult = resolver.resolveVersionRange(session, versionRangeRequest);

            if (versionRangeResult.getHighestVersion() == null) {
                throw new UnresolvableModelException(
                        String.format(
                                "No versions matched the requested dependency version range '%s'",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            if (versionRangeResult.getVersionConstraint() != null
                    && versionRangeResult.getVersionConstraint().getRange() != null
                    && versionRangeResult.getVersionConstraint().getRange().getUpperBound() == null) {
                // Message below is checked for in the MNG-4463 core IT.
                throw new UnresolvableModelException(
                        String.format(
                                "The requested dependency version range '%s' does not specify an upper bound",
                                dependency.getVersion()),
                        dependency.getGroupId(),
                        dependency.getArtifactId(),
                        dependency.getVersion());
            }

            String newVersion = versionRangeResult.getHighestVersion().toString();
            if (!dependency.getVersion().equals(newVersion)) {
                modified.set(dependency.withVersion(newVersion));
            }

            if (modelPool != null) {
                Model model = modelPool.get(dependency.getGroupId(), dependency.getArtifactId(), newVersion);

                if (model != null) {
                    return new FileModelSource(model.getPomFile());
                }
            }

            return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), newVersion);
        } catch (VersionRangeResolutionException e) {
            throw new UnresolvableModelException(
                    e.getMessage(), dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), e);
        }
    }

    @Override
    public ModelSource resolveModel(org.apache.maven.model.Parent parent) throws UnresolvableModelException {
        AtomicReference<org.apache.maven.api.model.Parent> resolvedParent = new AtomicReference<>();
        ModelSource result = resolveModel(parent.getDelegate(), resolvedParent);
        if (resolvedParent.get() != null) {
            parent.setVersion(resolvedParent.get().getVersion());
        }
        return result;
    }

    @Override
    public ModelSource resolveModel(org.apache.maven.model.Dependency dependency) throws UnresolvableModelException {
        AtomicReference<org.apache.maven.api.model.Dependency> resolvedDependency = new AtomicReference<>();
        ModelSource result = resolveModel(dependency.getDelegate(), resolvedDependency);
        if (resolvedDependency.get() != null) {
            dependency.setVersion(resolvedDependency.get().getVersion());
        }
        return result;
    }

    @Override
    public void addRepository(org.apache.maven.model.Repository repository) throws InvalidRepositoryException {
        addRepository(repository.getDelegate());
    }

    @Override
    public void addRepository(org.apache.maven.model.Repository repository, boolean replace)
            throws InvalidRepositoryException {
        addRepository(repository.getDelegate(), replace);
    }
}
