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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3600">MNG-3600</a>.
 * 
 *
 */
public class MavenITmng3600DeploymentModeDefaultsTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3600DeploymentModeDefaultsTest()
    {
        super( "(2.1.0-M1,3.0-alpha-1),[3.0.1,)" );
    }

    public void testitMNG3600NoSettings()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3600" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        new File( testDir, "wagon.properties" ).delete();
        verifier.setLogFileName( "log-no-settings.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "wagon.properties" );
        Properties props = verifier.loadProperties( "wagon.properties" );
        assertNull( props.get( "directory.mode" ) );
        assertNull( props.get( "file.mode" ) );
    }

    public void testitMNG3600ServerDefaults()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3600" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        new File( testDir, "wagon.properties" ).delete();
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings-server-defaults.xml" );
        verifier.setLogFileName( "log-server-defaults.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "wagon.properties" );
        Properties props = verifier.loadProperties( "wagon.properties" );
        assertNull( props.get( "directory.mode" ) );
        assertNull( props.get( "file.mode" ) );
    }

    public void testitMNG3600ModesSet()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3600" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );

        new File( testDir, "wagon.properties" ).delete();
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings-modes-set.xml" );
        verifier.setLogFileName( "log-modes-set.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFilePresent( "wagon.properties" );
        Properties props = verifier.loadProperties( "wagon.properties" );
        assertEquals( "700", props.get( "directory.mode" ) );
        assertEquals( "600", props.get( "file.mode" ) );
    }

}
