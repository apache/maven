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

import java.util.List;

import org.apache.maven.settings.Settings;

/**
 * Collects the output of the settings builder.
 *
 * @author Benjamin Bentmann
 */
public interface SettingsBuildingResult {

    /**
     * Gets the assembled settings.
     *
     * @return The assembled settings, never {@code null}.
     */
    Settings getEffectiveSettings();

    /**
     * Gets the problems that were encountered during the settings building. Note that only problems of severity
     * {@link SettingsProblem.Severity#WARNING} and below are reported here. Problems with a higher severity level cause
     * the settings builder to fail with a {@link SettingsBuildingException}.
     *
     * @return The problems that were encountered during the settings building, can be empty but never {@code null}.
     */
    List<SettingsProblem> getProblems();
}
