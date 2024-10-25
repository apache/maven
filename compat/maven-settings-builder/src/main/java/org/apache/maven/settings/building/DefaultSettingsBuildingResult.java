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
package org.apache.maven.settings.building;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Settings;

/**
 * Collects the output of the settings builder.
 *
 * @deprecated since 4.0.0, use {@link org.apache.maven.api.services.SettingsBuilder} instead
 */
@Deprecated(since = "4.0.0")
class DefaultSettingsBuildingResult implements SettingsBuildingResult {

    private Settings effectiveSettings;

    private List<SettingsProblem> problems;

    DefaultSettingsBuildingResult(Settings effectiveSettings, List<SettingsProblem> problems) {
        this.effectiveSettings = effectiveSettings;
        this.problems = (problems != null) ? problems : new ArrayList<>();
    }

    @Override
    public Settings getEffectiveSettings() {
        return effectiveSettings;
    }

    @Override
    public List<SettingsProblem> getProblems() {
        return problems;
    }
}
