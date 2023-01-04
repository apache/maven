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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4106">MNG-4106</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4106InterpolationUsesDominantProfileTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4106InterpolationUsesDominantProfileTest()
    {
        super( "[2.0.5,)" );
    }

    /**
     * Test that interpolation uses the property values from the dominant (i.e. last) profile among a group
     * of active profiles that define the same properties. This boils down to the proper order of profile
     * injection and interpolation, i.e. interpolate after all profiles are injected.
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testitMNG4106()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4106" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliArgument( "--settings" );
        verifier.addCliArgument( "settings.xml" );
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            verifier.addCliArgument( "-Ppom-a,pom-b,settings-a,settings-b" );
        }
        else
        {
            verifier.addCliArgument( "-Ppom-a,pom-b,profiles-a,profiles-b,settings-a,settings-b" );
        }
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/pom.properties" );

        assertEquals( "b", props.getProperty( "project.properties.pomProperty" ) );
        assertEquals( "b", props.getProperty( "project.properties.pom" ) );

        assertEquals( "b", props.getProperty( "project.properties.settingsProperty" ) );
        assertEquals( "b", props.getProperty( "project.properties.settings" ) );

        if ( matchesVersionRange( "(,3.0-alpha-1)" ) )
        {
            // MNG-4060, profiles.xml support dropped
            assertEquals( "b", props.getProperty( "project.properties.profilesProperty" ) );
            assertEquals( "b", props.getProperty( "project.properties.profiles" ) );
        }
    }

}
