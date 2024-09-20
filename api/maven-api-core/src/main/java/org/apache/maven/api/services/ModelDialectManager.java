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
 * Dialect manager, that offers passage between various dialects.
 */
public interface ModelDialectManager extends Service {
    /**
     * Returns the available dialects, never {@code null}. Result set has at least one element, the {@link org.apache.maven.api.Dialect#XML}
     * which is present in core.
     */
    @Nonnull
    Set<Dialect> getAvailableDialects();

    /**
     * Reads the model from given directory in given dialect.
     *
     * @param dir the directory from where to read, never {@code null}
     * @param dialect the dialect to use to read, never {@code null}
     * @param options the options for reading
     * @return optional with the model that was read or empty
     * @throws IllegalArgumentException if unknown dialect was asked for
     */
    @Nonnull
    Optional<Model> readModel(@Nonnull Path dir, @Nonnull Dialect dialect, @Nullable Map<String, ?> options);

    /**
     * Write the model to given directory in given dialect.
     *
     * @param dir the directory to where to write, never {@code null}
     * @param dialect the dialect to use to write, never {@code null}
     * @param model the model to write, never {@code null}
     * @param options the options for writing
     * @return optional with file path where write happened or empty
     * @throws IllegalArgumentException if unknown dialect was asked for
     */
    @Nonnull
    Optional<Path> writeModel(
            @Nonnull Path dir, @Nonnull Dialect dialect, @Nonnull Model model, @Nullable Map<String, ?> options);
}
