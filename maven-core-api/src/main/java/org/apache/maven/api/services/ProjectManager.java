package org.apache.maven.api.services;

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

import javax.annotation.Nonnull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.maven.api.Artifact;
import org.apache.maven.api.Node;
import org.apache.maven.api.Project;
import org.apache.maven.api.RemoteRepository;

/**
 * Interface to manage the project during its lifecycle
 */
public interface ProjectManager extends Service
{
    /**
     * Returns the path to the resolved file in the local repository
     * if the artifact has been resolved.
     *
     * @return the path of the resolved artifact
     */
    @Nonnull
    Optional<Path> getPath( Project project );

    @Nonnull
    Collection<Artifact> getAttachedArtifacts( Project project );

    void attachArtifact( Project project, String type, Path path );

    void attachArtifact( Project project, String trim, String trim1, Path path );

    List<String> getCompileSourceRoots( Project project );

    void addCompileSourceRoot( Project project, String sourceRoot );

    List<String> getTestCompileSourceRoots( Project project );

    void addTestCompileSourceRoot( Project project, String sourceRoot );

    List<RemoteRepository> getRepositories( Project project );

    List<Artifact> getResolvedDependencies( Project project, ResolutionScope scope );

    Node getCollectedDependencies( Project project, ResolutionScope scope );

    enum ResolutionScope
    {
        Compile,
        CompileRuntime,
        Runtime,
        RuntimeSystem,
        Test
    }
}
