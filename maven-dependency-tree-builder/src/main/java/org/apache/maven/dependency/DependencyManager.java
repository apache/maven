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

import org.apache.maven.model.Dependency;

/**
 * Applies dependency management to the dependencies of a dependency node. The dependency tree builder will maintain one
 * dependency manager for each node of the dependency tree.
 * 
 * @author Benjamin Bentmann
 */
public interface DependencyManager
{

    /**
     * Applies dependency management to the specified dependency of a dependency node. The management information (if
     * any) will be directly injected into the specified dependency.
     * 
     * @param node The node whose child dependency should be managed, must not be {@code null}.
     * @param dependency The dependency to manage, must not be {@code null}.
     */
    void manageDependency( DependencyNode node, Dependency dependency );

    /**
     * Derives a dependency manager for the specified child node of the current node, i.e. the parent of the specified
     * node. This method is called by the dependency tree builder just before it recurses into the child node.
     * Implementors are expected to calculate a new dependency manager for the dependencies of the child node.
     * 
     * @param childNode The child node to derive a manager for, must not be {@code null}.
     * @return The dependency manager for the child node, must not be {@code null}.
     */
    DependencyManager deriveChildManager( DependencyNode childNode );

}
