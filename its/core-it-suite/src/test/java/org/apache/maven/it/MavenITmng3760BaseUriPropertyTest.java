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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-3760">MNG-3760</a>.
 * 
 * @author Brett Porter
 *
 */
public class MavenITmng3760BaseUriPropertyTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng3760BaseUriPropertyTest()
    {
        super( "(2.1.0-M1,3.0-alpha-1),(3.0-alpha-2,3.5.4)" ); // 2.1.0-M2+
    }

    public void testitMNG3760()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3760" ).getCanonicalFile();

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.setLogFileName( "log-basic.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/profile.properties" );
        // set via project
        assertEquals( testDir.toURI().toString(), props.getProperty( "project.properties.pomProperty" ) );
        // check that project prefix is required
        assertEquals( "${baseUri}", props.getProperty( "project.properties.baseUriProperty" ) );
    }

    public void testitMNG3760SystemPropertyOverride()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3760" ).getCanonicalFile();

        // check that setting baseUri doesn't override project value
        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-DbaseUri=myBaseUri" );
        verifier.setLogFileName( "log-sysprop.txt" );
        verifier.executeGoal( "validate" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        Properties props = verifier.loadProperties( "target/profile.properties" );
        // set via project
        assertEquals( testDir.toURI().toString(), props.getProperty( "project.properties.pomProperty" ) );
        // check that project prefix is required
        assertEquals( "myBaseUri", props.getProperty( "project.properties.baseUriProperty" ) );
    }

}
