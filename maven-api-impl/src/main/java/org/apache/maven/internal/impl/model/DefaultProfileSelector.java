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
package org.apache.maven.internal.impl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
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
        Collection<String> activatedIds = new HashSet<>(context.getActiveProfileIds());
        Collection<String> deactivatedIds = new HashSet<>(context.getInactiveProfileIds());

        List<Profile> activeProfiles = new ArrayList<>(profiles.size());
        List<Profile> activePomProfilesByDefault = new ArrayList<>();
        boolean activatedPomProfileNotByDefault = false;

        for (Profile profile : profiles) {
            if (!deactivatedIds.contains(profile.getId())) {
                if (activatedIds.contains(profile.getId()) || isActive(profile, context, problems)) {
                    activeProfiles.add(profile);
                    if (Profile.SOURCE_POM.equals(profile.getSource())) {
                        activatedPomProfileNotByDefault = true;
                    }
                } else if (isActiveByDefault(profile)) {
                    if (Profile.SOURCE_POM.equals(profile.getSource())) {
                        activePomProfilesByDefault.add(profile);
                    } else {
                        activeProfiles.add(profile);
                    }
                }
            }
        }

        if (!activatedPomProfileNotByDefault) {
            activeProfiles.addAll(activePomProfilesByDefault);
        }

        return activeProfiles;
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
}
