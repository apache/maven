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
 * This is a test set for <a href="http://jira.codehaus.org/browse/MNG-768">MNG-768</a>.
 * 
 * @author John Casey
 * @version $Id$
 */
public class MavenITmng0768OfflineModeTest
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng0768OfflineModeTest()
    {
        super( ALL_MAVEN_VERSIONS );
    }

    /**
     * Test offline mode.
     */
    public void testitMNG768()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-0768" );

        {
            // phase 1: run build in online mode to fill local repo
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            verifier.deleteArtifacts( "org.apache.maven.its.it0069" );
            verifier.setLogFileName( "log1.txt" );
            verifier.filterFile( "settings-template.xml", "settings.xml", "UTF-8", verifier.newDefaultFilterProperties() );
            verifier.getCliOptions().add( "--settings" );
            verifier.getCliOptions().add( "settings.xml" );
            verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
            verifier.assertFilePresent( "target/compile.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }

        {
            // phase 2: run build in offline mode to check it still passes
            // NOTE: We don't add the settings here to ensure Maven has no chance to access the required remote repo
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            verifier.getCliOptions().add( "-o" );
            verifier.setLogFileName( "log2.txt" );
            verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
            verifier.assertFilePresent( "target/compile.txt" );
            verifier.verifyErrorFreeLog();
            verifier.resetStreams();
        }

        {
            // phase 3: delete test artifact and run build in offline mode to check it fails now
            // NOTE: We add the settings again to offer Maven the bad choice of using the remote repo
            Verifier verifier = new Verifier( testDir.getAbsolutePath() );
            verifier.setAutoclean( false );
            verifier.deleteDirectory( "target" );
            verifier.deleteArtifacts( "org.apache.maven.its.it0069" );
            verifier.getCliOptions().add( "-o" );
            verifier.getCliOptions().add( "--settings" );
            verifier.getCliOptions().add( "settings.xml" );
            verifier.setLogFileName( "log3.txt" );
            try
            {
                verifier.executeGoal( "org.apache.maven.its.plugins:maven-it-plugin-dependency-resolution:2.1-SNAPSHOT:compile" );
                verifier.verifyErrorFreeLog();
                fail( "Build did not fail to resolve missing dependency although Maven ought to work offline!" );
            }
            catch( VerificationException e )
            {
                // expected, should fail
            }
            verifier.resetStreams();
        }
    }

}
