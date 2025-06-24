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
package org.apache.maven.api.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * A global pool for interning {@link Dependency} objects to reduce memory usage.
 * <p>
 * This class provides automatic memory optimization by ensuring that equivalent
 * dependencies share the same object instance. Dependencies are considered equivalent
 * if they have the same groupId, artifactId, version, type, classifier, scope,
 * optional flag, exclusions, and systemPath.
 * <p>
 * The pooling is transparent and automatic - when a {@link Dependency} is built
 * via {@link Dependency.Builder#build()}, it is automatically interned in this pool.
 * <p>
 * This class is thread-safe and uses hard references to prevent premature
 * garbage collection of pooled dependencies.
 *
 * @since 4.0.0
 */
class DependencyPool {

    private static final Cache<PoolKey, Dependency> POOL;
    private static final AtomicLong TOTAL_INTERN_CALLS = new AtomicLong(0);
    private static final AtomicLong CACHE_HITS = new AtomicLong(0);

    static {
        // Initialize cache
        String prop = System.getProperty("maven.cache.model.dependency", "none");
        POOL = switch (prop.toLowerCase().trim()) {
            case "none" -> Cache.newCache(Cache.ReferenceType.NONE);
            case "weak" -> Cache.newCache(Cache.ReferenceType.WEAK);
            case "soft" -> Cache.newCache(Cache.ReferenceType.SOFT);
            case "hard" -> Cache.newCache(Cache.ReferenceType.HARD);
            default -> throw new IllegalArgumentException("Unknown cache model property: " + prop);};
        // Add shutdown hook to print statistics
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            long totalCalls = getTotalInternCalls();
                            if (totalCalls > 0) {
                                System.err.println("[INFO] DependencyPool Statistics: " + getStatistics());
                            }
                        },
                        "DependencyPool-Statistics"));
    }

    /**
     * Comprehensive equality predicate for dependencies.
     * <p>
     * Compares all dependency fields including:
     * - groupId, artifactId, version, type, classifier, scope
     * - optional flag
     * - exclusions list
     * - systemPath
     * - All InputLocation mappings (using content-based comparison)
     */
    private static final BiPredicate<Dependency, Dependency> DEPENDENCY_EQUALITY = (dep1, dep2) -> {
        if (dep1 == dep2) {
            return true;
        }
        if (dep1 == null || dep2 == null) {
            return false;
        }

        return Objects.equals(dep1.getGroupId(), dep2.getGroupId())
                && Objects.equals(dep1.getArtifactId(), dep2.getArtifactId())
                && Objects.equals(dep1.getVersion(), dep2.getVersion())
                && Objects.equals(dep1.getType(), dep2.getType())
                && Objects.equals(dep1.getClassifier(), dep2.getClassifier())
                && Objects.equals(dep1.getScope(), dep2.getScope())
                && Objects.equals(dep1.getSystemPath(), dep2.getSystemPath())
                && dep1.isOptional() == dep2.isOptional()
                && exclusionsEqual(dep1.getExclusions(), dep2.getExclusions())
                && inputLocationsEqual(dep1, dep2);
    };

    /**
     * Hash function for dependencies that matches the DEPENDENCY_EQUALITY predicate.
     * This ensures that dependencies that are equal according to DEPENDENCY_EQUALITY
     * have the same hash code, which is required for proper HashMap/ConcurrentHashMap behavior.
     */
    private static final ToIntFunction<Dependency> DEPENDENCY_HASH = dep -> {
        return Objects.hash(
                dep.getGroupId(),
                dep.getArtifactId(),
                dep.getVersion(),
                dep.getType(),
                dep.getClassifier(),
                dep.getScope(),
                dep.getSystemPath(),
                dep.isOptional()
                // Note: exclusions are intentionally excluded from hash for performance
                // The equals method will handle exclusion comparison
                );
    };

    /**
     * Interns a dependency object in the global pool.
     * <p>
     * If an equivalent dependency already exists in the pool, that dependency is returned.
     * Otherwise, the provided dependency is added to the pool and returned.
     * <p>
     * This method is automatically called by Dependency.Builder.build() to provide
     * transparent memory optimization.
     *
     * @param dependency the dependency to intern
     * @return the pooled dependency (either existing or newly added)
     * @throws NullPointerException if dependency is null
     */
    static Dependency intern(Dependency dependency) {
        TOTAL_INTERN_CALLS.incrementAndGet();

        PoolKey key = new PoolKey(dependency);
        Dependency existing = POOL.get(key);
        if (existing != null) {
            CACHE_HITS.incrementAndGet();
            return existing;
        }

        return POOL.computeIfAbsent(key, k -> dependency);
    }

    /**
     * Returns the approximate number of dependencies currently in the pool.
     * <p>
     * This method is primarily useful for monitoring and debugging purposes.
     *
     * @return the approximate size of the dependency pool
     */
    static int size() {
        return POOL.size();
    }

    /**
     * Returns the total number of intern calls made.
     */
    static long getTotalInternCalls() {
        return TOTAL_INTERN_CALLS.get();
    }

    /**
     * Returns the number of cache hits.
     */
    static long getCacheHits() {
        return CACHE_HITS.get();
    }

    /**
     * Returns the cache hit ratio as a value between 0.0 and 1.0.
     */
    static double getCacheHitRatio() {
        long total = TOTAL_INTERN_CALLS.get();
        return total > 0 ? (double) CACHE_HITS.get() / total : 0.0;
    }

    /**
     * Returns statistics about the dependency pool.
     */
    static String getStatistics() {
        long total = getTotalInternCalls();
        long hits = getCacheHits();
        double hitRatio = getCacheHitRatio();
        return String.format(
                "DependencyPool[size=%d, totalCalls=%d, hits=%d, hitRatio=%.2f%%, evictions=0]",
                size(), total, hits, hitRatio * 100);
    }

    /**
     * Deep equality comparison for exclusion lists.
     * Compares exclusions by content (groupId, artifactId) rather than object identity.
     */
    private static boolean exclusionsEqual(List<Exclusion> list1, List<Exclusion> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        if (list1.size() != list2.size()) {
            return false;
        }

        // Compare each exclusion by content
        for (int i = 0; i < list1.size(); i++) {
            Exclusion exc1 = list1.get(i);
            Exclusion exc2 = list2.get(i);

            if (!exclusionEqual(exc1, exc2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Deep equality comparison for individual exclusions.
     * Compares exclusions by content (groupId, artifactId) rather than object identity.
     */
    private static boolean exclusionEqual(Exclusion exc1, Exclusion exc2) {
        if (exc1 == exc2) {
            return true;
        }
        if (exc1 == null || exc2 == null) {
            return false;
        }
        return Objects.equals(exc1.getGroupId(), exc2.getGroupId())
                && Objects.equals(exc1.getArtifactId(), exc2.getArtifactId());
    }

    /**
     * Compares all InputLocation mappings between two dependencies using proper equals() methods.
     * Now that InputLocation implements equals(), we can use standard equality comparison.
     */
    private static boolean inputLocationsEqual(Dependency dep1, Dependency dep2) {
        Set<Object> keys1 = dep1.getLocationKeys();
        Set<Object> keys2 = dep2.getLocationKeys();

        if (!keys1.equals(keys2)) {
            return false;
        }

        // Compare each location mapping using proper equals()
        for (Object key : keys1) {
            InputLocation loc1 = dep1.getLocation(key);
            InputLocation loc2 = dep2.getLocation(key);

            if (!Objects.equals(loc1, loc2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A wrapper class that combines a dependency with its hash code for efficient pooling.
     */
    private static class PoolKey {
        private final Dependency dependency;
        private final int hashCode;

        PoolKey(Dependency dependency) {
            this.dependency = Objects.requireNonNull(dependency, "dependency cannot be null");
            this.hashCode = DEPENDENCY_HASH.applyAsInt(dependency);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PoolKey other)) {
                return false;
            }
            return DEPENDENCY_EQUALITY.test(this.dependency, other.dependency);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "PoolKey{dependency=" + dependency + "}";
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private DependencyPool() {}
}
