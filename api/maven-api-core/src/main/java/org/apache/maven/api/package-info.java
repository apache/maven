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

/**
 * Core API for Maven plugins.
 *
 * <h2>Definitions of terms</h2>
 * <p><dfn>Artifact resolution</dfn> is the process of {@linkplain org.apache.maven.api.services.VersionResolver
 * resolving the version} and then downloading the file.</p>
 *
 * <p><dfn>Dependency resolution</dfn> is the process of collecting dependencies, flattening the graph,
 * and then downloading the file. The flattening phase removes branches of the graph so that one artifact
 * per ({@code groupId}, {@code artifactId}) pair is present.</p>
 *
 * <h2>Dependency management</h2>
 * <p>{@link org.apache.maven.api.ArtifactCoordinate} instances are used to locate artifacts in a repository.
 * Each instance is basically a pointer to a file in the Maven repository, except that the version may not be
 * defined precisely.</p>
 *
 * <p>{@link org.apache.maven.api.Artifact} instances are the pointed artifacts in the repository.
 * They are created when <dfn>resolving</dfn> an {@code ArtifactCoordinate}. Resolving is the process
 * that selects a particular version and downloads the artifact in the local repository.
 * The download may be deferred to the first time that the file is needed.</p>
 *
 * <p>{@link org.apache.maven.api.DependencyCoordinate} instances are used to express a dependency.
 * They are an {@code ArtifactCoordinate} completed with information about how the artifact will be used:
 * type, scope and obligation (whether the dependency is optional or mandatory).
 * The version and the obligation may not be defined precisely.</p>
 *
 * <p>{@link org.apache.maven.api.Dependency} instances are the pointed dependencies in the repository.
 * They are created when <dfn>resolving</dfn> a {@code DependencyCoordinate}.
 * Resolving is the process that clarifies the obligation (optional or mandatory status),
 * selects a particular version and downloads the artifact in the local repository.</p>
 *
 * <p>{@link org.apache.maven.api.Node} is the main output of the <dfn>dependency collection</dfn> process.
 * it's the graph of dependencies. The above-cited {@code Dependency} instances are the outputs of the
 * collection process, part of the graph computed from one or more {@code DependencyCoordinate}s.</p>
 */
package org.apache.maven.api;
