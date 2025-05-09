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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@PlexusTest
@Deprecated
class DefaultProfileManagerTest {

    @Inject
    PlexusContainer container;

    protected PlexusContainer getContainer() {
        return container;
    }

    @Test
    void shouldActivateDefaultProfile() throws Exception {
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

        assertThat(active).isNotNull();
        assertThat(active.size()).isEqualTo(1);
        assertThat(((Profile) active.get(0)).getId()).isEqualTo("defaultActivated");
    }

    @Test
    void shouldNotActivateDefaultProfile() throws Exception {
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

        assertThat(active).isNotNull();
        assertThat(active.size()).isEqualTo(1);
        assertThat(((Profile) active.get(0)).getId()).isEqualTo("syspropActivated");
    }

    @Test
    void shouldNotActivateReversalOfPresentSystemProperty() throws Exception {
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

        assertThat(active).isNotNull();
        assertThat(active.size()).isEqualTo(0);
    }

    @Test
    void shouldOverrideAndActivateInactiveProfile() throws Exception {
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

        assertThat(active).isNotNull();
        assertThat(active.size()).isEqualTo(1);
        assertThat(((Profile) active.get(0)).getId()).isEqualTo("syspropActivated");
    }

    @Test
    void shouldOverrideAndDeactivateActiveProfile() throws Exception {
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

        assertThat(active).isNotNull();
        assertThat(active.size()).isEqualTo(0);
    }
}
