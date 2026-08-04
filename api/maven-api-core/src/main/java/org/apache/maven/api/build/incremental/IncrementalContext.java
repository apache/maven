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

import java.nio.file.Path;
import java.util.Collection;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Provides incremental build support by tracking inputs, outputs and their relationships.
 *
 * <p>This is the primary entry point for mojos that want to participate in incremental builds.
 * The build context tracks which files were processed in the previous build, detects changes
 * (new, modified, removed), and automatically cleans up stale outputs whose inputs no longer
 * exist.</p>
 *
 * <h2>One-to-one transformation</h2>
 *
 * <p>The most common pattern: each input file produces one output file. Use
 * {@link #registerAndProcessInputs(Path, Collection, Collection)} to register and filter
 * in a single call — only changed inputs are returned:</p>
 *
 * <pre>{@code
 * @Inject
 * IncrementalContext buildContext;
 *
 * public void execute() {
 *     for (Input input : buildContext.registerAndProcessInputs(
 *             sourceDir, List.of("**&#47;*.xml"), null)) {
 *
 *         Path outPath = outputDir.resolve(sourceDir.relativize(input.getPath()));
 *         Output output = input.associateOutput(outPath);
 *         try (OutputStream os = output.newOutputStream()) {
 *             transform(input.getPath(), os);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Two-pass processing with status inspection</h2>
 *
 * <p>When you need to inspect the change status before deciding what to do, use
 * {@link #registerInputs(Path, Collection, Collection)} to get {@link Metadata} wrappers,
 * then call {@link Metadata#process()} on the ones you want to process:</p>
 *
 * <pre>{@code
 * for (Metadata<Input> meta : buildContext.registerInputs(sourceDir, null, null)) {
 *     if (meta.getStatus() != Status.UNMODIFIED) {
 *         Input input = meta.process();
 *         // ... process the changed input
 *     }
 * }
 * }</pre>
 *
 * <h2>Configuration and non-file state</h2>
 *
 * <p>The build context automatically tracks <strong>non-file state</strong> that affects
 * outputs: mojo parameter values, plugin classpath contents, and any other configuration
 * provided through the
 * {@link org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment#getParameters()
 * environment parameters}. If any configuration value changes between builds, the context
 * <em>escalates</em> — all inputs are treated as modified, forcing a full rebuild.
 * Mojos do not need to implement their own options-change detection.</p>
 *
 * <h2>Aggregated builds</h2>
 *
 * <p>When multiple inputs contribute to a single output (e.g., merging property files,
 * generating an index), use {@link #newInputSet()} to create an {@link InputSet}:</p>
 *
 * <pre>{@code
 * InputSet inputSet = buildContext.newInputSet();
 * inputSet.registerInputs(sourceDir, List.of("**&#47;*.properties"), null);
 * inputSet.aggregate(mergedOutput, (output, inputs) -> {
 *     // only called if any input changed since the last build
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
 * @since 4.1.0
 * @see Input
 * @see Output
 * @see InputSet
 * @see Metadata
 * @see Status
 */
@Experimental
@NotThreadSafe
@Provider
public interface IncrementalContext {

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
     * Registers a single input file with this build context.
     *
     * <p>This method is useful for registering individual files that are not part of a
     * directory tree — for example, dependency JARs, configuration files, or other
     * external inputs whose change should trigger reprocessing. Like directory-based
     * registration, the returned {@link Metadata} provides the file's change
     * {@link Status} relative to the previous build.</p>
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
     * @throws IncrementalContextException if an I/O error occurs
     */
    @Nonnull
    Collection<? extends Metadata<Input>> registerInputs(
            @Nonnull Path basedir, @Nullable Collection<String> includes, @Nullable Collection<String> excludes);

    /**
     * Registers inputs identified by {@code basedir} and {@code includes}/{@code excludes} ant
     * patterns, then marks inputs that are {@link Status#NEW NEW} or {@link Status#MODIFIED MODIFIED}
     * as processed.
     *
     * <p>The returned collection contains <strong>all</strong> matching inputs, including
     * {@link Status#UNMODIFIED UNMODIFIED} ones. Unmodified inputs are included so that
     * output associations and metadata can be carried forward. Only new and modified inputs
     * are marked as processed internally.</p>
     *
     * @param basedir  the base directory to look for inputs, must not be {@code null}
     * @param includes patterns of the files to register, may be {@code null}
     * @param excludes patterns of the files to ignore, may be {@code null}
     * @return all registered inputs (new, modified, and unmodified)
     * @throws IncrementalContextException if an I/O error occurs
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
    boolean isFailOnError();
}
