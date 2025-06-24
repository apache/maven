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

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for loading and managing ModelObjectPool implementations.
 *
 * <p>This factory uses the Java ServiceLoader mechanism to discover available
 * pool implementations and selects the one with the highest priority.</p>
 *
 * @since 4.0.0
 */
public final class ModelObjectPoolFactory {

    private static final AtomicReference<ModelObjectPool> INSTANCE = new AtomicReference<>();
    private static final ModelObjectPool NO_OP_POOL = new NoOpPool();

    private ModelObjectPoolFactory() {
        // Utility class
    }

    /**
     * Get the current ModelObjectPool instance.
     *
     * <p>This method returns a singleton instance that is discovered via ServiceLoader.
     * If no implementations are found, a no-op implementation is returned.</p>
     *
     * @return the current pool instance
     */
    public static ModelObjectPool getInstance() {
        ModelObjectPool pool = INSTANCE.get();
        if (pool == null) {
            pool = loadPool();
            if (!INSTANCE.compareAndSet(null, pool)) {
                // Another thread beat us to it
                pool = INSTANCE.get();
            }
        }
        return pool;
    }

    /**
     * Reset the pool instance, forcing a reload on next access.
     * This method is primarily for testing purposes.
     */
    public static void reset() {
        INSTANCE.set(null);
    }

    /**
     * Load the highest priority pool implementation via ServiceLoader.
     */
    private static ModelObjectPool loadPool() {
        try {
            ServiceLoader<ModelObjectPool> loader = ServiceLoader.load(ModelObjectPool.class);
            return loader.stream()
                    .map(ServiceLoader.Provider::get)
                    .max(Comparator.comparingInt(ModelObjectPool::getPriority))
                    .orElse(NO_OP_POOL);
        } catch (Exception e) {
            // If service loading fails, fall back to no-op
            return NO_OP_POOL;
        }
    }

    /**
     * No-op implementation that returns objects unchanged.
     */
    private static class NoOpPool implements ModelObjectPool {
        @Override
        public <T> T intern(T object) {
            return object;
        }

        @Override
        public int getPriority() {
            return Integer.MIN_VALUE; // Lowest priority
        }

        @Override
        public String toString() {
            return "NoOpPool";
        }
    }
}
