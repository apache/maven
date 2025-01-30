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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.cache.CacheMetadata;
import org.apache.maven.api.cache.CacheRetention;

import static java.util.Objects.requireNonNull;

/**
 * Factory methods for creating different types of sources.
 * <p>
 * This class provides specialized source implementations for different use cases:
 * <ul>
 *   <li>Path sources - simple access to file content</li>
 *   <li>Build sources - POM files being actively built by Maven</li>
 *   <li>Resolved sources - POMs resolved from repositories</li>
 * </ul>
 *
 * @since 4.0.0
 */
@Experimental
public final class Sources {

    private Sources() {}

    /**
     * Creates a new source for the specified path.
     *
     * @param path the path to the file
     * @return a new Source instance
     * @throws NullPointerException if path is null
     */
    @Nonnull
    public static Source fromPath(@Nonnull Path path) {
        return new PathSource(requireNonNull(path, "path"));
    }

    /**
     * Creates a new build source for the specified path.
     * Build sources are used for POM files of projects being built by Maven
     * in the filesystem and support resolving related POMs.
     *
     * @param path the path to the POM file or project directory
     * @return a new ModelSource instance configured as a build source
     * @throws NullPointerException if path is null
     */
    @Nonnull
    public static ModelSource buildSource(@Nonnull Path path) {
        return new BuildPathSource(requireNonNull(path, "path"));
    }

    /**
     * Creates a new resolved source for the specified path and location.
     * Resolved sources are used for artifacts that have been resolved by Maven
     * from repositories (using groupId:artifactId:version coordinates) and
     * downloaded to the local repository. These sources do not support resolving
     * other sources.
     *
     * @param path the path to the POM file or project directory
     * @param location optional logical location of the source, used for reporting purposes
     * @return a new ModelSource instance configured as a resolved source
     * @throws NullPointerException if path is null
     */
    @Nonnull
    public static ModelSource resolvedSource(@Nonnull Path path, @Nullable String location) {
        return new ResolvedPathSource(requireNonNull(path, "path"), location);
    }

    /**
     * Basic implementation of {@link Source} that uses a {@link Path} as the underlying source.
     */
    static class PathSource implements Source {
        @Nonnull
        protected final Path path;

        @Nonnull
        protected final String location;

        /**
         * Constructs a new PathSource with the specified path.
         *
         * @param path the filesystem path to the source content
         * @throws NullPointerException if path is null
         */
        PathSource(Path path) {
            this(path, null);
        }

        /**
         * Constructs a new PathSource with the specified path and location.
         *
         * @param path the filesystem path to the source content
         * @param location the logical location of the source, used for reporting purposes.
         *                 If null, the path string representation is used
         */
        protected PathSource(Path path, String location) {
            this.path = requireNonNull(path, "path").normalize();
            this.location = location != null ? location : this.path.toString();
        }

        @Override
        @Nullable
        public Path getPath() {
            return path;
        }

        @Override
        @Nonnull
        public InputStream openStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        @Nonnull
        public String getLocation() {
            return location;
        }

        @Override
        @Nullable
        public Source resolve(@Nonnull String relative) {
            return new PathSource(path.resolve(relative));
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof PathSource that && Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[location='" + location + "', path=" + path + ']';
        }
    }

    /**
     * Implementation of {@link ModelSource} for POM files that have been resolved
     * from repositories. Does not support resolving related sources.
     */
    static class ResolvedPathSource extends PathSource implements ModelSource {
        ResolvedPathSource(Path path, String location) {
            super(path, location);
        }

        @Override
        public Path getPath() {
            return null;
        }

        @Override
        public Source resolve(String relative) {
            return null;
        }

        @Override
        @Nullable
        public ModelSource resolve(@Nonnull ModelLocator modelLocator, @Nonnull String relative) {
            return null;
        }
    }

    /**
     * Implementation of {@link ModelSource} that extends {@link PathSource} with model-specific
     * functionality. This implementation uses request-scoped caching ({@link CacheRetention#REQUEST_SCOPED})
     * since it represents a POM file that is actively being built and may change during the build process.
     * <p>
     * The request-scoped retention policy ensures that:
     * <ul>
     *   <li>Changes to the POM file during the build are detected</li>
     *   <li>Cache entries don't persist beyond the current build request</li>
     *   <li>Memory is freed once the build request completes</li>
     * </ul>
     */
    static class BuildPathSource extends PathSource implements ModelSource, CacheMetadata {

        /**
         * Constructs a new ModelPathSource.
         *
         * @param path the filesystem path to the source content
         */
        BuildPathSource(Path path) {
            super(path, null);
        }

        @Override
        public Path getPath() {
            return path;
        }

        @Override
        public Source resolve(@Nonnull String relative) {
            return new BuildPathSource(path.resolve(relative));
        }

        @Override
        @Nullable
        public ModelSource resolve(@Nonnull ModelLocator locator, @Nonnull String relative) {
            String norm = relative.replace('\\', File.separatorChar).replace('/', File.separatorChar);
            Path path = getPath().getParent().resolve(norm);
            Path relatedPom = locator.locateExistingPom(path);
            if (relatedPom != null) {
                return new BuildPathSource(relatedPom);
            }
            return null;
        }

        @Override
        public CacheRetention getCacheRetention() {
            return CacheRetention.REQUEST_SCOPED;
        }
    }
}
