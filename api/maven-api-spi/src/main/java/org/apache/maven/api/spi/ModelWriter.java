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
package org.apache.maven.api.spi;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;

/**
 * The {@code ModelWriter} interface is used to write {@link Model}s to the file system.
 * This allows plugging in additional syntaxes for the main model write.
 * <p>
 * Note: to provide a Maven model "dialect" that supports symmetric translation, implement
 * {@link ModelDialectProvider}, which composes a pair of parser and writer.
 *
 * @since 4.1.0
 * @see ModelDialectProvider
 * @see DialectProvider
 */
@Experimental
@Consumer
@Named
public interface ModelWriter extends SpiService {

    /**
     * Targets the pom file in the given directory.
     *
     * @param dir the directory to target the pom for, never {@code null}
     * @return a {@code Path} pointing to the targeted pom file,
     *         or an empty {@code Optional} if this writer refuses to target the directory
     */
    @Nonnull
    Optional<Path> target(@Nonnull Path dir);

    /**
     * Writes the given model to the specified target path.
     * The actual file is decided by the implementation via {@link #target(Path)}.
     *
     * @param target the file path to write the model to, never {@code null}
     * @param model the model to write, never {@code null}
     * @param options possible writing options, may be {@code null}
     * @throws ModelWriterException if the model could not be written
     */
    void write(@Nonnull Path target, @Nonnull Model model, @Nullable Map<String, ?> options)
            throws ModelWriterException;

    /**
     * Targets and writes the model in the specified directory.
     * This is equivalent to {@code target(dir).map(path -> { write(path, model, options); return path; })}.
     *
     * @param dir the directory to target the pom for, never {@code null}
     * @param model the model to write, never {@code null}
     * @param options possible writing options, may be {@code null}
     * @return an {@link Optional} containing the written file path, or empty if this writer refused the directory
     * @throws ModelWriterException if the model could not be written
     */
    @Nonnull
    default Optional<Path> targetAndWrite(@Nonnull Path dir, @Nonnull Model model, @Nullable Map<String, ?> options)
            throws ModelWriterException {
        return target(dir).map(path -> {
            write(path, model, options);
            return path;
        });
    }
}
