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

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link JdkVersionProfileActivator}.
 *
 */
class JdkVersionProfileActivatorTest extends AbstractProfileActivatorTest<JdkVersionProfileActivator> {

    @Override
    @BeforeEach
    void setUp() throws Exception {
        activator = new JdkVersionProfileActivator();
    }

    private Profile newProfile(String jdkVersion) {
        Activation a = Activation.newBuilder().jdk(jdkVersion).build();

        Profile p = Profile.newBuilder().activation(a).build();

        return p;
    }

    private Map<String, String> newProperties(String javaVersion) {
        return Map.of("java.version", javaVersion);
    }

    @Test
    void nullSafe() throws Exception {
        Profile p = Profile.newInstance();

        assertActivation(false, p, newContext(null, null));

        p = p.withActivation(Activation.newInstance());

        assertActivation(false, p, newContext(null, null));
    }

    @Test
    void prefix() throws Exception {
        Profile profile = newProfile("1.4");

        assertActivation(true, profile, newContext(null, newProperties("1.4")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(false, profile, newContext(null, newProperties("1.3")));

        assertActivation(false, profile, newContext(null, newProperties("1.5")));
    }

    @Test
    void prefixNegated() throws Exception {
        Profile profile = newProfile("!1.4");

        assertActivation(false, profile, newContext(null, newProperties("1.4")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.3")));

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
    }

    @Test
    void versionRangeInclusiveBounds() throws Exception {
        Profile profile = newProfile("[1.5,1.6]");

        assertActivation(false, profile, newContext(null, newProperties("1.4")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.1")));

        assertActivation(true, profile, newContext(null, newProperties("1.6")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0_09-b03")));
    }

    @Test
    void versionRangeExclusiveBounds() throws Exception {
        Profile profile = newProfile("(1.3,1.6)");

        assertActivation(false, profile, newContext(null, newProperties("1.3")));
        assertActivation(false, profile, newContext(null, newProperties("1.3.0")));
        assertActivation(false, profile, newContext(null, newProperties("1.3.0_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.3.0_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.3.1")));
        assertActivation(true, profile, newContext(null, newProperties("1.3.1_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.3.1_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.1")));

        assertActivation(false, profile, newContext(null, newProperties("1.6")));
    }

    @Test
    void versionRangeInclusiveLowerBound() throws Exception {
        Profile profile = newProfile("[1.5,)");

        assertActivation(false, profile, newContext(null, newProperties("1.4")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.1")));

        assertActivation(true, profile, newContext(null, newProperties("1.6")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.6.0_09-b03")));
    }

    @Test
    void versionRangeExclusiveUpperBound() throws Exception {
        Profile profile = newProfile("(,1.6)");

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.0_09-b03")));
        assertActivation(true, profile, newContext(null, newProperties("1.5.1")));

        assertActivation(false, profile, newContext(null, newProperties("1.6")));
        assertActivation(false, profile, newContext(null, newProperties("1.6.0")));
        assertActivation(false, profile, newContext(null, newProperties("1.6.0_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.6.0_09-b03")));
    }

    @Test
    void rubbishJavaVersion() {
        Profile profile = newProfile("[1.8,)");

        assertActivationWithProblems(profile, newContext(null, newProperties("Pūteketeke")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("rubbish")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("1.a.0_09")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("1.a.2.b")), "invalid JDK version");
    }

    private void assertActivationWithProblems(
            Profile profile, ProfileActivationContext context, String warningContains) {
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertThat(activator.isActive(profile, context, problems)).isFalse();

        assertThat(problems.getErrors().size()).isEqualTo(0);
        assertThat(problems.getWarnings().size()).isEqualTo(1);
        assertThat(problems.getWarnings().get(0).contains(warningContains)).isTrue();
    }
}
