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
 * Pointer to a resolved resource such as a <abbr>JAR</abbr> file or <abbr>WAE</abbr> application.
 * Each {@code Artifact} instance is basically an exact pointer to a file in a Maven repository.
 * {@code Artifact} instances are created when <dfn>resolving</dfn> {@link ArtifactCoordinate} instances.
 * Resolving is the process that selects a {@linkplain #getVersion() particular version}
 * and downloads the artifact in the local repository.  This operation returns a {@link ResolvedArtifact}.
 *
 * @since 4.0.0
 */
@Experimental
@Immutable
public interface Artifact {
    /**
     * {@return a unique identifier for this artifact}.
     * The identifier is composed of groupId, artifactId, extension, classifier, and version.
     *
     * @see ArtifactCoordinate#getId()
     */
    @Nonnull
    default String key() {
        String c = getClassifier();
        return getGroupId()
                + ':'
                + getArtifactId()
                + ':'
                + getExtension()
                + (c.isEmpty() ? "" : ":" + c)
                + ':'
                + getVersion();
    }

    /**
     * {@return the group identifier of the artifact}.
     *
     * @see ArtifactCoordinate#getGroupId()
     */
    @Nonnull
    String getGroupId();

    /**
     * {@return the identifier of the artifact}.
     *
     * @see ArtifactCoordinate#getArtifactId()
     */
    @Nonnull
    String getArtifactId();

    /**
     * {@return the version of the artifact}. Contrarily to {@link ArtifactCoordinate},
     * each {@code Artifact} is associated to a specific version instead of a range of versions.
     * If the {@linkplain #getBaseVersion() base version} contains a meta-version such as {@code SNAPSHOT},
     * those keywords are replaced by, for example, the actual timestamp.
     *
     * @see ArtifactCoordinate#getVersionConstraint()
     */
    @Nonnull
    Version getVersion();

    /**
     * {@return the version or meta-version of the artifact}.
     * A meta-version is a version suffixed with the {@code SNAPSHOT} keyword.
     * Meta-versions are represented in a base version by their symbols (e.g., {@code SNAPSHOT}),
     * while they are replaced by, for example, the actual timestamp in the {@linkplain #getVersion() version}.
     */
    @Nonnull
    Version getBaseVersion();

    /**
     * Returns the classifier of the artifact.
     *
     * @return the classifier or an empty string if none, never {@code null}
     * @see ArtifactCoordinate#getClassifier()
     */
    @Nonnull
    String getClassifier();

    /**
     * Returns the file extension of the artifact.
     * The dot separator is <em>not</em> included in the returned string.
     *
     * @return the file extension or an empty string if none, never {@code null}
     * @see ArtifactCoordinate#getExtension()
     */
    @Nonnull
    String getExtension();

    /**
     * Determines whether this artifact uses a snapshot version.
     *
     * @return {@code true} if the artifact is a snapshot, {@code false} otherwise
     * @see org.apache.maven.api.Session#isVersionSnapshot(String)
     */
    boolean isSnapshot();

    /**
     * {@return coordinate with the same identifiers as this artifact}.
     * This is a shortcut for {@code session.createArtifactCoordinate(artifact)}.
     *
     * @see org.apache.maven.api.Session#createArtifactCoordinate(Artifact)
     */
    @Nonnull
    ArtifactCoordinate toCoordinate();
}
