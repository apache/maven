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

import java.util.Objects;
import java.util.Properties;

import junit.framework.TestCase;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;

/**
 * Provides common services to test {@link ProfileActivator} implementations.
 *
 * @author Benjamin Bentmann
 */
public abstract class AbstractProfileActivatorTest<T extends ProfileActivator> extends TestCase {

    private Class<T> activatorClass;

    protected T activator;

    public AbstractProfileActivatorTest(Class<T> activatorClass) {
        this.activatorClass = Objects.requireNonNull(activatorClass, "activatorClass cannot be null");
        ;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        activator = activatorClass.getConstructor().newInstance();
    }

    @Override
    protected void tearDown() throws Exception {
        activator = null;

        super.tearDown();
    }

    protected ProfileActivationContext newContext(final Properties userProperties, final Properties systemProperties) {
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        return context.setUserProperties(userProperties).setSystemProperties(systemProperties);
    }

    protected void assertActivation(boolean active, Profile profile, ProfileActivationContext context) {
        SimpleProblemCollector problems = new SimpleProblemCollector();

        assertEquals(active, activator.isActive(profile, context, problems));

        assertEquals(problems.getErrors().toString(), 0, problems.getErrors().size());
        assertEquals(
                problems.getWarnings().toString(), 0, problems.getWarnings().size());
    }
}
