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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.settings.InputSource;
import org.apache.maven.building.FileSource;
import org.apache.maven.building.Source;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.TrackableBase;
import org.apache.maven.settings.io.SettingsParseException;
import org.apache.maven.settings.io.SettingsReader;
import org.apache.maven.settings.io.SettingsWriter;
import org.apache.maven.settings.merge.MavenSettingsMerger;
import org.apache.maven.settings.v4.SettingsTransformer;
import org.apache.maven.settings.validation.SettingsValidator;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 */
@Named
@Singleton
public class DefaultSettingsBuilder implements SettingsBuilder {

    private SettingsReader settingsReader;

    private SettingsWriter settingsWriter;

    private SettingsValidator settingsValidator;

    private final MavenSettingsMerger settingsMerger = new MavenSettingsMerger();

    @Inject
    public DefaultSettingsBuilder(
            SettingsReader settingsReader, SettingsWriter settingsWriter, SettingsValidator settingsValidator) {
        this.settingsReader = settingsReader;
        this.settingsWriter = settingsWriter;
        this.settingsValidator = settingsValidator;
    }

    public DefaultSettingsBuilder setSettingsReader(SettingsReader settingsReader) {
        this.settingsReader = settingsReader;
        return this;
    }

    public DefaultSettingsBuilder setSettingsWriter(SettingsWriter settingsWriter) {
        this.settingsWriter = settingsWriter;
        return this;
    }

    public DefaultSettingsBuilder setSettingsValidator(SettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
        return this;
    }

    @Override
    public SettingsBuildingResult build(SettingsBuildingRequest request) throws SettingsBuildingException {
        DefaultSettingsProblemCollector problems = new DefaultSettingsProblemCollector(null);

        Source globalSettingsSource =
                getSettingsSource(request.getGlobalSettingsFile(), request.getGlobalSettingsSource());
        Settings globalSettings = readSettings(globalSettingsSource, false, request, problems);

        Source projectSettingsSource =
                getSettingsSource(request.getProjectSettingsFile(), request.getProjectSettingsSource());
        Settings projectSettings = readSettings(projectSettingsSource, true, request, problems);

        Source userSettingsSource = getSettingsSource(request.getUserSettingsFile(), request.getUserSettingsSource());
        Settings userSettings = readSettings(userSettingsSource, false, request, problems);

        settingsMerger.merge(projectSettings, globalSettings, TrackableBase.GLOBAL_LEVEL);
        settingsMerger.merge(userSettings, projectSettings, TrackableBase.PROJECT_LEVEL);

        // If no repository is defined in the user/global settings,
        // it means that we have "old" settings (as those are new in 4.0)
        // so add central to the computed settings for backward compatibility.
        if (userSettings.getRepositories().isEmpty()
                && userSettings.getPluginRepositories().isEmpty()) {
            Repository central = new Repository();
            central.setId("central");
            central.setName("Central Repository");
            central.setUrl("https://repo.maven.apache.org/maven2");
            RepositoryPolicy disabledPolicy = new RepositoryPolicy();
            disabledPolicy.setEnabled(false);
            central.setSnapshots(disabledPolicy);
            userSettings.getRepositories().add(central);
            central = central.clone();
            RepositoryPolicy updateNeverPolicy = new RepositoryPolicy();
            disabledPolicy.setUpdatePolicy("never");
            central.setReleases(updateNeverPolicy);
            userSettings.getPluginRepositories().add(central);
        }

        problems.setSource("");

        // for the special case of a drive-relative Windows path, make sure it's absolute to save plugins from trouble
        String localRepository = userSettings.getLocalRepository();
        if (localRepository != null && localRepository.length() > 0) {
            File file = new File(localRepository);
            if (!file.isAbsolute() && file.getPath().startsWith(File.separator)) {
                userSettings.setLocalRepository(file.getAbsolutePath());
            }
        }

        if (hasErrors(problems.getProblems())) {
            throw new SettingsBuildingException(problems.getProblems());
        }

        return new DefaultSettingsBuildingResult(userSettings, problems.getProblems());
    }

    private boolean hasErrors(List<SettingsProblem> problems) {
        if (problems != null) {
            for (SettingsProblem problem : problems) {
                if (SettingsProblem.Severity.ERROR.compareTo(problem.getSeverity()) >= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private Source getSettingsSource(File settingsFile, Source settingsSource) {
        if (settingsSource != null) {
            return settingsSource;
        } else if (settingsFile != null && settingsFile.exists()) {
            return new FileSource(settingsFile);
        }
        return null;
    }

    private Settings readSettings(
            Source settingsSource,
            boolean isProjectSettings,
            SettingsBuildingRequest request,
            DefaultSettingsProblemCollector problems) {
        if (settingsSource == null) {
            return new Settings();
        }

        problems.setSource(settingsSource.getLocation());

        Settings settings;

        try {
            Map<String, Object> options = new HashMap<>();
            options.put(SettingsReader.IS_STRICT, Boolean.TRUE);
            options.put(InputSource.class.getName(), new InputSource(settingsSource.getLocation()));
            try {
                settings = settingsReader.read(settingsSource.getInputStream(), options);
            } catch (SettingsParseException e) {
                options = Collections.singletonMap(SettingsReader.IS_STRICT, Boolean.FALSE);

                settings = settingsReader.read(settingsSource.getInputStream(), options);

                problems.add(
                        SettingsProblem.Severity.WARNING, e.getMessage(), e.getLineNumber(), e.getColumnNumber(), e);
            }
        } catch (SettingsParseException e) {
            problems.add(
                    SettingsProblem.Severity.FATAL,
                    "Non-parseable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    e.getLineNumber(),
                    e.getColumnNumber(),
                    e);
            return new Settings();
        } catch (IOException e) {
            problems.add(
                    SettingsProblem.Severity.FATAL,
                    "Non-readable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    -1,
                    -1,
                    e);
            return new Settings();
        }

        settings = interpolate(settings, request, problems);

        settingsValidator.validate(settings, isProjectSettings, problems);

        if (isProjectSettings) {
            settings.setLocalRepository(null);
            settings.setInteractiveMode(true);
            settings.setOffline(false);
            settings.setProxies(Collections.emptyList());
            settings.setUsePluginRegistry(false);
            for (Server server : settings.getServers()) {
                server.setUsername(null);
                server.setPassword(null);
                server.setPrivateKey(null);
                server.setPassword(null);
                server.setFilePermissions(null);
                server.setDirectoryPermissions(null);
            }
        }

        return settings;
    }

    private Settings interpolate(
            Settings settings, SettingsBuildingRequest request, SettingsProblemCollector problems) {

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource(new PropertiesBasedValueSource(request.getUserProperties()));

        interpolator.addValueSource(new PropertiesBasedValueSource(request.getSystemProperties()));

        try {
            interpolator.addValueSource(new EnvarBasedValueSource());
        } catch (IOException e) {
            problems.add(
                    SettingsProblem.Severity.WARNING,
                    "Failed to use environment variables for interpolation: " + e.getMessage(),
                    -1,
                    -1,
                    e);
        }

        return new Settings(new SettingsTransformer(value -> {
                    try {
                        return value != null ? interpolator.interpolate(value) : null;
                    } catch (InterpolationException e) {
                        problems.add(
                                SettingsProblem.Severity.WARNING,
                                "Failed to interpolate settings: " + e.getMessage(),
                                -1,
                                -1,
                                e);
                        return value;
                    }
                })
                .visit(settings.getDelegate()));
    }
}
