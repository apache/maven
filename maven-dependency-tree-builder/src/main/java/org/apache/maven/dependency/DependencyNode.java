package org.apache.maven.dependency;

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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

/**
 * Describes a single dependency within the dependency tree.
 * 
 * @author Benjamin Bentmann
 */
public interface DependencyNode
{

    /**
     * The dependency forming this node. If the parent of this node declared a dependency on a relocated artifact, the
     * returned dependency information will reflect the artifact coordinates after relocation.
     * 
     * @return The dependency forming this node, never {@code null}.
     */
    Dependency getDependency();

    /**
     * The metadata of the dependency.
     * 
     * @return The metadata or {@code null} if the metadata could not be read.
     */
    Model getMetadata();

    /**
     * The identifier of the repository in which the metadata of the dependency was discovered. If known, later
     * resolution of the dependency's file should prefer this repository.
     * 
     * @return The identifier of the repository or {@code null} if unknown.
     */
    String getRepositoryId();

    /**
     * Gets the chain of relocations that were resolved to fetch the metadata of the dependency.
     * 
     * @return The relocation chain or an empty list if the artiact was not relocated, never {@code null}.
     */
    List<Model> getRelocations();

    /**
     * The parent node of this node.
     * 
     * @return The parent node or {@code null} if this is the root node.
     */
    DependencyNode getParent();

    /**
     * Gets the direct child nodes of this node.
     * 
     * @return The child nodes or an empty list if none, never {@code null}.
     */
    List<DependencyNode> getChildren();

    /**
     * Gets the zero-based depth of this node within the tree. The root node has depth {@code 0}.
     * 
     * @return The zero-based depth of this node within the tree.
     */
    int getDepth();

}
