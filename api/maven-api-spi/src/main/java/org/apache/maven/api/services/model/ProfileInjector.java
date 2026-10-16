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
package org.apache.maven.api.services.model;

import java.util.List;

import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.ModelProblemCollector;

/**
 * Handles profile injection into the model.
 *
 * <p>In addition to injecting standard profile properties into the model, implementations
 * are expected to surface profile-scoped properties under {@code profile.<id>.<property>}
 * for active profiles. This enables POM interpolation to target properties from specific
 * active profiles without cross-profile collisions.</p>
 *
 * <h3>Conventions and Semantics</h3>
 * <ul>
 *   <li><b>Active Profiles Only:</b> Only active profiles contribute profile-scoped properties.</li>
 *   <li><b>Reserved Prefix:</b> Property names beginning with {@code profile.<id>.*} are reserved
 *       for profile-scoped resolution. In case of collisions with explicit model properties,
 *       the active profile value takes precedence and a warning is logged.</li>
 *   <li><b>Prefix Exclusion:</b> Profile properties that already start with {@code profile.} are not
 *       re-prefixed.</li>
 *   <li><b>Profile ID Naming:</b> Using hyphens in profile IDs is recommended over dots to avoid
 *       dot-delimiter collision ambiguities across overlapping profile IDs and property names.</li>
 * </ul>
 *
 * @since 4.0.0
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
    default Model injectProfile(
            Model model, Profile profile, ModelBuilderRequest request, ModelProblemCollector problems) {
        return injectProfiles(model, List.of(profile), request, problems);
    }

    /**
     * Merges values from the specified profile into the given model. Implementations are expected to keep the profile
     * and model completely decoupled by injecting deep copies rather than the original objects from the profile.
     *
     * @param model The model into which to merge the values defined by the profile, must not be <code>null</code>.
     * @param profiles The (read-only) list of profiles whose values should be injected, must not be <code>null</code>.
     * @param request The model building request that holds further settings, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    Model injectProfiles(
            Model model, List<Profile> profiles, ModelBuilderRequest request, ModelProblemCollector problems);
}
