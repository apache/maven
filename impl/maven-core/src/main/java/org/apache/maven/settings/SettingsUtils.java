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
package org.apache.maven.settings;

import org.apache.maven.internal.impl.SettingsUtilsV4;

/**
 * Several convenience methods to handle settings
 *
 */
public final class SettingsUtils {

    private SettingsUtils() {
        // don't allow construction.
    }

    /**
     * @param dominant
     * @param recessive
     * @param recessiveSourceLevel
     */
    public static void merge(Settings dominant, Settings recessive, String recessiveSourceLevel) {
        if (dominant != null && recessive != null) {
            dominant.delegate = SettingsUtilsV4.merge(dominant.getDelegate(), recessive.getDelegate());
        }
    }

    /**
     * @param modelProfile
     * @return a profile
     */
    public static Profile convertToSettingsProfile(org.apache.maven.model.Profile modelProfile) {
        return new Profile(SettingsUtilsV4.convertToSettingsProfile(modelProfile.getDelegate()));
    }

    /**
     * @param settingsProfile
     * @return a profile
     */
    public static org.apache.maven.model.Profile convertFromSettingsProfile(Profile settingsProfile) {
        return new org.apache.maven.model.Profile(
                SettingsUtilsV4.convertFromSettingsProfile(settingsProfile.getDelegate()));
    }

    /**
     * @param settings could be null
     * @return a new instance of settings or null if settings was null.
     */
    public static Settings copySettings(Settings settings) {
        if (settings == null) {
            return null;
        }

        return new Settings(settings.getDelegate());
    }
}
