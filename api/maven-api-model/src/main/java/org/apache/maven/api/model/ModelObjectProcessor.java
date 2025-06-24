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

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A pluggable service for processing model objects during model building.
 *
 * <p>This service allows implementations to:</p>
 * <ul>
 *   <li>Pool identical objects to reduce memory footprint</li>
 *   <li>Intern objects for faster equality comparisons</li>
 *   <li>Apply custom optimization strategies</li>
 *   <li>Transform or modify objects during building</li>
 * </ul>
 *
 * <p>Implementations are discovered via the Java ServiceLoader mechanism and should
 * be registered in {@code META-INF/services/org.apache.maven.api.model.ModelObjectProcessor}.</p>
 *
 * <p>The service is called during model building for all model objects, allowing
 * implementations to decide which objects to process and how to optimize them.</p>
 *
 * @since 4.0.0
 */
public interface ModelObjectProcessor {

    /**
     * Process a model object, potentially returning a pooled or optimized version.
     *
     * <p>This method is called during model building for various model objects.
     * Implementations can:</p>
     * <ul>
     *   <li>Return the same object if no processing is desired</li>
     *   <li>Return a pooled equivalent object to reduce memory usage</li>
     *   <li>Return a modified or optimized version of the object</li>
     * </ul>
     *
     * <p>The implementation must ensure that the returned object is functionally
     * equivalent to the input object from the perspective of the Maven model.</p>
     *
     * @param <T> the type of the model object
     * @param object the model object to process
     * @return the processed object (may be the same instance, a pooled instance, or a modified instance)
     * @throws IllegalArgumentException if the object cannot be processed
     */
    <T> T process(T object);

    /**
     * Process a model object using the first available processor implementation.
     *
     * <p>This method discovers processor implementations via ServiceLoader and
     * uses the first one found. If no implementations are available, the object
     * is returned unchanged. The processor is cached for performance.</p>
     *
     * @param <T> the type of the model object
     * @param object the model object to process
     * @return the processed object
     */
    static <T> T processObject(T object) {
        class ProcessorHolder {
            /**
             * Cached processor instance for performance.
             */
            private static final AtomicReference<ModelObjectProcessor> CACHED_PROCESSOR = new AtomicReference<>();
        }

        ModelObjectProcessor processor = ProcessorHolder.CACHED_PROCESSOR.get();
        if (processor == null) {
            processor = loadProcessor();
            ProcessorHolder.CACHED_PROCESSOR.compareAndSet(null, processor);
            processor = ProcessorHolder.CACHED_PROCESSOR.get();
        }
        return processor.process(object);
    }

    /**
     * Load the first available processor implementation.
     */
    private static ModelObjectProcessor loadProcessor() {
        /*
         * No-op processor that returns objects unchanged.
         */
        class NoOpProcessor implements ModelObjectProcessor {
            @Override
            public <T> T process(T object) {
                return object;
            }
        }

        try {
            ServiceLoader<ModelObjectProcessor> loader = ServiceLoader.load(ModelObjectProcessor.class);
            for (ModelObjectProcessor processor : loader) {
                return processor;
            }
        } catch (Exception e) {
            // If service loading fails, use no-op processor
        }
        return new NoOpProcessor();
    }
}
