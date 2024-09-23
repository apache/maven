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
package org.apache.maven.model.management;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles injection of dependency management into the model.
 *
 * @deprecated use {@link org.apache.maven.api.services.ModelBuilder} instead
 */
@Deprecated(since = "4.0.0")
public interface DependencyManagementInjector {

    /**
     * Merges default values from the dependency management section of the given model into itself.
     *
     * @param model The model into which to merge the values specified by its dependency management sections, must not
     *            be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void injectManagement(Model model, ModelBuildingRequest request, ModelProblemCollector problems);
}
