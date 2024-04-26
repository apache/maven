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
package org.apache.maven.repository.internal;

import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;

/**
 * Maven relocation source.
 * Note: implementations of this component should avoid the "default" name (has special meaning in Eclipse Sisu) and
 * explicitly order implementations using Sisu priorities.
 *
 * @since 4.0.0
 */
public interface MavenArtifactRelocationSource {
    /**
     * Returns {@link Artifact} instance where to relocate to, or {@code null}.
     *
     * @param session The session, never {@code null}.
     * @param result The artifact descriptor result, never {@code null}.
     * @param model The artifact model, never {@code null}.
     * @return The {@link Artifact} to relocate to, or {@code null} if no relocation wanted.
     */
    Artifact relocatedTarget(RepositorySystemSession session, ArtifactDescriptorResult result, Model model)
            throws ArtifactDescriptorException;
}
