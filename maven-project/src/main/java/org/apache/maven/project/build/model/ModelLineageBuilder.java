package org.apache.maven.project.build.model;

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

import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.List;

/**
 * Builds the lineage of Model instances, starting from a given POM file, and stretching back through
 * all of the parent POMs that are defined in the respective <parent/> sections.
 *
 * NOTE: In all of the build/resume methods below, each Model MUST have its active profiles searched
 * for new repositories from which to discover parent POMs.
 */
public interface ModelLineageBuilder
{

    String ROLE = ModelLineageBuilder.class.getName();

    /**
     * Construct a lineage of the current POM plus all of its ancestors.
     *
     * @param pom The current POM, whose Model will terminate the constructed lineage
     *
     * @param config The project-building configuration to use, which contains the global profile manager,
     *   local repository, and execution- and user-level properties.
     *
     * @param remoteRepositories List of ArtifactRepository instances against which parent POMs
     *   should be resolved
     *
     * @param allowStubs Whether stubbed-out Model instances should be constructed in the event that
     *   a parent-POM cannot be resolved.
     *
     * @param isReactorProject Whether the model being built is part of the build we're trying to execute,
     *   or if it's actually being read from the repository.
     */
    ModelLineage buildModelLineage( File pom,
                                    ProjectBuilderConfiguration config,
                                    List remoteRepositories,
                                    boolean allowStubs,
                                    boolean isReactorProject )
        throws ProjectBuildingException;

    /**
     * Resume the process of constructing a lineage of inherited models, picking up using the deepest
     * parent already in the lineage.
     *
     * @param lineage The ModelLineage instance in progress, which should be completed.
     *
     * @param config The project-building configuration to use, which contains the global profile manager,
     *   local repository, and execution- and user-level properties.
     *
     * @param allowStubs Whether stubbed-out Model instances should be constructed in the event that
     *   a parent-POM cannot be resolved.
     *
     * @param isReactorProject Whether the model being built is part of the build we're trying to execute,
     *   or if it's actually being read from the repository.
     */
    void resumeBuildingModelLineage( ModelLineage lineage,
                                     ProjectBuilderConfiguration config,
                                     boolean allowStubs,
                                     boolean isReactorProject )
        throws ProjectBuildingException;

}
