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
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DefaultProfileSelector}.
 */
class DefaultProfileSelectorTest {

    private Profile profile(String id, String source) {
        return Profile.newBuilder().id(id).source(source).build();
    }

    private Profile activeByDefaultProfile(String id, String source) {
        Activation activation = Activation.newBuilder().activeByDefault(true).build();
        return Profile.newBuilder().id(id).source(source).activation(activation).build();
    }

    private ProfileActivationContext contextWithActiveIds(String... activeIds) {
        List<String> active = Arrays.asList(activeIds);
        return new ProfileActivationContext() {
            public boolean isProfileActive(String profileId) { return active.contains(profileId); }
            public boolean isProfileInactive(String profileId) { return false; }
            public String getSystemProperty(String key) { return null; }
            public String getUserProperty(String key) { return null; }
            public String getModelProperty(String key) { return null; }
            public String getModelArtifactId() { return null; }
            public String getModelPackaging() { return null; }
            public String getModelRootDirectory() { return null; }
            public String getModelBaseDirectory() { return null; }
            public String interpolatePath(String path) { return path; }
            public boolean exists(String path, boolean glob) { return false; }
        };
    }

    private ModelProblemCollector noopCollector() {
        return (severity, version, message, location, cause) -> {};
    }

    /**
     * MNG-6787: when -P is used with an external profile, an external activeByDefault
     * profile (settings.xml) must NOT be active.
     */
    @Test
    void externalActiveByDefaultSuppressedWhenProfileExplicitlyActivated() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile explicitProfile = profile("explicit", Profile.SOURCE_SETTINGS);
        Profile defaultProfile = activeByDefaultProfile("defaults", Profile.SOURCE_SETTINGS);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(explicitProfile, defaultProfile),
                contextWithActiveIds("explicit"),
                noopCollector());

        assertEquals(1, active.size(), "Only the explicitly activated profile should be active");
        assertEquals("explicit", active.get(0).getId());
        assertFalse(active.stream().anyMatch(p -> "defaults".equals(p.getId())),
                "activeByDefault external profile must be suppressed when -P is used");
    }

    /**
     * MNG-6787: explicitly activating a POM profile via -P must also suppress
     * external (settings.xml) activeByDefault profiles, because
     * anyProfileExplicitlyActivated fires regardless of the activated profile's source.
     */
    @Test
    void externalActiveByDefaultSuppressedWhenPomProfileExplicitlyActivated() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile explicitPomProfile = profile("pom-explicit", Profile.SOURCE_POM);
        Profile externalDefault = activeByDefaultProfile("ext-default", Profile.SOURCE_SETTINGS);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(explicitPomProfile, externalDefault),
                contextWithActiveIds("pom-explicit"),
                noopCollector());

        assertTrue(active.stream().anyMatch(p -> "pom-explicit".equals(p.getId())));
        assertFalse(active.stream().anyMatch(p -> "ext-default".equals(p.getId())),
                "External activeByDefault must be suppressed when any profile is explicitly activated via -P");
    }

    /**
     * MNG-6787: when NO -P is used, external activeByDefault profiles must be active.
     */
    @Test
    void externalActiveByDefaultReturnedWhenNoProfileExplicitlyActivated() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile defaultProfile = activeByDefaultProfile("defaults", Profile.SOURCE_SETTINGS);

        List<Profile> active = selector.getActiveProfiles(
                Collections.singletonList(defaultProfile),
                contextWithActiveIds(),
                noopCollector());

        assertEquals(1, active.size(), "activeByDefault external profile must be active when no -P given");
        assertEquals("defaults", active.get(0).getId());
    }

    /**
     * Regression: POM activeByDefault profiles must still be suppressed when
     * another POM profile is explicitly activated.
     */
    @Test
    void pomActiveByDefaultSuppressedWhenOtherPomProfileExplicitlyActivated() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        Profile explicitPomProfile = profile("explicit", Profile.SOURCE_POM);
        Profile defaultPomProfile = activeByDefaultProfile("pom-default", Profile.SOURCE_POM);

        List<Profile> active = selector.getActiveProfiles(
                Arrays.asList(explicitPomProfile, defaultPomProfile),
                contextWithActiveIds("explicit"),
                noopCollector());

        assertTrue(active.stream().anyMatch(p -> "explicit".equals(p.getId())));
        assertFalse(active.stream().anyMatch(p -> "pom-default".equals(p.getId())),
                "POM activeByDefault must be suppressed when another POM profile is activated");
    }
}
