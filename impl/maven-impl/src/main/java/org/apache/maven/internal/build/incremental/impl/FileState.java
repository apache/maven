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
package org.apache.maven.internal.build.incremental.impl;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.build.incremental.IncrementalContextException;
import org.apache.maven.api.build.incremental.Status;

public class FileState implements Serializable {

    private static final long serialVersionUID = 1;

    @Nonnull
    private final Path path;

    private final FileTime lastModified;
    private final long size;

    public FileState(@Nonnull Path path, FileTime lastModified, long size) {
        this.path = Objects.requireNonNull(path, "path can not be null");
        this.lastModified = lastModified;
        this.size = size;
    }

    @Nonnull
    public Path getPath() {
        return path;
    }

    public FileTime getLastModified() {
        return lastModified;
    }

    public long getSize() {
        return size;
    }

    public Status getStatus() {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attrs.isRegularFile()) {
                return Status.REMOVED;
            }
            if (size == attrs.size() && Objects.equals(lastModified, attrs.lastModifiedTime())) {
                return Status.UNMODIFIED;
            }
            return Status.MODIFIED;
        } catch (java.nio.file.NoSuchFileException e) {
            return Status.REMOVED;
        } catch (IOException e) {
            throw new IncrementalContextException(e);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, lastModified, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FileState)) {
            return false;
        }
        FileState other = (FileState) obj;
        return Objects.equals(path, other.path)
                && Objects.equals(lastModified, other.lastModified)
                && size == other.size;
    }

    @Override
    public String toString() {
        return "FileState[path=" + path + ", lastModified=" + lastModified + ", size=" + size + "]";
    }
}
