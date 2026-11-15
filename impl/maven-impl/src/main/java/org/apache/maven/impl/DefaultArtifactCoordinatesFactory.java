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
package org.apache.maven.impl;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.ArtifactCoordinatesFactory;
import org.eclipse.aether.artifact.ArtifactType;

import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class DefaultArtifactCoordinatesFactory implements ArtifactCoordinatesFactory {
    @Override
    public ArtifactCoordinates create(@Nonnull Session session, @Nonnull String coordinatesString) {
        return new DefaultArtifactCoordinates(
                InternalSession.from(requireNonNull(session, "session")),
                new org.eclipse.aether.artifact.DefaultArtifact(
                        requireNonNull(coordinatesString, "coordinatesString")));
    }

    @Override
    public ArtifactCoordinates create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String typeId) {
        InternalSession internalSession = InternalSession.from(requireNonNull(session, "session"));
        ArtifactType type = typeId != null
                ? internalSession.getSession().getArtifactTypeRegistry().get(typeId)
                : null;
        String effectiveClassifier =
                classifier != null && !classifier.isEmpty() ? classifier : type != null ? type.getClassifier() : "";
        String effectiveExtension =
                extension != null && !extension.isEmpty() ? extension : type != null ? type.getExtension() : "";
        return new DefaultArtifactCoordinates(
                internalSession,
                new org.eclipse.aether.artifact.DefaultArtifact(
                        groupId, artifactId, effectiveClassifier, effectiveExtension, version, type));
    }
}
