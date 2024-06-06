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
package org.apache.maven.model.profile;

import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.SimpleProblemCollector;
import org.apache.maven.model.profile.activation.ProfileActivator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link DefaultProfileSelector}.
 */
public class DefaultProfileSelectorTest {
    private Profile newProfile(String id) {
        Activation activation = new Activation();
        Profile profile = new Profile();
        profile.setId(id);
        profile.setActivation(activation);
        return profile;
    }

    @Test
    public void testThrowingActivator() {
        DefaultProfileSelector selector = new DefaultProfileSelector();
        selector.addProfileActivator(new ProfileActivator() {
            @Override
            public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
                throw new RuntimeException("BOOM");
            }

            @Override
            public boolean presentInConfig(
                    Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
                return true;
            }
        });

        List<Profile> profiles = Collections.singletonList(newProfile("one"));
        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        SimpleProblemCollector problems = new SimpleProblemCollector();
        List<Profile> active = selector.getActiveProfiles(profiles, context, problems);
        assertTrue(active.isEmpty());
        assertEquals(1, problems.getErrors().size());
        assertEquals(
                "Failed to determine activation for profile one: BOOM",
                problems.getErrors().get(0));
    }
}
