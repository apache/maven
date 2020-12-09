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

import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3732">MNG-3732</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng3732ActiveProfilesTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3732ActiveProfilesTest()
    {
        super( "[2.0,)" );
    }

    /**
     * Verify that MavenProject.getActiveProfiles() includes profiles from all sources.
     */
    public void testitMNG3732()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3732" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        if ( matchesVersionRange( "[4.0.0-alpha-1,)" ) )
        {
            verifier.addCliOption( "-Ppom,settings" );
        }
        else
        {
            verifier.addCliOption( "-Ppom,profiles,settings" );
        }
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/profile.properties" );
        List<String> ids = new ArrayList<>();

        // support for profiles.xml removed from 3.x (see MNG-4060)
        if ( matchesVersionRange( "[2.0,3.0-alpha-1)" ) )
        {
            ids.add( props.getProperty( "project.activeProfiles.0.id", "" ) );
            ids.add( props.getProperty( "project.activeProfiles.1.id", "" ) );
            ids.add( props.getProperty( "project.activeProfiles.2.id", "" ) );
            ids.add( props.getProperty( "project.activeProfiles.3.id", "" ) );
            ids.remove( "it-defaults" );
            Collections.sort( ids );

            assertEquals( Arrays.asList( new String[]{ "pom", "profiles", "settings" } ), ids );
            assertEquals( "4", props.getProperty( "project.activeProfiles" ) );

            assertEquals( "PASSED-1", props.getProperty( "project.properties.pomProperty" ) );
            assertEquals( "PASSED-2", props.getProperty( "project.properties.settingsProperty" ) );
            assertEquals( "PASSED-3", props.getProperty( "project.properties.profilesProperty" ) );
        }
        else
        {
            ids.add( props.getProperty( "project.activeProfiles.0.id", "" ) );
            ids.add( props.getProperty( "project.activeProfiles.1.id", "" ) );
            ids.add( props.getProperty( "project.activeProfiles.2.id", "" ) );
            ids.remove( "it-defaults" );
            Collections.sort( ids );

            assertEquals( Arrays.asList( new String[]{ "pom", "settings" } ), ids );
            assertEquals( "3", props.getProperty( "project.activeProfiles" ) );

            assertEquals( "PASSED-1", props.getProperty( "project.properties.pomProperty" ) );
            assertEquals( "PASSED-2", props.getProperty( "project.properties.settingsProperty" ) );
        }
    }

}
