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
import java.util.Properties;

import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.apache.maven.shared.verifier.Verifier;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-2309">MNG-2309</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng2309ProfileInjectionOrderTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng2309ProfileInjectionOrderTest()
    {
        super( "[2.0.5,)" );
    }

    /**
     * Test that profiles are injected in declaration order, with the last profile being the most dominant.
     *
     * @throws Exception in case of failure
     */
    public void testitMNG2309()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-2309" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            verifier.addCliOption( "-P"
                    + "pom-a,pom-b,pom-e,pom-c,pom-d"
                    + ",settings-a,settings-b,settings-e,settings-c,settings-d" );
        }
        else
        {
            verifier.addCliOption( "-P"
                    + "pom-a,pom-b,pom-e,pom-c,pom-d"
                    + ",profiles-a,profiles-b,profiles-e,profiles-c,profiles-d"
                    + ",settings-a,settings-b,settings-e,settings-c,settings-d" );
        }
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "e", props.getProperty( "project.properties.pomProperty" ) );
        assertEquals( "e", props.getProperty( "project.properties.settingsProperty" ) );
        if ( matchesVersionRange( "(,3.0-alpha-1)" ) )
        {
            // MNG-4060, profiles.xml support dropped
            assertEquals( "e", props.getProperty( "project.properties.profilesProperty" ) );
        }
    }

}
