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

import java.util.List;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.DependencyCoordinates;
import org.apache.maven.api.Node;
import org.apache.maven.api.PathScope;
import org.apache.maven.api.Project;
import org.apache.maven.api.Service;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;

/**
 * Collects, flattens and resolves dependencies.
 */
@Experimental
public interface DependencyResolver extends Service {

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph for the given path scope.
     * Note that this operation is only concerned about determining the coordinates of the transitive dependencies and
     * does not actually resolve the artifact files.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param root the Maven Dependency, must not be {@code null}
     * @param scope the {link PathScope} to collect dependencies, must not be {@code null}
     * @return the collection result, never {@code null}
     * @throws DependencyResolverException if the dependency tree could not be built
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyResolverRequest)
     */
    @Nonnull
    default DependencyResolverResult collect(
            @Nonnull Session session, @Nonnull DependencyCoordinates root, @Nonnull PathScope scope) {
        return collect(
                DependencyResolverRequest.build(session, DependencyResolverRequest.RequestType.COLLECT, root, scope));
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph for the given path scope.
     * Note that this operation is only concerned about determining the coordinates of the transitive dependencies and
     * does not actually resolve the artifact files.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param project the {@link Project}, must not be {@code null}
     * @param scope the {link PathScope} to collect dependencies, must not be {@code null}
     * @return the collection result, never {@code null}
     * @throws DependencyResolverException if the dependency tree could not be built
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyResolverRequest)
     */
    @Nonnull
    default DependencyResolverResult collect(
            @Nonnull Session session, @Nonnull Project project, @Nonnull PathScope scope) {
        return collect(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.COLLECT, project, scope));
    }

    /**
     * Collects the transitive dependencies of some artifacts and builds a dependency graph for the given path scope.
     * Note that this operation is only concerned about determining the coordinates of the transitive dependencies and
     * does not actually resolve the artifact files.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param artifact the {@link Artifact}, must not be {@code null}
     * @param scope the {link PathScope} to collect dependencies, must not be {@code null}
     * @return the collection result, never {@code null}
     * @throws DependencyResolverException if the dependency tree could not be built
     * @throws IllegalArgumentException if an argument is null or invalid
     * @see #collect(DependencyResolverRequest)
     */
    @Nonnull
    default DependencyResolverResult collect(
            @Nonnull Session session, @Nonnull Artifact artifact, @Nonnull PathScope scope) {
        return collect(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.COLLECT, artifact, scope));
    }

    /**
     * Collects the transitive dependencies and builds a dependency graph.
     * Note that this operation is only concerned about determining the coordinates of the
     * transitive dependencies and does not actually resolve the artifact files.
     *
     * @param request the dependency collection request, must not be {@code null}
     * @return the collection result, never {@code null}
     * @throws DependencyResolverException if the dependency tree could not be built
     * @throws IllegalArgumentException if an argument is null or invalid
     *
     * @see DependencyResolver#collect(Session, Project, PathScope)
     * @see DependencyResolver#collect(Session, DependencyCoordinates, PathScope)
     * @see DependencyResolver#collect(Session, Artifact, PathScope)
     */
    @Nonnull
    default DependencyResolverResult collect(@Nonnull DependencyResolverRequest request) {
        if (request.getRequestType() != DependencyResolverRequest.RequestType.COLLECT) {
            throw new IllegalArgumentException("requestType should be COLLECT when calling collect()");
        }
        return resolve(request);
    }

    /**
     * Flattens a list of nodes.
     * Note that the {@code PathScope} argument should usually be null as the dependency tree has been
     * filtered during collection for the appropriate scope.
     *
     * @param session the {@link Session}, must not be {@code null}
     * @param node the {@link Node} to flatten, must not be {@code null}
     * @param scope an optional {@link PathScope} to filter out dependencies
     * @return the flattened list of node
     * @throws DependencyResolverException
     */
    List<Node> flatten(@Nonnull Session session, @Nonnull Node node, @Nullable PathScope scope)
            throws DependencyResolverException;

    @Nonnull
    default DependencyResolverResult flatten(
            @Nonnull Session session, @Nonnull Project project, @Nonnull PathScope scope) {
        return flatten(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.FLATTEN, project, scope));
    }

    @Nonnull
    default DependencyResolverResult flatten(@Nonnull DependencyResolverRequest request) {
        if (request.getRequestType() != DependencyResolverRequest.RequestType.FLATTEN) {
            throw new IllegalArgumentException("requestType should be FLATTEN when calling flatten()");
        }
        return resolve(request);
    }

    @Nonnull
    default DependencyResolverResult resolve(@Nonnull Session session, @Nonnull Project project) {
        return resolve(
                DependencyResolverRequest.build(session, DependencyResolverRequest.RequestType.RESOLVE, project));
    }

    @Nonnull
    default DependencyResolverResult resolve(
            @Nonnull Session session, @Nonnull Project project, @Nonnull PathScope scope) {
        return resolve(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.RESOLVE, project, scope));
    }

    @Nonnull
    default DependencyResolverResult resolve(@Nonnull Session session, @Nonnull DependencyCoordinates dependency) {
        return resolve(
                DependencyResolverRequest.build(session, DependencyResolverRequest.RequestType.RESOLVE, dependency));
    }

    @Nonnull
    default DependencyResolverResult resolve(
            @Nonnull Session session, @Nonnull DependencyCoordinates dependency, @Nonnull PathScope scope) {
        return resolve(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.RESOLVE, dependency, scope));
    }

    @Nonnull
    default DependencyResolverResult resolve(
            @Nonnull Session session, @Nonnull List<DependencyCoordinates> dependencies) {
        return resolve(
                DependencyResolverRequest.build(session, DependencyResolverRequest.RequestType.RESOLVE, dependencies));
    }

    @Nonnull
    default DependencyResolverResult resolve(
            @Nonnull Session session, @Nonnull List<DependencyCoordinates> dependencies, @Nonnull PathScope scope) {
        return resolve(DependencyResolverRequest.build(
                session, DependencyResolverRequest.RequestType.RESOLVE, dependencies, scope));
    }

    /**
     * This method collects, flattens and resolves the dependencies.
     *
     * @param request the request to resolve
     * @return the result of the resolution
     * @throws DependencyResolverException
     * @throws ArtifactResolverException
     *
     * @see DependencyResolver#collect(DependencyResolverRequest)
     * @see #flatten(Session, Node, PathScope)
     * @see ArtifactResolver#resolve(ArtifactResolverRequest)
     */
    @Nonnull
    DependencyResolverResult resolve(@Nonnull DependencyResolverRequest request)
            throws DependencyResolverException, ArtifactResolverException;
}
