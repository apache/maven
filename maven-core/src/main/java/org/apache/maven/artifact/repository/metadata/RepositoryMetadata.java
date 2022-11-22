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
package org.apache.maven.artifact.repository.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;

/**
 * Describes repository directory metadata.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * TODO not happy about the store method - they use "this"
 */
public interface RepositoryMetadata extends org.apache.maven.artifact.metadata.ArtifactMetadata {

    int RELEASE = 1;

    int SNAPSHOT = 2;

    int RELEASE_OR_SNAPSHOT = RELEASE | SNAPSHOT;

    /**
     * Get the repository the metadata was located in.
     *
     * @return the repository
     */
    ArtifactRepository getRepository();

    /**
     * Set the repository the metadata was located in.
     *
     * @param remoteRepository the repository
     */
    void setRepository(ArtifactRepository remoteRepository);

    /**
     * Get the repository metadata associated with this marker.
     *
     * @return the metadata, or <code>null</code> if none loaded
     */
    Metadata getMetadata();

    /**
     * Set the metadata contents.
     *
     * @param metadata the metadata
     */
    void setMetadata(Metadata metadata);

    /**
     * Whether this represents a snapshot.
     *
     * @return if it is a snapshot
     */
    boolean isSnapshot();

    /**
     * Gets the artifact quality this metadata refers to. One of {@link #RELEASE}, {@link #SNAPSHOT} or
     * {@link #RELEASE_OR_SNAPSHOT}.
     *
     * @return The artifact quality this metadata refers to.
     */
    int getNature();

    /**
     * Gets the policy that applies to this metadata regarding the specified repository.
     *
     * @param repository The repository for which to determine the policy, must not be {@code null}.
     * @return The policy, never {@code null}.
     */
    ArtifactRepositoryPolicy getPolicy(ArtifactRepository repository);
}
