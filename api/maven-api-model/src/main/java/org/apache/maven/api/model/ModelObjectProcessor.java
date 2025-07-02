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

/**
 * A pluggable service for processing model objects during model building.
 *
 * <p>This service allows implementations to:
 * <ul>
 *   <li>Pool identical objects to reduce memory footprint</li>
 *   <li>Intern objects for faster equality comparisons</li>
 *   <li>Apply custom optimization strategies</li>
 *   <li>Transform or modify objects during building</li>
 * </ul>
 *
 * <p>Implementations are discovered via the Java ServiceLoader mechanism and should
 * be registered in {@code META-INF/services/org.apache.maven.api.model.ModelObjectProcessor}.
 *
 * <p>The service is called during model building for all model objects, allowing
 * implementations to decide which objects to process and how to optimize them.
 *
 * @since 4.0.0
 */
public interface ModelObjectProcessor {

    /**
     * Process a model object, potentially returning a pooled or optimized version.
     *
     * <p>This method is called during model building for various model objects.
     * Implementations can:
     * <ul>
     *   <li>Return the same object if no processing is desired</li>
     *   <li>Return a pooled equivalent object to reduce memory usage</li>
     *   <li>Return a modified or optimized version of the object</li>
     * </ul>
     *
     * <p>The implementation must ensure that the returned object is functionally
     * equivalent to the input object from the perspective of the Maven model.
     *
     * @param <T> the type of the object being processed
     * @param object the object to process
     * @return the processed object (may be the same instance or a different one)
     */
    <T> T processObject(T object);

    /**
     * Static utility method to process an object through all registered processors.
     * This method discovers and applies all available ModelObjectProcessor implementations.
     *
     * @param <T> the type of the object being processed
     * @param object the object to process
     * @return the processed object after applying all registered processors
     */
    static <T> T process(T object) {
        return ProcessorHolder.INSTANCE.processObject(object);
    }

    /**
     * Holder class for lazy initialization of the processor chain.
     */
    class ProcessorHolder {
        static final ModelObjectProcessor INSTANCE = createProcessorChain();

        private static ModelObjectProcessor createProcessorChain() {
            ServiceLoader<ModelObjectProcessor> loader = ServiceLoader.load(ModelObjectProcessor.class);
            ModelObjectProcessor chain = null;

            for (ModelObjectProcessor processor : loader) {
                if (chain == null) {
                    chain = processor;
                } else {
                    // Chain processors together
                    ModelObjectProcessor current = chain;
                    chain = new ModelObjectProcessor() {
                        @Override
                        public <T> T processObject(T object) {
                            return processor.processObject(current.processObject(object));
                        }
                    };
                }
            }

            // Return a no-op processor if none are found
            return chain != null
                    ? chain
                    : new ModelObjectProcessor() {
                        @Override
                        public <T> T processObject(T object) {
                            return object;
                        }
                    };
        }
    }
}
