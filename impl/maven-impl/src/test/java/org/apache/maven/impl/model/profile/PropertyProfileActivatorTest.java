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
package org.apache.maven.impl.model.profile;

import java.util.Map;

import org.apache.maven.api.Constants;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.impl.DefaultModelVersionParser;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests {@link PropertyProfileActivator}.
 *
 */
class PropertyProfileActivatorTest extends AbstractProfileActivatorTest<PropertyProfileActivator> {

    @BeforeEach
    @Override
    void setUp() throws Exception {
        activator = new PropertyProfileActivator(new DefaultModelVersionParser(new GenericVersionScheme()));
    }

    private Profile newProfile(String key, String value) {
        ActivationProperty ap =
                ActivationProperty.newBuilder().name(key).value(value).build();

        Activation a = Activation.newBuilder().property(ap).build();

        Profile p = Profile.newBuilder().activation(a).build();

        return p;
    }

    private Map<String, String> newProperties(String key, String value) {
        return Map.of(key, value);
    }

    @Test
    void testNullSafe() throws Exception {
        Profile p = Profile.newInstance();

        assertActivation(false, p, newContext(null, null));

        p = p.withActivation(Activation.newInstance());

        assertActivation(false, p, newContext(null, null));
    }

    @Test
    void testWithNameOnlyUserProperty() throws Exception {
        Profile profile = newProfile("prop", null);

        assertActivation(true, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(false, profile, newContext(newProperties("prop", ""), null));

        assertActivation(false, profile, newContext(newProperties("other", "value"), null));
    }

    @Test
    void testWithNameOnlySystemProperty() throws Exception {
        Profile profile = newProfile("prop", null);

        assertActivation(true, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(false, profile, newContext(null, newProperties("prop", "")));

        assertActivation(false, profile, newContext(null, newProperties("other", "value")));
    }

    @Test
    void testWithNegatedNameOnlyUserProperty() throws Exception {
        Profile profile = newProfile("!prop", null);

        assertActivation(false, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(true, profile, newContext(newProperties("prop", ""), null));

        assertActivation(true, profile, newContext(newProperties("other", "value"), null));
    }

    @Test
    void testWithNegatedNameOnlySystemProperty() throws Exception {
        Profile profile = newProfile("!prop", null);

        assertActivation(false, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(true, profile, newContext(null, newProperties("prop", "")));

        assertActivation(true, profile, newContext(null, newProperties("other", "value")));
    }

    @Test
    void testWithValueUserProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        assertActivation(true, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(false, profile, newContext(newProperties("prop", "other"), null));

        assertActivation(false, profile, newContext(newProperties("prop", ""), null));
    }

    @Test
    void testWithValueSystemProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        assertActivation(true, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(false, profile, newContext(null, newProperties("prop", "other")));

        assertActivation(false, profile, newContext(null, newProperties("other", "")));
    }

    @Test
    void testWithNegatedValueUserProperty() throws Exception {
        Profile profile = newProfile("prop", "!value");

        assertActivation(false, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(true, profile, newContext(newProperties("prop", "other"), null));

        assertActivation(true, profile, newContext(newProperties("prop", ""), null));
    }

    @Test
    void testWithNegatedValueSystemProperty() throws Exception {
        Profile profile = newProfile("prop", "!value");

        assertActivation(false, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(true, profile, newContext(null, newProperties("prop", "other")));

        assertActivation(true, profile, newContext(null, newProperties("other", "")));
    }

    @Test
    void testWithValueUserPropertyDominantOverSystemProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        Map<String, String> props1 = newProperties("prop", "value");
        Map<String, String> props2 = newProperties("prop", "other");

        assertActivation(true, profile, newContext(props1, props2));

        assertActivation(false, profile, newContext(props2, props1));
    }

    @Test
    void testMavenVersionRange() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "[4.0.0-rc-5,4.0.0)");

        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-5")));
        assertActivation(
                true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-6-SNAPSHOT")));
        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0")));
        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "3.9.11")));
        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "")));
        assertActivation(false, profile, newContext(null, Map.of()));
    }

    @Test
    void testMavenVersionLowerBoundedRange() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "[4.0.0,)");

        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-5")));
        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0")));
        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.1.0")));
    }

    @Test
    void testMavenVersionUpperBoundedRange() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "(,4.0.0)");

        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "3.9.11")));
        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-5")));
        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0")));
    }

    @Test
    void testMavenVersionRangeBoundaries() {
        Profile inclusive = newProfile(Constants.MAVEN_VERSION, "[4.0.0,5.0.0]");
        Profile exclusive = newProfile(Constants.MAVEN_VERSION, "(4.0.0,5.0.0)");

        assertActivation(true, inclusive, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0")));
        assertActivation(true, inclusive, newContext(null, newProperties(Constants.MAVEN_VERSION, "5.0.0")));
        assertActivation(false, exclusive, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0")));
        assertActivation(true, exclusive, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.1.0")));
        assertActivation(false, exclusive, newContext(null, newProperties(Constants.MAVEN_VERSION, "5.0.0")));
    }

    @Test
    void testNegatedMavenVersionRange() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "![4.0.0,5.0.0)");

        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.1.0")));
        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "5.0.0")));
        assertActivation(true, profile, newContext(null, Map.of()));
    }

    @Test
    void testMavenVersionExactMatchIsPreserved() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "4.0.0-rc-5");

        assertActivation(true, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-5")));
        assertActivation(false, profile, newContext(null, newProperties(Constants.MAVEN_VERSION, "4.0.0-rc-6")));
    }

    @Test
    void testRangeSyntaxRemainsAnExactMatchForOtherProperties() {
        Profile profile = newProfile("prop", "[1,2)");

        assertActivation(true, profile, newContext(null, newProperties("prop", "[1,2)")));
        assertActivation(false, profile, newContext(null, newProperties("prop", "1.5")));
    }

    @Test
    void testMalformedMavenVersionRangeDoesNotActivate() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "[4.0.0,");
        ProfileActivationContext context = newContext(null, newProperties(Constants.MAVEN_VERSION, "4.1.0"));
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertFalse(activator.isActive(profile, context, problems));
        assertEquals(0, problems.getErrors().size());
        assertEquals(1, problems.getWarnings().size());
        assertEquals(
                "Failed to determine Maven version activation for profile default due to invalid version range: '[4.0.0,'",
                problems.getWarnings().get(0));
    }

    @Test
    void testMalformedNegatedMavenVersionRangeDoesNotActivate() {
        Profile profile = newProfile(Constants.MAVEN_VERSION, "![4.0.0,");
        ProfileActivationContext context = newContext(null, newProperties(Constants.MAVEN_VERSION, "4.1.0"));
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertFalse(activator.isActive(profile, context, problems));
        assertEquals(0, problems.getErrors().size());
        assertEquals(1, problems.getWarnings().size());
        assertEquals(
                "Failed to determine Maven version activation for profile default due to invalid version range: '[4.0.0,'",
                problems.getWarnings().get(0));
    }
}
