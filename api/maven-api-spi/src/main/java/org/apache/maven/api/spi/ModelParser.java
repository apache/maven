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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.Source;

/**
 * The {@code ModelParser} interface is used to locate and read {@link Model}s from the file system.
 * This allows plugging in additional syntaxes for the main model read by Maven when building a project.
 */
@Experimental
public interface ModelParser {

    /**
     * Locates the pom in the given directory.
     *
     * @param dir the directory to locate the pom for, never {@code null}
     * @return a {@code Source} pointing to the located pom or an empty {@code Optional} if none was found by this parser
     */
    @Nonnull
    Optional<Source> locate(@Nonnull Path dir);

    /**
     * Parse the model obtained previously by a previous call to {@link #locate(Path)}.
     *
     * @param source the source to parse, never {@code null}
     * @param options possible parsing options, may be {@code null}
     * @return the parsed {@link Model}, never {@code null}
     * @throws ModelParserException if the model cannot be parsed
     */
    @Nonnull
    Model parse(@Nonnull Source source, @Nullable Map<String, ?> options) throws ModelParserException;

    /**
     * Locate and parse the model in the specified directory.
     * This is equivalent to {@code locate(dir).map(s -> parse(s, options))}.
     *
     * @param dir the directory to locate the pom for, never {@code null}
     * @param options possible parsing options, may be {@code null}
     * @return an optional parsed {@link Model} or {@code null} if none could be found
     * @throws ModelParserException if the located model cannot be parsed
     */
    default Optional<Model> locateAndParse(@Nonnull Path dir, @Nullable Map<String, ?> options)
            throws ModelParserException {
        return locate(dir).map(s -> parse(s, options));
    }
}
