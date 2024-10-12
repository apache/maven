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
package org.apache.maven.api.build;

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
 * Represents input set being aggregated.
 */
@Experimental
@NotThreadSafe
@Provider
public interface InputSet {

    void addInput(@Nonnull Metadata<Input> inputMetadata);

    /**
     * Register an input file.
     *
     * @param inputFile
     * @return
     */
    @Nonnull
    Metadata<Input> registerInput(@Nonnull Path inputFile);

    /**
     * Uses a scanner to register input files in a given directory.
     *
     * @param basedir
     * @param includes
     * @param excludes
     * @return
     */
    @Nonnull
    Collection<? extends Metadata<Input>> registerInputs(
            @Nonnull Path basedir, @Nullable Collection<String> includes, @Nullable Collection<String> excludes);

    /**
     * Aggregate registed inputs to the given output file.
     *
     * @param outputFile
     * @param aggregator
     * @return
     */
    boolean aggregate(@Nonnull Path outputFile, @Nonnull BiConsumer<Output, Collection<Input>> aggregator);

    /**
     * Performs an indirect metadata aggregation.
     * The benefit is that the metadata for each input file is cached instead of being recomputed.
     *
     * @param outputFile the output file
     * @param stepId a unique id identifying this aggregation
     * @param identity the metadata identity
     * @param mapper the mapper function to extract the metadata from an input file
     * @param accumulator the metadata accumulator
     * @param writer the metadata writer
     * @return a boolean indicating if the output file has been rewritten or not
     * @param <T> the metadata type which needs to implement {@link Serializable}
     * @throws BuildContextException if an exception occurs
     */
    <T extends Serializable> boolean aggregate(
            @Nonnull Path outputFile,
            @Nonnull String stepId,
            @Nonnull T identity,
            @Nonnull Function<Input, T> mapper,
            @Nonnull BinaryOperator<T> accumulator,
            @Nonnull BiConsumer<Output, T> writer);
}
