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

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Provider;

/**
 * Wraps a registered resource with its file path and change status,
 * and provides a {@link #process()} method to obtain the full resource handle.
 *
 * <p>{@code Metadata} is a lightweight wrapper returned by
 * {@link IncrementalContext#registerInput(java.nio.file.Path)} and
 * {@link IncrementalContext#registerInputs(java.nio.file.Path, java.util.Collection, java.util.Collection)}.
 * It allows mojos to inspect a resource's {@link Status} <em>before</em> deciding whether to
 * process it, enabling selective processing patterns:</p>
 *
 * <pre>{@code
 * for (Metadata<Input> meta : buildContext.registerInputs(sourceDir, null, null)) {
 *     switch (meta.getStatus()) {
 *         case NEW:
 *         case MODIFIED:
 *             Input input = meta.process();
 *             compile(input.getPath());
 *             break;
 *         case REMOVED:
 *             // The build context handles cleanup of associated outputs
 *             break;
 *         case UNMODIFIED:
 *             // Nothing to do — skip this input
 *             break;
 *     }
 * }
 * }</pre>
 *
 * <p>Calling {@link #process()} marks the resource as "processed" in this build. The build
 * context uses this information to determine which outputs are stale: if an input was
 * registered but not processed, its associated outputs from the previous build are
 * carried over unchanged.</p>
 *
 * <h2>Two-pass processing</h2>
 *
 * <p>The separation between registration and processing enables a <em>two-pass</em>
 * pattern useful for tools whose outputs cannot be predicted in advance (e.g., a Java
 * compiler producing inner-class files). In the first pass, register inputs and inspect
 * their status to decide what to process. In the second pass — after the tool has run —
 * call {@link #process()} and associate the actual outputs:</p>
 *
 * <pre>{@code
 * // Pass 1: determine what changed
 * var all = buildContext.registerInputs(sourceDir, null, null);
 * var changed = all.stream()
 *         .filter(m -> m.getStatus() != Status.UNMODIFIED)
 *         .toList();
 *
 * // Run the tool on changed inputs only
 * tool.process(changed.stream().map(Metadata::getPath).toList());
 *
 * // Pass 2: associate outputs discovered after processing
 * for (Metadata<Input> meta : all) {
 *     Input input = meta.process();
 *     for (Path output : discoverOutputs(input.getPath())) {
 *         input.associateOutput(output);
 *     }
 * }
 * }</pre>
 *
 * @param <R> the resource type ({@link Input} or {@link Output})
 * @since 4.1.0
 * @see IncrementalContext#registerInput(java.nio.file.Path)
 * @see IncrementalContext#registerInputs(java.nio.file.Path, java.util.Collection, java.util.Collection)
 * @see Status
 */
@Experimental
@NotThreadSafe
@Provider
public interface Metadata<R extends Resource> {

    /**
     * {@return the path of the registered resource}
     */
    @Nonnull
    Path getPath();

    /**
     * {@return the change status of the resource relative to the previous build}
     */
    @Nonnull
    Status getStatus();

    /**
     * Marks this resource for processing and returns the full resource handle.
     *
     * @return the resource to process
     */
    @Nonnull
    R process();
}
