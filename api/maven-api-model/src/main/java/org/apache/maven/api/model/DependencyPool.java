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

import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * Global dependency pool for automatic memory optimization.
 * <p>
 * This class provides automatic pooling of Dependency objects to reduce memory
 * consumption in large Maven projects. All Dependency instances created through
 * the Builder.build() method are automatically pooled.
 * <p>
 * The pool uses comprehensive equality comparison including all dependency fields
 * and InputLocation information to ensure correct behavior.
 * <p>
 * This class is package-protected and intended for internal use by generated model classes.
 */
class DependencyPool {

    private static final ObjectPool<Dependency> POOL = new ObjectPool<>();

    static {
        // Add shutdown hook to print statistics
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            long totalCalls = POOL.getTotalInternCalls();
                            if (totalCalls > 0) {
                                System.err.println("[INFO] DependencyPool Statistics: " + POOL.getStatistics());
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
     * - InputLocation (for tracking where the dependency was defined)
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
                && Objects.equals(dep1.getExclusions(), dep2.getExclusions())
                && Objects.equals(dep1.getLocation(""), dep2.getLocation(""));
    };

    /**
     * Private constructor to prevent instantiation.
     */
    private DependencyPool() {}

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
        return POOL.intern(dependency, DEPENDENCY_EQUALITY);
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
     * Removes all dependencies from the pool.
     * <p>
     * This method is primarily useful for testing and cleanup purposes.
     * In normal operation, the pool should not need to be cleared as it uses
     * weak references that allow garbage collection when memory is needed.
     */
    static void clear() {
        POOL.clear();
    }

    /**
     * Returns true if the pool contains no dependencies.
     *
     * @return true if the pool is empty, false otherwise
     */
    static boolean isEmpty() {
        return POOL.isEmpty();
    }
}
