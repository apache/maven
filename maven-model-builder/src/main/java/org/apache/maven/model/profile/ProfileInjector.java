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
package org.apache.maven.model.profile;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;

/**
 * Handles profile injection into the model.
 *
 * @author Benjamin Bentmann
 */
public interface ProfileInjector {

    /**
     * Merges values from the specified profile into the given model. Implementations are expected to keep the profile
     * and model completely decoupled by injecting deep copies rather than the original objects from the profile.
     *
     * @param model The model into which to merge the values defined by the profile, must not be <code>null</code>.
     * @param profile The (read-only) profile whose values should be injected, may be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void injectProfile(Model model, Profile profile, ModelBuildingRequest request, ModelProblemCollector problems);
}
