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
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.profile.ProfileActivationContext;

/**
 * Tests {@link JdkVersionProfileActivator}.
 *
 * @author Benjamin Bentmann
 */
public class JdkVersionProfileActivatorTest extends AbstractProfileActivatorTest<JdkVersionProfileActivator> {

    public JdkVersionProfileActivatorTest() {
        super(JdkVersionProfileActivator.class);
    }

    private Profile newProfile(String jdkVersion) {
        Activation a = new Activation();
        a.setJdk(jdkVersion);

        Profile p = new Profile();
        p.setActivation(a);

        return p;
    }

    private Properties newProperties(String javaVersion) {
        Properties props = new Properties();
        props.setProperty("java.version", javaVersion);
        return props;
    }

    public void testNullSafe() throws Exception {
        Profile p = new Profile();

        assertActivation(false, p, newContext(null, null));

        p.setActivation(new Activation());

        assertActivation(false, p, newContext(null, null));
    }

    public void testPrefix() throws Exception {
        Profile profile = newProfile("1.4");

        assertActivation(true, profile, newContext(null, newProperties("1.4")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(true, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(false, profile, newContext(null, newProperties("1.3")));

        assertActivation(false, profile, newContext(null, newProperties("1.5")));
    }

    public void testPrefixNegated() throws Exception {
        Profile profile = newProfile("!1.4");

        assertActivation(false, profile, newContext(null, newProperties("1.4")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09")));
        assertActivation(false, profile, newContext(null, newProperties("1.4.2_09-b03")));

        assertActivation(true, profile, newContext(null, newProperties("1.3")));

        assertActivation(true, profile, newContext(null, newProperties("1.5")));
    }

    public void testVersionRangeInclusiveBounds() throws Exception {
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

    public void testVersionRangeExclusiveBounds() throws Exception {
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

    public void testVersionRangeInclusiveLowerBound() throws Exception {
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

    public void testVersionRangeExclusiveUpperBound() throws Exception {
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

    public void testRubbishJavaVersion() {
        Profile profile = newProfile("[1.8,)");

        assertActivationWithProblems(profile, newContext(null, newProperties("Pūteketeke")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("rubbish")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("1.a.0_09")), "invalid JDK version");
        assertActivationWithProblems(profile, newContext(null, newProperties("1.a.2.b")), "invalid JDK version");
    }

    private void assertActivationWithProblems(
            Profile profile, ProfileActivationContext context, String warningContains) {
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertEquals(false, activator.isActive(profile, context, problems));

        assertEquals(problems.getErrors().toString(), 0, problems.getErrors().size());
        assertEquals(
                problems.getWarnings().toString(), 1, problems.getWarnings().size());
        assertTrue(problems.getWarnings().get(0), problems.getWarnings().get(0).contains(warningContains));
    }
}
