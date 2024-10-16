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

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.settings.Activation;
import org.apache.maven.api.settings.ActivationOS;
import org.apache.maven.api.settings.ActivationProperty;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Repository;
import org.apache.maven.api.settings.RepositoryPolicy;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.v4.SettingsMerger;

/**
 * Several convenience methods to handle settings
 *
 */
public final class SettingsUtilsV4 {

    private SettingsUtilsV4() {
        // don't allow construction.
    }

    /**
     * @param dominant
     * @param recessive
     */
    public static Settings merge(Settings dominant, Settings recessive) {
        return new SettingsMerger().merge(dominant, recessive, true, Collections.emptyMap());
    }

    /**
     * @param modelProfile
     * @return a profile
     */
    public static Profile convertToSettingsProfile(org.apache.maven.api.model.Profile modelProfile) {
        Profile.Builder profile = Profile.newBuilder();

        profile.id(modelProfile.getId());

        org.apache.maven.api.model.Activation modelActivation = modelProfile.getActivation();

        if (modelActivation != null) {
            Activation.Builder activation = Activation.newBuilder();

            activation.activeByDefault(modelActivation.isActiveByDefault());

            activation.jdk(modelActivation.getJdk());

            org.apache.maven.api.model.ActivationProperty modelProp = modelActivation.getProperty();

            if (modelProp != null) {
                ActivationProperty prop = ActivationProperty.newBuilder()
                        .name(modelProp.getName())
                        .value(modelProp.getValue())
                        .build();
                activation.property(prop);
            }

            org.apache.maven.api.model.ActivationOS modelOs = modelActivation.getOs();

            if (modelOs != null) {
                ActivationOS os = ActivationOS.newBuilder()
                        .arch(modelOs.getArch())
                        .family(modelOs.getFamily())
                        .name(modelOs.getName())
                        .version(modelOs.getVersion())
                        .build();

                activation.os(os);
            }

            org.apache.maven.api.model.ActivationFile modelFile = modelActivation.getFile();

            if (modelFile != null) {
                org.apache.maven.api.settings.ActivationFile file =
                        org.apache.maven.api.settings.ActivationFile.newBuilder()
                                .exists(modelFile.getExists())
                                .missing(modelFile.getMissing())
                                .build();

                activation.file(file);
            }

            activation.packaging(modelActivation.getPackaging());

            profile.activation(activation.build());
        }

        profile.properties(modelProfile.getProperties().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(), e -> e.getValue().toString())));

        List<org.apache.maven.api.model.Repository> repos = modelProfile.getRepositories();
        if (repos != null) {
            List<Repository> repositories = new ArrayList<>();
            for (org.apache.maven.api.model.Repository repo : repos) {
                repositories.add(convertToSettingsRepository(repo));
            }
            profile.repositories(repositories);
        }

        List<org.apache.maven.api.model.Repository> pluginRepos = modelProfile.getPluginRepositories();
        if (pluginRepos != null) {
            List<Repository> repositories = new ArrayList<>();
            for (org.apache.maven.api.model.Repository pluginRepo : pluginRepos) {
                repositories.add(convertToSettingsRepository(pluginRepo));
            }
            profile.pluginRepositories(repositories);
        }

        return profile.build();
    }

    /**
     * @param settingsProfile
     * @return a profile
     */
    public static org.apache.maven.api.model.Profile convertFromSettingsProfile(Profile settingsProfile) {
        org.apache.maven.api.model.Profile.Builder profile = org.apache.maven.api.model.Profile.newBuilder();

        profile.id(settingsProfile.getId());

        Activation settingsActivation = settingsProfile.getActivation();

        if (settingsActivation != null) {
            org.apache.maven.api.model.Activation.Builder activation =
                    org.apache.maven.api.model.Activation.newBuilder();

            activation.activeByDefault(settingsActivation.isActiveByDefault());
            activation.location("activeByDefault", toLocation(settingsActivation.getLocation("activeByDefault")));

            activation.jdk(settingsActivation.getJdk());
            activation.location("jdk", toLocation(settingsActivation.getLocation("jdk")));

            ActivationProperty settingsProp = settingsActivation.getProperty();
            if (settingsProp != null) {
                activation.property(org.apache.maven.api.model.ActivationProperty.newBuilder()
                        .name(settingsProp.getName())
                        .value(settingsProp.getValue())
                        .location("name", toLocation(settingsProp.getLocation("name")))
                        .location("value", toLocation(settingsProp.getLocation("value")))
                        .build());
            }

            ActivationOS settingsOs = settingsActivation.getOs();
            if (settingsOs != null) {
                activation.os(org.apache.maven.api.model.ActivationOS.newBuilder()
                        .arch(settingsOs.getArch())
                        .family(settingsOs.getFamily())
                        .name(settingsOs.getName())
                        .version(settingsOs.getVersion())
                        .location("arch", toLocation(settingsOs.getLocation("arch")))
                        .location("family", toLocation(settingsOs.getLocation("family")))
                        .location("name", toLocation(settingsOs.getLocation("name")))
                        .location("version", toLocation(settingsOs.getLocation("version")))
                        .build());
            }

            org.apache.maven.api.settings.ActivationFile settingsFile = settingsActivation.getFile();
            if (settingsFile != null) {
                activation.file(ActivationFile.newBuilder()
                        .exists(settingsFile.getExists())
                        .missing(settingsFile.getMissing())
                        .location("exists", toLocation(settingsFile.getLocation("exists")))
                        .location("missing", toLocation(settingsFile.getLocation("missing")))
                        .build());
            }

            activation.packaging(settingsActivation.getPackaging());

            profile.activation(activation.build());
        }

        profile.properties(settingsProfile.getProperties());
        profile.location("properties", toLocation(settingsProfile.getLocation("properties")));

        List<Repository> repos = settingsProfile.getRepositories();
        if (repos != null) {
            profile.repositories(repos.stream()
                    .map(SettingsUtilsV4::convertFromSettingsRepository)
                    .collect(Collectors.toList()));
        }

        List<Repository> pluginRepos = settingsProfile.getPluginRepositories();
        if (pluginRepos != null) {
            profile.pluginRepositories(pluginRepos.stream()
                    .map(SettingsUtilsV4::convertFromSettingsRepository)
                    .collect(Collectors.toList()));
        }

        org.apache.maven.api.model.Profile value = profile.build();
        value.setSource("settings.xml");
        return value;
    }

    /**
     * @param settingsRepo
     * @return a repository
     */
    private static org.apache.maven.api.model.Repository convertFromSettingsRepository(Repository settingsRepo) {
        org.apache.maven.api.model.Repository.Builder repo = org.apache.maven.api.model.Repository.newBuilder();

        repo.id(settingsRepo.getId());
        repo.layout(settingsRepo.getLayout());
        repo.name(settingsRepo.getName());
        repo.url(settingsRepo.getUrl());

        repo.location("id", toLocation(settingsRepo.getLocation("id")));
        repo.location("layout", toLocation(settingsRepo.getLocation("layout")));
        repo.location("name", toLocation(settingsRepo.getLocation("name")));
        repo.location("url", toLocation(settingsRepo.getLocation("url")));

        if (settingsRepo.getSnapshots() != null) {
            repo.snapshots(convertRepositoryPolicy(settingsRepo.getSnapshots()));
        }
        if (settingsRepo.getReleases() != null) {
            repo.releases(convertRepositoryPolicy(settingsRepo.getReleases()));
        }

        return repo.build();
    }

    /**
     * @param settingsPolicy
     * @return a RepositoryPolicy
     */
    private static org.apache.maven.api.model.RepositoryPolicy convertRepositoryPolicy(
            RepositoryPolicy settingsPolicy) {
        org.apache.maven.api.model.RepositoryPolicy policy = org.apache.maven.api.model.RepositoryPolicy.newBuilder()
                .enabled(Boolean.toString(settingsPolicy.isEnabled()))
                .updatePolicy(settingsPolicy.getUpdatePolicy())
                .checksumPolicy(settingsPolicy.getChecksumPolicy())
                .location("enabled", toLocation(settingsPolicy.getLocation("enabled")))
                .location("updatePolicy", toLocation(settingsPolicy.getLocation("updatePolicy")))
                .location("checksumPolicy", toLocation(settingsPolicy.getLocation("checksumPolicy")))
                .build();
        return policy;
    }

    /**
     * @param modelRepo
     * @return a repository
     */
    private static Repository convertToSettingsRepository(org.apache.maven.api.model.Repository modelRepo) {
        Repository repo = Repository.newBuilder()
                .id(modelRepo.getId())
                .layout(modelRepo.getLayout())
                .name(modelRepo.getName())
                .url(modelRepo.getUrl())
                .snapshots(modelRepo.getSnapshots() != null ? convertRepositoryPolicy(modelRepo.getSnapshots()) : null)
                .releases(modelRepo.getReleases() != null ? convertRepositoryPolicy(modelRepo.getReleases()) : null)
                .build();

        return repo;
    }

    /**
     * @param modelPolicy
     * @return a RepositoryPolicy
     */
    private static RepositoryPolicy convertRepositoryPolicy(org.apache.maven.api.model.RepositoryPolicy modelPolicy) {
        RepositoryPolicy policy = RepositoryPolicy.newBuilder()
                .enabled(modelPolicy.isEnabled())
                .updatePolicy(modelPolicy.getUpdatePolicy())
                .checksumPolicy(modelPolicy.getChecksumPolicy())
                .build();
        return policy;
    }

    private static org.apache.maven.api.model.InputLocation toLocation(
            org.apache.maven.api.settings.InputLocation location) {
        if (location != null) {
            org.apache.maven.api.settings.InputSource source = location.getSource();
            Map<Object, InputLocation> locs = location.getLocations().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> toLocation(e.getValue())));
            return new org.apache.maven.api.model.InputLocation(
                    location.getLineNumber(),
                    location.getColumnNumber(),
                    source != null ? new org.apache.maven.api.model.InputSource("", source.getLocation()) : null,
                    locs);
        } else {
            return null;
        }
    }
}
