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
package org.apache.maven.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import junit.framework.TestCase;

public class SettingsUtilsTest extends TestCase {

    public void testShouldAppendRecessivePluginGroupIds() {
        Settings dominant = new Settings();
        dominant.addPluginGroup("org.apache.maven.plugins");
        dominant.addPluginGroup("org.codehaus.modello");

        Settings recessive = new Settings();
        recessive.addPluginGroup("org.codehaus.plexus");

        SettingsUtils.merge(dominant, recessive, Settings.GLOBAL_LEVEL);

        List<String> pluginGroups = dominant.getPluginGroups();

        assertNotNull(pluginGroups);
        assertEquals(3, pluginGroups.size());
        assertEquals("org.apache.maven.plugins", pluginGroups.get(0));
        assertEquals("org.codehaus.modello", pluginGroups.get(1));
        assertEquals("org.codehaus.plexus", pluginGroups.get(2));
    }

    public void testRoundTripProfiles() {
        Random entropy = new Random();
        Profile p = new Profile();
        p.setId("id" + Long.toHexString(entropy.nextLong()));
        Activation a = new Activation();
        a.setActiveByDefault(entropy.nextBoolean());
        a.setJdk("jdk" + Long.toHexString(entropy.nextLong()));
        ActivationFile af = new ActivationFile();
        af.setExists("exists" + Long.toHexString(entropy.nextLong()));
        af.setMissing("missing" + Long.toHexString(entropy.nextLong()));
        a.setFile(af);
        ActivationProperty ap = new ActivationProperty();
        ap.setName("name" + Long.toHexString(entropy.nextLong()));
        ap.setValue("value" + Long.toHexString(entropy.nextLong()));
        a.setProperty(ap);
        ActivationOS ao = new ActivationOS();
        ao.setArch("arch" + Long.toHexString(entropy.nextLong()));
        ao.setFamily("family" + Long.toHexString(entropy.nextLong()));
        ao.setName("name" + Long.toHexString(entropy.nextLong()));
        ao.setVersion("version" + Long.toHexString(entropy.nextLong()));
        a.setOs(ao);
        p.setActivation(a);
        Properties props = new Properties();
        int count = entropy.nextInt(10);
        for (int i = 0; i < count; i++) {
            props.setProperty(
                    "name" + Long.toHexString(entropy.nextLong()), "value" + Long.toHexString(entropy.nextLong()));
        }
        p.setProperties(props);
        count = entropy.nextInt(3);
        List<Repository> repos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Repository r = new Repository();
            r.setId("id" + Long.toHexString(entropy.nextLong()));
            r.setName("name" + Long.toHexString(entropy.nextLong()));
            r.setUrl("url" + Long.toHexString(entropy.nextLong()));
            repos.add(r);
        }
        p.setRepositories(repos);
        count = entropy.nextInt(3);
        repos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Repository r = new Repository();
            r.setId("id" + Long.toHexString(entropy.nextLong()));
            r.setName("name" + Long.toHexString(entropy.nextLong()));
            r.setUrl("url" + Long.toHexString(entropy.nextLong()));
            repos.add(r);
        }
        p.setPluginRepositories(repos);

        Profile clone = SettingsUtils.convertToSettingsProfile(SettingsUtils.convertFromSettingsProfile(p));

        assertEquals(p.getId(), clone.getId());
        assertEquals(p.getActivation().getJdk(), clone.getActivation().getJdk());
        assertEquals(
                p.getActivation().getFile().getExists(),
                clone.getActivation().getFile().getExists());
        assertEquals(
                p.getActivation().getFile().getMissing(),
                clone.getActivation().getFile().getMissing());
        assertEquals(
                p.getActivation().getProperty().getName(),
                clone.getActivation().getProperty().getName());
        assertEquals(
                p.getActivation().getProperty().getValue(),
                clone.getActivation().getProperty().getValue());
        assertEquals(
                p.getActivation().getOs().getArch(),
                clone.getActivation().getOs().getArch());
        assertEquals(
                p.getActivation().getOs().getFamily(),
                clone.getActivation().getOs().getFamily());
        assertEquals(
                p.getActivation().getOs().getName(),
                clone.getActivation().getOs().getName());
        assertEquals(
                p.getActivation().getOs().getVersion(),
                clone.getActivation().getOs().getVersion());
        assertEquals(p.getProperties(), clone.getProperties());
        assertEquals(p.getRepositories().size(), clone.getRepositories().size());
        // TODO deep compare the lists
        assertEquals(
                p.getPluginRepositories().size(), clone.getPluginRepositories().size());
        // TODO deep compare the lists
    }
}
