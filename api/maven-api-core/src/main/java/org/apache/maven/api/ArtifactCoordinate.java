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
package org.apache.maven.api;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Identification of an ensemble of {@code Artifact}s in a range of versions.
 * Each {@code ArtifactCoordinate} instance is basically a pointer to a file in the Maven repository,
 * except that the version may not be defined precisely.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface ArtifactCoordinate {
    /**
     * {@return the group identifier of the artifact}.
     */
    @Nonnull
    String getGroupId();

    /**
     * {@return the identifier of the artifact}.
     */
    @Nonnull
    String getArtifactId();

    /**
     * Returns the classifier of the artifact.
     *
     * @return the classifier or an empty string if none, never {@code null}
     */
    @Nonnull
    String getClassifier();

    /**
     * {@return the specific version, range of versions or meta-version of the artifact}.
     * A meta-version is a version suffixed with the {@code SNAPSHOT} keyword.
     */
    @Nonnull
    VersionConstraint getVersionConstraint();

    /**
     * Returns the file extension of the artifact.
     * The dot separator is <em>not</em> included in the returned string.
     *
     * @return the file extension or an empty string if none, never {@code null}
     */
    @Nonnull
    String getExtension();

    /**
     * {@return a unique string representation identifying this artifact}.
     * The default implementation returns a colon-separated list of group
     * identifier, artifact identifier, extension, classifier and version.
     *
     * @see Artifact#key()
     */
    @Nonnull
    default String getId() {
        String c = getClassifier();
        return getGroupId()
                + ':'
                + getArtifactId()
                + ':'
                + getExtension()
                + ':'
                + c
                + (c.isEmpty() ? "" : ":")
                + getVersionConstraint();
    }
}
