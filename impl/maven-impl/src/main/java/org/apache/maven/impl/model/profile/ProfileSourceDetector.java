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
package org.apache.maven.impl.model.profile;

import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;

/**
 * Utility for detecting the origin of a {@link Profile}.
 *
 * <p>Since model version 4.2.0, {@link Profile} no longer carries a mutable {@code source}
 * field. Origin is instead inferred from the {@link InputSource} attached to the profile's
 * root {@link InputLocation}: a non-null, non-empty {@code modelId} means the profile was
 * read from a POM file; an empty or null {@code modelId} indicates a settings profile
 * (settings.xml converted via {@code SettingsUtilsV4.toLocation}).
 *
 * @since 4.2.0
 */
public final class ProfileSourceDetector {

    private ProfileSourceDetector() {}

    /**
     * Returns {@code true} if the given profile originates from a POM file.
     *
     * <p>Detection is based on the profile's root {@link InputLocation}: if the attached
     * {@link InputSource} has a non-null, non-empty {@code modelId}, the profile is from a
     * POM. Settings profiles converted via {@code SettingsUtilsV4} receive an
     * {@code InputSource} with an empty {@code modelId}.
     *
     * <p>Defaults to {@code true} when no location information is available, preserving
     * the pre-4.2.0 behaviour.
     *
     * @param profile the profile to inspect; must not be {@code null}
     * @return {@code true} if the profile is from a POM, {@code false} if from settings
     */
    public static boolean isFromPom(Profile profile) {
        InputLocation location = profile.getLocation("");
        if (location != null) {
            InputSource source = location.getSource();
            if (source != null) {
                String modelId = source.getModelId();
                return modelId != null && !modelId.isEmpty();
            }
        }
        return true;
    }
}
