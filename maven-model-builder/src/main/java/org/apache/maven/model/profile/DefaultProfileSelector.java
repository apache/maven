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
package org.apache.maven.model.profile;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.activation.ProfileActivator;

/**
 * Calculates the active profiles among a given collection of profiles.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultProfileSelector implements ProfileSelector {

    @Inject
    private List<ProfileActivator> activators = new ArrayList<>();

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
            }
        }
        for (ProfileActivator activator : activators) {
            try {
                if (activator.presentInConfig(profile, context, problems)) {
                    isActive &= activator.isActive(profile, context, problems);
                }
            } catch (RuntimeException e) {
                problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                        .setMessage(
                                "Failed to determine activation for profile " + profile.getId() + ": " + e.getMessage())
                        .setLocation(profile.getLocation(""))
                        .setException(e));
                return false;
            }
        }
        return isActive;
    }

    private boolean isActiveByDefault(Profile profile) {
        Activation activation = profile.getActivation();
        return activation != null && activation.isActiveByDefault();
    }
}
