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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.NotThreadSafe;
import org.apache.maven.api.annotations.Provider;

/**
 * Represents an output resource in the incremental build context.
 *
 * <p>An {@code Output} is obtained by calling {@link Input#associateOutput(java.nio.file.Path)}
 * or {@link IncrementalContext#processOutput(java.nio.file.Path)}. It provides methods to write the
 * output content:</p>
 *
 * <pre>{@code
 * Output output = input.associateOutput(targetPath);
 *
 * // Binary output
 * try (OutputStream os = output.newOutputStream()) {
 *     writeBytes(os);
 * }
 *
 * // Text output
 * try (BufferedWriter w = output.newBufferedWriter(StandardCharsets.UTF_8)) {
 *     w.write("generated content");
 * }
 * }</pre>
 *
 * <p>The output stream returned by {@link #newOutputStream()} may be a <em>caching stream</em>
 * that only overwrites the target file when the content has actually changed. This prevents
 * downstream tools and IDEs from seeing a modified timestamp on an unchanged file, avoiding
 * unnecessary cascading rebuilds.</p>
 *
 * <p>The build context automatically creates parent directories for the output file
 * if they do not exist.</p>
 *
 * @since 4.1.0
 * @see Input#associateOutput(java.nio.file.Path)
 * @see IncrementalContext#processOutput(java.nio.file.Path)
 */
@Experimental
@NotThreadSafe
@Provider
public interface Output extends Resource {

    /**
     * Returns a new caching output stream for this output resource.
     *
     * @return a new output stream, never {@code null}
     * @throws IncrementalContextException if an I/O error occurs
     */
    @Nonnull
    OutputStream newOutputStream();

    /**
     * Returns a new buffered writer for this output resource using the given charset.
     *
     * @param charset the charset to use for encoding
     * @return a new buffered writer, never {@code null}
     * @throws IncrementalContextException if an I/O error occurs
     */
    @Nonnull
    default BufferedWriter newBufferedWriter(@Nonnull Charset charset) {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(), charset));
    }
}
