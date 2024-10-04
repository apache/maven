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
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.InterpolatorException;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.api.services.model.ProfileSelector;
import org.apache.maven.model.v4.MavenTransformer;

/**
 * Calculates the active profiles among a given collection of profiles.
 */
@Named
@Singleton
public class DefaultProfileSelector implements ProfileSelector {

    private final Interpolator interpolator;
    private final ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator;
    private final List<ProfileActivator> activators;

    public DefaultProfileSelector(
            Interpolator interpolator, ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator) {
        this(interpolator, profileActivationFilePathInterpolator, new ArrayList<>());
    }

    @Inject
    public DefaultProfileSelector(
            Interpolator interpolator,
            ProfileActivationFilePathInterpolator profileActivationFilePathInterpolator,
            List<ProfileActivator> activators) {
        this.interpolator = interpolator;
        this.profileActivationFilePathInterpolator = profileActivationFilePathInterpolator;
        this.activators = new ArrayList<>(activators);
    }

    @Override
    public List<Profile> getActiveProfiles(
            Collection<Profile> orgProfiles,
            ProfileActivationContext context,
            ModelProblemCollector problems,
            boolean cascade) {

        if (cascade) {
            return getActiveProfilesCascading(orgProfiles, context, problems);
        } else {
            return getActiveProfilesNonCascading(orgProfiles, context, problems);
        }
    }

    public List<Profile> getActiveProfilesNonCascading(
            Collection<Profile> profiles, ProfileActivationContext context, ModelProblemCollector problems) {

        Collection<String> activatedIds = new HashSet<>(context.getActiveProfileIds());
        Collection<String> deactivatedIds = new HashSet<>(context.getInactiveProfileIds());

        List<Profile> activeSettingsProfiles = new ArrayList<>();
        List<Profile> activePomProfiles = new ArrayList<>();
        List<Profile> activePomProfilesByDefault = new ArrayList<>();

        ProfileActivationInterpolator activationInterpolator = new ProfileActivationInterpolator(context, problems);
        for (String source : List.of(Profile.SOURCE_SETTINGS, Profile.SOURCE_POM)) {
            // Iterate over the profiles and check if a given profile is activated
            List<Profile> activatedProfiles = new ArrayList<>();
            for (Profile profile : profiles) {
                if (Objects.equals(source, profile.getSource())) {
                    Profile iprofile = activationInterpolator.apply(profile);
                    if (!deactivatedIds.contains(iprofile.getId())) {
                        boolean activated = activatedIds.contains(iprofile.getId());
                        boolean active = isActive(iprofile, context, problems);
                        boolean activeByDefault = isActiveByDefault(iprofile);
                        if (activated || active || activeByDefault) {
                            if (Profile.SOURCE_POM.equals(profile.getSource())) {
                                if (activated || active) {
                                    activePomProfiles.add(profile);
                                } else {
                                    activePomProfilesByDefault.add(profile);
                                }
                            } else {
                                activeSettingsProfiles.add(profile);
                            }
                            activatedProfiles.add(profile);
                        }
                    }
                }
            }
            context.addProfileProperties(activatedProfiles);
        }

        List<Profile> allActivated = new ArrayList<>();
        if (activePomProfiles.isEmpty()) {
            allActivated.addAll(activePomProfilesByDefault);
        } else {
            allActivated.addAll(activePomProfiles);
        }
        allActivated.addAll(activeSettingsProfiles);

        return allActivated;
    }

    public List<Profile> getActiveProfilesCascading(
            Collection<Profile> orgProfiles, ProfileActivationContext context, ModelProblemCollector problems) {

        Collection<String> activatedIds = new HashSet<>(context.getActiveProfileIds());
        Collection<String> deactivatedIds = new HashSet<>(context.getInactiveProfileIds());

        List<Profile> activeSettingsProfiles = new ArrayList<>();
        List<Profile> activePomProfiles = new ArrayList<>();
        List<Profile> activePomProfilesByDefault = new ArrayList<>();

        List<Profile> profiles = new ArrayList<>(orgProfiles);
        ProfileActivationInterpolator activationInterpolator = new ProfileActivationInterpolator(context, problems);
        List<Profile> activatedProfiles;
        do {
            // Iterate over the profiles and check if a given profile is activated
            activatedProfiles = new ArrayList<>();
            for (Profile profile : List.copyOf(profiles)) {
                Profile iprofile = activationInterpolator.apply(profile);
                if (!deactivatedIds.contains(iprofile.getId())) {
                    boolean activated = activatedIds.contains(iprofile.getId());
                    boolean active = isActive(iprofile, context, problems);
                    boolean activeByDefault = isActiveByDefault(iprofile);
                    if (activated || active || activeByDefault) {
                        if (Profile.SOURCE_POM.equals(profile.getSource())) {
                            if (activated || active) {
                                activePomProfiles.add(profile);
                            } else {
                                activePomProfilesByDefault.add(profile);
                            }
                        } else {
                            activeSettingsProfiles.add(profile);
                        }
                        profiles.remove(profile);
                        activatedProfiles.add(profile);
                    }
                }
            }
            context.addProfileProperties(activatedProfiles);
        } while (!activatedProfiles.isEmpty());

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

    private class ProfileActivationInterpolator extends MavenTransformer implements UnaryOperator<Profile> {
        private final ProfileActivationContext context;
        private final ModelProblemCollector problems;

        ProfileActivationInterpolator(ProfileActivationContext context, ModelProblemCollector problems) {
            super(s -> {
                try {
                    Map<String, String> map1 = context.getUserProperties();
                    Map<String, String> map2 = context.getProjectProperties();
                    Map<String, String> map3 = context.getSystemProperties();
                    return interpolator.interpolate(s, Interpolator.chain(List.of(map1::get, map2::get, map3::get)));
                } catch (InterpolatorException e) {
                    problems.add(Severity.ERROR, Version.BASE, e.getMessage(), e);
                }
                return s;
            });
            this.context = context;
            this.problems = problems;
        }

        @Override
        public Profile apply(Profile p) {
            return Profile.newBuilder(p)
                    .activation(transformActivation(p.getActivation()))
                    .build();
        }

        @Override
        protected Activation.Builder transformActivation_Condition(
                Supplier<? extends Activation.Builder> creator, Activation.Builder builder, Activation target) {
            // do not interpolate the condition activation
            return builder;
        }

        @Override
        protected ActivationFile.Builder transformActivationFile_Missing(
                Supplier<? extends ActivationFile.Builder> creator,
                ActivationFile.Builder builder,
                ActivationFile target) {
            String path = target.getMissing();
            String xformed = transformPath(path, target, "missing");
            return xformed != path ? (builder != null ? builder : creator.get()).missing(xformed) : builder;
        }

        @Override
        protected ActivationFile.Builder transformActivationFile_Exists(
                Supplier<? extends ActivationFile.Builder> creator,
                ActivationFile.Builder builder,
                ActivationFile target) {
            final String path = target.getExists();
            final String xformed = transformPath(path, target, "exists");
            return xformed != path ? (builder != null ? builder : creator.get()).exists(xformed) : builder;
        }

        private String transformPath(String path, ActivationFile target, String locationKey) {
            try {
                return profileActivationFilePathInterpolator.interpolate(path, context);
            } catch (InterpolatorException e) {
                problems.add(
                        Severity.ERROR,
                        Version.BASE,
                        "Failed to interpolate file location " + path + ": " + e.getMessage(),
                        target.getLocation(locationKey),
                        e);
            }
            return path;
        }
    }
}
