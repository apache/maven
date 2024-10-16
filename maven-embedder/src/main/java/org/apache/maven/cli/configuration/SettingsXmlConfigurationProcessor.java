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
package org.apache.maven.cli.configuration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.bridge.MavenRepositorySystem;
import org.apache.maven.building.Source;
import org.apache.maven.cli.CLIManager;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.cli.ResolveFile.resolveFile;

/**
 * SettingsXmlConfigurationProcessor
 */
@Named(SettingsXmlConfigurationProcessor.HINT)
@Singleton
public class SettingsXmlConfigurationProcessor implements ConfigurationProcessor {
    public static final String HINT = "settings";

    public static final String USER_HOME = System.getProperty("user.home");

    public static final File USER_MAVEN_CONFIGURATION_HOME = new File(USER_HOME, ".m2");

    public static final File DEFAULT_USER_SETTINGS_FILE = new File(USER_MAVEN_CONFIGURATION_HOME, "settings.xml");

    public static final File DEFAULT_PROJECT_SETTINGS_FILE = new File(".mvn", "settings.xml");

    public static final File DEFAULT_INSTALLATION_SETTINGS_FILE =
            new File(System.getProperty("maven.conf"), "settings.xml");

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsXmlConfigurationProcessor.class);

    private final SettingsBuilder settingsBuilder;
    private final SettingsDecrypter settingsDecrypter;

    @Inject
    public SettingsXmlConfigurationProcessor(SettingsBuilder settingsBuilder, SettingsDecrypter settingsDecrypter) {
        this.settingsBuilder = settingsBuilder;
        this.settingsDecrypter = settingsDecrypter;
    }

    @Override
    public void process(CliRequest cliRequest) throws Exception {
        CommandLine commandLine = cliRequest.getCommandLine();
        String workingDirectory = cliRequest.getWorkingDirectory();
        MavenExecutionRequest request = cliRequest.getRequest();

        File userSettingsFile;

        if (commandLine.hasOption(CLIManager.ALTERNATE_USER_SETTINGS)) {
            userSettingsFile = new File(commandLine.getOptionValue(CLIManager.ALTERNATE_USER_SETTINGS));
            userSettingsFile = resolveFile(userSettingsFile, workingDirectory);

            if (!userSettingsFile.isFile()) {
                throw new FileNotFoundException("The specified user settings file does not exist: " + userSettingsFile);
            }
        } else {
            userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        }

        File projectSettingsFile;

        if (commandLine.hasOption(CLIManager.ALTERNATE_PROJECT_SETTINGS)) {
            projectSettingsFile = new File(commandLine.getOptionValue(CLIManager.ALTERNATE_PROJECT_SETTINGS));
            projectSettingsFile = resolveFile(projectSettingsFile, workingDirectory);

            if (!projectSettingsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified project settings file does not exist: " + projectSettingsFile);
            }
        } else if (cliRequest.getRootDirectory() != null) {
            projectSettingsFile = DEFAULT_PROJECT_SETTINGS_FILE;
            projectSettingsFile = resolveFile(
                    projectSettingsFile, cliRequest.getRootDirectory().toString());
        } else {
            projectSettingsFile = null;
        }

        File installationSettingsFile;

        if (commandLine.hasOption(CLIManager.ALTERNATE_INSTALLATION_SETTINGS)) {
            installationSettingsFile = new File(commandLine.getOptionValue(CLIManager.ALTERNATE_INSTALLATION_SETTINGS));
            installationSettingsFile = resolveFile(installationSettingsFile, workingDirectory);

            if (!installationSettingsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified installation settings file does not exist: " + installationSettingsFile);
            }
        } else if (commandLine.hasOption(CLIManager.ALTERNATE_GLOBAL_SETTINGS)) {
            installationSettingsFile = new File(commandLine.getOptionValue(CLIManager.ALTERNATE_GLOBAL_SETTINGS));
            installationSettingsFile = resolveFile(installationSettingsFile, workingDirectory);

            if (!installationSettingsFile.isFile()) {
                throw new FileNotFoundException(
                        "The specified installation settings file does not exist: " + installationSettingsFile);
            }
        } else {
            installationSettingsFile = DEFAULT_INSTALLATION_SETTINGS_FILE;
        }

        request.setInstallationSettingsFile(installationSettingsFile);
        request.setProjectSettingsFile(projectSettingsFile);
        request.setUserSettingsFile(userSettingsFile);

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile(installationSettingsFile);
        settingsRequest.setProjectSettingsFile(projectSettingsFile);
        settingsRequest.setUserSettingsFile(userSettingsFile);
        settingsRequest.setSystemProperties(cliRequest.getSystemProperties());
        Properties props = cliRequest.getUserProperties();
        if (cliRequest.getRootDirectory() != null) {
            props = new Properties();
            props.putAll(cliRequest.getUserProperties());
            props.put("session.rootDirectory", cliRequest.getRootDirectory().toString());
        }
        settingsRequest.setUserProperties(props);

        if (request.getEventSpyDispatcher() != null) {
            request.getEventSpyDispatcher().onEvent(settingsRequest);
        }

        LOGGER.debug(
                "Reading installation settings from '{}'",
                getLocation(settingsRequest.getGlobalSettingsSource(), settingsRequest.getGlobalSettingsFile()));
        LOGGER.debug(
                "Reading project settings from '{}'",
                getLocation(settingsRequest.getProjectSettingsSource(), settingsRequest.getProjectSettingsFile()));
        LOGGER.debug(
                "Reading user settings from '{}'",
                getLocation(settingsRequest.getUserSettingsSource(), settingsRequest.getUserSettingsFile()));

        SettingsBuildingResult settingsResult = settingsBuilder.build(settingsRequest);

        if (request.getEventSpyDispatcher() != null) {
            request.getEventSpyDispatcher().onEvent(settingsResult);
        }

        populateFromSettings(request, settingsResult.getEffectiveSettings());

        if (!settingsResult.getProblems().isEmpty() && LOGGER.isWarnEnabled()) {
            LOGGER.warn("");
            LOGGER.warn("Some problems were encountered while building the effective settings");

            for (SettingsProblem problem : settingsResult.getProblems()) {
                LOGGER.warn("{} @ {}", problem.getMessage(), problem.getLocation());
            }
            LOGGER.warn("");
        }
    }

    private MavenExecutionRequest populateFromSettings(MavenExecutionRequest request, Settings settings)
            throws MavenExecutionRequestPopulationException {
        if (settings == null) {
            return request;
        }

        request.setOffline(settings.isOffline());

        request.setInteractiveMode(settings.isInteractiveMode());

        request.setPluginGroups(settings.getPluginGroups());

        request.setLocalRepositoryPath(settings.getLocalRepository());

        for (Server server : settings.getServers()) {
            request.addServer(server);
        }

        //  <proxies>
        //    <proxy>
        //      <active>true</active>
        //      <protocol>http</protocol>
        //      <host>proxy.somewhere.com</host>
        //      <port>8080</port>
        //      <username>proxyuser</username>
        //      <password>somepassword</password>
        //      <nonProxyHosts>www.google.com|*.somewhere.com</nonProxyHosts>
        //    </proxy>
        //  </proxies>

        for (Proxy proxy : settings.getProxies()) {
            if (!proxy.isActive()) {
                continue;
            }

            request.addProxy(proxy);
        }

        // <mirrors>
        //   <mirror>
        //     <id>nexus</id>
        //     <mirrorOf>*</mirrorOf>
        //     <url>http://repository.sonatype.org/content/groups/public</url>
        //   </mirror>
        // </mirrors>

        for (Mirror mirror : settings.getMirrors()) {
            request.addMirror(mirror);
        }

        for (Repository remoteRepository : settings.getRepositories()) {
            try {
                request.addRemoteRepository(MavenRepositorySystem.buildArtifactRepository(remoteRepository));
            } catch (InvalidRepositoryException e) {
                // do nothing for now
            }
        }

        for (Repository pluginRepository : settings.getPluginRepositories()) {
            try {
                request.addPluginArtifactRepository(MavenRepositorySystem.buildArtifactRepository(pluginRepository));
            } catch (InvalidRepositoryException e) {
                // do nothing for now
            }
        }

        request.setActiveProfiles(settings.getActiveProfiles());

        for (Profile rawProfile : settings.getProfiles()) {
            request.addProfile(SettingsUtils.convertFromSettingsProfile(rawProfile));

            if (settings.getActiveProfiles().contains(rawProfile.getId())) {
                List<Repository> remoteRepositories = rawProfile.getRepositories();
                for (Repository remoteRepository : remoteRepositories) {
                    try {
                        request.addRemoteRepository(MavenRepositorySystem.buildArtifactRepository(remoteRepository));
                    } catch (InvalidRepositoryException e) {
                        // do nothing for now
                    }
                }

                List<Repository> pluginRepositories = rawProfile.getPluginRepositories();
                for (Repository pluginRepository : pluginRepositories) {
                    try {
                        request.addPluginArtifactRepository(
                                MavenRepositorySystem.buildArtifactRepository(pluginRepository));
                    } catch (InvalidRepositoryException e) {
                        // do nothing for now
                    }
                }
            }
        }
        return request;
    }

    private Object getLocation(Source source, File defaultLocation) {
        if (source != null) {
            return source.getLocation();
        }
        return defaultLocation;
    }
}
