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
package org.apache.maven.api.services;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.api.Dialect;
import org.apache.maven.api.Service;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Model;

/**
 * The {@code ModelDialectManager} provides a mechanism for managing and 
 * translating between different dialects of Maven models. 
 * It allows reading and writing models in various dialects supported by the system.
 */
public interface ModelDialectManager extends Service {

    /**
     * Retrieves the set of all available dialects.
     * The result is never {@code null} and always contains at least one element, 
     * the core {@link org.apache.maven.api.Dialect#XML} dialect.
     *
     * @return a non-null {@link Set} of available dialects.
     */
    @Nonnull
    Set<Dialect> getAvailableDialects();

    /**
     * Reads a Maven model from the specified directory using the specified dialect.
     *
     * @param dir the directory from which to read the model, must not be {@code null}.
     * @param dialect the dialect to be used for reading, must not be {@code null}.
     * @param options optional parameters for reading, may be {@code null}.
     * @return an {@link Optional} containing the read model, or an empty {@link Optional} if reading fails.
     * @throws IllegalArgumentException if an unrecognized dialect is provided.
     */
    @Nonnull
    Optional<Model> readModel(@Nonnull Path dir, @Nonnull Dialect dialect, @Nullable Map<String, ?> options);

    /**
     * Writes the given Maven model to the specified directory using the specified dialect.
     *
     * @param dir the directory where the model will be written, must not be {@code null}.
     * @param dialect the dialect to be used for writing, must not be {@code null}.
     * @param model the Maven model to be written, must not be {@code null}.
     * @param options optional parameters for writing, may be {@code null}.
     * @return an {@link Optional} containing the path of the written file, or an empty {@link Optional} if the writing fails.
     * @throws IllegalArgumentException if an unrecognized dialect is provided.
     */
    @Nonnull
    Optional<Path> writeModel(
            @Nonnull Path dir, @Nonnull Dialect dialect, @Nonnull Model model, @Nullable Map<String, ?> options);
}
