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
package org.apache.maven.settings.validation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.regex.Pattern;

import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.building.SettingsProblemCollector;

/**
 * @deprecated since 4.0.0, use {@link org.apache.maven.internal.impl.DefaultSettingsValidator} instead
 */
@Named
@Singleton
@Deprecated(since = "4.0.0")
public class DefaultSettingsValidator implements SettingsValidator {

    private static final String ID = "[\\w.-]+";
    private static final Pattern ID_REGEX = Pattern.compile(ID);

    private final SettingsBuilder settingsBuilder;

    @Inject
    public DefaultSettingsValidator(SettingsBuilder settingsBuilder) {
        this.settingsBuilder = settingsBuilder;
    }

    @Override
    public void validate(Settings settings, SettingsProblemCollector problems) {
        validate(settings, false, problems);
    }

    @Override
    public void validate(Settings settings, boolean isProjectSettings, SettingsProblemCollector problems) {
        ProblemCollector<BuilderProblem> list = settingsBuilder.validate(settings.getDelegate(), isProjectSettings);
        for (BuilderProblem problem : list.problems().toList()) {
            addViolation(problems, Severity.valueOf(problem.getSeverity().name()), problem.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    // Field validation
    // ----------------------------------------------------------------------

    private static void addViolation(SettingsProblemCollector problems, Severity severity, String message) {
        problems.add(severity, message, -1, -1, null);
    }
}
