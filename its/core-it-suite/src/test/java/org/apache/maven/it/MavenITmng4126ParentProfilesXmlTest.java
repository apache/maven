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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4126">MNG-4126</a>.
 *
 * @author Benjamin Bentmann
 *
 */
public class MavenITmng4126ParentProfilesXmlTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4126ParentProfilesXmlTest()
    {
        // MNG-4060, profiles.xml support dropped
        super( "[2.0,2.1.0),(2.1.0,3.0-alpha-1)" );
    }

    /**
     * Verify that effects of active profiles from the profiles.xml of a local parent are inherited by children when
     * parent and child are build together during a reactor invocation. This boils down to the reactor cache not
     * interfering with profile injection from profiles.xml by properly tracking the base directory of cached projects.
     */
    public void testitReactorBuild()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4126" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "sub/target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "sub/target/pom.properties" );
        assertEquals( "PASSED", props.getProperty( "project.properties.testProperty" ) );
    }

    /**
     * Verify that effects of active profiles from the profiles.xml of a local parent are inherited by children when
     * parent and child are build together during a reactor invocation. This boils down to the reactor cache not
     * interfering with profile injection from profiles.xml by properly tracking the base directory of cached projects.
     */
    public void testitChildOnlyBuild()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4126" );

        Verifier verifier = newVerifier( new File( testDir, "sub" ).getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/pom.properties" );
        assertEquals( "PASSED", props.getProperty( "project.properties.testProperty" ) );
    }

}
