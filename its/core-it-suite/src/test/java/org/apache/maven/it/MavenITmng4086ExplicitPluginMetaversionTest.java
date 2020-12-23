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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-4086">MNG-4086</a>.
 *
 * @author Benjamin Bentmann
 */
public class MavenITmng4086ExplicitPluginMetaversionTest
    extends AbstractMavenIntegrationTestCase
{

    public MavenITmng4086ExplicitPluginMetaversionTest()
    {
        // metaversions no longer explicitly supported by 3.x (see MNG-4205)
        super( "[2.0.6,3.0-alpha-3)" );
    }

    /**
     * Verify that the plugin metaversion RELEASE can be explicitly used and especially is resolved
     * to a proper version before the plugin manager creates the key for the plugin realm.
     */
    public void testitRelease()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4086" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4086" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log-release.txt" );
        verifier.executeGoal( "org.apache.maven.its.mng4086:maven-it-plugin-a:RELEASE:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch-latest.txt" );
        verifier.assertFilePresent( "target/touch-release.txt" );
    }

    /**
     * Verify that the plugin metaversion LATEST can be explicitly used and especially is resolved
     * to a proper version before the plugin manager creates the key for the plugin realm.
     */
    public void testitLatest()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-4086" );

        Verifier verifier = newVerifier( testDir.getAbsolutePath() );
        verifier.setAutoclean( false );
        verifier.deleteDirectory( "target" );
        verifier.deleteArtifacts( "org.apache.maven.its.mng4086" );
        verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
        verifier.addCliOption( "--settings" );
        verifier.addCliOption( "settings.xml" );
        verifier.setLogFileName( "log-latest.txt" );
        verifier.executeGoal( "org.apache.maven.its.mng4086:maven-it-plugin-a:LATEST:touch" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        verifier.assertFileNotPresent( "target/touch-release.txt" );
        verifier.assertFilePresent( "target/touch-latest.txt" );
    }

}
