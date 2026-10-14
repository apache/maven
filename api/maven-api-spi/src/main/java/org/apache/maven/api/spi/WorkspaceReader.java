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
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.model.Model;

/**
 * SPI for IDE and tool integrators to provide workspace artifact resolution.
 *
 * <p>Implementations are discovered via DI and are automatically bridged into the
 * resolver's workspace reader chain, replacing the need for the internal
 * {@code @Named("ide")} resolver {@code WorkspaceReader} mechanism.
 *
 * <p>Implementations should be annotated with {@link Named} and registered as
 * Maven core extensions (via {@code .mvn/extensions.xml}).
 *
 * <p>Unlike the legacy {@code org.eclipse.aether.repository.WorkspaceReader}, this SPI
 * uses Maven 4 API types only (no maven-resolver-api dependency required).
 *
 * @since 4.1.0
 */
@Experimental
@Consumer
@Named
public interface WorkspaceReader extends SpiService {

    /**
     * Finds the path on disk for the given artifact in the workspace, if present.
     *
     * @param artifact the artifact to look up, never {@code null}
     * @return the path to the artifact file, or empty if not found in workspace
     */
    Optional<Path> findArtifact(Artifact artifact);

    /**
     * Returns the list of available versions for the given artifact in the workspace.
     *
     * @param artifact the artifact to look up (version is ignored), never {@code null}
     * @return list of available versions, may be empty
     */
    List<String> findVersions(Artifact artifact);

    /**
     * Finds the model for the given artifact in the workspace, if present.
     *
     * <p>This method allows workspace readers to supply a synthetic or pre-built model for
     * workspace artifacts that do not have a physical POM file on disk or in a repository.
     *
     * <p>The default implementation returns empty, indicating that the workspace reader
     * does not provide a model for the artifact and that normal model resolution should proceed.
     *
     * @param artifact the artifact whose model to look up, never {@code null}
     * @return the model, or empty if not provided by the workspace
     * @since 4.1.0
     */
    default Optional<Model> findModel(Artifact artifact) {
        return Optional.empty();
    }
}
