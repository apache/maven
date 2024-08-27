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

import java.nio.file.Path;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;

/**
 * An {@link Artifact} that is being produced by a {@link Project} during the build.
 *
 * <p>Produced artifacts includes:</p><ul>
 *     <li>{@linkplain Project#getPomArtifact() the project POM artifact}</li>
 *     <li>{@linkplain Project#getMainArtifact() the main artifact}</li>
 *     <li>{@linkplain org.apache.maven.api.services.ProjectManager#attachArtifact(Session, Project, Path) artifacts to be attached to a project}</li>
 * </ul>
 *
 * <p>For the main artifact and attached artifacts, the
 * {@link org.apache.maven.api.services.ArtifactManager ArtifactManager} service must be used
 * to point the artifact to a {@link Path} during the packaging phase.</p>
 *
 * @since 4.0.0
 * @see Project#getMainArtifact()
 * @see Project#getPomArtifact()
 * @see org.apache.maven.api.services.ProjectManager#attachArtifact(Session, Project, Path)
 * @see org.apache.maven.api.services.ArtifactManager#setPath(ProducedArtifact, Path)
 */
@Experimental
@Immutable
public interface ProducedArtifact extends Artifact {}
