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
import org.apache.maven.api.model.ActivationOS;
import org.apache.maven.api.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link OperatingSystemProfileActivator}.
 *
 */
class OperatingSystemProfileActivatorTest extends AbstractProfileActivatorTest<OperatingSystemProfileActivator> {

    @Override
    @BeforeEach
    void setUp() throws Exception {
        activator = new OperatingSystemProfileActivator();
    }

    private Profile newProfile(ActivationOS.Builder activationBuilder) {
        Activation a = Activation.newBuilder().os(activationBuilder.build()).build();

        Profile p = Profile.newBuilder().activation(a).build();

        return p;
    }

    private Map<String, String> newProperties(String osName, String osVersion, String osArch) {
        return Map.of("os.name", osName, "os.version", osVersion, "os.arch", osArch);
    }

    @Test
    void versionStringComparison() throws Exception {
        Profile profile = newProfile(ActivationOS.newBuilder().version("6.5.0-1014-aws"));

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newProperties("linux", "3.1.0", "amd64")));
    }

    @Test
    void versionRegexMatching() throws Exception {
        Profile profile = newProfile(ActivationOS.newBuilder().version("regex:.*aws"));

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newProperties("linux", "3.1.0", "amd64")));
    }

    @Test
    void name() {
        Profile profile = newProfile(ActivationOS.newBuilder().name("windows"));

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void negatedName() {
        Profile profile = newProfile(ActivationOS.newBuilder().name("!windows"));

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void arch() {
        Profile profile = newProfile(ActivationOS.newBuilder().arch("amd64"));

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void negatedArch() {
        Profile profile = newProfile(ActivationOS.newBuilder().arch("!amd64"));

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void family() {
        Profile profile = newProfile(ActivationOS.newBuilder().family("windows"));

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void negatedFamily() {
        Profile profile = newProfile(ActivationOS.newBuilder().family("!windows"));

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    @Test
    void allOsConditions() {
        Profile profile = newProfile(ActivationOS.newBuilder()
                .family("windows")
                .name("windows")
                .arch("aarch64")
                .version("99"));

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "99", "aarch64")));
    }

    @Test
    void capitalOsName() {
        Profile profile = newProfile(ActivationOS.newBuilder()
                .family("Mac")
                .name("Mac OS X")
                .arch("aarch64")
                .version("14.5"));

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("Mac OS X", "14.5", "aarch64")));
    }
}
