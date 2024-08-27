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
import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;

/**
 * Service used to create {@link ArtifactCoordinates} objects.
 *
 * @since 4.0.0
 */
@Experimental
public interface ArtifactCoordinatesFactory extends Service {

    /**
     * Creates artifact coordinates.
     *
     * @param request the request holding coordinates creation parameters
     * @return an {@code ArtifactCoordinates}, never {@code null}
     * @throws IllegalArgumentException if {@code request} is null or {@code request.session} is null or invalid
     */
    @Nonnull
    ArtifactCoordinates create(@Nonnull ArtifactCoordinatesFactoryRequest request);

    /**
     * Creates coordinates out of string that is formatted like:
     * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}
     *
     * @param session the session.
     * @param coordinatesString the string having "standard" coordinates.
     * @return an {@code ArtifactCoordinates}, never {@code null}
     * @throws IllegalArgumentException if {@code session} is null or invalid
     */
    @Nonnull
    default ArtifactCoordinates create(@Nonnull Session session, @Nonnull String coordinatesString) {
        return create(ArtifactCoordinatesFactoryRequest.build(session, coordinatesString));
    }

    @Nonnull
    default ArtifactCoordinates create(
            @Nonnull Session session, String groupId, String artifactId, String version, String extension) {
        return create(ArtifactCoordinatesFactoryRequest.build(session, groupId, artifactId, version, extension));
    }

    @Nonnull
    default ArtifactCoordinates create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type) {
        return create(ArtifactCoordinatesFactoryRequest.build(
                session, groupId, artifactId, version, classifier, extension, type));
    }

    @Nonnull
    default ArtifactCoordinates create(@Nonnull Session session, Artifact artifact) {
        return create(ArtifactCoordinatesFactoryRequest.build(
                session,
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion().asString(),
                artifact.getClassifier(),
                artifact.getExtension(),
                null));
    }
}
