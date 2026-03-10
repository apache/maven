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
package org.apache.maven.settings.building;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Benjamin Bentmann
 */
public class DefaultSettingsBuilderFactoryTest {

    private File getSettings(String name) {
        return new File("src/test/resources/settings/factory/" + name + ".xml").getAbsoluteFile();
    }

    @Test
    public void testCompleteWiring() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("simple"));

        SettingsBuildingResult result = builder.build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveSettings());
    }

    @Test
    public void testNonStringInterpolationHappyPath() throws Exception {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        boolean testActive = true;
        int testPort = 2026;
        Properties userProperties = new Properties();
        userProperties.setProperty("test.active", Boolean.toString(testActive));
        userProperties.setProperty("test.port", Integer.toString(testPort));
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        SettingsBuildingResult result = builder.build(request);
        assertNotNull(result);
        assertNotNull(result.getEffectiveSettings());
        assertEquals(
                testActive, result.getEffectiveSettings().getProxies().get(0).isActive());
        assertEquals(testPort, result.getEffectiveSettings().getProxies().get(0).getPort());
    }

    @Test
    public void testNonStringInterpolationNonHappyPath() {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        Properties userProperties = new Properties();
        userProperties.setProperty("test.active", "yes");
        userProperties.setProperty("test.port", "foo");
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        assertThrows(SettingsBuildingException.class, () -> builder.build(request));
    }

    @Test
    public void testNonStringInterpolationMissingProperties() {
        SettingsBuilder builder = new DefaultSettingsBuilderFactory().newInstance();
        assertNotNull(builder);

        Properties userProperties = new Properties();
        userProperties.setProperty("maven.settings.strictParsing", Boolean.TRUE.toString());

        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setUserProperties(userProperties);
        request.setSystemProperties(System.getProperties());
        request.setUserSettingsFile(getSettings("proxy"));

        assertThrows(SettingsBuildingException.class, () -> builder.build(request));
    }
}
