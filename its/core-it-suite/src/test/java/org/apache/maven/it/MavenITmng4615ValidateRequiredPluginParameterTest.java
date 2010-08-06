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

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.Properties;

/**
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-4615">MNG-4615</a>.
 * 
 * @author Benjamin Bentmann
 */
public class MavenITmng4615ValidateRequiredPluginParameterTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4615ValidateRequiredPluginParameterTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-beta-2,)" );
    }

    /**
     * Verify that Maven validates required mojo parameters (and doesn't just have the plugins die with NPEs).
     */
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4615" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        try
        {
            verifier.executeGoal( "validate" );
            verifier.verifyErrorFreeLog();
            fail( "Build did not fail despite required plugin parameter missing" );
        }
        catch ( VerificationException e )
        {
            // expected
        }

        // sanity check that adding the param makes it work

        verifier.setLogFileName( "log-2.txt" );
        verifier.getCliOptions().add( "-Pmng4615" );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        Properties props = verifier.loadProperties( "target/config.properties" );
        assertEquals( "PROFILE", props.get( "requiredParam" ) );

        verifier.setLogFileName( "log-3.txt" );
        verifier.getCliOptions().remove( "-Pmng4615" );
        verifier.setSystemProperty( "config.requiredParam", "CLI" );
        verifier.deleteDirectory( "target" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        props = verifier.loadProperties( "target/config.properties" );
        assertEquals( "CLI", props.get( "requiredParam" ) );

        verifier.resetStreams();
    }

}
