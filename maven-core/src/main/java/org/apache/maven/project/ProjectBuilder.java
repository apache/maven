package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.Artifact;

public interface ProjectBuilder
{

    ProjectBuildingResult build( File projectFile, ProjectBuildingRequest request )
        throws ProjectBuildingException;

    ProjectBuildingResult build( Artifact projectArtifact, ProjectBuildingRequest request )
        throws ProjectBuildingException;

    // TODO: this is only to provide a project for plugins that don't need a project to execute but need some
    // of the values from a MavenProject. Ideally this should be something internal and nothing outside Maven
    // would ever need this so it should not be exposed in a public API
    ProjectBuildingResult buildStandaloneSuperProject( ProjectBuildingRequest request )
        throws ProjectBuildingException;

    /**
     * Builds the projects for the specified POM files and optionally their children.
     * 
     * @param pomFiles The POM files to build, must not be {@code null}.
     * @param recursive {@code true} to recursively build sub modules referenced by the POM files, {@code false} to
     *            build only the specified POM files.
     * @param config The project builder configuration that provides further parameters, must not be {@code null}.
     * @return The results of the project builder where each result corresponds to one project that was built, never
     *         {@code null}.
     * @throws ProjectBuildingException If an error was encountered during building of any project.
     *             {@link ProjectBuildingException#getResults()} provides access to the details of the problems.
     */
    List<ProjectBuildingResult> build( List<File> pomFiles, boolean recursive, ProjectBuildingRequest config )
        throws ProjectBuildingException;

}
