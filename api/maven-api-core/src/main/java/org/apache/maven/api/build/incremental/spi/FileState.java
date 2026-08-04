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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.build.incremental.Status;

/**
 * Immutable snapshot of a file's state (path, last-modified time, size) and its
 * change {@link Status} relative to the previous build.
 *
 * <p>Instances are produced by {@link Workspace#walk(java.nio.file.Path)} and consumed
 * by the build context implementation to determine which inputs have changed. The
 * two-argument constructor reads file attributes from the filesystem automatically;
 * the four-argument constructor allows the workspace to supply pre-computed values
 * (e.g., from an IDE's file-watcher cache).</p>
 *
 * @since 4.1.0
 * @see Workspace#walk(java.nio.file.Path)
 * @see Status
 */
@Experimental
@Immutable
public final class FileState {

    private final Path path;
    private final FileTime lastModified;
    private final long size;
    private final Status status;

    /**
     * Creates a file state with explicit attributes.
     *
     * @param path         the file path
     * @param lastModified the last-modified time, or {@code null} for removed files
     * @param size         the file size in bytes
     * @param status       the change status
     */
    public FileState(@Nonnull Path path, @Nullable FileTime lastModified, long size, @Nonnull Status status) {
        this.path = path;
        this.lastModified = lastModified;
        this.size = size;
        this.status = status;
    }

    /**
     * Creates a file state by reading attributes from the file system.
     * For {@link Status#REMOVED} files, the last-modified time is set to {@code null}
     * and the size to {@code 0}.
     *
     * @param path   the file path
     * @param status the change status
     * @throws IncrementalContextException if the file attributes cannot be read
     */
    public FileState(@Nonnull Path path, @Nonnull Status status) {
        this.path = path;
        this.status = status;
        if (status == Status.REMOVED) {
            lastModified = null;
            size = 0;
        } else {
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                this.lastModified = attrs.lastModifiedTime();
                this.size = attrs.size();
            } catch (IOException e) {
                throw new IncrementalContextException(e);
            }
        }
    }

    /**
     * {@return the file path}
     */
    @Nonnull
    public Path getPath() {
        return path;
    }

    /**
     * {@return the last-modified time, or {@code null} for removed files}
     */
    @Nullable
    public FileTime getLastModified() {
        return lastModified;
    }

    /**
     * {@return the file size in bytes}
     */
    public long getSize() {
        return size;
    }

    /**
     * {@return the change status relative to the previous build}
     */
    @Nonnull
    public Status getStatus() {
        return status;
    }
}
