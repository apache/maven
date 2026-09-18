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

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Immutable;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.Nullable;
import org.apache.maven.api.annotations.Provider;

/**
 * Represents a dependency node within a Maven project's dependency collector.
 *
 * @since 4.0.0
 * @see org.apache.maven.api.services.DependencyResolverResult#getRoot()
 */
@Experimental
@Immutable
@Provider
public interface Node {

    /**
     * Returns the artifact for this node.
     *
     * @return artifact for this node, or {@code null} if none
     */
    @Nullable
    Artifact artifact();

    /**
     * Returns the dependency for this node.
     *
     * @return dependency for this node, or {@code null} if none
     */
    @Nullable
    Dependency dependency();

    /**
     * Gets the child nodes of this node.
     *
     * @return the child nodes of this node, never {@code null}
     */
    @Nonnull
    List<Node> children();

    /**
     * Returns the remote repositories of this node.
     *
     * @return repositories of this node, never {@code null}
     */
    @Nonnull
    List<RemoteRepository> remoteRepositories();

    /**
     * Returns the remote repository from which this artifact was downloaded, if known.
     *
     * @return an {@code Optional} containing the repository, or empty if not available (e.g. local artifact or root node)
     */
    @Nonnull
    Optional<RemoteRepository> repository();

    /**
     * @return artifact for this node
     * @deprecated Use {@link #artifact()} instead.
     */
    @Nullable
    @Deprecated(since = "4.1.0", forRemoval = true)
    default Artifact getArtifact() {
        return artifact();
    }

    /**
     * @return dependency for this node
     * @deprecated Use {@link #dependency()} instead.
     */
    @Nullable
    @Deprecated(since = "4.1.0", forRemoval = true)
    default Dependency getDependency() {
        return dependency();
    }

    /**
     * Gets the child nodes of this node.
     *
     * @return the child nodes of this node, never {@code null}
     * @deprecated Use {@link #children()} instead.
     */
    @Nonnull
    @Deprecated(since = "4.1.0", forRemoval = true)
    default List<Node> getChildren() {
        return children();
    }

    /**
     * @return repositories of this node
     * @deprecated Use {@link #remoteRepositories()} instead.
     */
    @Nonnull
    @Deprecated(since = "4.1.0", forRemoval = true)
    default List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories();
    }

    /**
     * The repository where this artifact has been downloaded from.
     *
     * @deprecated Use {@link #repository()} instead.
     */
    @Nonnull
    @Deprecated(since = "4.1.0", forRemoval = true)
    default Optional<RemoteRepository> getRepository() {
        return repository();
    }

    /**
     * Traverses this node and potentially its children using the specified visitor.
     *
     * @param visitor the visitor to call back, must not be {@code null}
     * @return {@code true} to visit siblings nodes of this node as well, {@code false} to skip siblings
     */
    boolean accept(@Nonnull NodeVisitor visitor);

    /**
     * Returns a new tree starting at this node, filtering the children.
     * Note that this node will not be filtered and only the children
     * and its descendant will be checked.
     *
     * @param filter the filter to apply
     * @return a new filtered graph
     */
    @Nonnull
    Node filter(@Nonnull Predicate<Node> filter);

    /**
     * Returns a detailed string representation of this dependency node.
     * <p>
     * When verbose mode is disabled, returns the basic string representation in the format:
     * {@code groupId:artifactId:version[:scope]}
     * <p>
     * When verbose mode is enabled, additional details are included with the following format:
     * <ul>
     *   <li>For included dependencies: {@code groupId:artifactId:version[:scope] (details)}</li>
     *   <li>For omitted dependencies: {@code (groupId:artifactId:version[:scope] - details)}</li>
     * </ul>
     * Where details may include:
     * <ul>
     *   <li>Version management information (if the version was managed from a different version)</li>
     *   <li>Scope management information (if the scope was managed from a different scope)</li>
     *   <li>Scope updates (if the scope was changed during resolution)</li>
     *   <li>Conflict resolution information (if the dependency was omitted due to conflicts or duplicates)</li>
     * </ul>
     *
     * @return a string representation of this dependency node with optional detailed information
     */
    @Nonnull
    String asString();

    /**
     * Obtain a Stream containing this node and all its descendants.
     *
     * @return a stream containing this node and its descendants
     */
    @Nonnull
    default Stream<Node> stream() {
        return Stream.concat(Stream.of(this), children().stream().flatMap(Node::stream));
    }
}
