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
package org.apache.maven.api.spi;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * WorkspaceReader SPI contract.
 *
 * @since 4.0.0
 */
@Experimental
public interface WorkspaceReader {

    /**
     * Finds the artifact in the workspace.
     *
     * @param artifact the artifact to find, must not be {@code null}
     * @return the path to the artifact, or {@code null} if not found
     */
    Path findArtifact(@Nonnull Artifact artifact);

    /**
     * Finds the available versions for the artifact in the workspace.
     *
     * @param artifact the artifact to find versions for, must not be {@code null}
     * @return the list of available versions, never {@code null}
     */
    @Nonnull
    List<String> findVersions(@Nonnull Artifact artifact);

    /**
     * Finds the model for the given artifact, if available.
     *
     * @param artifact the artifact to find the model for, must not be {@code null}
     * @return an optional path to the model, or empty if not found
     */
    default Optional<Path> findModel(@Nonnull Artifact artifact) {
        return Optional.empty();
    }
}
