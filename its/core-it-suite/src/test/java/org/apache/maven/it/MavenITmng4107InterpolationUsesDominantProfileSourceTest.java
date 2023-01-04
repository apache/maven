package org.apache.maven.it;

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

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

import java.io.File;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4107">MNG-4107</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4107InterpolationUsesDominantProfileSourceTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4107InterpolationUsesDominantProfileSourceTest()
    {
        super( "[2.0.5,)" );
    }

    /**
     * Test that POM interpolation uses the property values from the dominant profile source (POM vs. profiles.xml
     * vs. settings.xml). This boils down to the proper order of profile injection and interpolation, i.e.
     * interpolate after profiles from all sources are injected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4107()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4107" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        assertEquals( "applied", props.getProperty( "project.properties.pomProfile" ) );
        assertEquals( "applied", props.getProperty( "project.properties.settingsProfile" ) );
        assertEquals( "settings", props.getProperty( "project.properties.pomVsSettings" ) );
        assertEquals( "settings", props.getProperty( "project.properties.pomVsSettingsInterpolated" ) );

        if ( matchesVersionRange( "(,3.0-alpha-1)" ) )
        {
            // MNG-4060, profiles.xml support dropped
            assertEquals( "applied", props.getProperty( "project.properties.profilesProfile" ) );
            assertEquals( "profiles", props.getProperty( "project.properties.pomVsProfiles" ) );
            assertEquals( "profiles", props.getProperty( "project.properties.pomVsProfilesInterpolated" ) );
            assertEquals( "settings", props.getProperty( "project.properties.profilesVsSettings" ) );
            assertEquals( "settings", props.getProperty( "project.properties.profilesVsSettingsInterpolated" ) );
        }
    }

}
