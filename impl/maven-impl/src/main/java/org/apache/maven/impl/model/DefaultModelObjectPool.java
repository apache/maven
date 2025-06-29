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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.maven.api.Constants;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectProcessor;
import org.apache.maven.impl.cache.Cache;

/**
 * Default implementation of ModelObjectProcessor that provides memory optimization
 * through object pooling and interning.
 *
 * <p>This implementation can pool any model object type based on configuration.
 * By default, it pools {@link Dependency} objects, which are frequently duplicated
 * in large Maven projects. Other model objects are passed through unchanged unless
 * explicitly configured for pooling.</p>
 *
 * <p>The pool uses configurable reference types and provides thread-safe access
 * through ConcurrentHashMap-based caches.</p>
 *
 * @since 4.0.0
 */
public class DefaultModelObjectPool implements ModelObjectProcessor {

    // Cache for each pooled object type
    private static final Map<Class<?>, Cache<PoolKey, Object>> OBJECT_POOLS = new ConcurrentHashMap<>();

    // Statistics tracking
    private static final Map<Class<?>, AtomicLong> TOTAL_CALLS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, AtomicLong> CACHE_HITS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, AtomicLong> CACHE_MISSES = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T process(T object) {
        if (object == null) {
            return null;
        }

        Class<?> objectType = object.getClass();
        String simpleClassName = objectType.getSimpleName();

        // Check if this object type should be pooled (read configuration dynamically)
        Set<String> pooledTypes = getPooledTypes();
        if (!pooledTypes.contains(simpleClassName)) {
            return object;
        }

        // Get or create cache for this object type
        Cache<PoolKey, Object> cache = OBJECT_POOLS.computeIfAbsent(objectType, this::createCacheForType);

        return (T) internObject(object, cache, objectType);
    }

