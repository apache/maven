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
package org.apache.maven.api.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Provides access to the contents of a source independently of the
 * backing store (e.g. file system, database, memory).
 * <p>
 * This is mainly used to parse files into objects such as Maven projects,
 * models, settings, or toolchains. The source implementation handles
 * all the details of accessing the underlying content while providing
 * a uniform API to consumers.
 * <p>
 * Sources can represent:
 * <ul>
 *   <li>Local filesystem files</li>
 *   <li>In-memory content</li>
 *   <li>Database entries</li>
 *   <li>Network resources</li>
 * </ul>
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.ProjectBuilder#build(Session, Source)
 */
@Experimental
public interface Source {
    /**
     * Provides access to the file backing this source, if available.
     * Not all sources are backed by files - for example, in-memory sources
     * or database-backed sources will return null.
     *
     * @return the underlying {@code Path} if this source is file-backed,
     *         or {@code null} if this source has no associated file
     */
    @Nullable
    Path getPath();

    /**
     * Creates a new input stream to read the source contents.
     * Each call creates a fresh stream starting from the beginning.
     * The caller is responsible for closing the returned stream.
     *
     * @return a new input stream positioned at the start of the content
     * @throws IOException if the stream cannot be created or opened
     */
    @Nonnull
    InputStream openStream() throws IOException;

    /**
     * Returns a human-readable description of where this source came from,
     * used primarily for error messages and debugging.
     * <p>
     * Examples of locations:
     * <ul>
     *   <li>Absolute file path: {@code /path/to/pom.xml}</li>
     *   <li>Relative file path: {@code ../parent/pom.xml}</li>
     *   <li>URL: {@code https://repo.maven.org/.../pom.xml}</li>
     *   <li>Description: {@code <memory>} or {@code <database>}</li>
     * </ul>
     *
     * @return a non-null string describing the source location
     */
    @Nonnull
    String getLocation();

    /**
     * Resolves a new source relative to this one.
     * <p>
     * The resolution strategy depends on the source type:
     * <ul>
     *   <li>File sources resolve against their parent directory</li>
     *   <li>URL sources resolve against their base URL</li>
     *   <li>Other sources may not support resolution and return null</li>
     * </ul>
     * <p>
     * The implementation must handle:
     * <ul>
     *   <li>Both forward and back slashes as path separators</li>
     *   <li>Parent directory references (..)</li>
     *   <li>Both file and directory targets</li>
     * </ul>
     *
     * @param relative path to resolve relative to this source
     * @return the resolved source, or null if resolution not possible
     * @throws NullPointerException if relative is null
     */
    @Nullable
    Source resolve(@Nonnull String relative);
}
