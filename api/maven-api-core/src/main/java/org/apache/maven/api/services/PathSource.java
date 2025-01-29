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

import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ModelSource} that uses a {@link Path} as the underlying source
 * for Maven model content. This class provides path-based access to model files and
 * supports resolution of related resources.
 *
 * <p>The class implements two types of sources as defined in {@link ModelSource}:</p>
 * <ul>
 *   <li><b>Build sources</b> (buildSource = true): Created via {@link ModelSource#buildSource},
 *       these sources point to POM files of projects being built by Maven in the filesystem.
 *       They have full resolution capabilities for locating related POMs.</li>
 *   <li><b>Resolved sources</b> (buildSource = false): Created via {@link ModelSource#resolvedSource},
 *       these sources point to artifacts that have been resolved by Maven from repositories
 *       (using groupId:artifactId:version coordinates) and downloaded to the local repository.
 *       They do not support resolving other sources using the {@link ModelLocator}.</li>
 * </ul>
 *
 * <p>The source maintains both a filesystem path and a logical location, where the location
 * can be used for reporting purposes and may differ from the physical path.</p>
 *
 * @see ModelSource
 * @see ModelSource#buildSource(Path)
 * @see ModelSource#resolvedSource(Path, String)
 */
class PathSource implements ModelSource {

    private final boolean buildSource;
    private final Path path;
    private final String location;

    /**
     * Constructs a new PathSource with the specified build source flag and path.
     * This constructor is primarily used by {@link ModelSource#buildSource(Path)}.
     *
     * @param buildSource flag indicating whether this source represents a build source
     * @param path the filesystem path to the source content
     * @throws NullPointerException if path is null
     * @see ModelSource#buildSource(Path)
     */
    PathSource(boolean buildSource, Path path) {
        this(buildSource, path, null);
    }

    /**
     * Constructs a new PathSource with the specified build source flag, path, and location.
     *
     * @param buildSource flag indicating whether this source represents a build source
     * @param path the filesystem path to the source content
     * @param location the logical location of the source, used for reporting purposes.
     *                 If null, the path string representation is used
     */
    PathSource(boolean buildSource, Path path, String location) {
        this.buildSource = buildSource;
        this.path = requireNonNull(path, "path").normalize();
        this.location = location != null ? location : this.path.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @return the path if this is a build source, null otherwise
     */
    @Override
    public Path getPath() {
        return buildSource ? path : null;
    }

    /**
     * {@inheritDoc}
     *
     * @return a new input stream for reading the source content
     * @throws IOException if an I/O error occurs while opening the stream
     */
    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(path);
    }

    /**
     * {@inheritDoc}
     *
     * @return the location string for this source
     */
    @Override
    public String getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     *
     * @param relative the relative path to resolve
     * @return a new Source resolved against the relative path if this is a build source,
     *         null otherwise
     */
    @Override
    public Source resolve(String relative) {
        return buildSource ? new PathSource(true, path.resolve(relative)) : null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Resolves a model source relative to this source using the provided locator.
     * This operation normalizes path separators and attempts to locate an existing POM file.
     * </p>
     *
     * @param locator the ModelLocator to use for resolving the POM file
     * @param relative the relative path to resolve
     * @return a new ModelSource for the resolved POM if found and this is a build source,
     *         null otherwise
     */
    @Override
    @Nullable
    public ModelSource resolve(@Nonnull ModelLocator locator, @Nonnull String relative) {
        if (buildSource) {
            String norm = relative.replace('\\', File.separatorChar).replace('/', File.separatorChar);
            Path path = getPath().getParent().resolve(norm);
            Path relatedPom = locator.locateExistingPom(path);
            if (relatedPom != null) {
                return new PathSource(true, relatedPom);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Two PathSource instances are considered equal if they have the same build source flag
     * and path values.
     * </p>
     *
     * @param o the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return o == this ||
                o instanceof PathSource that
                        && Objects.equals(buildSource, that.buildSource)
                        && Objects.equals(path, that.path);
    }

    /**
     * {@inheritDoc}
     *
     * @return hash code based on the build source flag and path
     */
    @Override
    public int hashCode() {
        return Objects.hash(buildSource, path);
    }

    /**
     * {@inheritDoc}
     *
     * @return a string representation of this PathSource instance
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[buildSource=" + buildSource + ", location='" + location + "', " + "path="
                + path + ']';
    }
}