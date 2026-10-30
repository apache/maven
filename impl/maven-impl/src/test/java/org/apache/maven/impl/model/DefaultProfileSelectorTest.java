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
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
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
 * Tests {@link DefaultProfileSelector} with focus on cascading activation behavior.
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
        Profile profile1 = createProfile("profile1", "prop1", "value1", Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
        Profile defaultProfile = createActiveByDefaultProfile("default-profile", Profile.SOURCE_POM);
        Profile conditionalProfile = createProfile("conditional", "prop1", "value1", Profile.SOURCE_POM);

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
        Profile pomProfile = createProfile("pom-profile", "prop1", "value1", Profile.SOURCE_POM);
        Profile settingsProfile = createProfile("settings-profile", "prop2", "value2", Profile.SOURCE_SETTINGS);

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
        Profile profile1 = createProfile("profile1", "nonexistent", "value", Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfileWithProperties(
                "profile2", "prop2", "value2", Map.of("prop3", "value3"), Profile.SOURCE_POM);
        Profile profile3 = createProfile("profile3", "prop3", "value3", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfileWithProperties(
                "profile2", "prop2", "value2", Map.of("prop3", "value3"), Profile.SOURCE_POM);
        // profile3 requires prop4 which is never set, so cascading should stop
        Profile profile3 = createProfile("profile3", "prop4", "value4", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfileWithProperties(
                "profile2", "prop2", "value2", Map.of("prop1", "value1"), Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile profile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
                .build();
        defaultProfile.setSource(Profile.SOURCE_POM);

        Profile cascadeTarget = createProfile("cascade-target", "prop2", "value2", Profile.SOURCE_POM);
        Profile explicitProfile = createProfile("explicit", "trigger", "yes", Profile.SOURCE_POM);

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
        Profile runIts = createProfileWithProperties(
                "run-its", "nonexistent", "value", Map.of("mavenHome", "target/apache-maven"), Profile.SOURCE_POM);
        Profile mavenFromBuild = createProfile("maven-from-build", "mavenHome", null, Profile.SOURCE_POM);

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
        Profile originalProfile1 = createProfileWithProperties(
                "profile1", "prop1", "value1", Map.of("prop2", "value2"), Profile.SOURCE_POM);
        Profile originalProfile2 = createProfile("profile2", "prop2", "value2", Profile.SOURCE_POM);

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
        Profile profile1 = createProfileWithProperties(
                "profile1", "trigger", "yes", Map.of("my.path", existingDir), Profile.SOURCE_POM);

        // Profile 2: activates on file existence using cascaded property
        Profile profile2 = Profile.newBuilder()
                .id("profile2")
                .activation(Activation.newBuilder()
                        .file(ActivationFile.newBuilder().exists("${my.path}").build())
                        .build())
                .build();
        profile2.setSource(Profile.SOURCE_POM);

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

    // Helper methods for creating test profiles

    private Profile createProfile(String id, String propName, String propValue, String source) {
        Profile profile = Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .build();
        profile.setSource(source);
        return profile;
    }

    private Profile createProfileWithProperties(
            String id, String propName, String propValue, Map<String, String> profileProperties, String source) {
        Profile profile = Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder()
                        .property(ActivationProperty.newBuilder()
                                .name(propName)
                                .value(propValue)
                                .build())
                        .build())
                .properties(profileProperties)
                .build();
        profile.setSource(source);
        return profile;
    }

    private Profile createActiveByDefaultProfile(String id, String source) {
        Profile profile = Profile.newBuilder()
                .id(id)
                .activation(Activation.newBuilder().activeByDefault(true).build())
                .build();
        profile.setSource(source);
        return profile;
    }
}
