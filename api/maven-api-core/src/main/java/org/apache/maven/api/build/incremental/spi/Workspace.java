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
package org.apache.maven.api.build.incremental.spi;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;
import org.apache.maven.api.build.incremental.Status;

/**
 * Provides a layer of indirection between the {@link org.apache.maven.api.build.incremental.IncrementalContext}
 * and the underlying file store.
 *
 * <p>This is the most important SPI interface. IDE integrations supply a workspace implementation
 * that is aware of the IDE's virtual file system (e.g., Eclipse's {@code IWorkspace}), while
 * command-line Maven builds use a direct filesystem implementation. The workspace determines:</p>
 * <ul>
 *   <li>How files are read and written (allowing the IDE to intercept file operations)</li>
 *   <li>How change detection works (full scan vs. IDE-provided delta)</li>
 *   <li>When output files should be deleted (notifying the IDE's resource tracker)</li>
 * </ul>
 *
 * <p>The {@link Mode} controls how {@link #walk(java.nio.file.Path)} behaves, which in turn
 * controls how aggressively the build context scans for changes:</p>
 * <ul>
 *   <li><strong>{@link Mode#NORMAL}</strong> — Full scan; the build context compares timestamps
 *       against saved state. Used by command-line Maven.</li>
 *   <li><strong>{@link Mode#ESCALATED}</strong> — All files treated as new (full rebuild).
 *       Used when the user explicitly requests a full rebuild or when the change set may
 *       be incomplete (e.g., after an IDE crash).</li>
 *   <li><strong>{@link Mode#SUPPRESSED}</strong> — No processing; all inputs appear unmodified.
 *       Used for configuration-only builds or when incremental support is disabled.</li>
 * </ul>
 *
 * @since 4.1.0
 * @see IncrementalContextEnvironment#getWorkspace()
 * @see org.apache.maven.api.build.incremental.IncrementalContext
 */
@Experimental
@ThreadSafe
@Consumer
public interface Workspace {

    /**
     * {@return the current workspace mode}
     */
    @Nonnull
    Mode getMode();

    /**
     * Returns an escalated view of this workspace, where all files are treated as new.
     *
     * @return the escalated workspace
     */
    @Nonnull
    Workspace escalate();

    /**
     * {@return {@code true} if the file exists in this workspace}
     *
     * @param file the file path to check
     */
    boolean isPresent(@Nonnull Path file);

    /**
     * {@return {@code true} if the path is a regular file in this workspace}
     *
     * @param file the file path to check
     */
    boolean isRegularFile(@Nonnull Path file);

    /**
     * {@return {@code true} if the path is a directory in this workspace}
     *
     * @param file the file path to check
     */
    boolean isDirectory(@Nonnull Path file);

    /**
     * Deletes the specified file from this workspace.
     *
     * @param file the file to delete
     * @throws org.apache.maven.api.build.incremental.IncrementalContextException if an I/O error occurs
     */
    void deleteFile(@Nonnull Path file);

    /**
     * Notifies the workspace that the given output path has been processed.
     *
     * @param path the output path
     */
    void processOutput(@Nonnull Path path);

    /**
     * Returns an output stream for the specified file. The workspace may optimize this
     * using a caching stream that only overwrites the file when the content changes.
     *
     * @param path the file to write to
     * @return a new output stream
     * @throws org.apache.maven.api.build.incremental.IncrementalContextException if an I/O error occurs
     */
    @Nonnull
    OutputStream newOutputStream(@Nonnull Path path);

    /**
     * Determines the resource status based on its last-modified time and size.
     *
     * @param file         the file to check
     * @param lastModified the previously recorded last-modified time
     * @param size         the previously recorded file size
     * @return the change status
     */
    @Nonnull
    Status getResourceStatus(@Nonnull Path file, @Nonnull FileTime lastModified, long size);

    /**
     * Walks a file tree rooted at the given directory. The files visited and their status
     * depend on the workspace {@link Mode}:
     * <ul>
     * <li><strong>{@code NORMAL}</strong> — all files are visited with status {@link Status#NEW}.
     *     The build context calculates the actual input status.</li>
     * <li><strong>{@code ESCALATED}</strong> — all files are visited with status {@link Status#NEW}.
     *     Used when the user explicitly requests a full rebuild in an IDE.</li>
     * <li><strong>{@code SUPPRESSED}</strong> — used during "configuration" builds where all inputs
     *     are assumed up-to-date and no outputs are expected.</li>
     * </ul>
     *
     * @param basedir the root directory to walk
     * @return a stream of file states
     * @throws org.apache.maven.api.build.incremental.IncrementalContextException if an I/O error occurs
     */
    @Nonnull
    Stream<FileState> walk(@Nonnull Path basedir);

    /**
     * The workspace operating mode.
     *
     * @since 4.1.0
     */
    enum Mode {
        /** Normal mode — the build context determines resource status. */
        NORMAL,
        /** Escalated mode — all files treated as new (full rebuild). */
        ESCALATED,
        /** Suppressed mode — configuration-only build, no outputs expected. */
        SUPPRESSED
    }
}
