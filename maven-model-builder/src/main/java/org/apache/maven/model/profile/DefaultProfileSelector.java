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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.profile.activation.ProfileActivator;

/**
 * Calculates the active profiles among a given collection of profiles.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultProfileSelector implements ProfileSelector {

    private static Properties asProperties(Map<String, String> m) {
        return m.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (l, r) -> r, Properties::new));
    }

    @Inject
    private List<ProfileActivator> activators = new ArrayList<>();

    @Inject
    private ModelInterpolator interpolator = new ModelInterpolator() {

        @Override
        public Model interpolateModel(
                Model model, File projectDir, ModelBuildingRequest request, ModelProblemCollector problems) {
            return model;
        }
    };

    public DefaultProfileSelector addProfileActivator(ProfileActivator profileActivator) {
        if (profileActivator != null) {
            activators.add(profileActivator);
        }
        return this;
    }

    public void setInterpolator(ModelInterpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public List<Profile> getActiveProfiles(
            Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {

        if (profiles.stream().map(Profile::getId).distinct().count() < profiles.size()) {
            // invalid profile specification
            return Collections.emptyList();
        }
        Collection<String> activatedIds = new HashSet<>(context.getActiveProfileIds());
        Collection<String> deactivatedIds = new HashSet<>(context.getInactiveProfileIds());

        List<Profile> activeProfiles = new ArrayList<>(profiles.size());
        List<Profile> activePomProfilesByDefault = new ArrayList<>();
        boolean activatedPomProfileNotByDefault = false;

        Map<String, Profile> activation = earlyInterpolateProfileActivations(profiles, context);

        for (Profile profile : profiles) {
            if (!deactivatedIds.contains(profile.getId())) {
                if (activatedIds.contains(profile.getId())
                        || isActive(activation.get(profile.getId()), context, problems)) {
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

    private Map<String, Profile> earlyInterpolateProfileActivations(
            Collection<Profile> original, ProfileActivationContext context) {

        Model model = new Model();

        UnaryOperator<Profile> activatableProfile = p -> {
            Profile result = new Profile();
            result.setId(p.getId());
            result.setActivation(p.getActivation());
            return result;
        };
        model.setProfiles(original.stream().map(activatableProfile).collect(Collectors.toList()));

        ModelBuildingRequest mbr = new DefaultModelBuildingRequest()
                .setActiveProfileIds(context.getActiveProfileIds())
                .setInactiveProfileIds(context.getInactiveProfileIds())
                .setRawModel(model)
                .setSystemProperties(asProperties(context.getSystemProperties()))
                .setUserProperties(asProperties(context.getUserProperties()))
                .setTwoPhaseBuilding(true)
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        interpolator
                .interpolateModel(model, context.getProjectDirectory(), mbr, problem -> {})
                .getProfiles();

        return model.getProfiles().stream().collect(Collectors.toMap(Profile::getId, UnaryOperator.identity()));
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
                        .setMessage("Failed to determine activation for profile " + profile.getId())
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
