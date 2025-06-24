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
package org.apache.maven.building;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Wraps an ordinary {@link File} as a source.
 *
 * @deprecated since 4.0.0, use {@link org.apache.maven.api.services} instead
 */
@Deprecated(since = "4.0.0")
public class FileSource implements Source {
    private final Path path;

    private final int hashCode;

    /**
     * Creates a new source backed by the specified file.
     *
     * @param file The file, must not be {@code null}.
     * @deprecated Use {@link #FileSource(Path)} instead.
     */
    @Deprecated
    public FileSource(File file) {
        this(Objects.requireNonNull(file, "file cannot be null").toPath());
    }

    /**
     * Creates a new source backed by the specified file.
     *
     * @param path The file, must not be {@code null}.
     * @since 4.0.0
     */
    public FileSource(Path path) {
        this.path = Objects.requireNonNull(path, "path cannot be null").toAbsolutePath();
        this.hashCode = Objects.hash(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public String getLocation() {
        return path.toString();
    }

    /**
     * Gets the file of this source.
     *
     * @return The underlying file, never {@code null}.
     * @deprecated Use {@link #getPath()} instead.
     */
    @Deprecated
    public File getFile() {
        return path.toFile();
    }

    /**
     * Gets the file of this source.
     *
     * @return The underlying file, never {@code null}.
     * @since 4.0.0
     */
    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return getLocation();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!FileSource.class.equals(obj.getClass())) {
            return false;
        }

        FileSource other = (FileSource) obj;
        return this.path.equals(other.path);
    }
}
