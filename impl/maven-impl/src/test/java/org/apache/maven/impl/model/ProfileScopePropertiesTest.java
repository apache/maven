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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Session;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.Lookup;
import org.apache.maven.api.services.ModelBuilderRequest;
import org.apache.maven.api.services.model.ModelInterpolator;
import org.apache.maven.api.services.model.ProfileInjector;
import org.apache.maven.impl.model.profile.SimpleProblemCollector;
import org.apache.maven.impl.standalone.ApiRunner;
import org.apache.maven.slf4j.MavenSimpleLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileScopePropertiesTest {

    private Session session;
    private ProfileInjector profileInjector;
    private ModelInterpolator interpolator;

    @BeforeEach
    void setUp() {
        session = ApiRunner.createSession();
        profileInjector = session.getService(Lookup.class).lookup(ProfileInjector.class);
        interpolator = session.getService(Lookup.class).lookup(ModelInterpolator.class);
    }

    @Test
    void testProfileScopePropertiesInjection() {
        InputLocation loc1 = InputLocation.of(10, 5);
        InputLocation propsLoc1 = InputLocation.of(9, 5, null, Map.of("scala.version", loc1));

        InputLocation loc2 = InputLocation.of(20, 5);
        InputLocation propsLoc2 = InputLocation.of(19, 5, null, Map.of("scala.version", loc2));

        Profile profile1 = Profile.newBuilder()
                .id("scala-2.11")
                .properties(Map.of("scala.version", "2.11.12", "compiler.plugin", "scalamacros"))
                .location("properties", propsLoc1)
                .build();

        Profile profile2 = Profile.newBuilder()
                .id("scala-2.12")
                .properties(Map.of("scala.version", "2.12.18", "extra.arg", "-Xfatal-warnings"))
                .location("properties", propsLoc2)
                .build();

        Model model = Model.newBuilder()
                .properties(Map.of("common.prop", "common"))
                .build();

        SimpleProblemCollector problems = new SimpleProblemCollector();
        Model result = profileInjector.injectProfiles(
                model, Arrays.asList(profile1, profile2), ModelBuilderRequest.builder().session(session).build(), problems);

        // Later active profile overwrites unscoped property
        assertEquals("2.12.18", result.getProperties().get("scala.version"));
        // Profile-scoped properties are preserved without collision
        assertEquals("2.11.12", result.getProperties().get("profile.scala-2.11.scala.version"));
        assertEquals("2.12.18", result.getProperties().get("profile.scala-2.12.scala.version"));

        assertEquals("scalamacros", result.getProperties().get("profile.scala-2.11.compiler.plugin"));
        assertEquals("-Xfatal-warnings", result.getProperties().get("profile.scala-2.12.extra.arg"));

        // Verify InputLocation tracking on profile-scoped properties (nested inside "properties" location)
        InputLocation resultPropsLoc = result.getLocation("properties");
        assertEquals(loc1, resultPropsLoc.getLocation("profile.scala-2.11.scala.version"));
        assertEquals(loc2, resultPropsLoc.getLocation("profile.scala-2.12.scala.version"));
    }

    @Test
    void testProfileScopePropertiesOverridesExplicitModelProperty() {
        Profile profile = Profile.newBuilder()
                .id("dev")
                .properties(Map.of("server.port", "8080"))
                .build();

        Model model = Model.newBuilder()
                .properties(Map.of("profile.dev.server.port", "9090"))
                .build();

        List<String> capturedLogs = new ArrayList<>();
        MavenSimpleLogger.setLogSink(capturedLogs::add);
        try {
            SimpleProblemCollector problems = new SimpleProblemCollector();
            Model result = profileInjector.injectProfiles(
                    model,
                    Collections.singletonList(profile),
                    ModelBuilderRequest.builder().session(session).build(),
                    problems);

            // Profile-scoped property takes precedence over pre-existing explicit model property
            assertEquals("8080", result.getProperties().get("profile.dev.server.port"));

            // Verify warning log was emitted for the property collision
            assertTrue(
                    capturedLogs.stream().anyMatch(log -> log.contains("overrides an existing model property")
                            && log.contains("profile.dev.server.port")
                            && log.contains("dev")),
                    "Expected collision warning log was not found. Captured logs: " + capturedLogs);
        } finally {
            MavenSimpleLogger.setLogSink(null);
        }
    }

    @Test
    void testProfileScopePropertiesInterpolation() {
        Profile activeProfile = Profile.newBuilder()
                .id("dev")
                .properties(Map.of("env.name", "development"))
                .build();

        Profile inactiveProfile = Profile.newBuilder()
                .id("prod")
                .properties(Map.of("env.name", "production"))
                .build();

        Model rawModel = Model.newBuilder()
                .profiles(Arrays.asList(activeProfile, inactiveProfile))
                .description("${profile.dev.env.name}")
                .build();

        SimpleProblemCollector problems = new SimpleProblemCollector();
        // Only active profiles are injected
        Model injectedModel = profileInjector.injectProfiles(
                rawModel,
                Collections.singletonList(activeProfile),
                ModelBuilderRequest.builder().session(session).build(),
                problems);

        Model out = interpolator.interpolateModel(
                injectedModel,
                Paths.get("."),
                ModelBuilderRequest.builder().session(session).requestType(ModelBuilderRequest.RequestType.BUILD_PROJECT).build(),
                problems);

        // Active profile-scoped properties resolve cleanly
        assertEquals("development", out.getDescription());
        // Inactive profile properties are never injected into model properties
        assertNull(injectedModel.getProperties().get("profile.prod.env.name"));
    }

    @Test
    void testProfileScopePropertiesDotIdBehavior() {
        Profile profileDot = Profile.newBuilder()
                .id("my.dot.profile")
                .properties(Map.of("sample.prop", "val1"))
                .build();

        Profile profileHyphen = Profile.newBuilder()
                .id("my-hyphen-profile")
                .properties(Map.of("sample.prop", "val2"))
                .build();

        Model model = Model.newBuilder().build();
        SimpleProblemCollector problems = new SimpleProblemCollector();
        Model result = profileInjector.injectProfiles(
                model, Arrays.asList(profileDot, profileHyphen), ModelBuilderRequest.builder().session(session).build(), problems);

        assertEquals("val1", result.getProperties().get("profile.my.dot.profile.sample.prop"));
        assertEquals("val2", result.getProperties().get("profile.my-hyphen-profile.sample.prop"));
    }

    @Test
    void testProfileScopePropertiesDotIdCollisionLastWriteWins() {
        // Profile "foo" with property "bar.myprop" produces scoped key "profile.foo.bar.myprop"
        Profile profileFoo = Profile.newBuilder()
                .id("foo")
                .properties(Map.of("bar.myprop", "valueFoo"))
                .build();

        // Profile "foo.bar" with property "myprop" produces the same scoped key "profile.foo.bar.myprop"
        Profile profileFooBar = Profile.newBuilder()
                .id("foo.bar")
                .properties(Map.of("myprop", "valueFooBar"))
                .build();

        Model model1 = Model.newBuilder().build();
        SimpleProblemCollector problems1 = new SimpleProblemCollector();
        // Inject [foo, foo.bar]: foo.bar is injected last, so its value wins (last-write-wins)
        Model result1 = profileInjector.injectProfiles(
                model1,
                Arrays.asList(profileFoo, profileFooBar),
                ModelBuilderRequest.builder().session(session).build(),
                problems1);
        assertEquals("valueFooBar", result1.getProperties().get("profile.foo.bar.myprop"));

        Model model2 = Model.newBuilder().build();
        SimpleProblemCollector problems2 = new SimpleProblemCollector();
        // Inject [foo.bar, foo]: foo is injected last, so its value wins (last-write-wins)
        Model result2 = profileInjector.injectProfiles(
                model2,
                Arrays.asList(profileFooBar, profileFoo),
                ModelBuilderRequest.builder().session(session).build(),
                problems2);
        assertEquals("valueFoo", result2.getProperties().get("profile.foo.bar.myprop"));
    }

    @Test
    void testProfileScopePropertiesAlreadyPrefixed() {
        Profile profile = Profile.newBuilder()
                .id("myprofile")
                .properties(Map.of("profile.custom", "customValue", "regular.prop", "regularValue"))
                .build();

        Model model = Model.newBuilder().build();
        SimpleProblemCollector problems = new SimpleProblemCollector();
        Model result = profileInjector.injectProfiles(
                model, Collections.singletonList(profile), ModelBuilderRequest.builder().session(session).build(), problems);

        // Unscoped copy exists
        assertEquals("customValue", result.getProperties().get("profile.custom"));
        assertEquals("regularValue", result.getProperties().get("regular.prop"));

        // Regular property gets profile-scoped copy
        assertEquals("regularValue", result.getProperties().get("profile.myprofile.regular.prop"));

        // Property already starting with "profile." is NOT re-prefixed
        assertNull(result.getProperties().get("profile.myprofile.profile.custom"));
    }

    @Test
    void testProfileScopePropertiesWithNullOrEmptyProfileId() {
        Profile profileNullId = Profile.newBuilder()
                .id(null)
                .properties(Map.of("prop.null", "valNull"))
                .build();

        Profile profileEmptyId = Profile.newBuilder()
                .id("")
                .properties(Map.of("prop.empty", "valEmpty"))
                .build();

        Profile profileEmptyProps = Profile.newBuilder()
                .id("empty-props")
                .properties(Collections.emptyMap())
                .build();

        Model model = Model.newBuilder()
                .properties(Map.of("base.prop", "baseVal"))
                .build();

        SimpleProblemCollector problems = new SimpleProblemCollector();
        Model result = profileInjector.injectProfiles(
                model, Arrays.asList(profileNullId, profileEmptyId, profileEmptyProps), ModelBuilderRequest.builder().session(session).build(), problems);

        // Standard properties merged
        assertEquals("baseVal", result.getProperties().get("base.prop"));
        assertEquals("valNull", result.getProperties().get("prop.null"));
        assertEquals("valEmpty", result.getProperties().get("prop.empty"));

        // No empty or null prefix generated
        assertNull(result.getProperties().get("profile..prop.null"));
        assertNull(result.getProperties().get("profile..prop.empty"));
        assertNull(result.getProperties().get("profile.null.prop.null"));
    }
}
