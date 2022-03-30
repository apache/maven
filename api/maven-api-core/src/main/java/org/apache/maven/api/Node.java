package org.apache.maven.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.api.annotations.Immutable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a dependency node within a Maven project's dependency collector.
 */
@Immutable
public interface Node
{
    /**
     * @return artifact for this node
     */
    Artifact getArtifact();

    /**
     * @return dependency for this node
     */
    Dependency getDependency();

    /**
     * Gets the child nodes of this node.
     *
     * @return the child nodes of this node, never {@code null}
     */
    List<Node> getChildren();

    /**
     * @return repositories of this node
     */
    List<Repository> getRemoteRepositories();

    /**
     * The repository where this artifact has been downloaded from.
     */
    Optional<RemoteRepository> getRepository();

    /**
     * Traverses this node and potentially its children using the specified visitor.
     *
     * @param visitor The visitor to call back, must not be {@code null}.
     * @return {@code true} to visit siblings nodes of this node as well, {@code false} to skip siblings.
     */
    boolean accept( NodeVisitor visitor );

    /**
     * Returns a new tree starting at this node, filtering the children.
     * Note that this node will not be filtered and only the children
     * and its descendant will be checked.
     *
     * @param filter the filter to apply
     * @return a new filtered graph
     */
    Node filter( Predicate<Node> filter );

    /**
     * Obtain a Stream containing this node and all its descendant.
     *
     * @return a stream containing this node and its descendant
     */
    default Stream<Node> stream()
    {
        return Stream.concat( Stream.of( this ), getChildren().stream().flatMap( Node::stream ) );
    }

}
