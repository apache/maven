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
     * Whether this workspace reader should participate in plugin resolution.
     *
     * <p>IDE workspace readers should return {@code false} here: plugin realms are cached
     * by {@code DefaultPluginRealmCache} (which is {@code @Singleton}) and cannot be purged
     * within a session, so resolving plugins from the workspace can lead to stale classloaders
     * when workspace sources change.
     *
     * <p>Defaults to {@code true} (participates in plugin resolution).
     *
     * @return {@code true} if this reader should be consulted during plugin resolution
     */
    default boolean isApplicableForPluginResolution() {
        return true;
    }
}
