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
package org.apache.maven.api.build.incremental;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Represents a set of inputs being aggregated into one or more outputs.
 *
 * <p>An input set is the mechanism for <em>many-to-one</em> (or <em>many-to-few</em>)
 * transformations, where multiple input files contribute to a single output file. The
 * build context tracks which inputs belong to the set and only invokes the aggregation
 * callback when at least one input has changed since the previous build.</p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *   <li>Merging multiple property files or configuration fragments into one</li>
 *   <li>Generating an index or manifest from a set of source files</li>
 *   <li>Building a ZIP or JAR from a directory tree</li>
 *   <li>Computing aggregate statistics or checksums</li>
 * </ul>
 *
 * <h2>Basic aggregation</h2>
 *
 * <pre>{@code
 * InputSet inputSet = buildContext.newInputSet();
 * inputSet.registerInputs(sourceDir, List.of("**&#47;*.properties"), null);
 *
 * // The callback is only invoked if any input changed
 * boolean written = inputSet.aggregate(mergedOutput, (output, inputs) -> {
 *     Properties merged = new Properties();
 *     for (Input input : inputs) {
 *         try (InputStream is = Files.newInputStream(input.getPath())) {
 *             merged.load(is);
 *         }
 *     }
 *     try (OutputStream os = output.newOutputStream()) {
 *         merged.store(os, null);
 *     }
 * });
 * }</pre>
 *
 * <h2>Indirect metadata aggregation</h2>
 *
 * <p>The second {@link #aggregate(java.nio.file.Path, String, Serializable, Function,
 * BinaryOperator, BiConsumer) aggregate} overload supports caching per-input metadata
 * across builds. This is useful when extracting metadata from each input is expensive
 * (e.g., parsing an AST). Only changed inputs are re-processed; cached metadata is
 * reused for unchanged inputs.</p>
 *
 * @since 4.1.0
 * @see IncrementalContext#newInputSet()
 * @see Input
 * @see Output
 */
@Experimental
@NotThreadSafe
@Provider
public interface InputSet {

    /**
     * Adds a previously registered input to this set.
     *
     * @param inputMetadata the input metadata to add
     */
    void addInput(@Nonnull Metadata<Input> inputMetadata);

    /**
     * Registers an input file and adds it to this set.
     *
     * @param inputFile the input file to register
     * @return the metadata for the registered input
     */
    @Nonnull
    Metadata<Input> registerInput(@Nonnull Path inputFile);

    /**
     * Registers input files matching the given patterns and adds them to this set.
     *
     * @param basedir  the base directory to scan for inputs
     * @param includes patterns of files to include, may be {@code null} to match all
     * @param excludes patterns of files to exclude, may be {@code null}
     * @return the metadata for all registered inputs
     */
    @Nonnull
    Collection<? extends Metadata<Input>> registerInputs(
            @Nonnull Path basedir, @Nullable Collection<String> includes, @Nullable Collection<String> excludes);

    /**
     * Aggregates all registered inputs into the given output file.
     *
     * @param outputFile the output file to write
     * @param aggregator a consumer that receives the output and the collection of inputs
     * @return {@code true} if the output was written, {@code false} if it was up-to-date
     */
    boolean aggregate(@Nonnull Path outputFile, @Nonnull BiConsumer<Output, Collection<Input>> aggregator);

    /**
     * Performs an indirect metadata aggregation. The metadata for each input file is cached
     * across builds, avoiding recomputation when an input has not changed.
     *
     * @param outputFile  the output file to write
     * @param stepId      a unique identifier for this aggregation step
     * @param identity    the identity value for the accumulator
     * @param mapper      extracts metadata from each input
     * @param accumulator combines metadata values
     * @param writer      writes the accumulated result to the output
     * @param <T>         the metadata type, must be {@link Serializable}
     * @return {@code true} if the output was rewritten, {@code false} if it was up-to-date
     * @throws IncrementalContextException if an error occurs
     */
    <T extends Serializable> boolean aggregate(
            @Nonnull Path outputFile,
            @Nonnull String stepId,
            @Nonnull T identity,
            @Nonnull Function<Input, T> mapper,
            @Nonnull BinaryOperator<T> accumulator,
            @Nonnull BiConsumer<Output, T> writer);
}
