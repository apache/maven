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

import java.util.Collection;
import java.util.List;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Exclusion;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;

import static java.util.Objects.requireNonNull;

/**
 * Service for creating {@link DependencyCoordinates} instances from individual components,
 * model dependencies, plugins, or artifact coordinates.
 *
 * @since 4.0.0
 */
@Experimental
public interface DependencyCoordinatesFactory extends Service {

    /**
     * Creates dependency coordinates from individual components.
     *
     * @param session the session
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version constraint
     * @param classifier the classifier, or null
     * @param extension the extension, or null
     * @param type the artifact type, or null
     * @param scope the dependency scope, or null
     * @param optional whether the dependency is optional
     * @param exclusions the dependency exclusions
     * @return the dependency coordinates
     * @since 4.2.0
     */
    @Nonnull
    @SuppressWarnings("checkstyle:ParameterNumber")
    DependencyCoordinates create(
            @Nonnull Session session,
            String groupId,
            String artifactId,
            String version,
            String classifier,
            String extension,
            String type,
            String scope,
            boolean optional,
            @Nonnull Collection<Exclusion> exclusions);

    /**
     * Creates dependency coordinates from a standard artifact coordinates string.
     *
     * @param session the session
     * @param coordinatesString the artifact coordinates string
     * @param scope the dependency scope, or null
     * @param optional whether the dependency is optional
     * @param exclusions the dependency exclusions
     * @return the dependency coordinates
     * @since 4.2.0
     */
    @Nonnull
    DependencyCoordinates create(
            @Nonnull Session session,
            @Nonnull String coordinatesString,
            String scope,
            boolean optional,
            @Nonnull Collection<Exclusion> exclusions);

    /**
     * Creates dependency coordinates from artifact coordinates.
     *
     * @param session the session
     * @param coordinates the artifact coordinates
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(@Nonnull Session session, @Nonnull ArtifactCoordinates coordinates) {
        requireNonNull(coordinates, "coordinates");
        return create(
                session,
                coordinates.groupId(),
                coordinates.artifactId(),
                coordinates.versionConstraint().toString(),
                coordinates.classifier(),
                coordinates.extension(),
                null,
                null,
                false,
                List.of());
    }

    /**
     * Creates dependency coordinates from an existing dependency.
     * The dependency's scope, optional flag and exclusions are preserved.
     *
     * @param session the session
     * @param dependency the dependency
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(
            @Nonnull Session session, @Nonnull org.apache.maven.api.Dependency dependency) {
        requireNonNull(dependency, "dependency");
        return create(
                session,
                dependency.groupId(),
                dependency.artifactId(),
                dependency.version().toString(),
                dependency.classifier(),
                dependency.extension(),
                dependency.type().id(),
                dependency.scope().id(),
                dependency.isOptional(),
                dependency.exclusions());
    }

    /**
     * Creates dependency coordinates from a model dependency.
     *
     * @param session the session
     * @param dependency the model dependency
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(@Nonnull Session session, Dependency dependency) {
        return create(
                session,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getClassifier(),
                null,
                dependency.getType(),
                null,
                false,
                List.of());
    }

    /**
     * Creates dependency coordinates from a plugin model.
     *
     * @param session the session
     * @param plugin the plugin
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(@Nonnull Session session, Plugin plugin) {
        return create(
                session,
                plugin.getGroupId(),
                plugin.getArtifactId(),
                plugin.getVersion(),
                null,
                null,
                "maven-plugin",
                null,
                false,
                List.of());
    }

    /**
     * Creates dependency coordinates from a report plugin model.
     *
     * @param session the session
     * @param reportPlugin the report plugin
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(@Nonnull Session session, ReportPlugin reportPlugin) {
        return create(
                session,
                reportPlugin.getGroupId(),
                reportPlugin.getArtifactId(),
                reportPlugin.getVersion(),
                null,
                null,
                "maven-plugin",
                null,
                false,
                List.of());
    }
}
