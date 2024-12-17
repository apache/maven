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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.maven.api.Constants;
import org.apache.maven.api.ProtoSession;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.services.BuilderProblem;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.ProblemCollector;
import org.apache.maven.api.services.SettingsBuilder;
import org.apache.maven.api.services.SettingsBuilderException;
import org.apache.maven.api.services.SettingsBuilderRequest;
import org.apache.maven.api.services.SettingsBuilderResult;
import org.apache.maven.api.services.Source;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.XmlReaderException;
import org.apache.maven.api.services.xml.XmlReaderRequest;
import org.apache.maven.api.settings.Activation;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.RepositoryPolicy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.internal.impl.model.DefaultInterpolator;
import org.apache.maven.settings.v4.SettingsMerger;
import org.apache.maven.settings.v4.SettingsTransformer;
import org.codehaus.plexus.components.secdispatcher.Dispatcher;
import org.codehaus.plexus.components.secdispatcher.SecDispatcher;
import org.codehaus.plexus.components.secdispatcher.internal.DefaultSecDispatcher;

/**
 * Builds the effective settings from a user settings file and/or a global settings file.
 *
 */
@Named
public class DefaultSettingsBuilder implements SettingsBuilder {

    private final DefaultSettingsValidator settingsValidator = new DefaultSettingsValidator();

    private final SettingsMerger settingsMerger = new SettingsMerger();

    private final SettingsXmlFactory settingsXmlFactory;

    private final Interpolator interpolator;

    private final Map<String, Dispatcher> dispatchers;

    /**
     * This ctor is used in legacy components.
     */
    public DefaultSettingsBuilder() {
        this(new DefaultSettingsXmlFactory(), new DefaultInterpolator(), Map.of());
    }

    /**
     * In Maven4 the {@link SecDispatcher} is injected and build settings are fully decrypted as well.
     */
    @Inject
    public DefaultSettingsBuilder(
            SettingsXmlFactory settingsXmlFactory, Interpolator interpolator, Map<String, Dispatcher> dispatchers) {
        this.settingsXmlFactory = settingsXmlFactory;
        this.interpolator = interpolator;
        this.dispatchers = dispatchers;
    }

    @Override
    public SettingsBuilderResult build(SettingsBuilderRequest request) throws SettingsBuilderException {
        // TODO: config
        ProblemCollector<BuilderProblem> problems = new DefaultProblemCollector<>(100);

        Source installationSource = request.getInstallationSettingsSource().orElse(null);
        Settings installation = readSettings(installationSource, false, request, problems);

        Source projectSource = request.getProjectSettingsSource().orElse(null);
        Settings project = readSettings(projectSource, true, request, problems);

        Source userSource = request.getUserSettingsSource().orElse(null);
        Settings user = readSettings(userSource, false, request, problems);

        Settings effective =
                settingsMerger.merge(user, settingsMerger.merge(project, installation, false, null), false, null);

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

        if (problems.hasErrors()) {
            throw new SettingsBuilderException("Error building settings", problems);
        }

        return new DefaultSettingsBuilderResult(effective, problems);
    }

