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
package org.apache.maven.plugin.internal;

import java.util.List;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Service responsible for validating plugin dependencies.
 *
 * @since 3.9.2
 */
interface MavenPluginDependenciesValidator {
    /**
     * Checks mojo dependency issues by validating artifact descriptor (POM), hence, direct dependencies.
     */
    void validate(
            RepositorySystemSession session,
            Artifact pluginArtifact,
            ArtifactDescriptorResult artifactDescriptorResult);

    /**
     * Checks mojo dependency issues by validating transitive dependencies of plugin. The dependencies passed in here
     * does NOT contain plugin, and it's direct dependencies.
     *
     * @since 3.9.3
     */
    default void validate(RepositorySystemSession session, Artifact pluginArtifact, List<Dependency> dependencies) {
        // nothing
    }
}
