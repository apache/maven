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
package org.apache.maven.api.build.context;

import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Provides incremental build support by tracking inputs, outputs and their relationships.
 * <p>
 * A first use case is a <strong>basic build</strong> where inputs and outputs are identified but without
 * any relationship between those. For such cases, the code would look like:
 * <pre>
 *     context.registerInput(path1);
 *     context.registerInput(path2);
 * </pre>
 * <p>
 * A second use case is an <strong>aggregated build</strong> where multiple inputs contribute to a single
 * output. Use {@link #newInputSet()} to create an {@link InputSet} and register inputs that are then
 * aggregated into outputs.
 *
 * @since 4.0.0
 */
@Experimental
@NotThreadSafe
@Provider
public interface BuildContext {

    /**
     * {@return whether the build needs to process any inputs}
     */
    boolean isProcessingRequired();

    /**
     * Registers and processes the given output file.
     *
     * @param outputFile the output file path
     * @return the processed output resource
     */
    @Nonnull
    Output processOutput(@Nonnull Path outputFile);

    /**
     * Creates a new {@link InputSet} which can be used to associate inputs to outputs.
     *
     * @return a new input set, never {@code null}
     */
    @Nonnull
    InputSet newInputSet();

    /**
     * Registers the specified input {@code Path} with this build context.
     *
     * @param inputFile the input file to register
     * @return the metadata representing the input file, never {@code null}
     * @throws IllegalArgumentException if {@code inputFile} is not a file or cannot be read
     */
    @Nonnull
    Metadata<Input> registerInput(@Nonnull Path inputFile);

    /**
     * Registers inputs identified by {@code basedir} and {@code includes}/{@code excludes} ant
     * patterns.
     * <p>
     * When a file is found under {@code basedir}, it will be registered if it does not match
     * {@code excludes} patterns and matches {@code includes} patterns. {@code null} or empty includes
     * parameter will match all files. {@code excludes} match takes precedence over {@code includes}:
     * if a file matches one of the excludes patterns it will not be registered regardless of includes
     * patterns match.
     * <p>
     * The implementation is not expected to handle changes to {@code basedir}, {@code includes} or
     * {@code excludes} incrementally.
     *
     * @param basedir  the base directory to look for inputs, must not be {@code null}
     * @param includes patterns of the files to register, may be {@code null}
     * @param excludes patterns of the files to ignore, may be {@code null}
     * @return the metadata for the registered inputs
     * @throws BuildContextException if an I/O error occurs
     */
    @Nonnull
    Collection<? extends Metadata<Input>> registerInputs(
            @Nonnull Path basedir, @Nullable Collection<String> includes, @Nullable Collection<String> excludes);

    /**
     * Registers inputs identified by {@code basedir} and {@code includes}/{@code excludes} ant
     * patterns, then processes inputs that are new or modified since the previous build.
     *
     * @param basedir  the base directory to look for inputs, must not be {@code null}
     * @param includes patterns of the files to register, may be {@code null}
     * @param excludes patterns of the files to ignore, may be {@code null}
     * @return the processed inputs
     * @throws BuildContextException if an I/O error occurs
     */
    @Nonnull
    Collection<? extends Input> registerAndProcessInputs(
            @Nonnull Path basedir, @Nullable Collection<String> includes, @Nullable Collection<String> excludes);

    /**
     * Marks skipped build execution. All inputs, outputs and their associated metadata are carried
     * over to the next build as-is. No context modification operations ({@code register*} or
     * {@code process}) are permitted after this call.
     */
    void markSkipExecution();

    /**
     * Sets whether the build should continue even if there are build errors.
     *
     * @param failOnError {@code true} to fail on error, {@code false} to continue
     */
    void setFailOnError(boolean failOnError);

    /**
     * {@return whether the build should fail on error}
     */
    boolean getFailOnError();
}
