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

import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.ActivationProperty;
import org.apache.maven.api.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PropertyProfileActivator}.
 *
 */
class PropertyProfileActivatorTest extends AbstractProfileActivatorTest<PropertyProfileActivator> {

    @BeforeEach
    @Override
    void setUp() throws Exception {
        activator = new PropertyProfileActivator();
    }

    private Profile newProfile(String key, String value) {
        ActivationProperty ap =
                ActivationProperty.newBuilder().name(key).value(value).build();

        Activation a = Activation.newBuilder().property(ap).build();

        Profile p = Profile.newBuilder().activation(a).build();

        return p;
    }

    private Properties newProperties(String key, String value) {
        Properties props = new Properties();
        props.setProperty(key, value);
        return props;
    }

    @Test
    void testNullSafe() throws Exception {
        Profile p = Profile.newInstance();

        assertActivation(false, p, newContext(null, null));

        p = p.withActivation(Activation.newInstance());

        assertActivation(false, p, newContext(null, null));
    }

    @Test
    void testWithNameOnly_UserProperty() throws Exception {
        Profile profile = newProfile("prop", null);

        assertActivation(true, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(false, profile, newContext(newProperties("prop", ""), null));

        assertActivation(false, profile, newContext(newProperties("other", "value"), null));
    }

    @Test
    void testWithNameOnly_SystemProperty() throws Exception {
        Profile profile = newProfile("prop", null);

        assertActivation(true, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(false, profile, newContext(null, newProperties("prop", "")));

        assertActivation(false, profile, newContext(null, newProperties("other", "value")));
    }

    @Test
    void testWithNegatedNameOnly_UserProperty() throws Exception {
        Profile profile = newProfile("!prop", null);

        assertActivation(false, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(true, profile, newContext(newProperties("prop", ""), null));

        assertActivation(true, profile, newContext(newProperties("other", "value"), null));
    }

    @Test
    void testWithNegatedNameOnly_SystemProperty() throws Exception {
        Profile profile = newProfile("!prop", null);

        assertActivation(false, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(true, profile, newContext(null, newProperties("prop", "")));

        assertActivation(true, profile, newContext(null, newProperties("other", "value")));
    }

    @Test
    void testWithValue_UserProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        assertActivation(true, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(false, profile, newContext(newProperties("prop", "other"), null));

        assertActivation(false, profile, newContext(newProperties("prop", ""), null));
    }

    @Test
    void testWithValue_SystemProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        assertActivation(true, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(false, profile, newContext(null, newProperties("prop", "other")));

        assertActivation(false, profile, newContext(null, newProperties("other", "")));
    }

    @Test
    void testWithNegatedValue_UserProperty() throws Exception {
        Profile profile = newProfile("prop", "!value");

        assertActivation(false, profile, newContext(newProperties("prop", "value"), null));

        assertActivation(true, profile, newContext(newProperties("prop", "other"), null));

        assertActivation(true, profile, newContext(newProperties("prop", ""), null));
    }

    @Test
    void testWithNegatedValue_SystemProperty() throws Exception {
        Profile profile = newProfile("prop", "!value");

        assertActivation(false, profile, newContext(null, newProperties("prop", "value")));

        assertActivation(true, profile, newContext(null, newProperties("prop", "other")));

        assertActivation(true, profile, newContext(null, newProperties("other", "")));
    }

    @Test
    void testWithValue_UserPropertyDominantOverSystemProperty() throws Exception {
        Profile profile = newProfile("prop", "value");

        Properties props1 = newProperties("prop", "value");
        Properties props2 = newProperties("prop", "other");

        assertActivation(true, profile, newContext(props1, props2));

        assertActivation(false, profile, newContext(props2, props1));
    }
}
