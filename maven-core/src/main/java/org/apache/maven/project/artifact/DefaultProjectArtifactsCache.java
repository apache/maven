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
package org.apache.maven.project.artifact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceRepository;

/**
 * @author Igor Fedorenko
 * @author Benjamin Bentmann
 * @author Anton Tanasenko
 */
@Component(role = ProjectArtifactsCache.class)
public class DefaultProjectArtifactsCache implements ProjectArtifactsCache {
    /**
     * CacheKey
     */
    protected static class CacheKey implements Key {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final Set<String> dependencyArtifacts;

        private final WorkspaceRepository workspace;

        private final LocalRepository localRepo;

        private final List<RemoteRepository> repositories;

        private final Set<String> collect;

        private final Set<String> resolve;

        private boolean aggregating;

        private final int hashCode;

        public CacheKey(
                MavenProject project,
                List<RemoteRepository> repositories,
                Collection<String> scopesToCollect,
                Collection<String> scopesToResolve,
                boolean aggregating,
                RepositorySystemSession session) {

            groupId = project.getGroupId();
            artifactId = project.getArtifactId();
            version = project.getVersion();

            Set<String> deps = new LinkedHashSet<>();
            if (project.getDependencyArtifacts() != null) {
                for (Artifact dep : project.getDependencyArtifacts()) {
                    deps.add(dep.toString());
                }
            }
            dependencyArtifacts = Collections.unmodifiableSet(deps);

            workspace = RepositoryUtils.getWorkspace(session);
            this.localRepo = session.getLocalRepository();
            this.repositories = new ArrayList<>(repositories.size());
            for (RemoteRepository repository : repositories) {
                if (repository.isRepositoryManager()) {
                    this.repositories.addAll(repository.getMirroredRepositories());
                } else {
                    this.repositories.add(repository);
                }
            }
            collect = scopesToCollect == null
                    ? Collections.<String>emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(scopesToCollect));
            resolve = scopesToResolve == null
                    ? Collections.<String>emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(scopesToResolve));
            this.aggregating = aggregating;

            int hash = 17;
            hash = hash * 31 + Objects.hashCode(groupId);
            hash = hash * 31 + Objects.hashCode(artifactId);
            hash = hash * 31 + Objects.hashCode(version);
            hash = hash * 31 + Objects.hashCode(dependencyArtifacts);
            hash = hash * 31 + Objects.hashCode(workspace);
            hash = hash * 31 + Objects.hashCode(localRepo);
            hash = hash * 31 + RepositoryUtils.repositoriesHashCode(repositories);
            hash = hash * 31 + Objects.hashCode(collect);
            hash = hash * 31 + Objects.hashCode(resolve);
            hash = hash * 31 + Objects.hashCode(aggregating);
            this.hashCode = hash;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }

            if (!(o instanceof CacheKey)) {
                return false;
            }

            CacheKey that = (CacheKey) o;

            return Objects.equals(groupId, that.groupId)
                    && Objects.equals(artifactId, that.artifactId)
                    && Objects.equals(version, that.version)
                    && Objects.equals(dependencyArtifacts, that.dependencyArtifacts)
                    && Objects.equals(workspace, that.workspace)
                    && Objects.equals(localRepo, that.localRepo)
                    && RepositoryUtils.repositoriesEquals(repositories, that.repositories)
                    && Objects.equals(collect, that.collect)
                    && Objects.equals(resolve, that.resolve)
                    && aggregating == that.aggregating;
        }
    }

    protected final Map<Key, CacheRecord> cache = new ConcurrentHashMap<>();

    @Override
    public Key createKey(
            MavenProject project,
            Collection<String> scopesToCollect,
            Collection<String> scopesToResolve,
            boolean aggregating,
            RepositorySystemSession session) {
        return new CacheKey(
                project,
                project.getRemoteProjectRepositories(),
                scopesToCollect,
                scopesToResolve,
                aggregating,
                session);
    }

    @Override
    public CacheRecord get(Key key) throws LifecycleExecutionException {
        CacheRecord cacheRecord = cache.get(key);

        if (cacheRecord != null && cacheRecord.getException() != null) {
            throw cacheRecord.getException();
        }

        return cacheRecord;
    }

    @Override
    public CacheRecord put(Key key, Set<Artifact> projectArtifacts) {
        Objects.requireNonNull(projectArtifacts, "projectArtifacts cannot be null");

        assertUniqueKey(key);

        CacheRecord record = new CacheRecord(Collections.unmodifiableSet(new LinkedHashSet<>(projectArtifacts)));

        cache.put(key, record);

        return record;
    }

    protected void assertUniqueKey(Key key) {
        if (cache.containsKey(key)) {
            throw new IllegalStateException("Duplicate artifact resolution result for project " + key);
        }
    }

    @Override
    public CacheRecord put(Key key, LifecycleExecutionException exception) {
        Objects.requireNonNull(exception, "exception cannot be null");

        assertUniqueKey(key);

        CacheRecord record = new CacheRecord(exception);

        cache.put(key, record);

        return record;
    }

    @Override
    public void flush() {
        cache.clear();
    }

    @Override
    public void register(MavenProject project, Key cacheKey, CacheRecord record) {
        // default cache does not track record usage
    }
}
