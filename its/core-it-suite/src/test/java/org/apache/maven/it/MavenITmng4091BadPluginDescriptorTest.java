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
import org.apache.maven.shared.verifier.VerificationException;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4091">MNG-4091</a>:
 * Bad plugin descriptor error handling
 */
public class MavenITmng4091BadPluginDescriptorTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4091BadPluginDescriptorTest()
    {
        super( "[2.1.0,)" ); // only test in 2.1.0+
    }

    @Test
    public void testitMNG4091_InvalidDescriptor()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4091/invalid" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        try
        {
            verifier.addCliArgument( "validate" );
            verifier.execute();

            fail( "should throw an error during execution." );
        }
        catch ( VerificationException e )
        {
            // expected...it'd be nice if we could get the specifics of the exception right here...
        }

        List<String> logFile = verifier.loadFile( verifier.getBasedir(), verifier.getLogFileName(), false );

        String msg = "Plugin's descriptor contains the wrong version: 2.0-SNAPSHOT";

        boolean foundMessage = false;
        for ( String line : logFile )
        {
            if ( line.contains( msg ) )
            {
                foundMessage = true;
                break;
            }
        }

        assertTrue( "User-friendly message was not found in output.", foundMessage );
    }

    @Test
    public void testitMNG4091_PluginDependency()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4091/plugin-dependency" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );

        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props = verifier.loadProperties( "target/plugin-dependency.properties" );
        assertTrue( props.isEmpty() );
    }
}

