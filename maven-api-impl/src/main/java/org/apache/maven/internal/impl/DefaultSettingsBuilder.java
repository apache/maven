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
package org.apache.maven.internal.impl;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.RepositoryPolicy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.v4.SettingsMerger;
import org.apache.maven.settings.v4.SettingsTransformer;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 */
@Named
public class DefaultSettingsBuilder implements SettingsBuilder {

    private final DefaultSettingsValidator settingsValidator = new DefaultSettingsValidator();

    private final SettingsMerger settingsMerger = new SettingsMerger();

    @Override
    public SettingsBuilderResult build(SettingsBuilderRequest request) throws SettingsBuilderException {
        List<BuilderProblem> problems = new ArrayList<>();

        Source systemSource = request.getSystemSettingsSource().orElse(null);
        Settings system = readSettings(systemSource, false, request, problems);

        Source projectSource = request.getProjectSettingsSource().orElse(null);
        Settings project = readSettings(projectSource, true, request, problems);

        Source userSource = request.getUserSettingsSource().orElse(null);
        Settings user = readSettings(userSource, false, request, problems);

        Settings effective =
                settingsMerger.merge(user, settingsMerger.merge(project, system, false, null), false, null);

        // If no repository is defined in the user/global settings,
        // it means that we have "old" settings (as those are new in 4.0)
        // so add central to the computed settings for backward compatibility.
        if (effective.getRepositories().isEmpty()
                && effective.getPluginRepositories().isEmpty()) {
            Repository central = Repository.newBuilder()
                    .id("central")
                    .name("Central Repository")
                    .url("https://repo.maven.apache.org/maven2")
                    .snapshots(RepositoryPolicy.newBuilder().enabled(false).build())
                    .build();
            Repository centralWithNoUpdate = central.withReleases(
                    RepositoryPolicy.newBuilder().updatePolicy("never").build());
            effective = Settings.newBuilder(effective)
                    .repositories(List.of(central))
                    .pluginRepositories(List.of(centralWithNoUpdate))
                    .build();
        }

        // for the special case of a drive-relative Windows path, make sure it's absolute to save plugins from trouble
        String localRepository = effective.getLocalRepository();
        if (localRepository != null && !localRepository.isEmpty()) {
            Path file = Paths.get(localRepository);
            if (!file.isAbsolute() && file.toString().startsWith(File.separator)) {
                effective = effective.withLocalRepository(file.toAbsolutePath().toString());
            }
        }

        if (hasErrors(problems)) {
            throw new SettingsBuilderException("Error building settings", problems);
        }

        return new DefaultSettingsBuilderResult(effective, problems);
    }

    private boolean hasErrors(List<BuilderProblem> problems) {
        if (problems != null) {
            for (BuilderProblem problem : problems) {
                if (BuilderProblem.Severity.ERROR.compareTo(problem.getSeverity()) >= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private Settings readSettings(
            Source settingsSource,
            boolean isProjectSettings,
            SettingsBuilderRequest request,
            List<BuilderProblem> problems) {
        if (settingsSource == null) {
            return Settings.newInstance();
        }

        Settings settings;

        try {
            try (InputStream is = settingsSource.openStream()) {
                settings = request.getSession()
                        .getService(SettingsXmlFactory.class)
                        .read(XmlReaderRequest.builder()
                                .inputStream(is)
                                .location(settingsSource.getLocation())
                                .strict(true)
                                .build());
            } catch (XmlReaderException e) {
                try (InputStream is = settingsSource.openStream()) {
                    settings = request.getSession()
                            .getService(SettingsXmlFactory.class)
                            .read(XmlReaderRequest.builder()
                                    .inputStream(is)
                                    .location(settingsSource.getLocation())
                                    .strict(false)
                                    .build());
                    Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
                    problems.add(new DefaultBuilderProblem(
                            settingsSource.getLocation(),
                            loc != null ? loc.getLineNumber() : -1,
                            loc != null ? loc.getColumnNumber() : -1,
                            e,
                            e.getMessage(),
                            BuilderProblem.Severity.WARNING));
                }
            }
        } catch (XmlReaderException e) {
            Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
            problems.add(new DefaultBuilderProblem(
                    settingsSource.getLocation(),
                    loc != null ? loc.getLineNumber() : -1,
                    loc != null ? loc.getColumnNumber() : -1,
                    e,
                    "Non-parseable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return Settings.newInstance();
        } catch (IOException e) {
            problems.add(new DefaultBuilderProblem(
                    settingsSource.getLocation(),
                    -1,
                    -1,
                    e,
                    "Non-readable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return Settings.newInstance();
        }

        settings = interpolate(settings, request, problems);

        settingsValidator.validate(settings, isProjectSettings, problems);

        if (isProjectSettings) {
            settings = Settings.newBuilder(settings, true)
                    .localRepository(null)
                    .interactiveMode(false)
                    .offline(false)
                    .proxies(List.of())
                    .usePluginRegistry(false)
                    .servers(settings.getServers().stream()
                            .map(s -> Server.newBuilder(s, true)
                                    .username(null)
                                    .passphrase(null)
                                    .privateKey(null)
                                    .password(null)
                                    .filePermissions(null)
                                    .directoryPermissions(null)
                                    .build())
                            .toList())
                    .build();
        }

        return settings;
    }

    private Settings interpolate(Settings settings, SettingsBuilderRequest request, List<BuilderProblem> problems) {

        RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

        interpolator.addValueSource(new MapBasedValueSource(request.getSession().getUserProperties()));

        interpolator.addValueSource(new MapBasedValueSource(request.getSession().getSystemProperties()));

        try {
            interpolator.addValueSource(new EnvarBasedValueSource());
        } catch (IOException e) {
            problems.add(new DefaultBuilderProblem(
                    null,
                    -1,
                    -1,
                    e,
                    "Failed to use environment variables for interpolation: " + e.getMessage(),
                    BuilderProblem.Severity.WARNING));
        }

        return new SettingsTransformer(value -> {
                    try {
                        return value != null ? interpolator.interpolate(value) : null;
                    } catch (InterpolationException e) {
                        problems.add(new DefaultBuilderProblem(
                                null,
                                -1,
                                -1,
                                e,
                                "Failed to interpolate settings: " + e.getMessage(),
                                BuilderProblem.Severity.WARNING));
                        return value;
                    }
                })
                .visit(settings);
    }

    @Override
    public List<BuilderProblem> validate(Settings settings, boolean isProjectSettings) {
        ArrayList<BuilderProblem> problems = new ArrayList<>();
        settingsValidator.validate(settings, isProjectSettings, problems);
        return problems;
    }

    @Override
    public Profile convert(org.apache.maven.api.model.Profile profile) {
        return SettingsUtilsV4.convertToSettingsProfile(profile);
    }

    @Override
    public org.apache.maven.api.model.Profile convert(Profile profile) {
        return SettingsUtilsV4.convertFromSettingsProfile(profile);
    }

    /**
     * Collects the output of the settings builder.
     *
     */
    static class DefaultSettingsBuilderResult implements SettingsBuilderResult {

        private final Settings effectiveSettings;

        private final List<BuilderProblem> problems;

        DefaultSettingsBuilderResult(Settings effectiveSettings, List<BuilderProblem> problems) {
            this.effectiveSettings = effectiveSettings;
            this.problems = (problems != null) ? problems : new ArrayList<>();
        }

        @Override
        public Settings getEffectiveSettings() {
            return effectiveSettings;
        }

        @Override
        public List<BuilderProblem> getProblems() {
            return problems;
        }
    }
}
