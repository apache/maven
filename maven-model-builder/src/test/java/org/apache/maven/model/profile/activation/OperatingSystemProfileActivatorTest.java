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
package org.apache.maven.model.profile.activation;

import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.Profile;

/**
 * Tests {@link OperatingSystemProfileActivator}.
 *
 */
public class OperatingSystemProfileActivatorTest extends AbstractProfileActivatorTest<OperatingSystemProfileActivator> {

    public OperatingSystemProfileActivatorTest() throws Exception {
        super(OperatingSystemProfileActivator.class);
    }

    private Profile newProfile(ActivationOS os) {
        org.apache.maven.model.Activation a = new Activation();
        a.setOs(os);

        Profile p = new Profile();
        p.setActivation(a);

        return p;
    }

    private Properties newProperties(String osName, String osVersion, String osArch) {
        Properties props = new Properties();
        props.setProperty("os.name", osName);
        props.setProperty("os.version", osVersion);
        props.setProperty("os.arch", osArch);
        return props;
    }

    public void testVersionStringComparison() throws Exception {
        ActivationOS os = new ActivationOS();
        os.setVersion("6.5.0-1014-aws");
        Profile profile = newProfile(os);

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newProperties("linux", "3.1.0", "amd64")));
    }

    public void testVersionRegexMatching() throws Exception {
        ActivationOS os = new ActivationOS();
        os.setVersion("regex:.*aws");
        Profile profile = newProfile(os);

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));

        assertActivation(false, profile, newContext(null, newProperties("linux", "3.1.0", "amd64")));
    }

    public void testName() {
        ActivationOS os = new ActivationOS();
        os.setName("windows");
        Profile profile = newProfile(os);

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testNegatedName() {
        ActivationOS os = new ActivationOS();
        os.setName("!windows");
        Profile profile = newProfile(os);

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testArch() {
        ActivationOS os = new ActivationOS();
        os.setArch("amd64");
        Profile profile = newProfile(os);

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testNegatedArch() {
        ActivationOS os = new ActivationOS();
        os.setArch("!amd64");
        Profile profile = newProfile(os);

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testFamily() {
        ActivationOS os = new ActivationOS();
        os.setFamily("windows");
        Profile profile = newProfile(os);

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testNegatedFamily() {
        ActivationOS os = new ActivationOS();
        os.setFamily("!windows");
        Profile profile = newProfile(os);

        assertActivation(true, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "6.5.0-1014-aws", "aarch64")));
    }

    public void testAllOsConditions() {
        ActivationOS os = new ActivationOS();
        os.setFamily("windows");
        os.setName("windows");
        os.setArch("aarch64");
        os.setVersion("99");
        Profile profile = newProfile(os);

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("windows", "99", "aarch64")));
    }

    public void testCapitalOsName() {
        ActivationOS os = new ActivationOS();
        os.setFamily("Mac");
        os.setName("Mac OS X");
        os.setArch("aarch64");
        os.setVersion("14.5");
        Profile profile = newProfile(os);

        assertActivation(false, profile, newContext(null, newProperties("linux", "6.5.0-1014-aws", "amd64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "1", "aarch64")));
        assertActivation(false, profile, newContext(null, newProperties("windows", "99", "amd64")));
        assertActivation(true, profile, newContext(null, newProperties("Mac OS X", "14.5", "aarch64")));
    }
}
