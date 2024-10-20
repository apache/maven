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
 * Note: to provide Maven model "dialect", that can have models translated from-to, implement {@link ModelDialectProvider}
 * that compose pairs of parser and writer (make dialect symmetrical).
 *
 * @since 4.0.0
 * @see ModelDialectProvider
 * @see DialectProvider
 */
@Experimental
@Consumer
@Named
public interface ModelWriter extends SpiService {

    /**
     * Targets the pom in the given directory.
     *
     * @param dir the directory to target the pom for, never {@code null}
     * @return a {@code Path} pointing to the targeted pom file, or an empty {@code Optional} if serializer refuses to target.
     */
    @Nonnull
    Optional<Path> target(@Nonnull Path dir);

    /**
     * Write out given model into given directory. The actual file (within directory) is decided by implementation,
     * and is returned upon successful write operation.
     *
     * @param target the file where to write out model, never {@code null}.
     * @param model the model to write out. never {@code null}
     * @param options possible writing options, may be {@code null}
     * @throws ModelWriterException if the model could not be written
     */
    @Nonnull
    void write(@Nonnull Path target, @Nonnull Model model, @Nullable Map<String, ?> options)
            throws ModelWriterException;

    /**
     * Target and write out the model in the specified directory.
     * This is equivalent to {@code target(dir).map(s -> write(s, model, options))}.
     *
     * @param dir the directory to target the pom for, never {@code null}
     * @param model the model to write out, never {@code null}.
     * @param options possible parsing options, may be {@code null}
     * @return an optional with pom file path
     * @throws ModelWriterException if the target model cannot be written
     */
    @Nonnull
    default Optional<Path> targetAndWrite(@Nonnull Path dir, @Nonnull Model model, @Nullable Map<String, ?> options)
            throws ModelWriterException {
        return target(dir).map(s -> {
            write(s, model, options);
            return s;
        });
    }
}
