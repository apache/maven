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

import org.apache.maven.model.Repository;

/**
 * Reads dependency versions and metadata from repositories. The dependency tree builder will maintain one repository
 * reader for each node of the dependency tree.
 * 
 * @author Benjamin Bentmann
 */
public interface RepositoryReader
{

    /**
     * Resolves a version specification of a dependency to the matching versions that are available in the repositories.
     * 
     * @param request The version resolution request, must not be {@code null}.
     * @return The version resolution result, never {@code null}.
     */
    VersionResult getVersions( VersionRequest request );

    /**
     * Resolves the metadata for a dependency.
     * 
     * @param request The metadata resolution request, must not be {@code null}.
     * @return The metadata resolution result, never {@code null}.
     */
    MetadataResult getMetadata( MetadataRequest request );

    /**
     * Derives a new repository reader from this one that additionally consideres the specified repositories.
     * 
     * @param repositories The repositories to additionally consider for the new reader, must not be {@code null}.
     * @param problems The collector used to record any problems that are encountered while setting up the reader's
     *            internals to account for the specified repositories.
     * @return The extended repository reader, never {@code null}.
     */
    RepositoryReader addRepositories( List<Repository> repositories, DependencyProblemCollector problems );

}
