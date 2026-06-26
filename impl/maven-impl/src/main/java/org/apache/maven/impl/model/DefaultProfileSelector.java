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
package org.apache.maven.impl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.api.services.model.ProfileSelector;

/**
 * Calculates the active profiles among a given collection of profiles.
 *
 */
@Named
@Singleton
public class DefaultProfileSelector implements ProfileSelector {

    private final List<ProfileActivator> activators;

    public DefaultProfileSelector() {
        this.activators = new ArrayList<>();
    }

    @Inject
    public DefaultProfileSelector(List<ProfileActivator> activators) {
        this.activators = new ArrayList<>(activators);
    }

    public DefaultProfileSelector addProfileActivator(ProfileActivator profileActivator) {
        if (profileActivator != null) {
            activators.add(profileActivator);
        }
        return this;
    }

    @Override
    public List<Profile> getActiveProfiles(
            Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {

        List<Profile> activeSettingsProfiles = new ArrayList<>();
        List<Profile> activePomProfiles = new ArrayList<>();
        List<Profile> activePomProfilesByDefault = new ArrayList<>();

        // Cascading mode: iterate until no more profiles are activated
        List<Profile> remainingProfiles = new ArrayList<>(profiles);
        List<Profile> activatedProfiles;
        do {
            activatedProfiles = new ArrayList<>();
            for (Profile profile : List.copyOf(remainingProfiles)) {
                if (!context.isProfileInactive(profile.getId())) {
                    boolean activated = context.isProfileActive(profile.getId());
                    boolean active = isActive(profile, context, problems);
                    boolean activeByDefault = isActiveByDefault(profile);
                    if (activated || active || activeByDefault) {
                        if (isFromPom(profile)) {
                            if (activated || active) {
                                activePomProfiles.add(profile);
                            } else {
                                activePomProfilesByDefault.add(profile);
                            }
                        } else {
                            activeSettingsProfiles.add(profile);
                        }
                        remainingProfiles.remove(profile);
                        if (active) {
                            activatedProfiles.add(profile);
                        }
                    }
                }
            }
            // Add profile properties for cascading activation
            context.addProfileProperties(activatedProfiles);
        } while (!activatedProfiles.isEmpty());

        // Clear cascaded properties to prevent them from leaking into subsequent uses of the context
        context.clearCascadedProfileProperties();

        List<Profile> allActivated = new ArrayList<>();
        if (activePomProfiles.isEmpty()) {
            allActivated.addAll(activePomProfilesByDefault);
        } else {
            allActivated.addAll(activePomProfiles);
        }
        allActivated.addAll(activeSettingsProfiles);

        return allActivated;
    }

    private boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        boolean isActive = false;
        for (ProfileActivator activator : activators) {
            if (activator.presentInConfig(profile, context, problems)) {
                isActive = true;
                try {
                    if (!activator.isActive(profile, context, problems)) {
                        return false;
                    }
                } catch (RuntimeException e) {
                    problems.add(
                            Severity.ERROR,
                            Version.BASE,
                            "Failed to determine activation for profile " + profile.getId() + ": " + e.getMessage(),
                            profile.getLocation(""),
                            e);
                    return false;
                }
            }
        }
        return isActive;
    }

    private boolean isActiveByDefault(Profile profile) {
        Activation activation = profile.getActivation();
        return activation != null && activation.isActiveByDefault();
    }

    /**
     * Determines whether the given profile originates from a POM file
     * by checking if the profile's {@link InputSource} has a non-empty model id.
     * Profiles from settings.xml or other external sources will have
     * a {@code null} or empty model id.
     * Defaults to {@code true} if no location information is available,
     * matching the previous default behavior.
     */
    private boolean isFromPom(Profile profile) {
        InputLocation location = profile.getLocation("");
        if (location != null) {
            InputSource source = location.getSource();
            if (source != null) {
                return source.getModelId() != null && !source.getModelId().isEmpty();
            }
        }
        return true;
    }
}
