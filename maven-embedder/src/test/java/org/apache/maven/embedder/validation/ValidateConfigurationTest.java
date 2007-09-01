package org.apache.maven.embedder.validation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.embedder.Configuration;
import org.apache.maven.embedder.ConfigurationValidationResult;
import org.apache.maven.embedder.DefaultConfiguration;
import org.apache.maven.embedder.MavenEmbedder;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;

/** @author Jason van Zyl */
public class ValidateConfigurationTest
    extends PlexusTestCase
{
    public void testConfigurationOnlyUserSettingsAreActiveAndItIsValid()
    {
        File user = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isUserSettingsFilePresent() );
        assertTrue( result.isUserSettingsFileParses() );
        assertNotNull( result.getUserSettings() );
        assertNull( result.getUserSettingsException() );
        assertNull( result.getGlobalSettings() );
        assertNull( result.getGlobalSettingsException() );
    }

    public void testConfigurationOnlyUserSettingsAreActiveAndItIsInvalid()
    {
        File user = new File( getBasedir(), "src/test/resources/settings/invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setUserSettingsFile( user );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isUserSettingsFilePresent() );
        assertFalse( result.isUserSettingsFileParses() );
        assertNull( result.getUserSettings() );
        assertNotNull( result.getUserSettingsException() );
        assertTrue( result.getUserSettingsException() instanceof XmlPullParserException );
        assertNull( result.getGlobalSettings() );
        assertNull( result.getGlobalSettingsException() );
    }

    public void testConfigurationOnlyGlobalSettingsAreActiveAndItIsValid()
    {
        File global = new File( getBasedir(), "src/test/resources/settings/valid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setGlobalSettingsFile( global );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isGlobalSettingsFilePresent() );
        assertTrue( result.isGlobalSettingsFileParses() );
        assertNotNull( result.getGlobalSettings() );
        assertNull( result.getGlobalSettingsException() );
        assertNull( result.getUserSettings() );
        assertNull( result.getUserSettingsException() );
    }

    public void testConfigurationOnlyGlobalSettingsAreActiveAndItIsInvalid()
    {
        File global = new File( getBasedir(), "src/test/resources/settings/invalid-settings.xml" );

        Configuration configuration = new DefaultConfiguration()
            .setGlobalSettingsFile( global );

        ConfigurationValidationResult result = MavenEmbedder.validateConfiguration( configuration );

        assertTrue( result.isGlobalSettingsFilePresent() );
        assertFalse( result.isGlobalSettingsFileParses() );
        assertNull( result.getGlobalSettings() );
        assertNotNull( result.getGlobalSettingsException() );
        assertTrue( result.getGlobalSettingsException() instanceof XmlPullParserException );
        assertNull( result.getUserSettings() );
        assertNull( result.getUserSettingsException() );
    }
}
