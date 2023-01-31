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

import java.util.List;

import org.apache.maven.model.ActivationFile;
import org.apache.maven.settings.merge.MavenSettingsMerger;

/**
 * Several convenience methods to handle settings
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
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
        new MavenSettingsMerger().merge(dominant, recessive, recessiveSourceLevel);
    }

    /**
     * @param modelProfile
     * @return a profile
     */
    public static Profile convertToSettingsProfile(org.apache.maven.model.Profile modelProfile) {
        Profile profile = new Profile();

        profile.setId(modelProfile.getId());

        org.apache.maven.model.Activation modelActivation = modelProfile.getActivation();

        if (modelActivation != null) {
            Activation activation = new Activation();

            activation.setActiveByDefault(modelActivation.isActiveByDefault());

            activation.setJdk(modelActivation.getJdk());

            org.apache.maven.model.ActivationProperty modelProp = modelActivation.getProperty();

            if (modelProp != null) {
                ActivationProperty prop = new ActivationProperty();
                prop.setName(modelProp.getName());
                prop.setValue(modelProp.getValue());
                activation.setProperty(prop);
            }

            org.apache.maven.model.ActivationOS modelOs = modelActivation.getOs();

            if (modelOs != null) {
                ActivationOS os = new ActivationOS();

                os.setArch(modelOs.getArch());
                os.setFamily(modelOs.getFamily());
                os.setName(modelOs.getName());
                os.setVersion(modelOs.getVersion());

                activation.setOs(os);
            }

            ActivationFile modelFile = modelActivation.getFile();

            if (modelFile != null) {
                org.apache.maven.settings.ActivationFile file = new org.apache.maven.settings.ActivationFile();

                file.setExists(modelFile.getExists());
                file.setMissing(modelFile.getMissing());

                activation.setFile(file);
            }

            profile.setActivation(activation);
        }

        profile.setProperties(modelProfile.getProperties());

        List<org.apache.maven.model.Repository> repos = modelProfile.getRepositories();
        if (repos != null) {
            for (org.apache.maven.model.Repository repo : repos) {
                profile.addRepository(convertToSettingsRepository(repo));
            }
        }

        List<org.apache.maven.model.Repository> pluginRepos = modelProfile.getPluginRepositories();
        if (pluginRepos != null) {
            for (org.apache.maven.model.Repository pluginRepo : pluginRepos) {
                profile.addPluginRepository(convertToSettingsRepository(pluginRepo));
            }
        }

        return profile;
    }

    /**
     * @param settingsProfile
     * @return a profile
     */
    public static org.apache.maven.model.Profile convertFromSettingsProfile(Profile settingsProfile) {
        org.apache.maven.model.Profile profile = new org.apache.maven.model.Profile();

        profile.setId(settingsProfile.getId());

        profile.setSource("settings.xml");

        Activation settingsActivation = settingsProfile.getActivation();

        if (settingsActivation != null) {
            org.apache.maven.model.Activation activation = new org.apache.maven.model.Activation();

            activation.setActiveByDefault(settingsActivation.isActiveByDefault());

            activation.setJdk(settingsActivation.getJdk());

            ActivationProperty settingsProp = settingsActivation.getProperty();

            if (settingsProp != null) {
                org.apache.maven.model.ActivationProperty prop = new org.apache.maven.model.ActivationProperty();

                prop.setName(settingsProp.getName());
                prop.setValue(settingsProp.getValue());

                activation.setProperty(prop);
            }

            ActivationOS settingsOs = settingsActivation.getOs();

            if (settingsOs != null) {
                org.apache.maven.model.ActivationOS os = new org.apache.maven.model.ActivationOS();

                os.setArch(settingsOs.getArch());
                os.setFamily(settingsOs.getFamily());
                os.setName(settingsOs.getName());
                os.setVersion(settingsOs.getVersion());

                activation.setOs(os);
            }

            org.apache.maven.settings.ActivationFile settingsFile = settingsActivation.getFile();

            if (settingsFile != null) {
                ActivationFile file = new ActivationFile();

                file.setExists(settingsFile.getExists());
                file.setMissing(settingsFile.getMissing());

                activation.setFile(file);
            }

            profile.setActivation(activation);
        }

        profile.setProperties(settingsProfile.getProperties());

        List<Repository> repos = settingsProfile.getRepositories();
        if (repos != null) {
            for (Repository repo : repos) {
                profile.addRepository(convertFromSettingsRepository(repo));
            }
        }

        List<Repository> pluginRepos = settingsProfile.getPluginRepositories();
        if (pluginRepos != null) {
            for (Repository pluginRepo : pluginRepos) {
                profile.addPluginRepository(convertFromSettingsRepository(pluginRepo));
            }
        }

        return profile;
    }

    /**
     * @param settingsRepo
     * @return a repository
     */
    private static org.apache.maven.model.Repository convertFromSettingsRepository(Repository settingsRepo) {
        org.apache.maven.model.Repository repo = new org.apache.maven.model.Repository();

        repo.setId(settingsRepo.getId());
        repo.setLayout(settingsRepo.getLayout());
        repo.setName(settingsRepo.getName());
        repo.setUrl(settingsRepo.getUrl());

        if (settingsRepo.getSnapshots() != null) {
            repo.setSnapshots(convertRepositoryPolicy(settingsRepo.getSnapshots()));
        }
        if (settingsRepo.getReleases() != null) {
            repo.setReleases(convertRepositoryPolicy(settingsRepo.getReleases()));
        }

        return repo;
    }

    /**
     * @param settingsPolicy
     * @return a RepositoryPolicy
     */
    private static org.apache.maven.model.RepositoryPolicy convertRepositoryPolicy(RepositoryPolicy settingsPolicy) {
        org.apache.maven.model.RepositoryPolicy policy = new org.apache.maven.model.RepositoryPolicy();
        policy.setEnabled(settingsPolicy.isEnabled());
        policy.setUpdatePolicy(settingsPolicy.getUpdatePolicy());
        policy.setChecksumPolicy(settingsPolicy.getChecksumPolicy());
        return policy;
    }

    /**
     * @param modelRepo
     * @return a repository
     */
    private static Repository convertToSettingsRepository(org.apache.maven.model.Repository modelRepo) {
        Repository repo = new Repository();

        repo.setId(modelRepo.getId());
        repo.setLayout(modelRepo.getLayout());
        repo.setName(modelRepo.getName());
        repo.setUrl(modelRepo.getUrl());

        if (modelRepo.getSnapshots() != null) {
            repo.setSnapshots(convertRepositoryPolicy(modelRepo.getSnapshots()));
        }
        if (modelRepo.getReleases() != null) {
            repo.setReleases(convertRepositoryPolicy(modelRepo.getReleases()));
        }

        return repo;
    }

    /**
     * @param modelPolicy
     * @return a RepositoryPolicy
     */
    private static RepositoryPolicy convertRepositoryPolicy(org.apache.maven.model.RepositoryPolicy modelPolicy) {
        RepositoryPolicy policy = new RepositoryPolicy();
        policy.setEnabled(modelPolicy.isEnabled());
        policy.setUpdatePolicy(modelPolicy.getUpdatePolicy());
        policy.setChecksumPolicy(modelPolicy.getChecksumPolicy());
        return policy;
    }

    /**
     * @param settings could be null
     * @return a new instance of settings or null if settings was null.
     */
    public static Settings copySettings(Settings settings) {
        if (settings == null) {
            return null;
        }

        Settings clone = new Settings();
        clone.setActiveProfiles(settings.getActiveProfiles());
        clone.setInteractiveMode(settings.isInteractiveMode());
        clone.setLocalRepository(settings.getLocalRepository());
        clone.setMirrors(settings.getMirrors());
        clone.setModelEncoding(settings.getModelEncoding());
        clone.setOffline(settings.isOffline());
        clone.setPluginGroups(settings.getPluginGroups());
        clone.setProfiles(settings.getProfiles());
        clone.setProxies(settings.getProxies());
        clone.setServers(settings.getServers());
        clone.setSourceLevel(settings.getSourceLevel());
        clone.setUsePluginRegistry(settings.isUsePluginRegistry());

        return clone;
    }
}
