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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.model.rootlocator.DefaultRootLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DefaultProfileSelector}, in particular the POM vs settings profile
 * distinction via {@link InputSource} model id (issue #2569).
 */
class DefaultProfileSelectorTest {

    /**
     * Creates a POM profile with an InputSource that has a non-empty modelId,
     * simulating a profile read from a POM file.
     */
    private Profile newPomProfile(String id, boolean activeByDefault) {
        InputSource source = InputSource.of("org.example:myproject:1.0", "pom.xml");
        InputLocation location = InputLocation.of(1, 1, source);
        return Profile.newBuilder()
                .id(id)
                .activation(
                        Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", location)
                .build();
    }

    /**
     * Creates a settings profile with an InputSource that has an empty modelId,
     * simulating a profile read from settings.xml (via SettingsUtilsV4.toLocation).
     */
    private Profile newSettingsProfile(String id, boolean activeByDefault) {
        InputSource source = InputSource.of("", "~/.m2/settings.xml");
        InputLocation location = InputLocation.of(1, 1, source);
        return Profile.newBuilder()
                .id(id)
                .activation(
                        Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", location)
                .build();
    }

    /**
     * Creates a settings profile with a null modelId in its InputSource,
     * simulating a profile read from settings.xml before conversion.
     */
    private Profile newSettingsProfileNullModelId(String id, boolean activeByDefault) {
        InputSource source = InputSource.of(null, "~/.m2/settings.xml");
        InputLocation location = InputLocation.of(1, 1, source);
        return Profile.newBuilder()
                .id(id)
                .activation(
                        Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", location)
                .build();
    }

    /**
     * Creates a profile with no InputLocation at all.
     */
    private Profile newProfileNoLocation(String id, boolean activeByDefault) {
        return Profile.newBuilder()
                .id(id)
                .activation(
                        Activation.newBuilder().activeByDefault(activeByDefault).build())
                .build();
    }

    private DefaultProfileActivationContext newContext() {
        return new DefaultProfileActivationContext(
                new DefaultPathTranslator(), new DefaultRootLocator(), new DefaultInterpolator());
    }

    private DefaultProfileActivationContext newContext(List<String> activeProfileIds) {
        return new DefaultProfileActivationContext(
                new DefaultPathTranslator(),
                new DefaultRootLocator(),
                new DefaultInterpolator(),
                activeProfileIds,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                null);
    }

    // ---- activeByDefault behavior: POM profiles ----

    @Test
    void testPomActiveByDefaultProfileIsActivatedWhenNoOtherPomProfileActive() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile defaultProfile = newPomProfile("default", true);

        List<Profile> active = selector.getActiveProfiles(
                Collections.singletonList(defaultProfile), newContext(), new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("default", active.get(0).getId());
    }

    @Test
    void testPomActiveByDefaultProfileIsDeactivatedWhenAnotherPomProfileIsExplicitlyActive() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile defaultProfile = newPomProfile("default", true);
        Profile explicitProfile = newPomProfile("explicit", false);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(defaultProfile, explicitProfile),
                newContext(Collections.singletonList("explicit")),
                new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("explicit", active.get(0).getId());
    }

    // ---- activeByDefault behavior: settings profiles ----

    @Test
    void testSettingsActiveByDefaultProfileIsAlwaysActivated() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile settingsDefault = newSettingsProfile("settings-default", true);

        List<Profile> active = selector.getActiveProfiles(
                Collections.singletonList(settingsDefault), newContext(), new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("settings-default", active.get(0).getId());
    }

    @Test
    void testSettingsActiveByDefaultProfileIsNotSuppressedByExplicitPomProfile() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile settingsDefault = newSettingsProfile("settings-default", true);
        Profile pomExplicit = newPomProfile("pom-explicit", false);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(settingsDefault, pomExplicit),
                newContext(Collections.singletonList("pom-explicit")),
                new SimpleProblemCollector());

        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(p -> "settings-default".equals(p.getId())));
        assertTrue(active.stream().anyMatch(p -> "pom-explicit".equals(p.getId())));
    }

    @Test
    void testSettingsProfileWithNullModelIdTreatedAsNonPom() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile settingsDefault = newSettingsProfileNullModelId("settings-null", true);
        Profile pomExplicit = newPomProfile("pom-explicit", false);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(settingsDefault, pomExplicit),
                newContext(Collections.singletonList("pom-explicit")),
                new SimpleProblemCollector());

        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(p -> "settings-null".equals(p.getId())));
        assertTrue(active.stream().anyMatch(p -> "pom-explicit".equals(p.getId())));
    }

    // ---- mixed POM + settings profiles ----

    @Test
    void testMixedPomAndSettingsActiveByDefault() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile pomDefault = newPomProfile("pom-default", true);
        Profile settingsDefault = newSettingsProfile("settings-default", true);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(pomDefault, settingsDefault), newContext(), new SimpleProblemCollector());

        // Both should activate: POM default (no other POM profile active) + settings always
        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(p -> "pom-default".equals(p.getId())));
        assertTrue(active.stream().anyMatch(p -> "settings-default".equals(p.getId())));
    }

    @Test
    void testExplicitPomProfileSuppressesPomDefaultButNotSettingsDefault() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile pomDefault = newPomProfile("pom-default", true);
        Profile pomExplicit = newPomProfile("pom-explicit", false);
        Profile settingsDefault = newSettingsProfile("settings-default", true);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(pomDefault, pomExplicit, settingsDefault),
                newContext(Collections.singletonList("pom-explicit")),
                new SimpleProblemCollector());

        // pom-default suppressed (another POM profile explicitly active), settings-default stays
        assertEquals(2, active.size());
        assertTrue(active.stream().anyMatch(p -> "pom-explicit".equals(p.getId())));
        assertTrue(active.stream().anyMatch(p -> "settings-default".equals(p.getId())));
    }

    // ---- edge case: profile with no location info ----

    @Test
    void testProfileWithNoLocationDefaultsToPom() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile noLocation = newProfileNoLocation("no-location", true);
        Profile pomExplicit = newPomProfile("pom-explicit", false);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(noLocation, pomExplicit),
                newContext(Collections.singletonList("pom-explicit")),
                new SimpleProblemCollector());

        // no-location defaults to "from POM", so it should be suppressed
        assertEquals(1, active.size());
        assertEquals("pom-explicit", active.get(0).getId());
    }

    // ---- throwing activator ----

    @Test
    void testThrowingActivator() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        selector.addProfileActivator(new ProfileActivator() {
            @Override
            public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
                throw new RuntimeException("BOOM");
            }

            @Override
            public boolean presentInConfig(
                    Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
                return true;
            }
        });

        List<Profile> profiles = Collections.singletonList(newPomProfile("one", false));
        SimpleProblemCollector problems = new SimpleProblemCollector();
        List<Profile> active = selector.getActiveProfiles(profiles, newContext(), problems);

        assertTrue(active.isEmpty());
        assertEquals(1, problems.getErrors().size());
        assertEquals(
                "Failed to determine activation for profile one: BOOM",
                problems.getErrors().get(0));
    }
}
