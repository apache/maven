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

import java.util.Collection;

import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.services.DependencyCoordinatesFactory;
import org.eclipse.aether.artifact.ArtifactType;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.impl.ImplUtils.map;

@Named
@Singleton
public class DefaultDependencyCoordinatesFactory implements DependencyCoordinatesFactory {
    @Nonnull
    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    public DependencyCoordinates create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String typeId,
            String scope,
            boolean optional,
            @Nonnull Collection<Exclusion> exclusions) {
        InternalSession internalSession = InternalSession.from(requireNonNull(session, "session"));
        ArtifactType type = typeId != null
                ? internalSession.getSession().getArtifactTypeRegistry().get(typeId)
                : null;
        return new DefaultDependencyCoordinates(
                internalSession,
                new org.eclipse.aether.graph.Dependency(
                        new org.eclipse.aether.artifact.DefaultArtifact(
                                groupId, artifactId, classifier, extension, version, type),
                        scope,
                        optional,
                        map(requireNonNull(exclusions, "exclusions"), this::toExclusion)));
    }

    @Nonnull
    @Override
    public DependencyCoordinates create(
            @Nonnull Session session,
            @Nonnull String coordinatesString,
            String scope,
            boolean optional,
            @Nonnull Collection<Exclusion> exclusions) {
        InternalSession internalSession = InternalSession.from(requireNonNull(session, "session"));
        return new DefaultDependencyCoordinates(
                internalSession,
                new org.eclipse.aether.graph.Dependency(
                        new org.eclipse.aether.artifact.DefaultArtifact(
                                requireNonNull(coordinatesString, "coordinatesString")),
                        scope,
                        optional,
                        map(requireNonNull(exclusions, "exclusions"), this::toExclusion)));
    }

    private org.eclipse.aether.graph.Exclusion toExclusion(Exclusion exclusion) {
        return new org.eclipse.aether.graph.Exclusion(exclusion.getGroupId(), exclusion.getArtifactId(), "*", "*");
    }
}