    /**
     * Gets the set of object types that should be pooled.
     */
    private static Set<String> getPooledTypes() {
        String pooledTypesProperty = System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_POOLED_TYPES, "Dependency");
        return Arrays.stream(pooledTypesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Creates a cache for the specified object type with the appropriate reference type.
     */
    private Cache<PoolKey, Object> createCacheForType(Class<?> objectType) {
        Cache.ReferenceType referenceType = getReferenceTypeForClass(objectType);
        return Cache.newCache(referenceType);
    }

    /**
     * Gets the reference type to use for a specific object type.
     * Checks for per-type configuration first, then falls back to default.
     */
    private static Cache.ReferenceType getReferenceTypeForClass(Class<?> objectType) {
        String className = objectType.getSimpleName();

        // Check for per-type configuration first
        String perTypeProperty = Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE_PREFIX + className;
        String perTypeValue = System.getProperty(perTypeProperty);

        if (perTypeValue != null) {
            try {
                return Cache.ReferenceType.valueOf(perTypeValue.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown reference type for " + className + ": " + perTypeValue + ", using default");
            }
        }

        // Fall back to default reference type
        return getDefaultReferenceType();
    }

    /**
     * Gets the default reference type from system properties.
     */
    private static Cache.ReferenceType getDefaultReferenceType() {
        try {
            String referenceTypeProperty =
                    System.getProperty(Constants.MAVEN_MODEL_PROCESSOR_REFERENCE_TYPE, Cache.ReferenceType.HARD.name());
            return Cache.ReferenceType.valueOf(referenceTypeProperty.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown default reference type, using HARD");
            return Cache.ReferenceType.HARD;
        }
    }

    /**
     * Interns an object in the appropriate pool.
     */
    private Object internObject(Object object, Cache<PoolKey, Object> cache, Class<?> objectType) {
        // Update statistics
        TOTAL_CALLS.computeIfAbsent(objectType, k -> new AtomicLong(0)).incrementAndGet();

        PoolKey key = new PoolKey(object);
        Object existing = cache.get(key);
        if (existing != null) {
            CACHE_HITS.computeIfAbsent(objectType, k -> new AtomicLong(0)).incrementAndGet();
            return existing;
        }

        // Use computeIfAbsent to handle concurrent access
        existing = cache.computeIfAbsent(key, k -> object);
        if (existing == object) {
            // We added the object to the cache
            CACHE_MISSES.computeIfAbsent(objectType, k -> new AtomicLong(0)).incrementAndGet();
        } else {
            // Another thread added it first
            CACHE_HITS.computeIfAbsent(objectType, k -> new AtomicLong(0)).incrementAndGet();
        }

        return existing;
    }

    /**
     * Key class for pooling any model object based on their content.
     * Uses custom equality strategies for different object types.
     */
    private static class PoolKey {
        private final Object object;
        private final int hashCode;

        PoolKey(Object object) {
            this.object = object;
            this.hashCode = computeHashCode(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PoolKey other)) {
                return false;
            }

            return objectsEqual(object, other.object);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        /**
         * Custom equality check for different object types.
         */
        private static boolean objectsEqual(Object obj1, Object obj2) {
            if (obj1 == obj2) {
                return true;
            }
            if (obj1 == null || obj2 == null) {
                return false;
            }
            if (obj1.getClass() != obj2.getClass()) {
                return false;
            }

            // Custom equality for Dependency objects
            if (obj1 instanceof org.apache.maven.api.model.Dependency) {
                return dependenciesEqual(
                        (org.apache.maven.api.model.Dependency) obj1, (org.apache.maven.api.model.Dependency) obj2);
            }

            // For other objects, use default equals
            return obj1.equals(obj2);
        }

        /**
         * Custom equality check for Dependency objects based on all fields.
         */
        private static boolean dependenciesEqual(
                org.apache.maven.api.model.Dependency dep1, org.apache.maven.api.model.Dependency dep2) {
            return Objects.equals(dep1.getGroupId(), dep2.getGroupId())
                    && Objects.equals(dep1.getArtifactId(), dep2.getArtifactId())
                    && Objects.equals(dep1.getVersion(), dep2.getVersion())
                    && Objects.equals(dep1.getType(), dep2.getType())
                    && Objects.equals(dep1.getClassifier(), dep2.getClassifier())
                    && Objects.equals(dep1.getScope(), dep2.getScope())
                    && Objects.equals(dep1.getSystemPath(), dep2.getSystemPath())
                    && Objects.equals(dep1.getExclusions(), dep2.getExclusions())
                    && Objects.equals(dep1.getOptional(), dep2.getOptional())
                    && Objects.equals(dep1.getLocationKeys(), dep2.getLocationKeys())
                    && locationsEqual(dep1, dep2)
                    && Objects.equals(dep1.getImportedFrom(), dep2.getImportedFrom());
        }

        /**
         * Compare locations maps for two dependencies.
         */
        private static boolean locationsEqual(
                org.apache.maven.api.model.Dependency dep1, org.apache.maven.api.model.Dependency dep2) {
            var keys1 = dep1.getLocationKeys();
            var keys2 = dep2.getLocationKeys();

            if (!Objects.equals(keys1, keys2)) {
                return false;
            }

            for (Object key : keys1) {
                if (!Objects.equals(dep1.getLocation(key), dep2.getLocation(key))) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Custom hash code computation for different object types.
         */
        private static int computeHashCode(Object obj) {
            if (obj instanceof org.apache.maven.api.model.Dependency) {
                return dependencyHashCode((org.apache.maven.api.model.Dependency) obj);
            }
            return obj.hashCode();
        }

        /**
         * Custom hash code for Dependency objects based on all fields.
         */
        private static int dependencyHashCode(org.apache.maven.api.model.Dependency dep) {
            return Objects.hash(
                    dep.getGroupId(),
                    dep.getArtifactId(),
                    dep.getVersion(),
                    dep.getType(),
                    dep.getClassifier(),
                    dep.getScope(),
                    dep.getSystemPath(),
                    dep.getExclusions(),
                    dep.getOptional(),
                    dep.getLocationKeys(),
                    locationsHashCode(dep),
                    dep.getImportedFrom());
        }

        /**
         * Compute hash code for locations map.
         */
        private static int locationsHashCode(org.apache.maven.api.model.Dependency dep) {
            int hash = 1;
            for (Object key : dep.getLocationKeys()) {
                hash = 31 * hash + Objects.hashCode(key);
                hash = 31 * hash + Objects.hashCode(dep.getLocation(key));
            }
            return hash;
        }
    }

    /**
     * Get statistics for a specific object type.
     * Useful for monitoring and debugging.
     */
    public static String getStatistics(Class<?> objectType) {
        AtomicLong totalCalls = TOTAL_CALLS.get(objectType);
        AtomicLong hits = CACHE_HITS.get(objectType);
        AtomicLong misses = CACHE_MISSES.get(objectType);

        if (totalCalls == null) {
            return objectType.getSimpleName() + ": No statistics available";
        }

        long total = totalCalls.get();
        long hitCount = hits != null ? hits.get() : 0;
        long missCount = misses != null ? misses.get() : 0;
        double hitRatio = total > 0 ? (double) hitCount / total : 0.0;

        return String.format(
                "%s: Total=%d, Hits=%d, Misses=%d, Hit Ratio=%.2f%%",
                objectType.getSimpleName(), total, hitCount, missCount, hitRatio * 100);
    }

    /**
     * Get statistics for all pooled object types.
     */
    public static String getAllStatistics() {
        StringBuilder sb = new StringBuilder("ModelObjectPool Statistics:\n");
        for (Class<?> type : OBJECT_POOLS.keySet()) {
            sb.append("  ").append(getStatistics(type)).append("\n");
        }
        return sb.toString();
    }
}
