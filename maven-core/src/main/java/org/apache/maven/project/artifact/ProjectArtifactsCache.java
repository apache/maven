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

import java.util.Collection;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * Caches project artifacts. <strong>Warning:</strong> This is an internal utility interface that is only public for
 * technical reasons, it is not part of the public API. In particular, this interface can be changed or deleted without
 * prior notice.
 *
 * @author Igor Fedorenko
 * @author Benjamin Bentmann
 * @author Anton Tanasenko
 */
public interface ProjectArtifactsCache {

    /**
     * A cache key.
     */
    interface Key {
        // marker interface for cache keys
    }

    /**
     * CacheRecord
     */
    class CacheRecord {

        public Set<Artifact> getArtifacts() {
            return artifacts;
        }

        private final Set<Artifact> artifacts;

        public LifecycleExecutionException getException() {
            return exception;
        }

        private final LifecycleExecutionException exception;

        CacheRecord(Set<Artifact> artifacts) {
            this.artifacts = artifacts;
            this.exception = null;
        }

        CacheRecord(LifecycleExecutionException exception) {
            this.artifacts = null;
            this.exception = exception;
        }
    }

    Key createKey(
            MavenProject project,
            Collection<String> scopesToCollect,
            Collection<String> scopesToResolve,
            boolean aggregating,
            RepositorySystemSession session);

    CacheRecord get(Key key) throws LifecycleExecutionException;

    CacheRecord put(Key key, Set<Artifact> pluginArtifacts);

    CacheRecord put(Key key, LifecycleExecutionException e);

    void flush();

    /**
     * Registers the specified cache record for usage with the given project. Integrators can use the information
     * collected from this method in combination with a custom cache implementation to dispose unused records from the
     * cache.
     *
     * @param project The project that employs the plugin realm, must not be {@code null}.
     * @param record The cache record being used for the project, must not be {@code null}.
     */
    void register(MavenProject project, Key cacheKey, CacheRecord record);
}
