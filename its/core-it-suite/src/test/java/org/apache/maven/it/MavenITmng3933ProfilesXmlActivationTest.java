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

import java.io.File;
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.shared.utils.Os;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3933">MNG-3933</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3933ProfilesXmlActivationTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3933ProfilesXmlActivationTest()
    {
        // support for profiles.xml removed from 3.x (see MNG-4060)
        super( "[2.0,3.0-alpha-1)" );
    }

    /**
     * Test that profiles from an external profiles.xml are properly activated. This is really a different story
     * than profiles in the settings.xml or the POM.
     */
    public void testitMNG3933()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3933" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.getSystemProperties().setProperty( "maven.profile.activator", "test" );
        verifier.executeGoal( "validate", Collections.singletonMap( "MAVEN_PROFILE", "test" ) );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/profile.properties" );

        assertEquals( "DEFAULT-ACTIVATION", props.getProperty( "project.properties.defaultProperty" ) );

        assertEquals( "SYS-PROP-ACTIVATION", props.getProperty( "project.properties.sysProperty" ) );

        if ( matchesVersionRange( "(2.0.8,)" ) )
        {
            // MNG-2848
            assertEquals( "ENV-PROP-ACTIVATION", props.getProperty( "project.properties.envProperty" ) );
        }

        assertEquals( "MISSING-FILE-ACTIVATION", props.getProperty( "project.properties.fileProperty" ) );

        assertEquals( "JDK-ACTIVATION", props.getProperty( "project.properties.jdkProperty" ) );

        if ( matchesVersionRange( "(2.0.10,2.1.0-M1),(2.1.0-M1,)" ) )
        {
            // MNG-3933
            if ( Os.isFamily( Os.FAMILY_WINDOWS ) || Os.isFamily( Os.FAMILY_MAC ) || Os.isFamily( Os.FAMILY_UNIX ) )
            {
                assertEquals( "OS-FAMILY-ACTIVATION", props.getProperty( "project.properties.osFamilyProperty" ) );
            }
            else
            {
                System.out.println();
                System.out.println( "[WARNING] Skipping OS activation test on unrecognized OS: " + Os.OS_NAME );
                System.out.println();
            }
        }

        assertNull( props.getProperty( "project.properties.sysPropertyMissing" ) );
        assertNull( props.getProperty( "project.properties.envPropertyMissing" ) );
        assertNull( props.getProperty( "project.properties.filePropertyMissing" ) );
    }

}
