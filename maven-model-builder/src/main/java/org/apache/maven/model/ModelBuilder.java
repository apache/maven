package org.apache.maven.model;

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

import java.io.File;

import org.apache.maven.model.resolution.ModelResolver;

/**
 * Builds the effective model from a POM.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelBuilder
{

    /**
     * Builds the effective model of the specified POM file. Note that this method overload is meant to build the
     * effective model for the build process of a project. Hence the effective model supports the notion of a project
     * directory.
     * 
     * @param pomFile The POM file of the project to build the effective model from, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param modelResolver The model resolver used to resolve parent POMs that are not locally reachable from the
     *            project directory, must not be {@code null}.
     * @return The result of the model building, never {@code null}.
     * @throws ModelBuildingException If the effective model could not be built.
     */
    ModelBuildingResult build( File pomFile, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException;

    /**
     * Builds the effective model for the specified POM. In contrast to
     * {@link #build(File, ModelBuildingRequest, ModelResolver)} the resulting model does not support the notion of a
     * project directory. As a consequence, parent POMs are always resolved via the provided model resolver.
     * 
     * @param modelSource The source of the POM, must not be {@code null}.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param modelResolver The model resolver used to resolve parent POMs, must not be {@code null}.
     * @return The result of the model building, never {@code null}.
     * @throws ModelBuildingException If the effective model could not be built.
     */
    ModelBuildingResult build( ModelSource modelSource, ModelBuildingRequest request, ModelResolver modelResolver )
        throws ModelBuildingException;

}
