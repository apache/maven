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
package org.apache.maven.profiles.manager;

import javax.inject.Inject;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@PlexusTest
@Deprecated
class DefaultProfileManagerTest {

    @Inject
    PlexusContainer container;

    protected PlexusContainer getContainer() {
        return container;
    }

    @Test
    void testShouldActivateDefaultProfile() throws Exception {
        Profile notActivated = new Profile();
        notActivated.setId("notActivated");

        Activation nonActivation = new Activation();

        nonActivation.setJdk("19.2");

        notActivated.setActivation(nonActivation);

        Profile defaultActivated = new Profile();
        defaultActivated.setId("defaultActivated");

        Activation defaultActivation = new Activation();

        defaultActivation.setActiveByDefault(true);

        defaultActivated.setActivation(defaultActivation);

        Properties props = System.getProperties();

        ProfileManager profileManager = new DefaultProfileManager(getContainer(), props);

        profileManager.addProfile(notActivated);
        profileManager.addProfile(defaultActivated);

        List active = profileManager.getActiveProfiles();

        assertNotNull(active);
        assertEquals(1, active.size());
        assertEquals("defaultActivated", ((Profile) active.get(0)).getId());
    }

    @Test
    void testShouldNotActivateDefaultProfile() throws Exception {
        Profile syspropActivated = new Profile();
        syspropActivated.setId("syspropActivated");

        Activation syspropActivation = new Activation();

        ActivationProperty syspropProperty = new ActivationProperty();
        syspropProperty.setName("java.version");

        syspropActivation.setProperty(syspropProperty);

        syspropActivated.setActivation(syspropActivation);

        Profile defaultActivated = new Profile();
        defaultActivated.setId("defaultActivated");

        Activation defaultActivation = new Activation();

        defaultActivation.setActiveByDefault(true);

        defaultActivated.setActivation(defaultActivation);

        Properties props = System.getProperties();

        ProfileManager profileManager = new DefaultProfileManager(getContainer(), props);

        profileManager.addProfile(syspropActivated);
        profileManager.addProfile(defaultActivated);

        List active = profileManager.getActiveProfiles();

        assertNotNull(active);
        assertEquals(1, active.size());
        assertEquals("syspropActivated", ((Profile) active.get(0)).getId());
    }

    @Test
    void testShouldNotActivateReversalOfPresentSystemProperty() throws Exception {
        Profile syspropActivated = new Profile();
        syspropActivated.setId("syspropActivated");

        Activation syspropActivation = new Activation();

        ActivationProperty syspropProperty = new ActivationProperty();
        syspropProperty.setName("!java.version");

        syspropActivation.setProperty(syspropProperty);

        syspropActivated.setActivation(syspropActivation);

        Properties props = System.getProperties();

        ProfileManager profileManager = new DefaultProfileManager(getContainer(), props);

        profileManager.addProfile(syspropActivated);

        List active = profileManager.getActiveProfiles();

        assertNotNull(active);
        assertEquals(0, active.size());
    }

    @Test
    void testShouldOverrideAndActivateInactiveProfile() throws Exception {
        Profile syspropActivated = new Profile();
        syspropActivated.setId("syspropActivated");

        Activation syspropActivation = new Activation();

        ActivationProperty syspropProperty = new ActivationProperty();
        syspropProperty.setName("!java.version");

        syspropActivation.setProperty(syspropProperty);

        syspropActivated.setActivation(syspropActivation);

        Properties props = System.getProperties();

        ProfileManager profileManager = new DefaultProfileManager(getContainer(), props);

        profileManager.addProfile(syspropActivated);

        profileManager.explicitlyActivate("syspropActivated");

        List active = profileManager.getActiveProfiles();

        assertNotNull(active);
        assertEquals(1, active.size());
        assertEquals("syspropActivated", ((Profile) active.get(0)).getId());
    }

    @Test
    void testShouldOverrideAndDeactivateActiveProfile() throws Exception {
        Profile syspropActivated = new Profile();
        syspropActivated.setId("syspropActivated");

        Activation syspropActivation = new Activation();

        ActivationProperty syspropProperty = new ActivationProperty();
        syspropProperty.setName("java.version");

        syspropActivation.setProperty(syspropProperty);

        syspropActivated.setActivation(syspropActivation);

        Properties props = System.getProperties();

        ProfileManager profileManager = new DefaultProfileManager(getContainer(), props);

        profileManager.addProfile(syspropActivated);

        profileManager.explicitlyDeactivate("syspropActivated");

        List active = profileManager.getActiveProfiles();

        assertNotNull(active);
        assertEquals(0, active.size());
    }

    @Test
    @Disabled
    void testOsActivationProfile() throws Exception {
        /*
        Profile osActivated = new Profile();
        osActivated.setId( "os-profile" );

        Activation osActivation = new Activation();

        ActivationOS activationOS = new ActivationOS();

        activationOS.setName( "!dddd" );

        osActivation.setOs( activationOS );

        osActivated.setActivation( osActivation );

        Properties props = System.getProperties();
        ProfileActivationContext ctx = new ProfileActivationContext( props, false );

        ProfileManager profileManager = new DefaultProfileManager( getContainer(), props );

        profileManager.addProfile( osActivated );

        List active = profileManager.getActiveProfiles( null );

        assertNotNull( active );
        assertEquals( 1, active.size() );
        */
    }
}
