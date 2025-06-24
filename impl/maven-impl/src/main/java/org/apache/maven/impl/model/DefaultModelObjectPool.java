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
package org.apache.maven.impl.model;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectPool;

/**
 * Default implementation of ModelObjectPool that provides memory optimization
 * through object pooling and interning.
 * 
 * <p>This implementation focuses on pooling {@link Dependency} objects, which are
 * frequently duplicated in large Maven projects. Other model objects are passed
 * through unchanged.</p>
 * 
 * <p>The pool uses hard references to prevent premature garbage collection and
 * provides thread-safe access through ConcurrentHashMap.</p>
 * 
 * @since 4.0.0
 */
public class DefaultModelObjectPool implements ModelObjectPool {

    private static final ConcurrentHashMap<PoolKey, Dependency> DEPENDENCY_POOL = new ConcurrentHashMap<>();
    
    // Statistics tracking
    private static final AtomicLong TOTAL_INTERN_CALLS = new AtomicLong(0);
    private static final AtomicLong CACHE_HITS = new AtomicLong(0);
    private static final AtomicLong CACHE_MISSES = new AtomicLong(0);

    @Override
    @SuppressWarnings("unchecked")
    public <T> T intern(T object) {
        if (object instanceof Dependency dependency) {
            return (T) internDependency(dependency);
        }
        // For other types, return as-is (could be extended in the future)
        return object;
    }

    @Override
    public int getPriority() {
        return 100; // Higher than default (0)
    }

    @Override
    public boolean supports(Class<?> objectType) {
        return Dependency.class.isAssignableFrom(objectType);
    }

    @Override
    public PoolStatistics getStatistics() {
        return new DefaultPoolStatistics();
    }

    /**
     * Interns a dependency object in the pool.
     */
    private Dependency internDependency(Dependency dependency) {
        TOTAL_INTERN_CALLS.incrementAndGet();

        PoolKey key = new PoolKey(dependency);
        Dependency existing = DEPENDENCY_POOL.get(key);
        if (existing != null) {
            CACHE_HITS.incrementAndGet();
            return existing;
        }

        // Use putIfAbsent to handle concurrent access
        existing = DEPENDENCY_POOL.putIfAbsent(key, dependency);
        if (existing != null) {
            CACHE_HITS.incrementAndGet();
            return existing;
        }

        CACHE_MISSES.incrementAndGet();
        return dependency;
    }

    /**
     * Key class for pooling dependencies based on their content.
     */
    private static class PoolKey {
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String type;
        private final String classifier;
        private final String scope;
        private final String systemPath;
        private final String optional;
        private final Set<Object> exclusionKeys;
        private final boolean inputLocationsEqual;
        private final int hashCode;

        PoolKey(Dependency dependency) {
            this.groupId = dependency.getGroupId();
            this.artifactId = dependency.getArtifactId();
            this.version = dependency.getVersion();
            this.type = dependency.getType();
            this.classifier = dependency.getClassifier();
            this.scope = dependency.getScope();
            this.systemPath = dependency.getSystemPath();
            this.optional = dependency.getOptional();
            this.exclusionKeys = dependency.getLocationKeys();
            this.inputLocationsEqual = true; // Simplified for now
            this.hashCode = computeHashCode();
        }

        private int computeHashCode() {
            return Objects.hash(
                    groupId, artifactId, version, type, classifier, 
                    scope, systemPath, optional, exclusionKeys);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PoolKey other)) return false;
            
            return Objects.equals(groupId, other.groupId)
                    && Objects.equals(artifactId, other.artifactId)
                    && Objects.equals(version, other.version)
                    && Objects.equals(type, other.type)
                    && Objects.equals(classifier, other.classifier)
                    && Objects.equals(scope, other.scope)
                    && Objects.equals(systemPath, other.systemPath)
                    && Objects.equals(optional, other.optional)
                    && Objects.equals(exclusionKeys, other.exclusionKeys);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * Implementation of PoolStatistics.
     */
    private static class DefaultPoolStatistics implements PoolStatistics {
        @Override
        public long getPoolSize() {
            return DEPENDENCY_POOL.size();
        }

        @Override
        public long getHitCount() {
            return CACHE_HITS.get();
        }

        @Override
        public long getMissCount() {
            return CACHE_MISSES.get();
        }
    }
}
