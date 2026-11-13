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
 * Represents an input resource in the incremental build context.
 *
 * <p>An {@code Input} is obtained either by calling {@link Metadata#process()} on a registered
 * input metadata, or directly from
 * {@link IncrementalContext#registerAndProcessInputs(java.nio.file.Path, java.util.Collection, java.util.Collection)}.
 * Once you have an {@code Input}, you can read its file and associate it with one or more
 * {@link Output} resources to establish the input-to-output relationship used for
 * stale-output cleanup:</p>
 *
 * <pre>{@code
 * Input input = buildContext.registerInput(sourceFile).process();
 *
 * // One input -> one output
 * Output output = input.associateOutput(targetFile);
 * try (OutputStream os = output.newOutputStream()) {
 *     transform(input.getPath(), os);
 * }
 *
 * // One input -> multiple outputs
 * Output header = input.associateOutput(headerFile);
 * Output body   = input.associateOutput(bodyFile);
 * }</pre>
 *
 * <p>When an input is removed in a subsequent build, the build context automatically deletes
 * all outputs that were associated with it in the previous build.</p>
 *
 * @since 4.1.0
 * @see IncrementalContext#registerInput(java.nio.file.Path)
 * @see Output
 * @see Metadata#process()
 */
@Experimental
@NotThreadSafe
@Provider
public interface Input extends Resource {

    /**
     * Associates this input with the given output file and returns the output resource.
     *
     * @param outputFile the path of the output file to associate
     * @return the associated output resource
     */
    @Nonnull
    Output associateOutput(@Nonnull Path outputFile);
}
