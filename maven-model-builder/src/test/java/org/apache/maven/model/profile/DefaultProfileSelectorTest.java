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
import java.util.Map;
import java.util.Optional;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.interpolation.ModelInterpolator;
import org.apache.maven.model.profile.activation.PropertyProfileActivator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class DefaultProfileSelectorTest {
    private DefaultProfileSelector selector;
    private ModelInterpolator interpolator;

    @Before
    public void setup() {
        interpolator = Mockito.mock(ModelInterpolator.class);

        selector = new DefaultProfileSelector();
        selector.addProfileActivator(new PropertyProfileActivator());
        selector.setInterpolator(interpolator);
    }

    @Test
    public void testProfileActivationInterpolation() {
        Map<String, String> userProperties = Collections.singletonMap("foo", "bar");

        Mockito.when(interpolator.interpolateModel(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> {
                    Model m = invocation.getArgument(0);

                    m.getProfiles().forEach(p -> {
                        Optional.ofNullable(p.getActivation())
                                .map(Activation::getProperty)
                                .ifPresent(ap -> {
                                    String name = ap.getName();
                                    if (name != null) {
                                        ap.setValue(userProperties.get(name));
                                    }
                                });
                    });
                    return m;
                });

        ActivationProperty ap = new ActivationProperty();
        ap.setName("foo");

        Activation act = new Activation();
        act.setProperty(ap);
        Profile profile = new Profile();
        profile.setId("foo");
        profile.setActivation(act);

        DefaultProfileActivationContext context = new DefaultProfileActivationContext();
        context.setUserProperties(userProperties);

        List<Profile> activeProfiles = selector.getActiveProfiles(Collections.singleton(profile), context, p -> {});

        assertEquals(1, activeProfiles.size());
        assertSame(profile, activeProfiles.get(0));
    }
}
