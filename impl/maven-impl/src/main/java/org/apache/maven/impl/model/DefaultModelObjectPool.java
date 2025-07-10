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

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.ModelObjectProcessor;

/**
 * Default implementation of ModelObjectProcessor that pools Dependency objects
 * to reduce memory usage in large builds with many repeated dependencies.
 *
 * <p>This implementation uses soft references to allow garbage collection
 * under memory pressure while still providing memory benefits for typical builds.
 */
public class DefaultModelObjectPool implements ModelObjectProcessor {

    private final ConcurrentMap<Dependency, SoftReference<Dependency>> dependencyPool = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T processObject(T object) {
        if (object instanceof Dependency dependency) {
            return (T) poolDependency(dependency);
        }
        return object;
    }

    private Dependency poolDependency(Dependency dependency) {
        // Try to get existing pooled instance
        SoftReference<Dependency> ref = dependencyPool.get(dependency);
        if (ref != null) {
            Dependency pooled = ref.get();
            if (pooled != null) {
                return pooled;
            }
            // Reference was cleared, remove it
            dependencyPool.remove(dependency, ref);
        }

        // Pool the new dependency
        dependencyPool.put(dependency, new SoftReference<>(dependency));
        return dependency;
    }

    /**
     * Get the current size of the dependency pool (for monitoring/debugging).
     * Note: This may include cleared references that haven't been cleaned up yet.
     *
     * @return the current pool size
     */
    public int getPoolSize() {
        return dependencyPool.size();
    }

    /**
     * Clear the pool (mainly for testing).
     */
    public void clear() {
        dependencyPool.clear();
    }
}
