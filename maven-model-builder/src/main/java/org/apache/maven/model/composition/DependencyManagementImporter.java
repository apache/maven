package org.apache.maven.model.composition;

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

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles the import of dependency management from other models into the target model.
 *
 * @author Benjamin Bentmann
 */
public interface DependencyManagementImporter
{

    /**
     * Imports the specified dependency management sections into the given target model.
     *
     * @param target The model into which to import the dependency management section, must not be <code>null</code>.
     * @param sources The dependency management sections to import, may be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void importManagement( Model target, List<? extends DependencyManagement> sources, ModelBuildingRequest request,
                           ModelProblemCollector problems );

}
