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

/**
 * Incremental build context API for Apache Maven 4.
 *
 * <h2>Overview</h2>
 *
 * <p>This package provides the API for <em>incremental builds</em> in Maven. An incremental
 * build skips work that is already up-to-date, avoiding redundant processing when only a
 * subset of source files have changed. The central abstraction is {@link IncrementalContext},
 * which tracks <strong>inputs</strong> (source files), <strong>outputs</strong> (generated
 * files), and the relationships between them across successive builds.</p>
 *
 * <p>Mojos that perform file transformations (compilation, code generation, resource
 * filtering, etc.) can use this API to:</p>
 * <ul>
 *   <li>Detect which input files are new, modified, or removed since the last build</li>
 *   <li>Process only the changed inputs and regenerate only the affected outputs</li>
 *   <li>Automatically clean up stale outputs whose inputs have been removed</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 *
 * <p>The API is organized around four key abstractions:</p>
 *
 * <pre>
 *  IncrementalContext              The entry point. Registers inputs, creates outputs,
 *   +-- Input                tracks relationships between them.
 *   +-- Output
 *   +-- InputSet             Groups inputs for aggregated operations
 *
 *  Metadata&lt;R&gt;              Wraps a resource with its change status before
 *                            it is "processed" into a full Input or Output handle.
 *
 *  Status                   Change status: NEW, MODIFIED, UNMODIFIED, REMOVED.
 * </pre>
 *
 * <h2>Use Case 1: One-to-one file transformation</h2>
 *
 * <p>The simplest pattern: each input file produces exactly one output file.
 * Only changed inputs are processed; stale outputs are cleaned up automatically.</p>
 *
 * <pre>{@code
 * @Inject
 * IncrementalContext buildContext;
 *
 * public void execute() {
 *     // Register all .xml files under src/main/resources, excluding tests
 *     for (Input input : buildContext.registerAndProcessInputs(
 *             sourceDir,
 *             List.of("**&#47;*.xml"),
 *             List.of("**&#47;test-*"))) {
 *
 *         Path outputPath = outputDir.resolve(
 *                 sourceDir.relativize(input.getPath()));
 *
 *         Output output = input.associateOutput(outputPath);
 *         try (OutputStream os = output.newOutputStream()) {
 *             transform(input.getPath(), os);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Use Case 2: Aggregated output (many inputs to one output)</h2>
 *
 * <p>Some operations aggregate multiple inputs into a single output (e.g., generating
 * an index, merging property files, building a ZIP archive). Use {@link InputSet} for this:</p>
 *
 * <pre>{@code
 * @Inject
 * IncrementalContext buildContext;
 *
 * public void execute() {
 *     InputSet inputSet = buildContext.newInputSet();
 *     inputSet.registerInputs(sourceDir, List.of("**&#47;*.properties"), null);
 *
 *     // aggregate() only invokes the callback if any input changed
 *     inputSet.aggregate(mergedOutput, (output, inputs) -> {
 *         Properties merged = new Properties();
 *         for (Input input : inputs) {
 *             try (InputStream is = Files.newInputStream(input.getPath())) {
 *                 merged.load(is);
 *             }
 *         }
 *         try (OutputStream os = output.newOutputStream()) {
 *             merged.store(os, "Merged properties");
 *         }
 *     });
 * }
 * }</pre>
 *
 * <h2>Use Case 3: Conditional execution with status checks</h2>
 *
 * <p>When a mojo does not produce individual output files but performs an action
 * (e.g., deploying, validating), use {@link IncrementalContext#isProcessingRequired()}
 * or inspect the {@link Status} of individual resources to decide whether to act:</p>
 *
 * <pre>{@code
 * @Inject
 * IncrementalContext buildContext;
 *
 * public void execute() {
 *     Iterable<? extends Metadata<Input>> inputs =
 *             buildContext.registerInputs(sourceDir, null, null);
 *
 *     boolean hasChanges = false;
 *     for (Metadata<Input> meta : inputs) {
 *         if (meta.getStatus() != Status.UNMODIFIED) {
 *             hasChanges = true;
 *             Input input = meta.process();
 *             validate(input.getPath());
 *         }
 *     }
 *
 *     if (!hasChanges) {
 *         buildContext.markSkipExecution();
 *     }
 * }
 * }</pre>
 *
 * <h2>Use Case 4: Two-pass processing (compile, then associate outputs)</h2>
 *
 * <p>When a tool produces outputs that cannot be predicted before processing
 * (e.g., a Java compiler generating inner-class files like {@code Foo$1.class},
 * {@code Foo$Inner.class}), use the two-pass API: register inputs first to determine
 * what changed, run the tool, then associate the actual outputs afterward:</p>
 *
 * <pre>{@code
 * @Inject
 * IncrementalContext buildContext;
 *
 * public void execute() {
 *     // Pass 1: register sources and inspect their status
 *     var allInputs = buildContext.registerInputs(sourceDir, List.of("**&#47;*.java"), null);
 *     List<Metadata<Input>> toCompile = new ArrayList<>();
 *     for (Metadata<Input> meta : allInputs) {
 *         if (meta.getStatus() != Status.UNMODIFIED) {
 *             toCompile.add(meta);
 *         }
 *     }
 *
 *     if (toCompile.isEmpty()) {
 *         buildContext.markSkipExecution();
 *         return;
 *     }
 *
 *     // Compile the changed sources
 *     List<Path> sourceFiles = toCompile.stream()
 *             .map(Metadata::getPath).toList();
 *     compiler.compile(sourceFiles, outputDir);
 *
 *     // Pass 2: associate the actual outputs (now known after compilation)
 *     for (Metadata<Input> meta : allInputs) {
 *         Input input = meta.process();
 *         String baseName = getClassName(input.getPath()); // Foo.java → Foo
 *         // Scan for Foo.class, Foo$Inner.class, Foo$1.class, etc.
 *         for (Path classFile : findClassFiles(outputDir, baseName)) {
 *             input.associateOutput(classFile);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>When a previously registered input is removed in a subsequent build, the build
 * context automatically deletes all outputs that were associated with it — including
 * any inner-class files discovered during the previous build's pass 2.</p>
 *
 * <h2>Configuration change detection (non-file state)</h2>
 *
 * <p>Incremental builds must consider more than just file changes. A mojo's behavior
 * also depends on its <strong>configuration</strong> — compiler flags, plugin versions,
 * dependency classpath, and other parameters. If any of these change between builds,
 * all inputs must be reprocessed even if no source files were modified.</p>
 *
 * <p>The Maven runtime handles this <strong>automatically</strong>. Before each mojo
 * execution, Maven:</p>
 * <ol>
 *   <li><strong>Digests all mojo parameters</strong> — every {@code @Parameter}-annotated
 *       field is reflected, its value evaluated, and a digest computed. This covers
 *       compiler options, output directories, filter configurations, etc.</li>
 *   <li><strong>Digests the plugin classpath</strong> — the SHA-1 hash of every plugin
 *       dependency JAR's contents (not just timestamps) is computed and cached per
 *       session.</li>
 *   <li><strong>Compares with the previous build</strong> — if any digest differs from
 *       the value stored in the state file, the build context <em>escalates</em>: all
 *       inputs are treated as modified, forcing a full rebuild.</li>
 * </ol>
 *
 * <p>This means mojos do <strong>not</strong> need to implement their own
 * options-change or classpath-change detection. The build context infrastructure
 * handles it transparently. A mojo that only uses {@code registerInputs()} and
 * {@code associateOutput()} will automatically get correct incremental behavior
 * even when configuration changes — no additional code required.</p>
 *
 * <p>The configuration digest is provided through
 * {@link org.apache.maven.api.build.incremental.spi.IncrementalContextEnvironment#getParameters()
 * IncrementalContextEnvironment.getParameters()}, which the Maven runtime populates via
 * a {@code MojoConfigurationDigester}.</p>
 *
 * <h2>Escalation</h2>
 *
 * <p>The build context <em>escalates</em> to a full build (treating all inputs as
 * modified) in several situations:</p>
 * <ul>
 *   <li>No previous state file exists (first build, or after a clean)</li>
 *   <li>The configuration digest has changed (mojo parameters or plugin classpath)</li>
 *   <li>Previously tracked output files are missing on disk</li>
 *   <li>The workspace explicitly requests escalation
 *       ({@link org.apache.maven.api.build.incremental.spi.Workspace.Mode#ESCALATED})</li>
 * </ul>
 *
 * <p>Escalation is transparent to mojos — they use the same API regardless. The only
 * visible effect is that {@link Status#UNMODIFIED} inputs become rare or absent,
 * so the mojo ends up processing everything.</p>
 *
 * <h2>Lifecycle and state persistence</h2>
 *
 * <p>The build context persists its state (file timestamps, input-output associations,
 * resource attributes, and configuration digests) between builds in a state file
 * managed by the implementation. On each build:</p>
 * <ol>
 *   <li>The context loads the previous state (if any), compares configuration digests,
 *       and compares file timestamps to determine each input's {@link Status}</li>
 *   <li>The mojo registers inputs and creates outputs through the API</li>
 *   <li>At commit time, the context saves the new state and cleans up stale outputs
 *       (outputs associated with inputs that no longer exist)</li>
 * </ol>
 *
 * <p>Mojos do not manage the state file directly. The Maven runtime (or IDE integration)
 * handles initialization and commit through the
 * {@link org.apache.maven.api.build.incremental.spi SPI} interfaces.</p>
 *
 * @since 4.1.0
 * @see IncrementalContext
 * @see Input
 * @see Output
 * @see InputSet
 * @see Metadata
 * @see org.apache.maven.api.build.incremental.spi
 */
@Experimental
package org.apache.maven.api.build.incremental;

import org.apache.maven.api.annotations.Experimental;
