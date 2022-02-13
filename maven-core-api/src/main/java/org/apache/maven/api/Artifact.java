package org.apache.maven.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import java.nio.file.Path;
import java.util.Optional;

@Immutable
public interface Artifact
{

    /**
     * The groupId of the artifact.
     *
     * @return The groupId.
     */
    @Nonnull
    String getGroupId();

    /**
     * The artifactId of the artifact.
     *
     * @return The artifactId.
     */
    @Nonnull
    String getArtifactId();

    /**
     * The version of the artifact.
     *
     * @return The version.
     */
    @Nonnull
    String getVersion();

    /**
     * The file-extension of the artifact.
     *
     * @return The extension.
     */
    @Nonnull
    String getExtension();

    /**
     * The classifier of the artifact.
     *
     * @return The classifier.
     */
    @Nonnull
    String getClassifier();

    /**
     * Gets the base version of this artifact, for example "1.0-SNAPSHOT". In contrast to the {@link #getVersion()}, the
     * base version will always refer to the unresolved meta version.
     *
     * @return The base version, never {@code null}.
     */
    @Nonnull
    String getBaseVersion();

    /**
     * Determines whether this artifact uses a snapshot version.
     *
     * @return {@code true} if the artifact is a snapshot, {@code false} otherwise.
     */
    boolean isSnapshot();

    /**
     * Gets the file of this artifact. Note that only resolved artifacts have a file associated with them. In general,
     * callers must not assume any relationship between an artifact's filename and its coordinates.
     *
     * @return The file or {@link Optional#empty()} if the artifact isn't resolved.
     */
    @Nonnull
    Optional<Path> getPath();

}
