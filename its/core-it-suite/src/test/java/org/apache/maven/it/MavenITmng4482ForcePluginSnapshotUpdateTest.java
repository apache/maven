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
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4482">MNG-4482</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4482ForcePluginSnapshotUpdateTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4482ForcePluginSnapshotUpdateTest()
    {
        super( "[2.0.3,3.0-alpha-1),[3.0-alpha-6,)" );
    }

    /**
     * Verify that snapshot updates of plugins/extensions can be forced from the command line via "-U".
     *
     * @throws Exception in case of failure
     */
    @Test
    public void testit()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4482" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4482" );
        verifier.addCliOption( "-s" );
        verifier.addCliOption( "settings.xml" );

        /*
         * NOTE: The update of the extension plugin while still being referenced by a class loader from the first test
         * run make this test intermittently fail on *nix boxes, hence we enforce forking.
         */
        verifier.setForkJvm( true );

        Properties filterProps = verifier.newDefaultFilterProperties();

        filterProps.setProperty( "@repo@", "repo-1" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-1.txt" );
        verifier.deleteDirectory( "target" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();

        Properties props1 = verifier.loadProperties( "target/touch.properties" );
        assertEquals( "old", props1.getProperty( "one" ) );
        assertNull( props1.getProperty( "two" ) );

        filterProps.setProperty( "@repo@", "repo-2" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", filterProps );
        verifier.setLogFileName( "log-force-2.txt" );
        verifier.deleteDirectory( "target" );
        verifier.addCliOption( "-X" );
        verifier.addCliOption( "-U" );
        verifier.addCliArgument( "validate" );
        verifier.execute();
        verifier.verifyErrorFreeLog();


        Properties props2 = verifier.loadProperties( "target/touch.properties" );
        assertEquals( "new", props2.getProperty( "two" ) );
        assertNull( props2.getProperty( "one" ) );
    }

}