    private Settings readSettings(
            Source settingsSource,
            boolean isProjectSettings,
            SettingsBuilderRequest request,
            ProblemCollector<BuilderProblem> problems) {
        if (settingsSource == null) {
            return Settings.newInstance();
        }

        Settings settings;

        try {
            try (InputStream is = settingsSource.openStream()) {
                settings = settingsXmlFactory.read(XmlReaderRequest.builder()
                        .inputStream(is)
                        .location(settingsSource.getLocation())
                        .strict(true)
                        .build());
            } catch (XmlReaderException e) {
                try (InputStream is = settingsSource.openStream()) {
                    settings = settingsXmlFactory.read(XmlReaderRequest.builder()
                            .inputStream(is)
                            .location(settingsSource.getLocation())
                            .strict(false)
                            .build());
                    Location loc = e.getCause() instanceof XMLStreamException xe ? xe.getLocation() : null;
                    problems.reportProblem(new DefaultBuilderProblem(
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
            problems.reportProblem(new DefaultBuilderProblem(
                    settingsSource.getLocation(),
                    loc != null ? loc.getLineNumber() : -1,
                    loc != null ? loc.getColumnNumber() : -1,
                    e,
                    "Non-parseable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return Settings.newInstance();
        } catch (IOException e) {
            problems.reportProblem(new DefaultBuilderProblem(
                    settingsSource.getLocation(),
                    -1,
                    -1,
                    e,
                    "Non-readable settings " + settingsSource.getLocation() + ": " + e.getMessage(),
                    BuilderProblem.Severity.FATAL));
            return Settings.newInstance();
        }

        settings = interpolate(settings, request, problems);
        settings = decrypt(settingsSource, settings, request, problems);

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

    private Settings interpolate(
            Settings settings, SettingsBuilderRequest request, ProblemCollector<BuilderProblem> problems) {
        Function<String, String> src;
        if (request.getInterpolationSource().isPresent()) {
            src = request.getInterpolationSource().get();
        } else {
            Map<String, String> userProperties = request.getSession().getUserProperties();
            Map<String, String> systemProperties = request.getSession().getSystemProperties();
            src = Interpolator.chain(userProperties::get, systemProperties::get);
        }
        return new DefSettingsTransformer(value -> value != null ? interpolator.interpolate(value, src) : null)
                .visit(settings);
    }

    static class DefSettingsTransformer extends SettingsTransformer {
        DefSettingsTransformer(Function<String, String> transformer) {
            super(transformer);
        }

        @Override
        protected Activation.Builder transformActivation_Condition(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            // do not interpolate the condition activation
            return builder;
        }
    }

    private Settings decrypt(
            Source settingsSource,
            Settings settings,
            SettingsBuilderRequest request,
            ProblemCollector<BuilderProblem> problems) {
        if (dispatchers.isEmpty()) {
            return settings;
        }
        SecDispatcher secDispatcher = new DefaultSecDispatcher(dispatchers, getSecuritySettings(request.getSession()));
        final AtomicInteger preMaven4Passwords = new AtomicInteger(0);
        Function<String, String> decryptFunction = str -> {
            if (str != null && !str.isEmpty() && !str.contains("${") && secDispatcher.isAnyEncryptedString(str)) {
                if (secDispatcher.isLegacyEncryptedString(str)) {
                    // add a problem
                    preMaven4Passwords.incrementAndGet();
                }
                try {
                    return secDispatcher.decrypt(str);
                } catch (Exception e) {
                    problems.reportProblem(new DefaultBuilderProblem(
                            settingsSource.getLocation(),
                            -1,
                            -1,
                            e,
                            "Could not decrypt password (fix the corrupted password or remove it, if unused) " + str,
                            BuilderProblem.Severity.FATAL));
                }
            }
            return str;
        };
        Settings result = new SettingsTransformer(decryptFunction).visit(settings);
        if (preMaven4Passwords.get() > 0) {
            problems.reportProblem(new DefaultBuilderProblem(
                    settingsSource.getLocation(),
                    -1,
                    -1,
                    null,
                    "Detected " + preMaven4Passwords.get() + " pre-Maven 4 legacy encrypted password(s) "
                            + "- configure password encryption with the help of mvnenc for increased security.",
                    BuilderProblem.Severity.WARNING));
        }
        return result;
    }

    private Path getSecuritySettings(ProtoSession session) {
        Map<String, String> properties = session.getUserProperties();
        String settingsSecurity = properties.get(Constants.MAVEN_SETTINGS_SECURITY);
        if (settingsSecurity != null) {
            return Paths.get(settingsSecurity);
        }
        String mavenUserConf = properties.get(Constants.MAVEN_USER_CONF);
        if (mavenUserConf != null) {
            return Paths.get(mavenUserConf, Constants.MAVEN_SETTINGS_SECURITY_FILE_NAME);
        }
        return Paths.get(properties.get("user.home"), ".m2", Constants.MAVEN_SETTINGS_SECURITY_FILE_NAME);
    }

    @Override
    public ProblemCollector<BuilderProblem> validate(Settings settings, boolean isProjectSettings) {
        // TODO  config
        ProblemCollector<BuilderProblem> problems = new DefaultProblemCollector<>(100);
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

        private final ProblemCollector<BuilderProblem> problems;

        DefaultSettingsBuilderResult(Settings effectiveSettings, ProblemCollector<BuilderProblem> problems) {
            this.effectiveSettings = effectiveSettings;
            this.problems = (problems != null) ? problems : ProblemCollector.empty();
        }

        @Override
        public Settings getEffectiveSettings() {
            return effectiveSettings;
        }

        @Override
        public ProblemCollector<BuilderProblem> getProblems() {
            return problems;
        }
    }
}
