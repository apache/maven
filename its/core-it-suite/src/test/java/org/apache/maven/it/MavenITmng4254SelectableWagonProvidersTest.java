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
import java.io.IOException;
import java.util.Properties;

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4254">MNG-4254</a>.
 *
 * @author John Casey
 *
 */
public class MavenITmng4254SelectableWagonProvidersTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4254SelectableWagonProvidersTest()
    {
        // not supported in 3.x, there will be a single HTTP wagon
        super( "(2.2.0,3.0-alpha-1)" );
    }

    public void testCliUsage()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.addCliOption( "-Dmaven.wagon.provider.http=coreit" );
        verifier.addCliOption( "-V" );

        verifier.setLogFileName( "log-cli.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/wagon-impl.properties" );
        assertEquals( "org.apache.maven.wagon.providers.coreit.CoreItHttpWagon", props.getProperty( "wagon.class" ) );
    }

    public void testSettingsUsage()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.addCliOption( "-V" );

        verifier.setLogFileName( "log-settings.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/wagon-impl.properties" );
        assertEquals( "org.apache.maven.wagon.providers.coreit.CoreItHttpWagon", props.getProperty( "wagon.class" ) );
    }

    public void testDefaultHttpWagon()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.addCliOption( "-V" );

        verifier.setLogFileName( "log-default-http.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/wagon-impl.properties" );
        assertEquals( "org.apache.maven.wagon.providers.http.LightweightHttpWagon", props.getProperty( "wagon.class" ) );
    }

    public void testDefaultHttpsWagon()
        throws IOException, VerificationException
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4254" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );

        verifier.addCliOption( "-V" );
        verifier.addCliOption( "-DwagonProtocol=https" );

        verifier.setLogFileName( "log-default-https.txt" );
        verifier.executeGoal( "validate" );

        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/wagon-impl.properties" );
        assertEquals( "org.apache.maven.wagon.providers.http.LightweightHttpsWagon", props.getProperty( "wagon.class" ) );
    }

}
