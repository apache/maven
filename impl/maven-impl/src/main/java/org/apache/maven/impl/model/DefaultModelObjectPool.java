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
import java.util.concurrent.atomic.AtomicLong;

import org.apache.maven.api.Constants;
import org.apache.maven.api.model.Cache;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectProcessor;

/**
 * Default implementation of ModelObjectProcessor that provides memory optimization
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
public class DefaultModelObjectPool implements ModelObjectProcessor {

    private static final Cache<PoolKey, Dependency> DEPENDENCY_POOL = Cache.newCache(getReferenceType());

    // Statistics tracking
    private static final AtomicLong TOTAL_INTERN_CALLS = new AtomicLong(0);
    private static final AtomicLong CACHE_HITS = new AtomicLong(0);
    private static final AtomicLong CACHE_MISSES = new AtomicLong(0);

    @Override
    @SuppressWarnings("unchecked")
    public <T> T process(T object) {
        if (object instanceof Dependency dependency) {
            return (T) internDependency(dependency);
        }
        // For other types, return as-is (could be extended in the future)
        return object;
    }

    /**
     * Gets the reference type to use for the dependency pool from system properties.
     */
    private static Cache.ReferenceType getReferenceType() {
        String referenceTypeProperty = System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, "hard");
        return switch (referenceTypeProperty.toLowerCase()) {
            case "soft" -> Cache.ReferenceType.SOFT;
            case "weak" -> Cache.ReferenceType.WEAK;
            case "none" -> Cache.ReferenceType.NONE;
            case "hard" -> Cache.ReferenceType.HARD;
            default -> {
                System.err.println("Unknown reference type: " + referenceTypeProperty + ", using default HARD");
                yield Cache.ReferenceType.HARD;
            }
        };
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
        existing = DEPENDENCY_POOL.computeIfAbsent(key, k -> dependency);
        if (existing != null) {
            CACHE_HITS.incrementAndGet();
            return existing;
        }

        CACHE_MISSES.incrementAndGet();
        return dependency;
    }

    /**
     * Key class for pooling dependencies based on their content.
     * Holds the dependency directly and pre-computes hashCode for performance.
     */
    private static class PoolKey {
        private final Dependency dependency;
        private final int hashCode;

        PoolKey(Dependency dependency) {
            this.dependency = dependency;
            this.hashCode = computeHashCode(dependency);
        }

        private static int computeHashCode(Dependency dependency) {
            return Objects.hash(
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getType(),
                    dependency.getClassifier(),
                    dependency.getScope(),
                    dependency.getSystemPath(),
                    dependency.getOptional(),
                    dependency.getExclusions(),
                    dependency.getLocationKeys());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PoolKey other)) return false;

            // Use the dependency's equals method which now properly includes all fields
            return Objects.equals(dependency, other.dependency);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
