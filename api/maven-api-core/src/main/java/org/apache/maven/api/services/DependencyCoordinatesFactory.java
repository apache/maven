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

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Dependency;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.ReportPlugin;

/**
 *
 * @since 4.0.0
 */
@Experimental
public interface DependencyCoordinatesFactory extends Service {

    /**
     * Creates a new {@link DependencyCoordinates} object from the request.
     *
     * @param request the request containing the various data
     * @return a new {@link DependencyCoordinates} object
     *
     * @throws IllegalArgumentException if {@code request} is null or
     *         if {@code request.getSession()} is null or invalid
     */
    @Nonnull
    DependencyCoordinates create(@Nonnull DependencyCoordinatesFactoryRequest request);

    /**
     * Creates dependency coordinates from artifact coordinates.
     *
     * @param session the session
     * @param coordinates the artifact coordinates
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(@Nonnull Session session, @Nonnull ArtifactCoordinates coordinates) {
        return create(DependencyCoordinatesFactoryRequest.build(session, coordinates));
    }

    /**
     * Creates dependency coordinates from an existing dependency.
     *
     * @param session the session
     * @param dependency the dependency
     * @return a new {@link DependencyCoordinates}, never {@code null}
     */
    @Nonnull
    default DependencyCoordinates create(
            @Nonnull Session session, @Nonnull org.apache.maven.api.Dependency dependency) {
        return create(DependencyCoordinatesFactoryRequest.build(session, dependency));
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
        return create(DependencyCoordinatesFactoryRequest.build(
                session,
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getClassifier(),
                null,
                dependency.getType()));
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
        // TODO: hard coded string
        return create(DependencyCoordinatesFactoryRequest.build(
                session, plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion(), null, null, "maven-plugin"));
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
        // TODO: hard coded string
        return create(DependencyCoordinatesFactoryRequest.build(
                session,
                reportPlugin.getGroupId(),
                reportPlugin.getArtifactId(),
                reportPlugin.getVersion(),
                null,
                null,
                "maven-plugin"));
    }
}
