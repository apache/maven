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
import java.util.Map;

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationFile;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.apache.maven.impl.model.profile.FileProfileActivator;
import org.apache.maven.impl.model.profile.PropertyProfileActivator;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.model.rootlocator.DefaultRootLocator;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DefaultProfileSelector} with focus on cascading activation behavior
 * and POM vs settings profile distinction via {@link InputSource} model id (issue #2569).
 */
public class DefaultProfileSelectorTest {

    private DefaultProfileSelector selector;
    private SimpleProblemCollector problems;

    @BeforeEach
    void setUp() {
        selector = new DefaultProfileSelector();
        selector.addProfileActivator(
                new PropertyProfileActivator(new DefaultModelVersionParser(new GenericVersionScheme())));
        selector.addProfileActivator(new FileProfileActivator());
        problems = new SimpleProblemCollector();
    }

    @Test
    void testNonCascadingActivation() {
        // Create profiles with property-based activation
        Profile profile1 = createPomProfile("profile1", "prop1", "value1");
        Profile profile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setModel(Model.newInstance());
        context.setSystemProperties(Map.of("prop1", "value1"));

        // Test cascading mode (current implementation only supports cascading)
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        assertEquals(1, activeProfiles.size());
        assertEquals("profile1", activeProfiles.get(0).getId());
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingActivation() {
        // Create profiles where one activates another through properties
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set (should activate profile1, which sets prop2, which activates profile2)
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance()); // Set a model for property injection

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingVsNonCascadingDifference() {
        // Create profiles where cascading would activate more profiles
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance()); // Set a model for property injection

        // Cascading should activate both profile1 and profile2
        List<Profile> cascading = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, cascading.size());
    }

    @Test
    void testActiveByDefaultProfiles() {
        Profile defaultProfile = createPomActiveByDefaultProfile("default-profile");
        Profile conditionalProfile = createPomProfile("conditional", "prop1", "value1");

        List<Profile> profiles = Arrays.asList(defaultProfile, conditionalProfile);

        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setModel(Model.newInstance());

        // Should activate default profile when no conditions are met
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(1, activeProfiles.size());
        assertEquals("default-profile", activeProfiles.get(0).getId());

        // Should not activate default profile when conditional profile is active
        context.setSystemProperties(Map.of("prop1", "value1"));
        activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(1, activeProfiles.size());
        assertEquals("conditional", activeProfiles.get(0).getId());
    }

    @Test
    void testMixedSourceProfiles() {
        Profile pomProfile = createPomProfile("pom-profile", "prop1", "value1");
        Profile settingsProfile = createSettingsProfile("settings-profile", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(pomProfile, settingsProfile);

        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setModel(Model.newInstance());
        context.setSystemProperties(Map.of("prop1", "value1", "prop2", "value2"));

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, activeProfiles.size());

        // Settings profiles should come after POM profiles in the result
        assertEquals("pom-profile", activeProfiles.get(0).getId());
        assertEquals("settings-profile", activeProfiles.get(1).getId());
    }

    @Test
    void testEmptyProfilesList() {
        List<Profile> profiles = Collections.emptyList();
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setModel(Model.newInstance());

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertTrue(activeProfiles.isEmpty());
    }

    @Test
    void testExplicitlyActivatedProfiles() {
        Profile profile1 = createPomProfile("profile1", "nonexistent", "value");
        Profile profile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext(
                new DefaultRootLocator(),
                new DefaultInterpolator(),
                List.of("profile1"),
                List.of(),
                Map.of("prop2", "value2"),
                Map.of(),
                Model.newInstance());

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
    }

    @Test
    void testCascadingActivationChain() {
        // Create a chain of profiles: profile1 -> profile2 -> profile3
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfileWithProperties("profile2", "prop2", "value2", Map.of("prop3", "value3"));
        Profile profile3 = createPomProfile("profile3", "prop3", "value3");

        List<Profile> profiles = Arrays.asList(profile1, profile2, profile3);

        // Create context with prop1 set
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance());

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // All three profiles should be activated through cascading
        assertEquals(3, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile3".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingStopCondition() {
        // Test that cascading stops when no more profiles can be activated
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfileWithProperties("profile2", "prop2", "value2", Map.of("prop3", "value3"));
        // profile3 requires prop4 which is never set, so cascading should stop
        Profile profile3 = createPomProfile("profile3", "prop4", "value4");

        List<Profile> profiles = Arrays.asList(profile1, profile2, profile3);

        // Create context with prop1 set
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance());

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Only profile1 and profile2 should be activated, profile3 should not
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(activeProfiles.stream().noneMatch(p -> "profile3".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingWithCircularDependency() {
        // Test that cascading handles circular dependencies gracefully
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfileWithProperties("profile2", "prop2", "value2", Map.of("prop1", "value1"));

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance());

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Both profiles should be activated, but cascading should stop after first iteration
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingWithInactiveProfile() {
        // Create profiles where one would activate another, but the second is explicitly deactivated
        Profile profile1 = createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile profile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        // Create context with prop1 set and profile2 explicitly deactivated
        DefaultProfileActivationContext context = new DefaultProfileActivationContext(
                new DefaultRootLocator(),
                new DefaultInterpolator(),
                List.of(),
                List.of("profile2"),
                Map.of("prop1", "value1"),
                Map.of(),
                Model.newInstance());

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Only profile1 should be activated, profile2 should be deactivated despite cascading
        assertEquals(1, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().noneMatch(p -> "profile2".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testActiveByDefaultDoesNotCascade() {
        // activeByDefault profiles must NOT inject cascading properties when suppressed
        // by a genuinely activated profile
        Profile defaultProfile = Profile.newBuilder()
                .id("default-on")
                .activation(Activation.newBuilder().activeByDefault(true).build())
                .properties(Map.of("prop2", "value2"))
                .location("", newPomLocation())
                .build();

        Profile cascadeTarget = createPomProfile("cascade-target", "prop2", "value2");
        Profile explicitProfile = createPomProfile("explicit", "trigger", "yes");

        List<Profile> profiles = Arrays.asList(defaultProfile, cascadeTarget, explicitProfile);

        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("trigger", "yes"));
        context.setModel(Model.newInstance());

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Only 'explicit' should be active. 'default-on' is suppressed because a non-default
        // POM profile activated. 'cascade-target' must NOT activate because the suppressed
        // default-on profile's properties should not cascade.
        assertEquals(1, activeProfiles.size());
        assertEquals("explicit", activeProfiles.get(0).getId());
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testExplicitlyActivatedProfileDoesNotCascade() {
        // Reproduces the Windows CI failure: -Prun-its sets mavenHome as a profile property,
        // which must NOT cascade to activate maven-from-build (which activates on mavenHome).
        Profile runIts = createPomProfileWithProperties(
                "run-its", "nonexistent", "value", Map.of("mavenHome", "target/apache-maven"));
        Profile mavenFromBuild = createPomProfile("maven-from-build", "mavenHome", null);

        List<Profile> profiles = Arrays.asList(runIts, mavenFromBuild);

        // Explicitly activate run-its via -P (not via activator match)
        DefaultProfileActivationContext context = new DefaultProfileActivationContext(
                new DefaultRootLocator(),
                new DefaultInterpolator(),
                List.of("run-its"),
                List.of(),
                Map.of(),
                Map.of(),
                Model.newInstance());

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Only run-its should be active; maven-from-build must NOT activate because
        // explicitly-activated profiles don't cascade their properties.
        assertEquals(1, activeProfiles.size());
        assertEquals("run-its", activeProfiles.get(0).getId());
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testCascadingWithRecordImmutability() {
        // Test that profile records remain immutable during cascading
        Profile originalProfile1 =
                createPomProfileWithProperties("profile1", "prop1", "value1", Map.of("prop2", "value2"));
        Profile originalProfile2 = createPomProfile("profile2", "prop2", "value2");

        List<Profile> profiles = Arrays.asList(originalProfile1, originalProfile2);

        // Create context with prop1 set
        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("prop1", "value1"));
        context.setModel(Model.newInstance());

        // Test cascading mode
        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Verify that original profiles are unchanged (immutable records)
        assertEquals("profile1", originalProfile1.getId());
        assertEquals("profile2", originalProfile2.getId());
        assertEquals(Map.of("prop2", "value2"), originalProfile1.getProperties());
        assertEquals(Map.of(), originalProfile2.getProperties());

        // Verify activation worked
        assertEquals(2, activeProfiles.size());
        assertTrue(problems.getErrors().isEmpty());
    }

    @Test
    void testFileExistenceCascade() {
        String existingDir = System.getProperty("java.io.tmpdir");

        // Profile 1: activates on trigger=yes, sets my.path to an existing directory
        Profile profile1 = createPomProfileWithProperties("profile1", "trigger", "yes", Map.of("my.path", existingDir));

        // Profile 2: activates on file existence using cascaded property
        Profile profile2 = Profile.newBuilder()
                .id("profile2")
                .activation(Activation.newBuilder()
                        .file(ActivationFile.newBuilder().exists("${my.path}").build())
                        .build())
                .location("", newPomLocation())
                .build();

        List<Profile> profiles = Arrays.asList(profile1, profile2);

        DefaultProfileActivationContext context =
                new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
        context.setSystemProperties(Map.of("trigger", "yes"));
        context.setModel(Model.newInstance());

        List<Profile> activeProfiles = selector.getActiveProfiles(profiles, context, problems);

        // Both profiles should activate: profile1 via property match, profile2 via
        // file-existence check using the cascaded my.path property from profile1.
        assertEquals(2, activeProfiles.size());
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile1".equals(p.getId())));
        assertTrue(activeProfiles.stream().anyMatch(p -> "profile2".equals(p.getId())));
        assertTrue(problems.getErrors().isEmpty());
    }

    // ---- isFromPom() / activeByDefault: POM profiles (issue #2569) ----

    @Test
    void testPomActiveByDefaultProfileIsActivatedWhenNoOtherPomProfileActive() {
        Profile defaultProfile = newPomProfile("default", true);

        List<Profile> active = selector.getActiveProfiles(
                Collections.singletonList(defaultProfile), newContext(), new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("default", active.get(0).getId());
    }

    @Test
    void testPomActiveByDefaultProfileIsDeactivatedWhenAnotherPomProfileIsExplicitlyActive() {
        Profile defaultProfile = newPomProfile("default", true);
        Profile explicitProfile = newPomProfile("explicit", false);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(defaultProfile, explicitProfile),
                newContext(Collections.singletonList("explicit")),
                new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("explicit", active.get(0).getId());
    }

    // ---- isFromPom() / activeByDefault: settings profiles (issue #2569) ----

    @Test
    void testSettingsActiveByDefaultProfileIsAlwaysActivated() {
        Profile settingsDefault = newSettingsProfile("settings-default", true);

        List<Profile> active = selector.getActiveProfiles(
                Collections.singletonList(settingsDefault), newContext(), new SimpleProblemCollector());

        assertEquals(1, active.size());
        assertEquals("settings-default", active.get(0).getId());
    }

    @Test
    void testSettingsActiveByDefaultProfileIsNotSuppressedByExplicitPomProfile() {
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

    // ---- mixed POM + settings profiles (issue #2569) ----

    @Test
    void testMixedPomAndSettingsActiveByDefault() {
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

    // ---- edge case: profile with no location info (issue #2569) ----

    @Test
    void testProfileWithNoLocationDefaultsToPom() {
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
        DefaultProfileSelector localSelector = new DefaultProfileSelector();
        localSelector.addProfileActivator(new ProfileActivator() {
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
        SimpleProblemCollector localProblems = new SimpleProblemCollector();
        List<Profile> active = localSelector.getActiveProfiles(profiles, newContext(), localProblems);

        assertTrue(active.isEmpty());
        assertEquals(1, localProblems.getErrors().size());
        assertEquals("Failed to determine activation for profile one: BOOM", localProblems.getErrors().get(0));
    }

    // ---- Helper methods ----

    /** Creates an InputLocation simulating a POM file origin (non-empty modelId). */
    private InputLocation newPomLocation() {
        return InputLocation.of(1, 1, InputSource.of("org.example:myproject:1.0", "pom.xml"));
    }

    /** Creates an InputLocation simulating a settings.xml origin (empty modelId). */
    private InputLocation newSettingsLocation() {
        return InputLocation.of(1, 1, InputSource.of("", "~/.m2/settings.xml"));
    }

    /**
     * Creates a POM profile with property-based activation and a POM InputLocation
     * so that {@link DefaultProfileSelector#isFromPom} returns {@code true}.
     */
    private Profile createPomProfile(String id, String propName, String propValue) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .location("", newPomLocation())
                .build();
    }

    /**
     * Creates a settings profile with property-based activation and a settings InputLocation
     * so that {@link DefaultProfileSelector#isFromPom} returns {@code false}.
     */
    private Profile createSettingsProfile(String id, String propName, String propValue) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .location("", newSettingsLocation())
                .build();
    }

    /**
     * Creates a POM profile with property-based activation, extra properties, and a POM InputLocation.
     */
    private Profile createPomProfileWithProperties(
            String id, String propName, String propValue, Map<String, String> profileProperties) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .properties(profileProperties)
                .location("", newPomLocation())
                .build();
    }

    /**
     * Creates a POM activeByDefault profile with a POM InputLocation.
     */
    private Profile createPomActiveByDefaultProfile(String id) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder().activeByDefault(true).build())
                .location("", newPomLocation())
                .build();
    }

    // ---- Helpers for isFromPom() / #2569 tests ----

    /**
     * Creates a POM profile with an InputSource that has a non-empty modelId,
     * simulating a profile read from a POM file.
     */
    private Profile newPomProfile(String id, boolean activeByDefault) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", newPomLocation())
                .build();
    }

    /**
     * Creates a settings profile with an InputSource that has an empty modelId,
     * simulating a profile read from settings.xml (via SettingsUtilsV4.toLocation).
     */
    private Profile newSettingsProfile(String id, boolean activeByDefault) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", newSettingsLocation())
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
                .activation(Activation.newBuilder().activeByDefault(activeByDefault).build())
                .location("", location)
                .build();
    }

    /**
     * Creates a profile with no InputLocation at all.
     */
    private Profile newProfileNoLocation(String id, boolean activeByDefault) {
        return Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder().activeByDefault(activeByDefault).build())
                .build();
    }

    private DefaultProfileActivationContext newContext() {
        return new DefaultProfileActivationContext(new DefaultRootLocator(), new DefaultInterpolator());
    }

    private DefaultProfileActivationContext newContext(List<String> activeProfileIds) {
        return new DefaultProfileActivationContext(
                new DefaultRootLocator(),
                new DefaultInterpolator(),
                activeProfileIds,
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                null);
    }
}
