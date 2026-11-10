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
package org.apache.maven.api.services;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.ProducedArtifact;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service used to create {@link Artifact} objects.
 *
 * @since 4.0.0
 */
@Experimental
public interface ArtifactFactory extends Service {

    /**
     * Creates an artifact from individual components.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param extension the file extension
     * @return an {@code Artifact}, never {@code null}
     */
    @Nonnull
    default Artifact create(
            @Nonnull Session session, String groupId, String artifactId, String version, String extension) {
        return create(session, groupId, artifactId, version, null, extension, null);
    }

    /**
     * Creates an artifact from individual components including classifier and type.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param classifier the classifier, may be {@code null}
     * @param extension the file extension, may be {@code null}
     * @param type the artifact type, may be {@code null}
     * @return an {@code Artifact}, never {@code null}
     */
    @Nonnull
    Artifact create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type);

    /**
     * Creates a produced artifact from individual components.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param extension the file extension
     * @return a {@code ProducedArtifact}, never {@code null}
     */
    @Nonnull
    default ProducedArtifact createProduced(
            @Nonnull Session session, String groupId, String artifactId, String version, String extension) {
        return createProduced(session, groupId, artifactId, version, null, extension, null);
    }

    /**
     * Creates a produced artifact from individual components including classifier and type.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version
     * @param classifier the classifier, may be {@code null}
     * @param extension the file extension, may be {@code null}
     * @param type the artifact type, may be {@code null}
     * @return a {@code ProducedArtifact}, never {@code null}
     */
    @Nonnull
    ProducedArtifact createProduced(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type);
}
