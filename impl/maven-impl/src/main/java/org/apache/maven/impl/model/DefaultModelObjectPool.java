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

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectProcessor;
import org.apache.maven.impl.cache.Cache;

/**
 * Default implementation of ModelObjectProcessor that provides memory optimization
 * through object pooling and interning for Dependency objects.
 *
 * <p>This implementation pools {@link Dependency} objects, which are frequently duplicated
 * in large Maven projects. Other model objects are passed through unchanged.</p>
 *
 * <p>The pool uses weak references and provides thread-safe access.</p>
 *
 * @since 4.0.0
 */
public class DefaultModelObjectPool implements ModelObjectProcessor {

    // Cache for dependency objects
    private final Cache<Dependency, Dependency> dependencyCache =
            Cache.newCache(Cache.ReferenceType.WEAK, "ModelObjectPool-Dependencies");

    @Override
    @SuppressWarnings("unchecked")
    public <T> T process(T object) {
        if (object == null) {
            return null;
        }

        // Only pool Dependency objects
        if (object instanceof Dependency) {
            Dependency dependency = (Dependency) object;
            return (T) dependencyCache.computeIfAbsent(dependency, d -> d);
        }

        // Return other objects unchanged
        return object;
    }

    /**
     * Get statistics for a specific object type.
     *
     * @param <T> the type of object
     * @param type the class of the object type
     * @return statistics for the given type as a string
     */
    public static <T> String getStatistics(Class<T> type) {
        // For now, return a simple statistics string
        return "Statistics for " + type.getSimpleName() + ": pooled objects = 0";
    }

    /**
     * Get all statistics for all object types.
     *
     * @return all statistics as a string
     */
    public static String getAllStatistics() {
        // For now, return a simple statistics string
        return "ModelObjectPool Statistics: Total pooled types = 1 (Dependency)";
    }
}
